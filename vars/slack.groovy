import groovy.transform.Field

@Field String REPO_URL = "https://github.com/marioconcilio/sfdc/commit/"
@Field String BLUE_OCEAN_URL = 'http://ec2-3-19-239-186.us-east-2.compute.amazonaws.com:8080/blue/organizations/jenkins/sfdc/detail/'

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
    def color
    switch(status) {
        case 'Pending':
            color = '#D4DADF'
            break
        case 'Successful':
            color = '#2EB67D'
            break
        case 'Failed':
            color = '#E01E5A'
            break
        case 'Unstable':
            color = '#ECB22E'
            break
        case 'Aborted':
            color = '#4A154B'
            break
        default:
            color = '#E01E5A'
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
    def repoUrl = "${REPO_URL}${gitHash}"

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
            "text": "See commit on GitHub",
            "url": "${repoUrl}"
        }
    ]
}]"""

    def channel = env.SLACK_MSG_CHANNEL? env.SLACK_MSG_CHANNEL : '#salesforce-ci'
    slackSend channel: channel, attachments: attachments
}

return this