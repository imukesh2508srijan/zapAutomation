def SCRIPT_VERSION="1.0.0-rc5"

import java.time.*
import java.time.format.DateTimeFormatter
import groovy.transform.Field
import groovy.util.Eval
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.text.SimpleDateFormat
def dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm")
currentDate = dateFormat.format(new Date())

pipeline {
    agent {
		node {
			label 'master'
		}
	}

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

//Email notification method.
// def notifyViaEmail(String buildStatus = 'SUCCESS', String MESSAGE = 'Report') {
//     def decodedJobName = env.JOB_NAME.replaceAll("%2F", "/")

//     emailext (
//         subject: "${decodedJobName} - ${MESSAGE}",
//         body: '''${SCRIPT, template="groovy-html.template"}''',
//         to: "mukesh.sharma@srijan.net",
//         replyTo: 'mukesh.sharma@srijan.net',
//         attachLog: true
//     )
// }
node {
    try{
        stage("INFORMATION") {
            echo "Pipeline version: docker-ci.groovy-${SCRIPT_VERSION}"
        }
        //This stage is to cleanup the workspace.
        stage("PreBuild") {
            try {
                sh "rm -rf $WORKSPACE/*"
            }catch (error) {
                // Inform of the error
                echo "Unable to clean the workspace."
                throw error
                currentBuild.result = "FAILURE"
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

        //ZAP docker setup with configurations.
        stage('Setup Repository') {
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
			}catch (error) {
                echo "Unable to run the ZAP container."
                throw error
                currentBuild.result = "FAILURE"
            }
		}
        //Verify the ZAP is running and Open the targeted URL for scanning.
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
        //Execute Spider scan to crawl the targeted URL.
        stage('Execute Spider Scan'){
            try {
                sh "docker exec ${containerID} zap-cli -p ${port} spider -c zap -u manager ${TARGET_URL}"
            }catch (error) {
                echo "Unable execute Spider scan."
                throw error
                currentBuild.result = "FAILURE"
            }
        }
        //Execute Active scan to attack the targeted URL.
        stage('Execute Active Scan'){
            try {
                sh "docker exec ${containerID} zap-cli -p ${port} active-scan -r -c zap -u manager ${TARGET_URL}"
            }catch (error) {
                echo "Unable to execute Active scan."
                throw error
                currentBuild.result = "FAILURE"
            }
        }
        //Log the Alerts to generate the report and debug purpose.
        stage('Log Execution Alerts'){
            try {
                sh "docker exec ${containerID} zap-cli -p ${port} alerts"
            }catch (error) {
                echo "Unable to generate the Alerts."
                throw error
                currentBuild.result = "FAILURE"
            }
        }
        //Generate and save the execution report.
        stage('Generate the execution report'){
            try {
                sh "docker exec ${containerID} zap-cli -p ${port} report -o ZAP_Report_${PROJECT_NAME}-${currentDate}.html -f html"
                sh "docker cp ${containerID}:/zap/ZAP_Report_${PROJECT_NAME}-${currentDate}.html ${WORKSPACE}/ZAP_Report_${PROJECT_NAME}-${currentDate}.html"
            }catch (error) {
                echo "Unable to generate the execution report."
                throw error
                currentBuild.result = "FAILURE"
            }
        }
        //Execute container logs for debug purpose only.
        stage('Container logs'){
            try{
                sh "docker logs ${containerID}"
            }catch (error) {
                echo "Unable to stop the ZAP container."
                throw error
                currentBuild.result = "FAILURE"
            }
        }
    }catch(error) {
        currentBuild.result = "FAILURE"
        throw error
    }
    finally {
        sh "docker stop ${containerID}"
        notifyViaEmail(currentBuild.result, 'Report')
    }
}
}