// jobs/dataLoversPRMultibranch.groovy
// Multibranch Pipeline Job DSL for BOG001-data-lovers PR checks

multibranchPipelineJob('BOG001-data-lovers-pr-checks-multibranch') {
    description('Multibranch pipeline for BOG001-data-lovers - runs PR checks using Jenkinsfile from the repo')

    // Configure GitHub branch source
    branchSources {
        branchSource {
            source {
                github {
                    id('bog001-data-lovers-repo')
                    repoOwner('angelicab7')
                    repository('BOG001-data-lovers')
                    repositoryUrl('https://github.com/angelicab7/BOG001-data-lovers')
                    configuredByUrl(true)
                    credentialsId('0c90ddec-1d22-41c9-ba8b-bbce09886bc7')
                    traits {
                        gitHubBranchDiscovery {
                            strategyId(1) // Exclude branches that are also filed as PRs
                        }
                        gitHubPullRequestDiscovery {
                            strategyId(1) // Merging the pull request with the current target branch revision
                        }
                    }
                }
            }
        }
    }

    // Look for Jenkinsfile in the repository root
    factory {
        workflowBranchProjectFactory {
            scriptPath('Jenkinsfile')
        }
    }

    // Orphaned item strategy
    orphanedItemStrategy {
        discardOldItems {
            numToKeep(20)
        }
    }

    // Trigger configuration - scan every 5 minutes
    triggers {
        periodicFolderTrigger {
            interval('5')
        }
    }
}
