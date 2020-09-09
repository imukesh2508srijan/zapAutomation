pipeline {
  agent any
  stages {
    stage('Docker Deployment') {
      steps {
        bat 'CONTAINER_ID=$(docker run -u zap -p 2375:2375 -d owasp/zap2docker-weekly zap.sh -daemon -port 2375 -host 127.0.0.1 -config api.disablekey=true -config scanner.attackOnStart=true -config view.mode=attack -config connection.dnsTtlSuccessfulQueries=-1 -config api.addrs.addr.name=.* -config api.addrs.addr.regex=true)'
      }
    }
    stage('Open URL'){
        steps {
            bat 'TARGET_URL="https://natixis-stage.oncorps.io"'
            bat 'docker exec $CONTAINER_ID zap-cli -p 2375 status -t 120 && docker exec $CONTAINER_ID zap-cli -p 2375 open-url $TARGET_URL'
        }
    }
    stage('Execute Spider Scan'){
        steps {
            bat 'docker exec $CONTAINER_ID zap-cli -p 2375 spider $TARGET_URL'
        }
    }
    stage('Execute Active Scan'){
        steps {
            bat 'docker exec $CONTAINER_ID zap-cli -p 2375 active-scan -r $TARGET_URL'
        }
    }
    stage('Log Alerts'){
        steps {
            bat 'docker exec $CONTAINER_ID zap-cli -p 2375 alerts'
        }
    }
    stage('Generate HTML Report'){
        steps {
            bat 'docker exec $CONTAINER_ID zap-cli -p 2375 report -o ZAP_Report.html -f html'
            bat 'sudo docker cp $CONTAINER_ID:/zap/ZAP_Report.html /home/mukesh/zapTempDir/reports/ZAP_Report.html'
        }
    }
    stage('Stop and Remove Container'){
        steps {
            bat 'docker logs $CONTAINER_ID'
            bat 'docker stop $CONTAINER_ID'
            bat 'docker rm $CONTAINER_ID'
        }
    }       
    }
}