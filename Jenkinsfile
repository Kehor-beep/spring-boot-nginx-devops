pipeline {
    agent any

    parameters {
        booleanParam(
            name: 'DEPLOY',
            defaultValue: false,
            description: 'Deploy after successful build?'
        )
        choice(
            name: 'ENV',
            choices: ['local', 'remote'],
            description: 'Deployment environment'
        )
    }

    tools {
        jdk 'jdk21'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                sh './mvnw clean package'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t spring-boot-nginx-app:latest .'
            }
        }
    }
}

