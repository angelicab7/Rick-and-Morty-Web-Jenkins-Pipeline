// pipelines/prCheckPipeline.groovy
// PR Check Pipeline - Full Groovy script with type safety and IDE support

@Library('jenkins-shared-library') _

pipeline {
    agent {
        docker {
            image 'jenkins-agent-nodejs:latest'
        }
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    echo "Checking out PR branch..."
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: '${sha1}']],
                        userRemoteConfigs: [[
                            url: 'https://github.com/angelicab7/BOG001-data-lovers.git',
                            credentialsId: 'github-app-credentials',
                            refspec: '+refs/pull/*:refs/remotes/origin/pr/*'
                        ]]
                    ])
                }
            }
        }

        stage('Install Dependencies') {
            steps {
                script {
                    echo "Installing npm dependencies..."
                    sh 'npm install'
                }
            }
        }

        stage('Run Unit Tests') {
            steps {
                script {
                    echo "Running Jest unit tests with coverage..."
                    sh 'npm test -- --ci --coverage'
                }
            }
        }

        stage('Lint Check') {
            steps {
                script {
                    echo "Running ESLint..."
                    sh 'npm run eslint || true'
                }
            }
        }

        stage('HTML Hint') {
            steps {
                script {
                    echo "Running HTML validation..."
                    sh 'npm run htmlhint || true'
                }
            }
        }
    }

    post {
        always {
            script {
                echo "Publishing test coverage report..."
                publishHTML(target: [
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'coverage/lcov-report',
                    reportFiles: 'index.html',
                    reportName: 'Code Coverage Report'
                ])
            }
        }
        success {
            echo '✅ PR checks passed successfully!'
        }
        failure {
            echo '❌ PR checks failed. Please fix the issues.'
        }
    }
}
