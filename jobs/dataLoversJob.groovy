// jobs/dataLoversJob.groovy
// Job DSL for main branch pipeline - references external Groovy pipeline script

pipelineJob('BOG001-data-lovers-main-pipeline') {
    description('Main branch pipeline: Deploy to GitHub Pages and run integration tests with Allure reporting')

    // Trigger on pushes to main branch via GitHub webhook
    properties {
        githubProjectUrl('https://github.com/angelicab7/BOG001-data-lovers')
        pipelineTriggers {
            triggers {
                githubPush()
            }
        }
    }

    // Reference external Groovy script from THIS repository
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/angelicab7/Rick-and-Morty-Web-Jenkins-Pipeline.git')
                        credentials('0c90ddec-1d22-41c9-ba8b-bbce09886bc7')
                    }
                    branches('main')
                }
            }
            scriptPath('pipelines/mainBranchPipeline.groovy')
        }
    }

    logRotator {
        numToKeep(20)
    }
}
