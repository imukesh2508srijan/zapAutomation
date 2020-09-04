/* groovylint-disable DuplicateStringLiteral, LineLength, NestedBlockDepth */
/* LineLength,  CompileStatic, LineLength, UnnecessaryGString */

pipeline {
	agent {
		node {
			label 'master'
		}
	}
	options {
		buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '30'))
		timestamps()
	}
	parameters {
		string(name: 'CLONE_URL', defaultValue: '', description: '')
		string(name: 'PROJECT_NAME', defaultValue: '', description: '')
		string(name: 'BRANCH_NAME', defaultValue: '', description: '')
		string(name: 'FOLDER_PATH', defaultValue: '', description: '')
		string(name: 'DOCKER_BINARY_PATH', defaultValue: '', description: '')
		string(name: 'SNYK_PSW', defaultValue: '', description: '')
		string(name: 'TOPIC', defaultValue: '', description: '')
		string(name: 'TAG', defaultValue: '', description: '')
	}
    environment {
        SNYK = credentials('snyk_laurent-douchy')
    }
	stages {
		stage('Setup Repository') {
			steps {
				script {
					dir("${params.FOLDER_PATH}") {
						sh "rm -rf ${params.PROJECT_NAME}"
						sh "git clone ${params.CLONE_URL}"
					}
					if (params.TAG == '') {
						stage(params.PROJECT_NAME + ' - Branch : master') {
							dir("${params.FOLDER_PATH}${params.PROJECT_NAME}") {
								sh "git checkout ${params.BRANCH_NAME}"
								sh "git pull origin ${params.BRANCH_NAME}"
							}
							script {
								String topic = params.TOPIC
								if (topic.equalsIgnoreCase('JavaScript') || topic.equalsIgnoreCase('TypeScript')) {
									dir("${params.FOLDER_PATH}${params.PROJECT_NAME}") {
										sh "${params.DOCKER_BINARY_PATH} run -e \"SNYK_PSW=${params.SNYK_PSW}\" -e \"MONITOR=false\" -e \"TARGET_FILE=package-lock.json\" -v \"${params.FOLDER_PATH}${params.PROJECT_NAME}:/project\"  snyk/snyk-cli:npm test --severity-threshold=high"
									}
								} else if (topic.equalsIgnoreCase('php')) {
									dir("${params.FOLDER_PATH}${params.PROJECT_NAME}") {
										sh "${params.DOCKER_BINARY_PATH} run -e \"SNYK_PSW=${params.SNYK_PSW}\" -e \"MONITOR=false\" -e \"TARGET_FILE=composer.lock\" -v \"${params.FOLDER_PATH}${params.PROJECT_NAME}:/project\"  snyk/snyk-cli:composer test --severity-threshold=high"
									}
								} else if (topic.equalsIgnoreCase('Python')) {
									dir("${params.FOLDER_PATH}${params.PROJECT_NAME}") {
										sh "${params.DOCKER_BINARY_PATH} run -e \"SNYK_PSW=${params.SNYK_PSW}\" -e \"MONITOR=false\" -e \"TARGET_FILE=requirements.txt\" -v \"${params.FOLDER_PATH}${params.PROJECT_NAME}:/project\"  snyk/snyk-cli:python-3 test --severity-threshold=high"
									}
								}
							}
						}
					} else {
						stage(params.PROJECT_NAME + ' - Tag : ' + params.TAG) {
							dir("${params.FOLDER_PATH}${params.PROJECT_NAME}") {
								sh "git checkout ${params.TAG} -b latest"
							}
							script {
								String topic = params.TOPIC
								if (topic.equalsIgnoreCase('JavaScript') || topic.equalsIgnoreCase('TypeScript')) {
									dir("${params.FOLDER_PATH}${params.PROJECT_NAME}") {
										sh "${params.DOCKER_BINARY_PATH} run -e \"SNYK_PSW=${params.SNYK_PSW}\" -e \"MONITOR=false\" -e \"TARGET_FILE=package-lock.json\" -v \"${params.FOLDER_PATH}${params.PROJECT_NAME}:/project\"  snyk/snyk-cli:npm test --severity-threshold=high"
									}
								} else if (topic.equalsIgnoreCase('php')) {
									dir("${params.FOLDER_PATH}${params.PROJECT_NAME}") {
										sh "${params.DOCKER_BINARY_PATH} run -e \"SNYK_PSW=${params.SNYK_PSW}\" -e \"MONITOR=false\" -e \"TARGET_FILE=composer.lock\" -v \"${params.FOLDER_PATH}${params.PROJECT_NAME}:/project\"  snyk/snyk-cli:composer test --severity-threshold=high"
									}
								} else if (topic.equalsIgnoreCase('Python') || topic.equalsIgnoreCase('R')) {
									dir("${params.FOLDER_PATH}${params.PROJECT_NAME}") {
										sh "${params.DOCKER_BINARY_PATH} run -e \"SNYK_PSW=${params.SNYK_PSW}\" -e \"MONITOR=false\" -e \"TARGET_FILE=requirements.txt\" -v \"${params.FOLDER_PATH}${params.PROJECT_NAME}:/project\"  snyk/snyk-cli:python-3 test --severity-threshold=high"
									}
								}
							}
						}
						stage(params.PROJECT_NAME + ' - Branch : dev') {
							dir("${params.FOLDER_PATH}") {
								sh "rm -rf ${params.PROJECT_NAME}"
								sh "git clone ${params.CLONE_URL}"
							}

							dir("${params.FOLDER_PATH}${params.PROJECT_NAME}") {
								sh 'git checkout dev'
								sh 'git pull origin dev'
							}
							script {
								String topic = params.TOPIC
								if (topic.equalsIgnoreCase('JavaScript') || topic.equalsIgnoreCase('TypeScript')) {
									dir("${params.FOLDER_PATH}${params.PROJECT_NAME}") {
										sh "${params.DOCKER_BINARY_PATH} run -e \"SNYK_PSW=${params.SNYK_PSW}\" -e \"MONITOR=false\" -e \"TARGET_FILE=package-lock.json\" -v \"${params.FOLDER_PATH}${params.PROJECT_NAME}:/project\"  snyk/snyk-cli:npm test --severity-threshold=high"
									}
								} else if (topic.equalsIgnoreCase('php')) {
									dir("${params.FOLDER_PATH}${params.PROJECT_NAME}") {
										sh "${params.DOCKER_BINARY_PATH} run -e \"SNYK_PSW=${params.SNYK_PSW}\" -e \"MONITOR=false\" -e \"TARGET_FILE=composer.lock\" -v \"${params.FOLDER_PATH}${params.PROJECT_NAME}:/project\"  snyk/snyk-cli:composer test --severity-threshold=high"
									}
								} else if (topic.equalsIgnoreCase('Python') || topic.equalsIgnoreCase('R')) {
									dir("${params.FOLDER_PATH}${params.PROJECT_NAME}") {
										sh "${params.DOCKER_BINARY_PATH} run -e \"SNYK_PSW=${params.SNYK_PSW}\" -e \"MONITOR=false\" -e \"TARGET_FILE=requirements.txt\" -v \"${params.FOLDER_PATH}${params.PROJECT_NAME}:/project\"  snyk/snyk-cli:python-3 test --severity-threshold=high"
									}
								}
							}
						}
					}
				}
			}
		}
	}
}