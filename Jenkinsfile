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

		stage('Deploy to EC2 Blue or Green') {
			when {
				allOf {
					expression { params.DEPLOY }
					expression { params.ENV == 'remote' }
				}
			}
			steps {
				sshagent(['ec2-ssh-key']) {
					sh '''
						ssh -o StrictHostKeyChecking=no ubuntu@13.48.147.254 "
						set -e

						echo "==> Detecting active color from Nginx config"

						ACTIVE=""
						INACTIVE=""

						if grep -q "spring-web-app-blue" /home/ubuntu/nginx/default.conf; then
							ACTIVE="blue"
								INACTIVE="green"
								elif grep -q "spring-web-app-green" /home/ubuntu/nginx/default.conf; then
								ACTIVE="green"
								INACTIVE="blue"
						else
							echo "ERROR: Cannot detect active color in Nginx config"
								echo "Current config:"
								cat /home/ubuntu/nginx/default.conf
								exit 1
								fi

								echo "Active color: $ACTIVE"
								echo "Deploying color: $INACTIVE"

								echo "==> Ensuring network exists"
								docker network inspect app-network >/dev/null 2>&1 || docker network create app-network

								echo "==> Pulling image version ${DEPLOY_VERSION}"
								docker pull camildockerhub/spring-boot-nginx-app:${DEPLOY_VERSION}

					echo "==> Replacing inactive container"
						docker stop spring-web-app-$INACTIVE || true
						docker rm spring-web-app-$INACTIVE || true

						docker run -d \
						--name spring-web-app-$INACTIVE \
						--network app-network \
						camildockerhub/spring-boot-nginx-app:${DEPLOY_VERSION}

					echo "==> Waiting for new container to become healthy"

						i=1
						while [ "${i:-0}" -le 20 ]; do
							if docker exec spring-web-app-$INACTIVE curl -f http://localhost:8080 >/dev/null 2>&1; then
								echo "New version is healthy"
									break
									fi
									sleep 3
									i=$((i+1))
									done

									if [ "${i:-0}" -gt 20 ]; then
										echo "ERROR: New version did not become healthy"
											exit 1
											fi


											echo "==> Switching Nginx to new color"
											sed -i "s/spring-web-app-$ACTIVE/spring-web-app-$INACTIVE/" /home/ubuntu/nginx/default.conf
											docker exec nginx nginx -s reload

											echo "==> Blue-Green switch complete"

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
				sh '
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
	}
}

