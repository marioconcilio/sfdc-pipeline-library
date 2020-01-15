pipeline {
    agent { 
        docker { image 'salestrip/sfdx-cli' } 
    }
    environment {
        REPO_URL = "https://github.com/marioconcilio/sfdc/commit/"
    }
    stages {
        stage('build') {
            steps {
                script {
                    slack.notifyStarted()
                    sh 'echo Hello world!'
                }
            }
        }
    }
    post {
        success {
            script {
                slack.notifySuccess()
            }
        }
        failure {
            script {
                slack.notifyFailure()
            }
        }
        unstable {
            script {
                slack.notifyUnstable()
            }
        }
        aborted {
            script {
                slack.notifyAborted()
            }
        }
    }
}