//Email notification method.
def notifyViaEmail(String buildStatus = 'SUCCESS', String MESSAGE = 'Report') {
    def decodedJobName = env.JOB_NAME.replaceAll("%2F", "/")

    emailext (
        subject: "${decodedJobName} - ${MESSAGE}",
        body: '''${SCRIPT, template="groovy-html.template"}''',
        to: "mukesh.sharma@srijan.net",
        replyTo: 'mukesh.sharma@srijan.net',
        attachLog: true
    )
}
pipeline {
	agent any 
	options {
		buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10'))
		timestamps()
	}
	parameters {
        string(name: 'TARGET_URL', defaultValue: 'https://natixis-stage.oncorps.io', description: '')	    
	}
    try {
	    stages {
            //ZAP docker setup with configurations.
	        stage('Setup Repository') {
                try {
		            steps {
				        script {
					        sh "docker run --rm \
                                -v /var/lib/jenkins/workspace/DevSecOps/MukeshSharma/ZAPAutomation/zap_scanner:/root/.cache/ \
                                -v `pwd`:/tmp/local \
                                -u zap \
                                -p 2375:2375 \
                                -d owasp/zap2docker-weekly \
                                zap.sh -daemon \
                                -port 2375 \
                                -host 127.0.0.1 \
                                -config api.disablekey=true \
                                -config scanner.attackOnStart=true \
                                -config view.mode=attack \
                                -config connection.dnsTtlSuccessfulQueries=-1 \
                                -config api.addrs.addr.name=.* \
                                -config api.addrs.addr.regex=true"
					    }
				    }
                }catch (error) {
                        echo "Unable setup the ZAP docker container."
                        throw error
                        currentBuild.result = "FAILURE"
                }
			}
            //Verify the ZAP is running and Open taget URL.
		    stage('Initiate ZAP and Open URL') {
                try {
                    steps {
				        script {
				            C_ID = sh (
                                script: 'docker ps -l -q',
                                returnStdout: true
                                ).trim()
				            echo "${C_ID}"
					        sh "docker exec ${C_ID} zap-cli -p 2375 status -t 120"
                            sh "docker exec ${C_ID} zap-cli -p 2375 open-url ${params.TARGET_URL}"
					    }
				    }
                }catch (error) {
                    echo "ZAP is not initiated properly OR Unable to open the Targeted URL."
                    throw error
                    currentBuild.result = "FAILURE"
                }
			}
            //Execute Spider scan to crawl the targeted website.
            stage('Execute Spider Scan'){
                try {
                    steps {
                        sh "docker exec ${C_ID} zap-cli -p 2375 spider ${params.TARGET_URL}"
                    }
                }catch (error) {
                    echo "Unable execute Spider scan."
                    throw error
                    currentBuild.result = "FAILURE"
                }
            }
            //Execute Active scan to attack the targeted website.
            stage('Execute Active Scan'){
                try {
                    steps {
                        sh "docker exec ${C_ID} zap-cli -p 2375 active-scan -r ${params.TARGET_URL}"
                    }
                }catch (error) {
                    echo "Unable to execute Active scan."
                    throw error
                    currentBuild.result = "FAILURE"
                }
            }
            //Log the Alerts to generate the report and debug purpose.
            stage('Log Execution Alerts'){
                try {
                    steps {
                        sh "docker exec ${C_ID} zap-cli -p 2375 alerts"
                    }
                }catch (error) {
                    echo "Unable to generate the Alerts."
                    throw error
                    currentBuild.result = "FAILURE"
                }
            }
            //Generate and save the execution report.
            stage('Generate the execution report'){
                try {
                    steps {
                        sh "docker exec ${C_ID} zap-cli -p 2375 report -o ZAP_Report.html -f html"
                        //sh "sudo docker cp ${C_ID}:/zap/ZAP_Report.html /var/lib/jenkins/workspace/DevSecOps/MukeshSharma/ZAPAutomation/zap_scanner/ZAP_Report_${BUILD_ID}.html"
                    }
                }catch (error) {
                    echo "Unable to generate the execution report."
                    throw error
                    currentBuild.result = "FAILURE"
                }
            }
            //Executed container logs and stop the container.
            stage('Stop and Remove Container'){
                try{
                    steps {
                        sh "docker logs ${C_ID}"
                        sh "docker stop ${C_ID}"
                    }
                }catch (error) {
                    echo "Unbale to stop the ZAP container."
                    throw error
                    currentBuild.result = "FAILURE"
                }
            }
    
        }
	}catch (error) {
        currentBuild.result = "FAILURE"
        throw error
    }
    finally {
            notifyViaEmail(currentBuild.result, 'Report')
    }
}