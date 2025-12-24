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
				string(
						name: 'ROLLBACK_VERSION',
						defaultValue: '',
						description: 'Build number to rollback to (leave empty for normal deploy)'
				      )

		}

	environment {
		DEPLOY_VERSION = "${params.ROLLBACK_VERSION ?: BUILD_NUMBER}"
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
				sh '''
					docker build \
					-t spring-boot-nginx-app:${DEPLOY_VERSION} \
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

						docker tag spring-boot-nginx-app:${DEPLOY_VERSION} \
						$DOCKER_USER/spring-boot-nginx-app:${DEPLOY_VERSION}

					docker tag spring-boot-nginx-app:${DEPLOY_VERSION} \
						$DOCKER_USER/spring-boot-nginx-app:latest


						docker push $DOCKER_USER/spring-boot-nginx-app:${DEPLOY_VERSION}
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


		stage('Deploy to EC2 (Blue/Green)') {
			when {
				allOf
					expression { params.DEPLOY }
				expression { params.ENV == 'remote' }
			}
		}
		steps {
			sshagent(['ec2-ssh-key']) {
				sh '''
					ssh -o StrictHostKeyChecking=no ubuntu@13.48.147.254 "
					set -e
                                        set -u

					echo '==> Detecting active color from Nginx config'

					if grep -q spring-web-app-blue /home/ubuntu/nginx/default.conf; then
					        	ACTIVE=blue
							INACTIVE=green

							elif grep -q spring-web-app-green /home/ubuntu/nginx/default.conf; then

							ACTIVE=green
							INACTIVE=blue
					else
						echo 'ERROR: Could not detect active color from Nginx config'
							exit 1
							fi

							echo "Active color: $ACTIVE"
							echo "Deploying color: $INACTIVE"

							echo '==> Ensure network exists'
							docker network create app-network || true

							echo '==> Pull image'
							docker pull camildockerhub/spring-boot-nginx-app:${DEPLOY_VERSION}

				echo '==> Stop inactive container if exists'
					docker stop spring-web-app-$INACTIVE || true
					docker rm spring-web-app-$INACTIVE || true

					echo '==> Start new container'
					docker run -d \
					--name spring-web-app-$INACTIVE \
					--network app-network \
					camildockerhub/spring-boot-nginx-app:${DEPLOY_VERSION}

				echo '==> Waiting for new container to become healthy'
					i=1
					while [ $i -le 20 ]; do
						if docker exec spring-web-app-$INACTIVE curl -f http://localhost:8080 >/dev/null 2>&1; then
							echo 'New version is healthy'
								break
								fi
								sleep 3
								i=$((i+1))
								done

								if [ $i -gt 20 ]; then
									echo 'New version did not become healthy'
										exit 1
										fi

										echo '==> Switching Nginx to new color'
										sed -i \"s/spring-web-app-$ACTIVE/spring-web-app-$INACTIVE/\" /home/ubuntu/nginx/default.conf

										docker exec nginx nginx -s reload

										echo '==> Blue/Green switch completed successfully'
										"
										'''
			}
		}
	}

	stage('Health Check') {
		when {
			expression { params.DEPLOY }
		}
		steps {
			sh '''
				echo "Waiting for application to become healthy..."

				if [ "${ENV}" = "local" ]; then
					URL="http://localhost"
						echo "Target environment: local"
				else
					URL="http://13.48.147.254"
						echo "Target environment: remote"
						fi

						i=1
						while [ $i -le 20 ]; do
							echo "Health check attempt $i..."
								if curl -f "$URL" >/dev/null 2>&1; then
									echo "Application is healthy!"
										exit 0
										fi
										i=$((i+1))
										sleep 3
										done

										echo "Application did not become healthy in time"
										exit 1
										'''
		}
	}
}
