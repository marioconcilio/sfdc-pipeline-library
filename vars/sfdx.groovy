import groovy.transform.Field

@Field String LOGIN_URL = 'https://login.salesforce.com'
@Field String TEST_URL = 'https://test.salesforce.com'

@Field String DEV_ALIAS = 'DevOrg'
@Field String UAT_ALIAS = 'UatOrg'
@Field String PRD_ALIAS = 'PrdOrg'

@Field String LOCAL_TESTS = 'RunLocalTests'
@Field String ALL_TESTS = 'RunAllTestsInOrg'

def authDev(String username, String password, String file) {
    sh 'echo "Authenticating to dev"'
    auth(username, password, file, TEST_URL, DEV_ALIAS)
}

def authUat(String username, String password, String file) {
    sh """
        echo "Authenticating to uat"
    """
    auth(username, password, file, TEST_URL, UAT_ALIAS)
}

def authPrd(String username, String password, String file) {
    sh """
        echo "Authenticating to production"
    """
    auth(username, password, file, LOGIN_URL, PRD_ALIAS)
}

def auth(String username, String password, String file, String url, String alias) {
    sh """
        sfdx force:auth:jwt:grant --clientid ${password} --jwtkeyfile ${file} --username ${username} --instanceurl ${url} -a ${alias}
    """
}

def deployDev() {
    sh """
        echo "Deploying changes to dev & running local tests"
    """
    deploy(LOCAL_TESTS, DEV_ALIAS)
}

def deployUat() {
    sh """
        echo "Deploying changes to uat & running all tests"
    """
    deploy(ALL_TESTS, UAT_ALIAS)
}

def deployPrd() {
    sh """
        echo "Deploying changes to production & running all tests"
    """
    deploy(ALL_TESTS, PRD_ALIAS)
}

def deploy(String test, String alias) {
    sh """
        if [ -d ${env.SF_DEPLOY_SRC} ]
        then
            sfdx force:source:deploy -u ${alias} -l ${test} -p ${env.SF_DEPLOY_SRC}
        else
            echo "Path ${env.SF_DEPLOY_SRC} does not exist"
        fi
    """
}

def applyDestructiveChangesDev() {
    sh """
        echo "Applying destructive changes to dev & running local tests"
    """
    applyDestructiveChanges(LOCAL_TESTS, DEV_ALIAS)
}

def applyDestructiveChangesUat() {
    sh """
        echo "Applying destructive changes to dev & running local tests"
    """
    applyDestructiveChanges(ALL_TESTS, UAT_ALIAS)
}

def applyDestructiveChanges(String test, String alias) {
    sh """
        if [ -d ${env.SF_DEL_SRC} ]
        then
            sfdx force:source:delete -u ${alias} -p ${env.SF_DEL_SRC} --noprompt
        else
            echo "Nothing to delete"
        fi
    """
}
