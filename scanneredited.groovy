/* groovylint-disable DuplicateStringLiteral, LineLength, NestedBlockDepth */
/* LineLength,  CompileStatic, LineLength, UnnecessaryGString */
import java.time.*
import java.time.format.DateTimeFormatter
import groovy.transform.Field
import groovy.util.Eval



pipeline {
  agent {
    node { 
        label 'master' 
        }
    }

  environment {
        buildWorkspace = "${WORKSPACE}"
  }
  
  options {
    buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '30'))
    timestamps()
  }
  
  parameters {
    string(name: 'TARGET_URL', defaultValue: 'https://natixis-stage.oncorps.io', description: 'This parameter is to use the targeted website for zap scanning.')
    string(name: 'FOLDER_PATH', defaultValue: '/var/lib/jenkins', description: 'This parameter is to use the targeted website for zap scanning.')
    string(name: 'PROJECT_NAME', defaultValue: 'Natixis', description: 'This parameter is to use the targeted website for zap scanning.')	
    string(name: 'GIT_REPO', defaultValue: 'https://github.com/imukesh2508srijan/ui-natixis.git', description: 'This parameter is to use the targeted website for zap scanning.')
  }
  
  stages {
    stage("INFORMATION") {
        steps {
            // script {
            //    echo "Pipeline version: docker-ci.groovy-${SCRIPT_VERSION}"
            //     }
            // }
            
        }

    stage('Setup Github Repository') {
        steps {
            // script {
            //     // dir("${FOLDER_PATH}") {
            //     // sh "rm -rf ${PROJECT_NAME} && \
            //     // git clone ${GIT_REPO}"
            //     // echo "${GIT_REPO}"
            //     }
            }
        }
	}
}
