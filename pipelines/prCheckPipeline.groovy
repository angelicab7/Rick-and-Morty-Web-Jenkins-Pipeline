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
        // Skip the automatic checkout of the pipeline repo - we'll manually checkout the app repo instead
        skipDefaultCheckout()
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    echo "Checking out PR #${env.pr_number} from BOG001-data-lovers..."
                    echo "PR Head SHA: ${env.pr_head_sha}"
                    echo "PR Branch: ${env.pr_head_ref}"

                    // Checkout the PR code from the APPLICATION repository
                    def checkoutInfo = checkout([
                        $class: 'GitSCM',
                        branches: [[name: "${env.pr_head_sha}"]],
                        userRemoteConfigs: [[
                            url: 'https://github.com/angelicab7/BOG001-data-lovers.git',
                            credentialsId: '0c90ddec-1d22-41c9-ba8b-bbce09886bc7',
                            refspec: '+refs/pull/${pr_number}/head:refs/remotes/origin/PR-${pr_number}'
                        ]],
                        extensions: [[$class: 'CloneOption', depth: 1, shallow: true]]
                    ])

                    // Override Git context to point to the APPLICATION repo, not the pipeline repo
                    env.GIT_COMMIT = env.pr_head_sha
                    env.GIT_URL = 'https://github.com/angelicab7/BOG001-data-lovers.git'
                    env.GIT_BRANCH = env.pr_head_ref

                    echo "Set GIT_URL to: ${env.GIT_URL}"
                    echo "Set GIT_COMMIT to: ${env.GIT_COMMIT}"
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
                // Debug: Print all environment variables related to Git and GitHub
                echo "=== DEBUG: Git Environment Variables ==="
                echo "GIT_URL: ${env.GIT_URL}"
                echo "GIT_COMMIT: ${env.GIT_COMMIT}"
                echo "GIT_BRANCH: ${env.GIT_BRANCH}"
                echo "BUILD_URL: ${env.BUILD_URL}"
                echo "WORKSPACE: ${env.WORKSPACE}"

                // Print PR-specific variables
                echo "=== DEBUG: PR Variables ==="
                echo "pr_number: ${env.pr_number}"
                echo "pr_head_sha: ${env.pr_head_sha}"
                echo "pr_head_ref: ${env.pr_head_ref}"
                echo "pr_base_ref: ${env.pr_base_ref}"

                // Check what repository context Jenkins thinks we're in
                echo "=== DEBUG: Workspace Git Config ==="
                sh 'git config --get remote.origin.url || echo "No remote.origin.url"'
                sh 'git rev-parse HEAD || echo "No HEAD"'

                // Publish commit status to the APPLICATION repository (BOG001-data-lovers)
                echo "=== DEBUG: Publishing commit status ==="
                echo "  context: Jenkins PR Check"
                echo "  state: SUCCESS"
                echo "  commit SHA: ${env.pr_head_sha}"
                echo "  repo: https://github.com/angelicab7/BOG001-data-lovers"

                step([
                    $class: 'GitHubCommitStatusSetter',
                    contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'Jenkins PR Check'],
                    statusResultSource: [$class: 'ConditionalStatusResultSource',
                        results: [
                            [$class: 'AnyBuildResult', message: 'All tests passed!', state: 'SUCCESS']
                        ]
                    ],
                    reposSource: [$class: 'ManuallyEnteredRepositorySource', url: 'https://github.com/angelicab7/BOG001-data-lovers'],
                    commitShaSource: [$class: 'ManuallyEnteredShaSource', sha: "${env.pr_head_sha}"],
                    credentialsId: '0c90ddec-1d22-41c9-ba8b-bbce09886bc7'
                ])

                echo "=== DEBUG: Commit status published ==="
            }
        }
        failure {
            echo '❌ PR checks failed. Please fix the issues.'
            script {
                // Debug logging
                echo "=== DEBUG: Git Environment Variables ==="
                echo "GIT_URL: ${env.GIT_URL}"
                echo "GIT_COMMIT: ${env.GIT_COMMIT}"
                echo "GIT_BRANCH: ${env.GIT_BRANCH}"

                // Publish commit status to the APPLICATION repository (BOG001-data-lovers)
                step([
                    $class: 'GitHubCommitStatusSetter',
                    contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'Jenkins PR Check'],
                    statusResultSource: [$class: 'ConditionalStatusResultSource',
                        results: [
                            [$class: 'AnyBuildResult', message: 'Tests failed', state: 'FAILURE']
                        ]
                    ],
                    reposSource: [$class: 'ManuallyEnteredRepositorySource', url: 'https://github.com/angelicab7/BOG001-data-lovers'],
                    commitShaSource: [$class: 'ManuallyEnteredShaSource', sha: "${env.pr_head_sha}"],
                    credentialsId: '0c90ddec-1d22-41c9-ba8b-bbce09886bc7'
                ])
            }
        }
    }
}
