pipeline {
  agent any
  environment {
    // Specified for Jenkins server
    JAVA_HOME = "C:/Program Files/Android/Android Studio/jre"
    ANDROID_SDK_ROOT = "C:/Android/Sdk"
    GRADLE_USER_HOME = "C:/gradle-cache"
    KEYSTORE_LOCATION = credentials('apcupsd-monitor-keystore-location')
  }
  options {
    // Stop the build early in case of compile or test failures
    skipStagesAfterUnstable()
  }
  stages {
    stage('Google Services Json') {
      steps {
        bat 'copy C:\\Projects\\APCUPSDMonitor\\google-services.json %WORKSPACE%\\app\\'
      }
    }
    stage('Compile') {
      steps {
        // Compile the app and its dependencies
        bat './gradlew compileDebugSources'
      }
    }
    stage('Unit test') {
      steps {
        // Compile and run the unit tests for the app and its dependencies
        bat './gradlew testDebugUnitTest testDebugUnitTest'

        // Analyse the test results and update the build result as appropriate
        junit '**/TEST-*.xml'
      }
    }
    stage('Build APK') {
      steps {
        // Finish building and packaging the APK
        bat './gradlew assembleDebug'

        // Archive the APKs so that they can be downloaded from Jenkins
        // archiveArtifacts '**/*.apk'
      }
    }
    stage('Static analysis') {
      steps {
        // Run Lint and analyse the results
        bat './gradlew lintDebug'
        androidLint pattern: '**/lint-results-*.xml'
      }
    }
    stage('Deploy') {
      when {
        // Only execute this stage when building from the `master` branch
        branch 'master'
      }
      steps {

        // Execute bundle release build
        bat './gradlew :app:bundleRelease'

        // Sign bundle
        withCredentials([string(credentialsId: 'apcupsd-monitor-signing-password', variable: 'apcupsd-monitor-signing-password')]) {
            bat 'jarsigner -verbose -keystore %KEYSTORE_LOCATION% %WORKSPACE%\\app\\build\\outputs\\bundle\\release\\app-release.aab Nitramite --storepass "%apcupsd-monitor-signing-password%"'
        }

        // Archive the AAB (Android App Bundle) so that it can be downloaded from Jenkins
        archiveArtifacts '**/bundle/release/*.aab'

      }
    }
  }
}
