def SCRIPT_VERSION="1.0.0-rc5"

parameters {
    string(name: 'TARGET_URL', defaultValue: 'https://natixis-stage.oncorps.io', description: 'This parameter is to use the targeted website for zap scanning.')	    
}

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
node {
    try{
        stage("INFORMATION") {
            echo "Pipeline version: docker-ci.groovy-${SCRIPT_VERSION}"
        }
        //ZAP docker setup with configurations.
        stage('Setup Repository') {
            try {   
                    sh "docker run -v `pwd`::rw -t owasp/zap2docker-stable zap-full-scan.py \
                        -t ${params.TARGET_URL} -J testreportJson.json"
			}catch (error) {
                echo "Unable to run the ZAP container."
                throw error
                currentBuild.result = "FAILURE"
            }
		}
        //Verify the ZAP is running and Open the targeted URL for scanning.
		
        //Execute Spider scan to crawl the targeted URL.
        
        //Execute Active scan to attack the targeted URL.
        
        //Log the Alerts to generate the report and debug purpose.
        
        //Generate and save the execution report.
        
        //Execute container logs for debug purpose only.
        stage('Container logs'){
            try{
                containerID = sh (
                    script: 'docker ps -l -q',
                    returnStdout: true
                    ).trim()
				echo "${containerID}"
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