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

        stage('Deploy Locally') {
            when {
                allOf {
                    expression { params.DEPLOY }
                    expression { params.ENV == 'local' }
                }
            }
            steps {
                sh '''
                  echo "Ensuring network exists..."
                  docker network create app-network || true

                  echo "Stopping old containers..."
                  docker stop spring-web-app || true
                  docker rm spring-web-app || true
                  docker stop nginx || true
                  docker rm nginx || true

                  echo "Starting Spring Boot app..."
                  docker run -d \
                    --name spring-web-app \
                    --network app-network \
                    spring-boot-nginx-app:latest

                  echo "Starting Nginx..."
                  docker run -d \
                    --name nginx \
                    --network app-network \
                    -p 80:80 \
                    -v $(pwd)/nginx/default.conf:/etc/nginx/conf.d/default.conf:ro \
                    nginx:latest
                '''
            }
        }
    }
}

