pipeline {
	agent any

		parameters {
			booleanParam(
					name: 'DEPLOY',
					defaultValue: false,
					description: 'Deploy after successful build'
				    )
				choice(
						name: 'ENV',
						choices: ['local', 'remote'],
						description: 'Deployment environment'
				      )
				string(
						name: 'ROLLBACK_VERSION',
						defaultValue: '',
						description: 'Rollback to build number (optional)'
				      )
		}

	environment {
		DEPLOY_VERSION = "${params.ROLLBACK_VERSION ?: BUILD_NUMBER}"
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

					docker push $DOCKER_USER/spring-boot-nginx-app:${BUILD_NUMBER}
					docker push $DOCKER_USER/spring-boot-nginx-app:latest

						docker logout
						'''
				}
			}
		}
		stage('Deploy to EC2') {
			when {
				allOf {
					expression { params.DEPLOY }
					expression { params.ENV == 'remote' }
				}
			}
			steps {
				sshagent(['ec2-ssh-key']) {
					sh '''
						ssh -o StrictHostKeyChecking=no ubuntu@13.48.147.254
						set -e

						echo "==> Ensure network exists"
						docker network inspect app-network >/dev/null 2>&1 || docker network create app-network

						echo "==> Pull app image"
						docker pull camildockerhub/spring-boot-nginx-app:${DEPLOY_VERSION}

					echo "==> Stop old app if exists"
						docker stop spring-web-app || true
						docker rm spring-web-app || true

						echo "==> Start app container"
						docker run -d \
						--name spring-web-app \
						--network app-network \
						camildockerhub/spring-boot-nginx-app:${DEPLOY_VERSION}

					echo "==> Ensure nginx is running"
						docker stop nginx || true
						docker rm nginx || true

						docker run -d \
						--name nginx \
						--network app-network \
						-p 80:80 \
						-v /home/ubuntu/nginx:/etc/nginx/conf.d \
						nginx:latest

						echo "==> Deployment complete"

						'''
				}
			}
		}

	}
}

stage('Health Check') {
	when {
		expression { params.DEPLOY }
	}
	steps {
		sh '''
			if [ "${ENV}" = "local" ]; then
				URL=http://localhost
			else
				URL=http://13.48.147.254
					fi

					i=1
					while [ $i -le 20 ]; do
						if curl -f "$URL" >/dev/null 2>&1; then
							exit 0
								fi
								sleep 3
								i=$((i+1))
								done

								exit 1
								'''
	}
}
