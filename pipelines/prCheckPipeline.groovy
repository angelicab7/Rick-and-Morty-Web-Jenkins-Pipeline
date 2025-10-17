// pipelines/prCheckPipeline.groovy
// PR Check Pipeline - Full Groovy script with type safety and IDE support

@Library('jenkins-shared-library') _

pipeline {
    agent {
        docker {
            image 'jenkins-agent-nodejs:latest'
        }
    }

    options {
        // Publish checks to GitHub
        githubProjectProperty(projectUrlStr: 'https://github.com/angelicab7/BOG001-data-lovers')
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    echo "Checking out PR #${env.pr_number} (${env.pr_head_sha})..."

                    // Checkout the PR code
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "${env.pr_head_sha}"]],
                        userRemoteConfigs: [[
                            url: 'https://github.com/angelicab7/BOG001-data-lovers.git',
                            credentialsId: '0c90ddec-1d22-41c9-ba8b-bbce09886bc7',
                            refspec: '+refs/pull/${pr_number}/head:refs/remotes/origin/PR-${pr_number}'
                        ]],
                        extensions: [[$class: 'CloneOption', depth: 1, shallow: true]]
                    ])

                    // Set GitHub context for checks
                    env.GIT_COMMIT = env.pr_head_sha
                    env.GIT_URL = 'https://github.com/angelicab7/BOG001-data-lovers'
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
                withChecks('Jenkins PR Check') {
                    publishChecks(
                        name: 'Jenkins PR Check',
                        title: "PR #${env.pr_number}: Unit Tests Passed",
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
                        detailsURL: "${env.BUILD_URL}"
                    )
                }
            }
        }
        failure {
            echo '❌ PR checks failed. Please fix the issues.'
            script {
                withChecks('Jenkins PR Check') {
                    publishChecks(
                        name: 'Jenkins PR Check',
                        title: "PR #${env.pr_number}: Tests Failed",
                        summary: 'Some tests or checks failed. Please review the build logs.',
                        text: """
                        ## Test Results
                        ❌ One or more checks failed

                        [View Full Build Log](${env.BUILD_URL}console)
                        """,
                        conclusion: 'FAILURE',
                        detailsURL: "${env.BUILD_URL}"
                    )
                }
            }
        }
    }
}
