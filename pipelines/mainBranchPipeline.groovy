// pipelines/mainBranchPipeline.groovy
// Main Branch Pipeline - Deploy to GitHub Pages and run integration tests
// Uses specialized Docker agents for Node.js and Playwright

@Library('jenkins-shared-library') _

pipeline {
    agent none  // Use different agents for different stages

    environment {
        TEST_REPO_URL = 'https://github.com/angelicab7/Rick-and-Morty-API-testing.git'
        SERVER_PORT = '3000'
    }

    stages {
        stage('Build and Deploy') {
            agent {
                docker {
                    image 'jenkins-agent-nodejs:latest'
                }
            }
            stages {
                stage('Checkout Application') {
                    steps {
                        script {
                            echo "Checking out frontend application from BOG001-data-lovers/master..."
                            checkout([
                                $class: 'GitSCM',
                                branches: [[name: 'master']],
                                userRemoteConfigs: [[
                                    url: 'https://github.com/angelicab7/BOG001-data-lovers.git',
                                    credentialsId: '0c90ddec-1d22-41c9-ba8b-bbce09886bc7'
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
                            echo "Running unit tests before deployment..."
                            sh 'npm test -- --ci'
                        }
                    }
                }

                stage('Deploy to GitHub Pages') {
                    steps {
                        script {
                            echo "Deploying to GitHub Pages..."
                            sh '''
                                git config user.email "jenkins@ci.local"
                                git config user.name "Jenkins CI"

                                # GitHub App credentials work automatically with Git operations
                                # The checkout step already configured authentication
                                npm run deploy
                            '''
                        }
                    }
                }

                stage('Build for Testing') {
                    steps {
                        script {
                            echo "Building application for testing..."
                            sh 'npm install'
                        }
                    }
                }

                stage('Start Application Server') {
                    steps {
                        script {
                            echo "Starting development server on port ${env.SERVER_PORT}..."
                            sh """
                                nohup npm start > server.log 2>&1 &
                                echo \$! > server.pid
                            """

                            echo "Waiting for server to be ready..."
                            sh 'sleep 10'

                            echo "Verifying server is running..."
                            sh """
                                curl -f http://localhost:${env.SERVER_PORT} || \
                                curl -f http://localhost:5000 || \
                                (echo "Server failed to start" && cat server.log && exit 1)
                            """
                        }
                    }
                }

                stage('Archive Build') {
                    steps {
                        script {
                            echo "Archiving server PID for cleanup..."
                            archiveArtifacts artifacts: 'server.pid', allowEmptyArchive: true
                        }
                    }
                }
            }
        }

        stage('Integration Tests') {
            agent {
                docker {
                    image 'jenkins-agent-playwright:latest'
                    args '--network=host --ipc=host'
                }
            }
            stages {
                stage('Checkout Integration Tests') {
                    steps {
                        script {
                            echo "Checking out integration tests repository..."
                            checkout([
                                $class: 'GitSCM',
                                branches: [[name: 'main']],
                                userRemoteConfigs: [[
                                    url: env.TEST_REPO_URL,
                                    credentialsId: '0c90ddec-1d22-41c9-ba8b-bbce09886bc7'
                                ]]
                            ])
                        }
                    }
                }

                stage('Run Playwright Tests') {
                    steps {
                        script {
                            echo "Running Playwright integration tests..."
                            sh """
                                # Playwright and dependencies are already installed in the image
                                pytest \
                                    --alluredir=allure-results \
                                    --html=report.html \
                                    --self-contained-html \
                                    -v \
                                    --base-url=http://localhost:${env.SERVER_PORT}
                            """
                        }
                    }
                }

                stage('Generate Reports') {
                    steps {
                        script {
                            echo "Publishing test reports..."

                            // Publish Allure Report
                            allure([
                                includeProperties: false,
                                jdk: '',
                                properties: [],
                                reportBuildPolicy: 'ALWAYS',
                                results: [[path: 'allure-results']]
                            ])

                            // Publish HTML Report
                            publishHTML([
                                allowMissing: false,
                                alwaysLinkToLastBuild: true,
                                keepAll: true,
                                reportDir: '.',
                                reportFiles: 'report.html',
                                reportName: 'Integration Test Report'
                            ])
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo "Cleanup: Stopping application server..."
                sh '''
                    # Kill server process if it exists
                    pkill -f "npm start" || true
                    pkill -f "serve" || true
                '''
            }
        }
        success {
            echo '✅ Pipeline completed successfully! Application deployed and tests passed.'
        }
        failure {
            echo '❌ Pipeline failed. Check the logs and reports for details.'
        }
    }
}
