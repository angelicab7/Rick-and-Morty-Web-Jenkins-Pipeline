// vars/prCheck.groovy
// Shared library function for PR checks - runs unit tests

def call() {
    pipeline {
        agent any

        tools {
            nodejs 'NodeJS-18'
        }

        stages {
            stage('Checkout') {
                steps {
                    checkout scm
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
                        echo "Running Jest unit tests..."
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
        }

        post {
            always {
                // Publish test coverage report
                publishHTML(target: [
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'coverage/lcov-report',
                    reportFiles: 'index.html',
                    reportName: 'Code Coverage Report'
                ])
            }
            success {
                echo 'PR checks passed successfully!'
            }
            failure {
                echo 'PR checks failed. Please fix the issues.'
            }
        }
    }
}
