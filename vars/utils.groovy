import groovy.transform.Field

@Field String SF_ADD_SRC = 'add-app'
@Field String SF_MOD_SRC = 'mod-app'

@Field String SF_PACKAGE_NAME = 'package.xml'
@Field String SF_DESTRUCTIVE_PACKAGE_NAME = 'destructiveChanges.xml'
@Field String SF_DESTRUCTIVE_PKG = 'destructive-pkg'

def buildWorkingDir(def gitPreviousSuccessfulCommit, String line) {
    if (false) {
        currentBuild.result = 'SUCCESS'
        return;
    }

    if (line == null || line.size() == 0) {
        currentBuild.result = 'ABORTED'
        error('Nothing to deploy')
    }

    String wildCard = line.substring(0, 1)    
    String filePath = line.substring(1).trim()
    String sfTempSrc = ''

    String dirPathOld = ''
    String subDirPathOld = ''
    String fileNameOld = ''
    String filePathOld = ''
    String sfTempSrcOld = ''
    
    switch (wildCard) {
        case 'R':
            String[] splitted = filePath.split('\\t')
            filePathOld = splitted[1]
            filePath = splitted[2]

            sfTempSrc = SF_ADD_SRC
            sfTempSrcOld = env.SF_DEL_SRC
            break

        case 'A':
        case 'C':
            sfTempSrc = SF_ADD_SRC
            break

        case 'M':
            sfTempSrc = SF_MOD_SRC
            break

        case 'D':
            sfTempSrc = env.SF_DEL_SRC
            break
    }

    filePath = filePath.replace("\$", "\\\$")

    String dirPath = filePath.substring(0, filePath.lastIndexOf("/"))
    String subDirPath = sfTempSrc + dirPath.substring(dirPath.indexOf("/"))
    String fileName = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.indexOf("."))

    if (fileName.length() == 0) {
        return;
    }

    if (filePathOld?.trim()) {
        filePathOld = filePathOld.replace("\$", "\\\$")
        dirPathOld = filePathOld.substring(0, filePathOld.lastIndexOf("/"))
        subDirPathOld = sfTempSrcOld + dirPathOld.substring(dirPathOld.indexOf("/"))
        fileNameOld = filePathOld.substring(filePathOld.lastIndexOf("/") + 1, filePathOld.indexOf("."))
    }

    sh """
        if [ ! -d $subDirPath ] 
        then
            mkdir -p $subDirPath
        fi
        case "$wildCard" in
            A|C)
                find "$dirPath" -maxdepth 1 -name "$fileName.*" -type f -exec cp -f "{}" $subDirPath/. \\;
            ;;
            M)
                if [ ! -d old-$subDirPath ] 
                then
                    mkdir -p old-$subDirPath
                fi
                find "$dirPath" -maxdepth 1 -name "$fileName.*" -type f -exec cp -f "{}" $subDirPath/. \\;
                git checkout $gitPreviousSuccessfulCommit "$filePath"
                find "$dirPath" -maxdepth 1 -name "$fileName.*" -type f -exec cp -f "{}" old-$subDirPath/. \\;
                git checkout HEAD "$filePath"
            ;;
            D)
                git checkout $gitPreviousSuccessfulCommit "$filePath"
                find "$dirPath" -maxdepth 1 -name "$fileName.*" -type f -exec cp -f "{}" $subDirPath/. \\;
                rm -f "$filePath"
            ;;
            R)
                if [ ! -d $subDirPathOld ] 
                then
                    mkdir -p $subDirPathOld
                fi
                find "$dirPath" -maxdepth 1 -name "$fileName.*" -type f -exec cp -f "{}" $subDirPath/. \\;
                git checkout $gitPreviousSuccessfulCommit "$filePathOld"
                find "$dirPathOld" -maxdepth 1 -name "$fileNameOld.*" -type f -exec cp -f "{}" $subDirPathOld/. \\;
                rm -f "$filePathOld"
            ;;
        esac
    """
}

def hasPackage() {
    def exitCode = sh(
        script: "grep -qr xml ${env.SF_DEPLOY_SRC}", 
        returnStatus: true
    )

    return exitCode == 0;
}

def hasDestructivePackage() {
    def exitCode = sh(
        script: "grep -qr xml ${env.SF_DEL_SRC}", 
        returnStatus: true
    )

    return exitCode == 0;
}

def createPackage() {
    sh """
        mkdir ${env.SF_DEPLOY_SRC}
        if grep -qr xml ${SF_ADD_SRC}
        then
            cp -arf ${SF_ADD_SRC}/* ${env.SF_DEPLOY_SRC}/.
        fi
        if grep -qr xml ${SF_MOD_SRC}
        then
            cp -arf ${SF_MOD_SRC}/* ${env.SF_DEPLOY_SRC}/.
        fi
        if grep -qr xml ${env.SF_DEPLOY_SRC}
        then
            find ${env.SF_DEPLOY_SRC} -type f -name ".eslintrc*" -delete
            tar -czf ${env.SF_DEPLOY_PKG} ${env.SF_DEPLOY_SRC}
        fi
        if grep -qr xml ${env.SF_DEL_SRC}
        then
            find ${env.SF_DEL_SRC} -type f \\( -name "*.recordType-meta.xml" -o -name "*.entitlementProcess-meta.xml" -o -name "*.md-meta.xml" -o -name "*.milestoneType-meta.xml" -o -name ".eslintrc*" \\) -delete
            tar -czf ${env.SF_DESTRUCTIVE_PKG} ${env.SF_DEL_SRC}
        fi
    """
}

def createPackage(def deployPkg, def rollbackPkg, def compressExt) {
    sh """
        rm -rf $env.SF_FORCE_SRC/*
        if grep -qr xml $SF_ADD_SRC
        then
            cp -arf $SF_ADD_SRC/* $env.SF_FORCE_SRC/.
            echo "Converting to MDAPI format..."
            sfdx force:source:convert -d $rollbackPkg -r $env.SF_FORCE_SRC
            mv $rollbackPkg/$SF_PACKAGE_NAME $rollbackPkg/$SF_DESTRUCTIVE_PACKAGE_NAME
            sed -i '/objects./d' $rollbackPkg/$SF_DESTRUCTIVE_PACKAGE_NAME
            sed 'N;5,\$!P;4,\$!N;D' $rollbackPkg/$SF_DESTRUCTIVE_PACKAGE_NAME > $rollbackPkg/$SF_PACKAGE_NAME
            tar -czf $rollbackPkg$compressExt $rollbackPkg
        fi
        rm -rf $env.SF_FORCE_SRC/*
        if grep -qr xml old-$SF_MOD_SRC
        then
            cp -arf old-$SF_MOD_SRC/* $env.SF_FORCE_SRC/.
        fi
        if grep -qr xml ${env.SF_DEL_SRC}
        then
            cp -arf ${env.SF_DEL_SRC}/* $env.SF_FORCE_SRC/.
        fi
        if grep -qr xml $env.SF_FORCE_SRC
        then
            if [ -f $rollbackPkg/$SF_DESTRUCTIVE_PACKAGE_NAME ]
            then
                mv $rollbackPkg/$SF_DESTRUCTIVE_PACKAGE_NAME $SF_DESTRUCTIVE_PACKAGE_NAME
            fi
            rm -rf $rollbackPkg
            rm -f $rollbackPkg$compressExt
            echo "Converting to MDAPI format..."
            sfdx force:source:convert -d $rollbackPkg -r $env.SF_FORCE_SRC
            sed -i '/objects./d' $rollbackPkg/$SF_PACKAGE_NAME
            if [ -f $SF_DESTRUCTIVE_PACKAGE_NAME ]
            then
                mv $SF_DESTRUCTIVE_PACKAGE_NAME $rollbackPkg/$SF_DESTRUCTIVE_PACKAGE_NAME
            fi
            tar -czf $rollbackPkg$compressExt $rollbackPkg
        fi
    """
    sh """
        rm -rf $env.SF_FORCE_SRC/*
        if grep -qr xml $SF_ADD_SRC
        then
            cp -arf $SF_ADD_SRC/* $env.SF_FORCE_SRC/.
        fi
        if grep -qr xml $SF_MOD_SRC
        then
            cp -arf $SF_MOD_SRC/* $env.SF_FORCE_SRC/.
        fi
        if grep -qr xml $env.SF_FORCE_SRC
        then
            echo "Converting to MDAPI format..."
            sfdx force:source:convert -d $deployPkg -r $env.SF_FORCE_SRC
            sed -i '/objects./d' $deployPkg/$SF_PACKAGE_NAME
            tar -czf $deployPkg$compressExt $deployPkg
        fi
    """
    sh """
        rm -rf $env.SF_FORCE_SRC/*
        if grep -qr xml ${env.SF_DEL_SRC} 
        then
            cp -arf ${env.SF_DEL_SRC}/* $env.SF_FORCE_SRC/.
            echo "Converting to MDAPI format..."
            sfdx force:source:convert -d $SF_DESTRUCTIVE_PKG -r $env.SF_FORCE_SRC
            if [ -d $deployPkg ] 
            then
                cp $SF_DESTRUCTIVE_PKG/$SF_PACKAGE_NAME $deployPkg/$SF_DESTRUCTIVE_PACKAGE_NAME
                sed -i '/objects./d' $deployPkg/$SF_DESTRUCTIVE_PACKAGE_NAME
                rm -rf $SF_DESTRUCTIVE_PKG
                rm -f $deployPkg$compressExt
            else
                cp $SF_DESTRUCTIVE_PKG/$SF_PACKAGE_NAME $SF_DESTRUCTIVE_PACKAGE_NAME
                sed -i '/objects./d' $SF_DESTRUCTIVE_PACKAGE_NAME
                rm -rf $SF_DESTRUCTIVE_PKG/*
                mv $SF_DESTRUCTIVE_PKG $deployPkg
                mv $SF_DESTRUCTIVE_PACKAGE_NAME $deployPkg/.
                sed 'N;5,\$!P;4,\$!N;D' $deployPkg/$SF_DESTRUCTIVE_PACKAGE_NAME > $deployPkg/$SF_PACKAGE_NAME
            fi
            tar -czf $deployPkg$compressExt $deployPkg
        fi   
    """
}
