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

        stage('Push Image to Registry') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-creds',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh '''
                      echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin

                      docker tag spring-boot-nginx-app:latest \
                        $DOCKER_USER/spring-boot-nginx-app:latest

                      docker push $DOCKER_USER/spring-boot-nginx-app:latest

                      docker logout
                    '''
                }
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
                    -v $(pwd)/nginx:/etc/nginx/conf.d:ro \
                    nginx:latest
                '''
            }
        }
        stage('Deploy to EC2') {
            when {
                allOf {
                    expression { params.DEPLOY }
                    expression { params.ENV == 'remote' }
                }
            }
    sshagent(['ec2-ssh-key']) {
    sh '''
      ssh -o StrictHostKeyChecking=no ubuntu@13.60.22.37 "
        docker network create app-network || true

        docker stop spring-web-app || true
        docker rm spring-web-app || true
        docker stop nginx || true
        docker rm nginx || true

        docker pull camildockerhub/spring-boot-nginx-app:latest

        nohup docker run -d \
          --name spring-web-app \
          --network app-network \
          camildockerhub/spring-boot-nginx-app:latest \
          >/dev/null 2>&1 &

        docker run -d \
          --name nginx \
          --network app-network \
          -p 80:80 \
          -v $(pwd)/nginx:/etc/nginx/conf.d:ro \
          nginx:latest
      "
    '''
               }
            }
        }
    }
}

