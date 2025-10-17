# Jenkins as Code - Rick and Morty Web Testing Pipeline

Centralized Jenkins CI/CD pipeline configuration using Job DSL, Shared Libraries, and minimal Jenkinsfiles.

## Setup

**Prerequisites:**
- Docker and Docker Compose installed
- GitHub App credentials with:
  - **Checks**: Read & write
  - **Commit statuses**: Read & write
  - **Contents**: Read
  - **Metadata**: Read
  - **Pull requests**: Read & write

**Follow:** [START_HERE.md](START_HERE.md)

This setup uses the official Jenkins Docker-in-Docker configuration.

## What This Does

**PR Checks:** Runs unit tests automatically when pull requests are created using GitHub Checks API
**Main Pipeline:** When PR is merged:
- Deploys to GitHub Pages
- Runs Playwright integration tests
- Generates Allure reports

## Architecture: Centralized Pipeline Logic

This project follows Jenkins best practices with a **hybrid approach**:

- ✅ **Minimal Jenkinsfile** in each application repository (3 lines)
- ✅ **All pipeline logic** centralized in shared library (`vars/`)
- ✅ **Job definitions** managed via Job DSL in this repository
- ✅ **GitHub Checks API** works correctly (requires Jenkinsfile in app repo)

### Why This Approach?

Jenkins plugins (especially GitHub Checks) expect the pipeline to be defined in the application repository. However, we keep all actual logic centralized:

**In Application Repo (BOG001-data-lovers):**
```groovy
// Jenkinsfile - Just 3 lines!
@Library('jenkins-shared-library') _
prCheck()
```

**In This Repo (Rick-and-Morty-Web-Jenkins-Pipeline):**
- Complete pipeline implementation in `vars/prCheck.groovy`
- Job configuration in `jobs/dataLoversPRMultibranch.groovy`
- Docker agent definitions in `docker/`

## Project Structure

```
Rick-and-Morty-Web-Jenkins-Pipeline/
├── docker/
│   ├── Dockerfile.nodejs              # Node.js agent image
│   ├── Dockerfile.playwright          # Playwright agent image
│   └── build-agents.sh                # Build script
├── jobs/
│   ├── dataLoversJob.groovy           # Main branch pipeline job
│   └── dataLoversPRMultibranch.groovy # Multibranch pipeline for PRs
├── vars/
│   ├── prCheck.groovy                 # PR check pipeline logic (shared library)
│   └── mainBranchPipeline.groovy      # Main pipeline logic (shared library)
└── docs/
    └── Jenkinsfile.example            # Template for application repos
```

## How It Works

### PR Workflow
```
Developer creates PR → GitHub webhook → Jenkins Multibranch Pipeline
                                              ↓
                                   Discovers PR automatically
                                              ↓
                            Loads Jenkinsfile from PR branch
                                              ↓
                             Calls prCheck() from shared library
                                              ↓
                            Runs in jenkins-agent-nodejs Docker
                                              ↓
                       npm install → npm test → ESLint → HTMLHint
                                              ↓
                           GitHub Checks API publishes results to PR
```

### Main Branch Workflow
```
PR merged → GitHub webhook → Jenkins
                            ↓
              Stage 1: jenkins-agent-nodejs Docker
              - Deploy to GitHub Pages
              - Start application server
                            ↓
              Stage 2: jenkins-agent-playwright Docker
              - Run integration tests
              - Generate Allure Report
```

## Jobs Created

### BOG001-data-lovers-pr-checks-multibranch
- **Type**: Multibranch Pipeline
- **Trigger**: Automatic repository scan every 5 minutes + GitHub webhooks
- **Discovers**: All branches and pull requests automatically
- **Agent**: jenkins-agent-nodejs (Docker)
- **Pipeline**: Defined in `Jenkinsfile` in BOG001-data-lovers repo
- **Logic**: Implemented in `vars/prCheck.groovy` (centralized)
- **Actions**: Unit tests, ESLint, HTMLHint
- **Reports**: Code coverage via HTML Publisher
- **GitHub Integration**: Publishes checks to PRs via GitHub Checks API

### BOG001-data-lovers-main-pipeline
- **Type**: Pipeline
- **Trigger**: Push to main/master branch webhook
- **Agents**: jenkins-agent-nodejs + jenkins-agent-playwright (Docker)
- **Actions**: Deploy to GitHub Pages, run integration tests
- **Reports**: Allure Report, HTML Test Report

## Required Jenkins Plugins

```
- Job DSL Plugin
- Pipeline Plugin
- Git Plugin
- GitHub Plugin
- GitHub Branch Source Plugin
- Generic Webhook Trigger Plugin
- Checks API Plugin
- GitHub Checks Plugin
- Docker Plugin
- Docker Pipeline Plugin
- Allure Plugin
- HTML Publisher Plugin
```

## Configuration Files

**jobs/*.groovy** - Job DSL scripts that define Jenkins jobs (created by seed job)
**vars/*.groovy** - Shared library functions containing all pipeline logic
**docker/Dockerfile.*** - Docker images for build agents
**Jenkinsfile** (in app repos) - Minimal file that calls shared library functions

## Updating Pipelines

### To modify PR check behavior:
1. Edit `vars/prCheck.groovy` in this repository
2. Commit and push to main
3. Changes apply on next PR build (shared library auto-updates)

### To modify main pipeline behavior:
1. Edit `vars/mainBranchPipeline.groovy` in this repository
2. Commit and push to main
3. Changes apply on next main branch build

### To add new jobs:
1. Create new Job DSL file in `jobs/` directory
2. Create corresponding shared library function in `vars/`
3. Commit and push to this repository
4. Run the seed job in Jenkins
5. Add minimal Jenkinsfile to target application repository

### To onboard a new repository:
1. Add a new Job DSL file in `jobs/` directory (copy and modify existing)
2. Create a minimal Jenkinsfile in the target repository:
   ```groovy
   @Library('jenkins-shared-library') _
   prCheck()  // or your custom function
   ```
3. Run seed job to create the Jenkins job

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

**BOG001-data-lovers:**
- Requires minimal `Jenkinsfile` at root (3 lines - see `docs/Jenkinsfile.example`)
- All pipeline logic stays in this repo's shared library

**Rick-and-Morty-API-testing:**
- Add `requirements.txt` for Playwright tests

**This repo (Rick-and-Morty-Web-Jenkins-Pipeline):**
- Contains all Jenkins configuration
- Shared library functions in `vars/`
- Job DSL definitions in `jobs/`
- Docker agent images in `docker/`

## Troubleshooting

**Seed job fails:**
- Check GitHub App credentials (ID: `0c90ddec-1d22-41c9-ba8b-bbce09886bc7`)
- Verify Job DSL plugin is installed
- Check script approval in Manage Jenkins → In-process Script Approval

**Docker agent won't start:**
- Run `docker images | grep jenkins-agent` to verify images exist
- If missing, rebuild: `cd docker && ./build-agents.sh`
- For DinD setup, images must be in jenkins-docker container

**GitHub Checks not appearing in PR:**
- Verify GitHub App has "Checks: Read & write" permission
- Ensure Jenkinsfile exists in the application repository
- Check Jenkins logs: Manage Jenkins → System Log
- Verify Multibranch Pipeline discovered the PR (check job page)

**Multibranch Pipeline not discovering PRs:**
- Check branch source credentials are correct
- Verify GitHub App is installed on the repository
- Trigger manual scan: Go to job → "Scan Repository Now"

**PR checks don't run:**
- Verify GitHub App webhook is configured with correct Jenkins URL
- Check webhook delivery in GitHub App → Advanced → Recent Deliveries
- For local development, use ngrok to expose Jenkins

**Allure report missing:**
- Check integration tests generate `allure-results` folder
- Verify Allure plugin is installed and configured

## Key Differences from Traditional Setup

This setup uses a **hybrid centralized approach**:

| Traditional Jenkins | This Setup |
|---------------------|------------|
| Full Jenkinsfile in each repo | Minimal 3-line Jenkinsfile |
| Pipeline logic scattered | All logic in shared library |
| Hard to standardize | Easy to enforce standards |
| GitHub Checks doesn't work with external scripts | Works perfectly with minimal Jenkinsfile |
| Each team maintains pipelines | Platform team maintains centrally |

**Best of both worlds**: GitHub Checks API works correctly, but pipeline logic stays centralized and DRY.
