pipeline {
	agent any 
	options {
		buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '30'))
		timestamps()
	}
	parameters {
        string(name: 'CONTAINER_ID', defaultValue: 'docker run -u zap -p 2375:2375 -d owasp/zap2docker-weekly zap.sh -daemon -port 2375 -host 127.0.0.1 -config api.disablekey=true -config scanner.attackOnStart=true -config view.mode=attack -config connection.dnsTtlSuccessfulQueries=-1 -config api.addrs.addr.name=.* -config api.addrs.addr.regex=true', description: '')
        string(name: 'TARGET_URL', defaultValue: 'https://natixis-stage.oncorps.io', description: '')	    
	}
	stages {
		stage('Setup Repository') {
			steps {
				script {
						sh "${params.CONTAINER_ID}"
					}
				}
			}
		stage('Initiate ZAP and Open URL') {
			steps {
				script {
				    C_ID = sh (
                        script: 'docker ps -l -q',
                        returnStdout: true
                        ).trim()
				    echo "${C_ID}"
					sh "docker exec ${C_ID} zap-cli -p 2375 status -t 120 && docker exec ${C_ID} zap-cli -p 2375 open-url ${params.TARGET_URL}"
					}
				}
			}
        stage('Execute Spider Scan'){
        steps {
            sh "docker exec ${C_ID} zap-cli -p 2375 spider ${params.TARGET_URL}"
        }
    }
    stage('Execute Active Scan'){
        steps {
            sh "docker exec ${C_ID} zap-cli -p 2375 active-scan -r ${params.TARGET_URL}"
        }
    }
    stage('Log Execution Alerts'){
        steps {
            sh "docker exec ${C_ID} zap-cli -p 2375 alerts"
        }
    }
    stage('Generate HTML Report'){
        steps {
            sh "docker exec ${C_ID} zap-cli -p 2375 report -o ZAP_Report.html -f html"
            //sh "docker exec ${C_ID} zap-cli -p 2375 report -o ZAP_Report_JSON.json -f json"
            sh "sudo docker cp ${C_ID}:/zap/ZAP_Report.html http://44.234.88.110:8080/job/DevSecOps/job/MukeshSharma/job/ZAPAutomation/job/zap_scanner/29/execution/node/3/ws//ZAP_Report.html"
            //sh "sudo docker cp ${C_ID}:/zap/ZAP_Report_JSON.json /home/mukesh/zapTempDir/reports/ZAP_Report_JSON.json"
        }
    }
    stage('Stop and Remove Container'){
        steps {
            sh "docker logs ${C_ID}"
            sh "docker stop ${C_ID}"
            sh "docker rm ${C_ID}"
        }
    }    
	}
}