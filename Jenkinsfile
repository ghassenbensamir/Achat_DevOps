pipeline {
    agent any

    options {
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
    }

    // Default JDK for the whole pipeline (the project builds with Java 8)
    tools {
        jdk 'JDK8'
    }

    triggers {
        githubPush()                   // fired by GitHub via smee
        pollSCM('H/15 * * * *')        // fallback if smee is down
    }

    environment {
        MAVEN_OPTS = '-Xmx512m'        // keep Maven small on the 3.6 GiB VM
    }

    stages {
        stage('Build and Unit Tests') {
            steps {
                sh 'java -version'
                sh './mvnw -B clean verify'
            }
        }

        stage('SonarQube Analysis and Quality Gate') {
            tools { jdk 'JDK21' }      // overrides JDK8 for this stage only (Sonar scanner needs Java 17+)
            steps {
                sh 'java -version'
                withSonarQubeEnv('SonarQube') {
                    // No "clean": reuse the classes compiled by JDK 8 in stage 1.
                    // qualitygate.wait makes the scanner poll SonarQube and fail
                    // the build if the gate fails (no webhook needed).
                    sh '''./mvnw -B org.sonarsource.scanner.maven:sonar-maven-plugin:5.1.0.4751:sonar \
                        -Dsonar.projectKey=my-project \
                        -Dsonar.java.source=8 \
                        -Dsonar.qualitygate.wait=true \
                        -Dsonar.qualitygate.timeout=300'''
                }
            }
        }
    }

    post {
        always {
            junit testResults: '**/target/surefire-reports/TEST-*.xml, **/target/failsafe-reports/TEST-*.xml',
                  allowEmptyResults: true
        }

        success {
            archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true
            echo 'Pipeline succeeded!'
        }

        failure {
            echo 'Pipeline failed. Check the Jenkins console output.'
        }
    }
}