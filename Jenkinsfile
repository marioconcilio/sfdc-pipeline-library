pipeline {
    agent { 
        docker { image 'salestrip/sfdx-cli' } 
    }
    stages {
        stage('build') {
            steps {
                sh 'sfdx --version'
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