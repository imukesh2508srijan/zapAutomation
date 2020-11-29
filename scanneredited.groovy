pipeline {
    
    parameters {
    string(name: 'TARGET_URL', defaultValue: 'https://natixis-stage.oncorps.io', description: 'This parameter is to use the targeted website for zap scanning.')
    string(name: 'FOLDER_PATH', defaultValue: '/var/lib/jenkins', description: 'This parameter is to use the targeted website for zap scanning.')
    string(name: 'PROJECT_NAME', defaultValue: 'Natixis', description: 'This parameter is to use the targeted website for zap scanning.')	
    string(name: 'GIT_REPO', defaultValue: 'https://github.com/imukesh2508srijan/ui-natixis.git', description: 'This parameter is to use the targeted website for zap scanning.')	
}

environment {
    buildWorkspace = "${WORKSPACE}"
}

options {
    buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '30'))
    timestamps()
}
    agent any
    stages {

        stage('INFORMATION') {
            steps {
                echo "Pipeline version: docker-ci.groovy-${SCRIPT_VERSION}"
            }
        }

        stage('PreBuild') {
            steps {
                try {
                    sh "rm -rf $WORKSPACE/*"
                }   catch (error) {
                    // Inform of the error
                    echo "Unable to clean the workspace."
                    throw error
                    currentBuild.result = "FAILURE"
                }
            }
        }

        stage('Setup Github Repository') {
            steps {
                script {
                    dir("${FOLDER_PATH}") {
                    sh "rm -rf ${PROJECT_NAME} && \
                    git clone ${GIT_REPO}"
                    echo "${GIT_REPO}"
                    }
                }
            }
        }

        stage('Setup Repository') {
            steps {
                echo "Scanning Target: ${TARGET_URL}"
                try {   
                    //The port is required in order to execute zap and the same port can't be used if it is still in use.
                    //In order to achive the parallel exeuction, generating random port number between 2000 to 5000.
                    port = sh (
                        script: 'shuf -i 2000-5000 -n 1',
                        returnStdout: true
                        ).trim()
				    echo "${port}"
                    sh "docker run --rm \
                        -v $HOME/Library/Caches:/root/.cache/ \
                        -u zap \
                        -p ${port}:${port} \
                        -d owasp/zap2docker-weekly \
                            zap.sh -daemon \
                            -port ${port} \
                            -host 127.0.0.1 \
                            -config api.disablekey=true \
                            -config scanner.attackOnStart=true \
                            -config view.mode=attack \
                            -config connection.dnsTtlSuccessfulQueries=-1 \
                            -config api.addrs.addr.name=.* \
                            -config api.addrs.addr.regex=true"
			}   catch (error) {
                echo "Unable to run the ZAP container."
                throw error
                currentBuild.result = "FAILURE"
                }
		    }
        }

        stage('Initiate ZAP and Open Targeted URL') {
            try {
                //can be taken from the docker run --rm -d : Line51
				containerID = sh (
                    script: 'docker ps -l -q',
                    returnStdout: true
                    ).trim()
				echo "${containerID}"
                sh "docker cp ${FOLDER_PATH}/zap/zap.context ${containerID}:/"
				sh "docker exec ${containerID} zap-cli -p ${port} status -t 120"
                sh "docker exec ${containerID} zap-cli context import ${containerID}:/zap/zap.context"
                sh "docker exec ${containerID} zap-cli -p ${port} open-url ${TARGET_URL}"
			}catch (error) {
                echo "ZAP is not initiated properly OR Unable to open the Targeted URL."
                throw error
                currentBuild.result = "FAILURE"
            }
		}

        }
    }