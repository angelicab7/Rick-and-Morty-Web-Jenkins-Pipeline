// jobs/prCheckJob.groovy
// Job DSL for PR checks - references external Groovy pipeline script
// FULLY CENTRALIZED - No code in application repo, proper Groovy with IDE support

pipelineJob('BOG001-data-lovers-pr-checks') {
    description('Runs unit tests on pull requests for BOG001-data-lovers')

    properties {
        githubProjectUrl('https://github.com/angelicab7/BOG001-data-lovers')
    }

    // Trigger on PR events via GitHub webhook
    triggers {
        githubPullRequest {
            admins(['angelicab7'])
            orgWhitelist(['angelicab7'])
            cron('H/5 * * * *')
            triggerPhrase('.*test this please.*')

            extensions {
                commitStatus {
                    context('Jenkins PR Check')
                    statusUrl('${BUILD_URL}')
                    triggeredStatus('Build triggered')
                    startedStatus('Build started')
                    completedStatus('SUCCESS', 'All tests passed!')
                    completedStatus('FAILURE', 'Tests failed')
                    completedStatus('ERROR', 'Build error')
                }
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
            scriptPath('pipelines/prCheckPipeline.groovy')
        }
    }

    logRotator {
        numToKeep(20)
    }
}
