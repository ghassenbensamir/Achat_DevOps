
pipeline {
    agent any

    options {
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
    }

    tools {
        jdk 'JDK8'
    }

    triggers {
        githubPush()
        pollSCM('H/15 * * * *')
    }

    environment {
        MAVEN_OPTS = '-Xmx512m'
    }

    stages {
        stage('Build and Unit Tests') {
            steps {
                sh 'java -version'
                sh './mvnw -B -DfailIfNoTests=true clean verify'
            }
        }

        stage('SonarQube Analysis and Quality Gate') {
            tools {
                jdk 'JDK21'
            }

            steps {
                sh 'java -version'

                withSonarQubeEnv('SonarQube') {
                    sh '''./mvnw -B sonar:sonar \
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
            junit testResults: '**/target/*-reports/TEST-*.xml',
                  allowEmptyResults: true
        }

        success {
            archiveArtifacts artifacts: '**/target/*.jar',
                             allowEmptyArchive: true
            echo 'Pipeline succeeded!'
        }

        failure {
            echo 'Pipeline failed. Check the Jenkins console output.'
        }
    }
}
