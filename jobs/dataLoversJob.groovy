// jobs/dataLoversJob.groovy
// Job DSL for main branch pipeline - references external Groovy pipeline script
// FULLY CENTRALIZED - No code in application repo, proper Groovy with IDE support

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
                        credentials('github-app-credentials')
                    }
                    branches('main')
                }
            }
            scriptPath('pipelines/mainBranchPipeline.groovy')
        }
    }

    orphanedItemStrategy {
        discardOldItems {
            numToKeep(20)
        }
    }
}
