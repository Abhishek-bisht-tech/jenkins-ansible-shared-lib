def call(Map config = [:]) {
    def SLACK_CHANNEL       = config.SLACK_CHANNEL_NAME ?: 'jenkins-update'
    def ENVIRONMENT         = config.ENVIRONMENT ?: 'dev'
    def ACTION_MESSAGE      = config.ACTION_MESSAGE ?: "Deploying SonarQube to ${ENVIRONMENT}"
    def CODE_BASE_PATH      = config.CODE_BASE_PATH ?: 'Sonarqube'
    def KEEP_APPROVAL_STAGE = (config.KEEP_APPROVAL_STAGE ?: 'false').toBoolean()

    stage('Clone Repo') {
        dir(CODE_BASE_PATH) {
            git branch: 'main', url: 'https://github.com/Abhishek-bisht-tech/Sonarqube.git'
        }
    }

    if (KEEP_APPROVAL_STAGE) {
        stage('User Approval') {
            timeout(time: 5, unit: 'MINUTES') {
                input message: "Do you want to proceed with SonarQube deployment to ${ENVIRONMENT}?"
            }
        }
    }

    stage('Run Ansible Playbook') {
        dir(CODE_BASE_PATH) {
            sh """#!/bin/bash
                set -e

                echo "📂 Listing files in repo:"
                ls -la

                # Check if ansible-playbook is available
                if ! command -v ansible-playbook &>/dev/null; then
                    echo "🔧 Ansible not found, setting up virtual environment..."
                    sudo apt update
                    sudo apt install -y python3-venv python3-pip

                    python3 -m venv venv
                    source venv/bin/activate
                    pip install --upgrade pip
                    pip install ansible boto boto3
                    source venv/bin/activate
                else
                    echo "✅ Ansible is available globally."
                fi

                echo "🚀 Running Ansible Playbook..."
                ansible-playbook playbook.yml --extra-vars "env=${ENVIRONMENT}"
            """
        }
    }

    stage('Notify Slack') {
        slackSend(channel: SLACK_CHANNEL, message: "${ACTION_MESSAGE} ✅ completed on ${ENVIRONMENT}")
    }
}
