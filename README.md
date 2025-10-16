# Jenkins as Code - Rick and Morty Web Testing Pipeline

Jenkins CI/CD pipeline configuration using Job DSL and Groovy for BOG001-data-lovers project.

## Setup

**Prerequisites:**
- Docker and Docker Compose installed
- GitHub App credentials

**Follow:** [START_HERE.md](START_HERE.md)

This setup uses the official Jenkins Docker-in-Docker configuration.

## What This Does

**PR Checks:** Runs unit tests automatically when pull requests are created
**Main Pipeline:** When PR is merged:
- Deploys to GitHub Pages
- Runs Playwright integration tests
- Generates Allure reports

## Project Structure

```
Rick-and-Morty-Web-Jenkins-Pipeline/
├── docker/
│   ├── Dockerfile.nodejs          # Node.js agent image
│   ├── Dockerfile.playwright      # Playwright agent image
│   └── build-agents.sh            # Build script
├── jobs/
│   ├── seedJob.groovy            # Creates all jobs
│   ├── prCheckJob.groovy         # PR checks job
│   └── dataLoversJob.groovy      # Main pipeline job
├── pipelines/
│   ├── prCheckPipeline.groovy    # PR pipeline logic
│   └── mainBranchPipeline.groovy # Main pipeline logic
└── vars/
    └── # Shared library functions (optional)
```

## How It Works

### PR Workflow
```
Developer creates PR → GitHub webhook → Jenkins
                                      ↓
                          Runs in nodejs Docker agent
                                      ↓
                          npm install → npm test → ESLint
                                      ↓
                          Status posted back to PR
```

### Main Branch Workflow
```
PR merged → GitHub webhook → Jenkins
                            ↓
              Stage 1: nodejs Docker agent
              - Deploy to GitHub Pages
              - Start application server
                            ↓
              Stage 2: playwright Docker agent
              - Run integration tests
              - Generate Allure Report
```

## Jobs Created

### BOG001-data-lovers-pr-checks
- Trigger: Pull request webhook
- Agent: jenkins-agent-nodejs
- Actions: Unit tests, ESLint, HTMLHint
- Reports: Code coverage

### BOG001-data-lovers-main-pipeline
- Trigger: Push to main branch
- Agents: jenkins-agent-nodejs + jenkins-agent-playwright
- Actions: Deploy, integration tests
- Reports: Allure Report, HTML Test Report

## Required Jenkins Plugins

```
- Job DSL Plugin
- Pipeline Plugin
- Git Plugin
- GitHub Plugin
- GitHub Branch Source Plugin
- Docker Plugin
- Docker Pipeline Plugin
- Allure Plugin
- HTML Publisher Plugin
```

## Configuration Files

**jobs/*.groovy** - Job DSL scripts that define Jenkins jobs
**pipelines/*.groovy** - Pipeline scripts with build/test logic
**docker/Dockerfile.*** - Docker images for build agents

## Updating Pipelines

To modify pipeline behavior:
1. Edit `pipelines/prCheckPipeline.groovy` or `pipelines/mainBranchPipeline.groovy`
2. Commit and push
3. Changes apply on next build

To add new jobs:
1. Create new file in `jobs/` directory
2. Create corresponding pipeline in `pipelines/`
3. Commit and push
4. Seed job creates the new job automatically

## Viewing Reports

After builds complete:
- Go to build in Jenkins
- Click "Allure Report" for interactive test results
- Click "Code Coverage Report" for coverage details

## Docker Agents

This setup uses Docker containers as Jenkins agents:
- **jenkins-agent-nodejs** - Node.js 18 for frontend builds
- **jenkins-agent-playwright** - Python + Playwright for tests

Built with: `cd docker && ./build-agents.sh`

Jenkins spawns these containers on localhost as needed.

## Repository Configuration

**BOG001-data-lovers:** No changes needed
**Rick-and-Morty-API-testing:** Add requirements.txt
**This repo:** Contains all Jenkins configuration

## Troubleshooting

**Seed job fails:** Check GitHub credentials and Job DSL plugin
**Docker agent won't start:** Run `docker images | grep jenkins-agent` to verify images exist
**PR checks don't run:** Verify GitHub webhook is configured
**Allure report missing:** Check integration tests generate allure-results folder
