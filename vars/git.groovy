import groovy.transform.Field

def getBuilds() {
    return sh(
        script: """
            git log --pretty=oneline --abbrev-commit -1 | cut -d\\  -f2- | awk -F\\@ '{print \$2}'
        """,
        returnStdout: true
    ).trim()
}

def checkout() {
    def gitCheckoutResult = checkout([
        $class: "GitSCM",
        branches: [[name: "/$env.GIT_BRANCH"]],
        doGenerateSubmoduleConfigurations: scm.doGenerateSubmoduleConfigurations,
        extensions: scm.extensions,
        userRemoteConfigs: scm.userRemoteConfigs
    ])

    return (gitCheckoutResult.GIT_PREVIOUS_SUCCESSFUL_COMMIT?.trim()) ? gitCheckoutResult.GIT_PREVIOUS_SUCCESSFUL_COMMIT : "\$(git rev-list --max-parents=0 HEAD)"
}

def getManifest(def gitPreviousSuccessfulCommit) {
    def gitCheckoutResult = checkout([
        $class: "GitSCM",
        branches: [[name: "/$env.GIT_BRANCH"]],
        doGenerateSubmoduleConfigurations: scm.doGenerateSubmoduleConfigurations,
        extensions: scm.extensions,
        userRemoteConfigs: scm.userRemoteConfigs
    ])
    
    return sh(
        script: """
            git config core.quotepath off
            git diff ${gitPreviousSuccessfulCommit}..HEAD --raw | egrep -v '^\\.' | egrep ${env.SF_FORCE_SRC} | cut -d\\  -f5-
        """,
        returnStdout: true
    ).trim()
}