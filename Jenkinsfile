pipeline {
    agent any

    environment {
        IMAGE_NAME = "camildockerhub/spring-boot-nginx-app"
        IMAGE_TAG  = "${BUILD_NUMBER}"
    }

    parameters {
        booleanParam(
            name: 'DEPLOY',
            defaultValue: false,
            description: 'Deploy after successful build'
        )
        choice(
            name: 'ENV',
            choices: ['remote'],
            description: 'Deployment environment'
        )
        string(
            name: 'ROLLBACK_VERSION',
            defaultValue: '',
            description: 'Rollback to build number (optional)'
        )
    }

    stages {

        stage('Clean Workspace') {
            steps {
                deleteDir()
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
                sh '''
                  docker build \
                    -t spring-boot-nginx-app:${BUILD_NUMBER} \
                    -t spring-boot-nginx-app:latest \
                    .
                '''
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

                      docker tag spring-boot-nginx-app:${BUILD_NUMBER} \
                        $DOCKER_USER/spring-boot-nginx-app:${BUILD_NUMBER}

                      docker tag spring-boot-nginx-app:latest \
                        $DOCKER_USER/spring-boot-nginx-app:latest

                      docker push $DOCKER_USER/spring-boot-nginx-app:${BUILD_NUMBER}
                      docker push $DOCKER_USER/spring-boot-nginx-app:latest

                      docker logout
                    '''
                }
            }
        }

stage('Deploy to EC2') {
    when {
        expression { params.DEPLOY == true }
    }
    steps {
        sshagent(['ec2-ssh-key']) {
            sh '''
ssh -o StrictHostKeyChecking=no ubuntu@13.48.147.254 << 'EOF'
echo "Deploying image: camildockerhub/spring-boot-nginx-app:${BUILD_NUMBER}"

docker pull camildockerhub/spring-boot-nginx-app:${BUILD_NUMBER}

docker stop spring-app || true
docker rm spring-app || true

docker run -d \
  --name spring-app \
  -p 8080:8080 \
  camildockerhub/spring-boot-nginx-app:${BUILD_NUMBER}
EOF
'''
        }
    }
}


        stage('Health Check') {
            when {
                expression { params.DEPLOY == true }
            }
            steps {
                sh '''
                  URL=http://13.48.147.254

                  i=1
                  while [ "$i" -le 20 ]; do
                    if curl -f "$URL" >/dev/null 2>&1; then
                      echo "Application is healthy"
                      exit 0
                    fi
                    sleep 3
                    i=$((i+1))
                  done

                  echo "Health check failed"
                  exit 1
                '''
            }
        }
    }
}
