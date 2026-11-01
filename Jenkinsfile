
pipeline {
    agent any

    tools {
        jdk 'JDK8'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Source code retrieved from GitHub'
            }
        }

        stage('Build and Test') {
            steps {
                sh './mvnw -B clean verify'
            }
        }
    }
}
