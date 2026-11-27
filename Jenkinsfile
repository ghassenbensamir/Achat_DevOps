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
        MAVEN_OPTS = '-Xmx512m'        // keep Maven small on the VM
    }

    stages {
        stage('Build and Unit Tests') {
            steps {
                sh 'java -version'
                sh './mvnw -B clean verify'
            }
        }

        stage('SonarQube Analysis and Quality Gate') {
            tools { jdk 'JDK21' }      // Sonar scanner needs Java 17+
            steps {
                sh 'java -version'
                withSonarQubeEnv('SonarQube') {
                    // No "clean": reuse the classes compiled by JDK 8 in stage 1.
                    // qualitygate.wait makes the scanner poll SonarQube and fail
                    // the build if the gate fails (no webhook needed).
                    sh '''./mvnw -B org.sonarsource.scanner.maven:sonar-maven-plugin:5.1.0.4751:sonar \
                        -Dsonar.projectKey=achat \
                        -Dsonar.java.source=8 \
                        -Dsonar.qualitygate.wait=true \
                        -Dsonar.qualitygate.timeout=300'''
                }
            }
        }

        stage('Publish to Nexus') {
            // Only publish from main. To test from another branch, temporarily
            // change the condition (for example: env.GIT_BRANCH == 'origin/ci/your-branch').
            when {
                expression { env.GIT_BRANCH == 'origin/main' || env.GIT_BRANCH == 'main' }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'nexus-credentials',
                                                  usernameVariable: 'NEXUS_USER',
                                                  passwordVariable: 'NEXUS_PASS')]) {
                    // Tests already ran in stage 1, so skip them here.
                    sh './mvnw -B -s ci-settings.xml deploy -DskipTests'
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