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
}