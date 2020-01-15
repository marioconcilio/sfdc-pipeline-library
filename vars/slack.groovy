import groovy.transform.Field

@Field String BLUE_OCEAN_URL = 'http://ec2-18-221-129-121.us-east-2.compute.amazonaws.com:8080/blue/organizations/jenkins/sfdc/detail/'

def notifyStarted() {
    notifySlack('Pending', 'Build started')
}

def notifySuccess() {
    notifySlack('Successful', 'Build successful')
}

def notifyFailure() {
    notifySlack('Failed', 'Build failed')
}

def notifyUnstable() {
    notifySlack('Unstable', 'Build unstable')
}

def notifyAborted() {
    notifySlack('Aborted', 'Build aborted')
}

def notifySlack(String status, String message) {
    if (env.BRANCH_NAME != 'develop' && env.BRANCH_NAME != 'uat' && env.BRANCH_NAME != 'master') return

    def color
    switch(status) {
        case 'Pending':
            color = '#D4DADF'
            break
        case 'Successful':
            color = '#BDFFC3'
            break
        case 'Failed':
            color = '#FF9FA1'
            break
        case 'Unstable':
            color = '#FFFE89'
            break
        case 'Aborted':
            color = '#D4DADF'
            break
        default:
            color = '#FF9FA1'
    }

    def gitAuthor = sh(
        script: 'git show -s --pretty=%an',
        returnStdout: true
    ).trim()

    def gitCommit = sh(
        script: 'git log --oneline --format="%B" -n 1 HEAD | head -n 1',
        returnStdout: true
    ).trim().replaceAll('"', '\'')

    def gitHash = sh(
        script: 'git log -1 --pretty=%h',
        returnStdout: true
    ).trim()

    def ts = sh(
        script: '(date +%s)', 
        returnStdout: true
    ).trim()

    def msg = "${gitAuthor}: Build #${env.BUILD_NUMBER} ${status} on ${env.BRANCH_NAME}"
    def jenkinsUrl = "${BLUE_OCEAN_URL}${env.BRANCH_NAME}/${env.BUILD_NUMBER}"
    def bitbucketUrl = "${env.REPO_URL}${gitHash}"

    def attachments = """[{
    "fallback": "${msg}",
    "color": "${color}",
    "pretext": "@here",
    "author_name": "${gitAuthor}",
    "title": "${message}",
    "title_link": "${env.BUILD_URL}",
    "footer": "Jenkins",
    "ts": ${ts},
    "fields": [
        {
            "title": "Status",
            "value": "${status}",
            "short": true
        },
        {
            "title": "Build Number",
            "value": "${env.BUILD_NUMBER}",
            "short": true
        },
        {
            "title": "Git Branch",
            "value": "${env.BRANCH_NAME}",
            "short": true
        },
        {
            "title": "Git Hash",
            "value": "${gitHash}",
            "short": true
        },
        {
            "title": "Git Commit",
            "value": "${gitCommit}",
            "short": false
        }
    ],
    "actions": [
        {
            "type": "button",
            "text": "See build on Jenkins",
            "url": "${jenkinsUrl}"
        },
        {
            "type": "button",
            "text": "See commit on Bitbucket",
            "url": "${bitbucketUrl}"
        }
    ]
}]"""

    def channel = env.SLACK_MSG_CHANNEL? env.SLACK_MSG_CHANNEL : '#salesforce_ci'
    slackSend channel: channel, attachments: attachments
}

return this