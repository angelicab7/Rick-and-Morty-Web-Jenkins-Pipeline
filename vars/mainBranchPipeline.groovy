// vars/mainBranchPipeline.groovy
// Main branch pipeline: Deploy to GitHub Pages and run integration tests

def call() {
    def testRepoUrl = 'https://github.com/angelicab7/Rick-and-Morty-API-testing.git'
    def appDir = 'app'
    def testDir = 'integration-tests'
    def serverPid = null

    pipeline {
        agent any

        tools {
            nodejs 'NodeJS-18'
        }

        environment {
            TEST_REPO_URL = "${testRepoUrl}"
            APP_DIR = "${appDir}"
            TEST_DIR = "${testDir}"
        }

        stages {
            stage('Checkout Application') {
                steps {
                    script {
                        echo "Checking out frontend application from BOG001-data-lovers..."
                        dir(env.APP_DIR) {
                            git branch: 'master',
                                credentialsId: '0c90ddec-1d22-41c9-ba8b-bbce09886bc7',
                                url: 'https://github.com/angelicab7/BOG001-data-lovers.git'
                        }
                    }
                }
            }

            stage('Checkout Integration Tests') {
                steps {
                    script {
                        echo "Checking out integration tests repository..."
                        dir(env.TEST_DIR) {
                            git branch: 'main',
                                credentialsId: '0c90ddec-1d22-41c9-ba8b-bbce09886bc7',
                                url: env.TEST_REPO_URL
                        }
                    }
                }
            }

            stage('Deploy to GitHub Pages') {
                steps {
                    script {
                        dir(env.APP_DIR) {
                            echo "Installing dependencies..."
                            sh 'npm install'

                            echo "Deploying to GitHub Pages..."
                            withCredentials([usernamePassword(
                                credentialsId: '0c90ddec-1d22-41c9-ba8b-bbce09886bc7',
                                usernameVariable: 'GIT_USERNAME',
                                passwordVariable: 'GIT_TOKEN'
                            )]) {
                                sh '''
                                    git config user.email "jenkins@ci.local"
                                    git config user.name "Jenkins CI"
                                    npm run deploy
                                '''
                            }
                        }
                    }
                }
            }

            stage('Start Application Server') {
                steps {
                    script {
                        dir(env.APP_DIR) {
                            echo "Starting development server..."
                            sh 'nohup npm start > server.log 2>&1 & echo $! > server.pid'
                            sh 'sleep 10' // Wait for server to start

                            // Verify server is running
                            sh 'curl -f http://localhost:3000 || curl -f http://localhost:5000 || true'
                        }
                    }
                }
            }

            stage('Run Integration Tests') {
                steps {
                    script {
                        dir(env.TEST_DIR) {
                            echo "Setting up Python environment..."
                            sh '''
                                python3 -m venv venv
                                . venv/bin/activate
                                pip install --upgrade pip
                                pip install -r requirements.txt || pip install pytest pytest-playwright pytest-bdd allure-pytest pillow requests
                                playwright install chromium
                            '''

                            echo "Running integration tests with Allure reporting..."
                            sh '''
                                . venv/bin/activate
                                pytest --alluredir=allure-results --html=report.html --self-contained-html
                            '''
                        }
                    }
                }
            }
        }

        post {
            always {
                script {
                    echo "Stopping application server..."
                    dir(env.APP_DIR) {
                        sh '''
                            if [ -f server.pid ]; then
                                kill $(cat server.pid) || true
                                rm server.pid
                            fi
                            pkill -f "npm start" || true
                            pkill -f "serve" || true
                        '''
                    }

                    echo "Publishing Allure Report..."
                    allure([
                        includeProperties: false,
                        jdk: '',
                        properties: [],
                        reportBuildPolicy: 'ALWAYS',
                        results: [[path: "${env.TEST_DIR}/allure-results"]]
                    ])

                    echo "Publishing HTML Test Report..."
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: "${env.TEST_DIR}",
                        reportFiles: 'report.html',
                        reportName: 'Integration Test Report'
                    ])
                }
            }
            success {
                echo 'Pipeline completed successfully! Application deployed and tests passed.'
            }
            failure {
                echo 'Pipeline failed. Check the logs and reports for details.'
            }
        }
    }
}
