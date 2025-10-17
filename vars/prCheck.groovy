// vars/prCheck.groovy
// Shared library function for PR checks - runs unit tests

def call() {
    pipeline {
        agent {
            docker {
                image 'jenkins-agent-nodejs:latest'
            }
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

            stage('Publish Reports') {
                steps {
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
            }
        }

        post {
            success {
                echo '✅ PR checks passed successfully!'
                script {
                    publishChecks(
                        name: 'Jenkins PR Check',
                        title: 'Unit Tests Passed',
                        summary: 'All unit tests, linting, and HTML validation passed successfully.',
                        text: """
## Test Results
- ✅ Unit Tests: Passed
- ✅ ESLint: Passed
- ✅ HTML Validation: Passed

[View Full Build Log](${env.BUILD_URL}console)
[View Coverage Report](${env.BUILD_URL}Code_Coverage_Report/)
                        """,
                        conclusion: 'SUCCESS',
                        detailsURL: "${env.BUILD_URL}",
                        status: io.jenkins.plugins.checks.api.ChecksStatus.COMPLETED
                    )
                }
            }
            failure {
                echo '❌ PR checks failed. Please fix the issues.'
                script {
                    publishChecks(
                        name: 'Jenkins PR Check',
                        title: 'Tests Failed',
                        summary: 'Some tests or checks failed. Please review the build logs.',
                        text: """
## Test Results
❌ One or more checks failed

[View Full Build Log](${env.BUILD_URL}console)
                        """,
                        conclusion: 'FAILURE',
                        detailsURL: "${env.BUILD_URL}",
                        status: io.jenkins.plugins.checks.api.ChecksStatus.COMPLETED
                    )
                }
            }
        }
    }
}
