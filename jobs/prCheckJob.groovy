// jobs/prCheckJob.groovy
// Job DSL for PR checks - references external Groovy pipeline script

pipelineJob('BOG001-data-lovers-pr-checks') {
    description('Runs unit tests on pull requests for BOG001-data-lovers')

    properties {
        githubProjectUrl('https://github.com/angelicab7/BOG001-data-lovers')
        pipelineTriggers {
            triggers {
                genericTrigger {
                    // Extract PR information from GitHub webhook payload
                    genericVariables {
                        genericVariable {
                            key('action')
                            value('$.action')
                        }
                        genericVariable {
                            key('pr_number')
                            value('$.number')
                        }
                        genericVariable {
                            key('pr_head_sha')
                            value('$.pull_request.head.sha')
                        }
                        genericVariable {
                            key('pr_head_ref')
                            value('$.pull_request.head.ref')
                        }
                        genericVariable {
                            key('pr_base_ref')
                            value('$.pull_request.base.ref')
                        }
                    }

                    // Only trigger on PR opened, synchronize (new commits), or reopened events
                    regexpFilterText('$action')
                    regexpFilterExpression('^(opened|synchronize|reopened)$')

                    // Unique token for this job's webhook endpoint
                    token('BOG001-data-lovers-pr-checks')

                    // Print information about what triggered the build
                    printContributedVariables(true)
                    printPostContent(true)

                    causeString('GitHub PR #$pr_number')
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
