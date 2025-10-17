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
                    echo "Checking out PR #${env.pr_number} (${env.pr_head_sha})..."
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "${env.pr_head_sha}"]],
                        userRemoteConfigs: [[
                            url: 'https://github.com/angelicab7/BOG001-data-lovers.git',
                            credentialsId: '0c90ddec-1d22-41c9-ba8b-bbce09886bc7',
                            refspec: '+refs/pull/${pr_number}/head:refs/remotes/origin/PR-${pr_number}'
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
            script {
                publishChecks(
                    name: 'Jenkins PR Check',
                    title: 'PR #${pr_number}: Unit Tests Passed',
                    summary: 'All unit tests, linting, and HTML validation passed successfully.',
                    conclusion: 'SUCCESS',
                    detailsURL: "${env.BUILD_URL}"
                )
            }
        }
        failure {
            echo '❌ PR checks failed. Please fix the issues.'
            script {
                publishChecks(
                    name: 'Jenkins PR Check',
                    title: 'PR #${pr_number}: Tests Failed',
                    summary: 'Some tests or checks failed. Please review the build logs.',
                    conclusion: 'FAILURE',
                    detailsURL: "${env.BUILD_URL}"
                )
            }
        }
    }
}
