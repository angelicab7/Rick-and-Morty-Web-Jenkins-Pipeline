// jobs/seedJob.groovy
// Seed job that creates all other jobs from the Job DSL scripts

// This is the master seed job that processes all Job DSL scripts
// You need to create this manually in Jenkins once, then it will manage all other jobs

job('seed-job') {
    description('Master seed job that creates all Jenkins jobs from DSL scripts')

    scm {
        git {
            remote {
                url('https://github.com/angelicab7/Rick-and-Morty-Web-Jenkins-Pipeline.git')
                credentials('github-app-credentials')
            }
            branch('main')
        }
    }

    triggers {
        scm('H/5 * * * *') // Poll SCM every 5 minutes
    }

    steps {
        dsl {
            // Process all Groovy scripts in the jobs directory
            external('jobs/**/*.groovy')
            removeAction('DELETE') // Remove jobs that are no longer defined
            removeViewAction('DELETE')
            ignoreExisting(false)
            lookupStrategy('SEED_JOB')
        }
    }
}
