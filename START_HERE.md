# START HERE - Setup Guide

**Prerequisites:**
- Docker and Docker Compose installed
- GitHub App credentials ready with permissions:
  - Repository permissions:
    - **Contents**: Read
    - **Metadata**: Read
    - **Checks**: Read & write
    - **Commit statuses**: Read & write
    - **Pull requests**: Read & write
  - Subscribe to events: Push, Pull request
- GitHub App must be installed on the BOG001-data-lovers repository

Total time: ~50 minutes

---

## Step 1: Start Jenkins with Docker-in-Docker (5 min)

```bash
# Build and start Jenkins with DinD
docker-compose up -d

# Wait for Jenkins to start (check logs)
docker-compose logs -f jenkins
```

Jenkins will be available at http://localhost:8080

Get initial admin password:
```bash
docker exec jenkins-blueocean cat /var/jenkins_home/secrets/initialAdminPassword
```

Complete the setup wizard in the browser.

---

## Step 2: Build Docker Agent Images (5 min)

```bash
cd docker
./build-agents.sh
```

Verify:
```bash
docker images | grep jenkins-agent
```

You should see `jenkins-agent-nodejs` and `jenkins-agent-playwright`.

---

## Step 3: Install Additional Plugins (5 min)

The Dockerfile already installed some plugins. Add these:

Go to: Jenkins → Manage Jenkins → Manage Plugins → Available

Install:
- Job DSL Plugin
- Allure Plugin
- HTML Publisher Plugin
- Generic Webhook Trigger Plugin
- Checks API Plugin
- GitHub Checks Plugin

(GitHub Branch Source and Pipeline plugins are already included)

Restart Jenkins after installation.

---

## Step 4: Configure Docker in Jenkins (10 min)

### 4.1 Create Docker Server Credentials

Go to: Manage Jenkins → Manage Credentials → System → Global credentials → Add Credentials

**Type:** X.509 Client Certificate

Get the certificates from Jenkins container:
```bash
docker exec jenkins-blueocean cat /certs/client/key.pem
docker exec jenkins-blueocean cat /certs/client/cert.pem
docker exec jenkins-blueocean cat /certs/client/ca.pem
```

Fill in:
- **Client Key:** Paste output from `key.pem`
- **Client Certificate:** Paste output from `cert.pem`
- **Server CA Certificate:** Paste output from `ca.pem`
- **ID:** `docker-client-cert`
- **Description:** Docker TLS Client Certificate

Click **Create**

### 4.2 Configure Docker Cloud

Go to: Manage Jenkins → Manage Nodes and Clouds → Configure Clouds → Add a new cloud → Docker

Configure:
- **Name:** `docker`
- **Docker Host URI:** `tcp://docker:2376`
- **Server credentials:** Select `docker-client-cert` (the one you just created)
- **Enabled:** ✓
- Click **Test Connection** (should show Docker version)

Click **Save**

---

## Step 5: Configure Tools (2 min)

Go to: Manage Jenkins → Global Tool Configuration

**Allure Commandline:**
- Name: `Allure`
- Install automatically: ✓
- Version: 2.24.0

Save.

---

## Step 6: Configure Shared Library (3 min)

Go to: Manage Jenkins → Configure System → Global Pipeline Libraries

Add:
- **Name:** `jenkins-shared-library`
- **Default version:** `main`
- **Retrieval method:** Modern SCM
- **Source Code Management:** Git
- **Project Repository:** `https://github.com/angelicab7/Rick-and-Morty-Web-Jenkins-Pipeline.git`
- **Credentials:** Select your GitHub App credentials (ID: `0c90ddec-1d22-41c9-ba8b-bbce09886bc7`)

Save.

---

## Step 7: Create Seed Job (5 min)

Jenkins → New Item
- **Name:** `seed-job`
- **Type:** Freestyle project

**Source Code Management:**
- Git
- Repository URL: `https://github.com/angelicab7/Rick-and-Morty-Web-Jenkins-Pipeline.git`
- Credentials: Select your GitHub App credentials (ID: `0c90ddec-1d22-41c9-ba8b-bbce09886bc7`)
- Branch: `main`

**Build Triggers:**
- Poll SCM: `H/5 * * * *`

**Build Steps:**
- Add build step → Process Job DSLs
- Look on Filesystem
- DSL Scripts: `jobs/**/*.groovy`
- Action for removed jobs: Delete
- Action for removed views: Delete

Save → Click **Build Now**

**First-time run will fail** - this is expected!

Go to: **Manage Jenkins** → **In-process Script Approval**
- You'll see pending signatures from the Job DSL scripts
- Click **Approve** for each one
- This is a security feature - you only need to do this once

Go back to seed-job and click **Build Now** again.

This creates two jobs:
- `BOG001-data-lovers-pr-checks-multibranch` (Multibranch Pipeline)
- `BOG001-data-lovers-main-pipeline` (Regular Pipeline)

---

## Step 8: Add Jenkinsfile to BOG001-data-lovers (5 min)

The Multibranch Pipeline requires a `Jenkinsfile` in the application repository.

**Create the Jenkinsfile:**

```bash
cd ../BOG001-data-lovers
git checkout master  # or main

# Create minimal Jenkinsfile (3 lines)
cat > Jenkinsfile << 'EOF'
// All pipeline logic is in jenkins-shared-library
@Library('jenkins-shared-library') _
prCheck()
EOF

git add Jenkinsfile
git commit -m "Add minimal Jenkinsfile for Jenkins CI/CD"
git push origin master
```

**Why this works:**
- The Jenkinsfile is minimal (just 3 lines)
- All actual pipeline logic lives in `vars/prCheck.groovy` in the Rick-and-Morty-Web-Jenkins-Pipeline repo
- This allows GitHub Checks API to work correctly while keeping logic centralized

---

## Step 9: Configure GitHub App Webhook (3 min)

### Configure Webhook URL in GitHub App

Go to: **Settings → Developer settings → GitHub Apps → [Your App] → General**

Scroll to **Webhook**:
- **Webhook URL:** `http://localhost:8080/github-webhook/`
- **Webhook secret:** (leave empty or set if desired)
- **Active:** ✓

**Note:** If Jenkins isn't publicly accessible, use ngrok:
```bash
ngrok http 8080
# Use the ngrok URL: https://your-id.ngrok-free.app/github-webhook/
```

### Verify Webhook Events

In the same GitHub App settings, scroll to **Permissions & events** and verify:
- **Subscribe to events:** Push, Pull request

This single webhook handles both PR checks (via Multibranch Pipeline) and main branch deployments.

---

## Step 10: Trigger Repository Scan (2 min)

After adding the Jenkinsfile, trigger Jenkins to discover branches and PRs:

Go to Jenkins → `BOG001-data-lovers-pr-checks-multibranch` → Click **"Scan Repository Now"**

**Expected result:**
- Jenkins scans the repository
- Discovers the `master` branch
- Creates a job for the master branch
- Future PRs will be automatically discovered

---

## Step 11: Test PR Pipeline (5 min)

```bash
cd BOG001-data-lovers
git checkout -b test-jenkins-checks
echo "// Test GitHub Checks" >> src/main.js
git add .
git commit -m "Test Jenkins CI with GitHub Checks"
git push origin test-jenkins-checks
```

Create PR on GitHub.

**Expected result:**
- Multibranch Pipeline automatically discovers the new PR
- Jenkins creates a job for the PR branch
- Unit tests execute
- **GitHub Checks** appear in the PR with test results

Check:
- Jenkins → `BOG001-data-lovers-pr-checks-multibranch` → PR-XX branch
- GitHub PR → Should show "Jenkins PR Check" with green checkmark

---

## Step 12: Test Main Pipeline (5 min)

Merge the PR on GitHub.

**Expected result:**
- Jenkins job `BOG001-data-lovers-main-pipeline` runs
- Deploys to GitHub Pages
- Runs Playwright tests
- Generates Allure Report

Check: Jenkins → `BOG001-data-lovers-main-pipeline` → Latest build → Allure Report

---

## Verification Checklist

- [ ] Docker images built in jenkins-docker container
- [ ] Jenkins plugins installed (including Checks API and GitHub Checks)
- [ ] Docker cloud configured in Jenkins
- [ ] Shared library configured (jenkins-shared-library)
- [ ] GitHub App credentials added with correct permissions
- [ ] Seed job created and ran successfully
- [ ] Two jobs created (Multibranch + Main pipeline)
- [ ] Jenkinsfile added to BOG001-data-lovers repository
- [ ] GitHub App webhook configured
- [ ] Multibranch Pipeline scanned and discovered branches
- [ ] Test PR created and checks appeared in GitHub PR
- [ ] GitHub Checks show green checkmark
- [ ] Main pipeline ran and Allure Report visible

---

## Troubleshooting

**Docker agent won't start:**
```bash
# Check if images exist in jenkins-docker container
docker exec jenkins-docker docker images | grep jenkins-agent

# If missing, rebuild inside the container:
cd docker
cat Dockerfile.nodejs | docker exec -i jenkins-docker docker build -t jenkins-agent-nodejs:latest -
cat Dockerfile.playwright | docker exec -i jenkins-docker docker build -t jenkins-agent-playwright:latest -
```

**Seed job fails:**
- First run? Go to Manage Jenkins → In-process Script Approval and approve pending scripts
- Check GitHub App credentials ID is `0c90ddec-1d22-41c9-ba8b-bbce09886bc7`
- Verify Job DSL plugin is installed
- Check error message for specific method signature issues

**Multibranch Pipeline not discovering PRs:**
- Click "Scan Repository Now" manually
- Check GitHub App is installed on the repository
- Verify GitHub App has correct permissions (Checks, Contents, Metadata, Pull requests)
- Check Jenkins logs: Manage Jenkins → System Log

**GitHub Checks not appearing in PR:**
1. **Most common:** Ensure `Jenkinsfile` exists in the BOG001-data-lovers repository
2. Verify GitHub App has "Checks: Read & write" AND "Commit statuses: Read & write"
3. Check that GitHub App webhook URL is configured correctly
4. Go to GitHub App → Advanced → Recent Deliveries - should show successful (green checkmark) deliveries
5. In Jenkins build console, look for "[GitHub Checks] GitHub check has been published"
6. Verify the Multibranch Pipeline discovered the PR branch

**Webhook shows 403 error:**
- This means CSRF protection is blocking the request
- Ensure webhook URL in GitHub App settings is correct
- For GitHub Apps, use: `http://your-jenkins/github-webhook/`
- Not: `http://your-jenkins/` (root path will give 403)

**Tests fail:**
- Check console output in Jenkins Multibranch Pipeline → PR branch → Build
- Verify Docker agents can start
- Check that npm dependencies install correctly

---

## Done!

You now have:
- ✅ **Multibranch Pipeline** for automatic PR discovery and checks
- ✅ **GitHub Checks API** integration - checks appear directly in PRs
- ✅ **Centralized pipeline logic** in shared library
- ✅ **Minimal Jenkinsfile** (3 lines) in application repo
- ✅ **Deployment pipeline** to GitHub Pages
- ✅ **Integration tests** with Allure reports
- ✅ **Everything as code** with Job DSL and Docker

## Architecture Summary

**Hybrid Approach Benefits:**
- Pipeline logic centralized in `Rick-and-Morty-Web-Jenkins-Pipeline/vars/`
- Minimal Jenkinsfile in `BOG001-data-lovers` (just calls shared library)
- GitHub Checks API works correctly (requires Jenkinsfile in app repo)
- Easy to standardize and maintain across multiple projects
- Follows Jenkins best practices

**To modify pipeline behavior:** Edit `vars/prCheck.groovy` in this repository

**To onboard new repositories:** Add Job DSL + minimal Jenkinsfile

For more details, see README.md "Updating Pipelines" section.
