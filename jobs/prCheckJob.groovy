// jobs/prCheckJob.groovy
// Job DSL for PR checks - references external Groovy pipeline script

pipelineJob('BOG001-data-lovers-pr-checks') {
    description('Runs unit tests on pull requests for BOG001-data-lovers')

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
            scriptPath('pipelines/prCheckPipeline.groovy')
        }
    }

    logRotator {
        numToKeep(20)
    }
}
