pipeline {
    agent any
    stages {

        stage('Stage 1') {
            steps {
                echo 'Stage 1'
                script {
                    echo 'Stage 1'
                    }
                }
            }

        stage('Stage 2') {
            steps {
                echo 'Stage 2'
                script {
                    echo 'Stage 2'
                    }
                }
            }

        stage('Stage 3') {
            steps {
                echo 'Stage 3'
                script {
                    echo 'Stage 3'
                    }
                }
            }

        stage('Stage 4') {
            steps {
                echo 'Stage 4'
                script {
                    echo 'Stage 4'
                    }
                }
            }

        stage('Stage 5') {
            steps {
                echo 'Stage 5'
                script {
                    echo 'Stage 5'
                    }
                }
            }

        }
    }