// jobs/dataLoversPRMultibranch.groovy
// Multibranch Pipeline Job DSL for BOG001-data-lovers PR checks

multibranchPipelineJob('BOG001-data-lovers-pr-checks-multibranch') {
    description('Multibranch pipeline for BOG001-data-lovers - runs PR checks using Jenkinsfile from the repo')

    // Configure GitHub branch source
    branchSources {
        github {
            id('bog001-data-lovers-repo')
            repoOwner('angelicab7')
            repository('BOG001-data-lovers')
            scanCredentialsId('0c90ddec-1d22-41c9-ba8b-bbce09886bc7')
        }
    }

    // Configure what to discover
    configure {
        def traits = it / sources / data / 'jenkins.branch.BranchSource' / source / traits
        traits << 'org.jenkinsci.plugins.github__branch__source.BranchDiscoveryTrait' {
            strategyId(1) // Exclude branches that are also filed as PRs
        }
        traits << 'org.jenkinsci.plugins.github__branch__source.OriginPullRequestDiscoveryTrait' {
            strategyId(1) // Merging the pull request with the current target branch revision
        }
        traits << 'org.jenkinsci.plugins.github__branch__source.ForkPullRequestDiscoveryTrait' {
            strategyId(1) // Merging the pull request with the current target branch revision
            trust(class: 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait$TrustPermission')
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

    // Trigger configuration
    triggers {
        periodic(5) // Scan repository every 5 minutes
    }
}
