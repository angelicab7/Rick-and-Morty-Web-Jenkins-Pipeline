# START HERE - Setup Guide

**Prerequisites:**
- Docker and Docker Compose installed
- GitHub App credentials ready

Total time: ~45 minutes

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
- **Credentials:** (select your GitHub App credentials)

Save.

---

## Step 7: Create Seed Job (5 min)

Jenkins → New Item
- **Name:** `seed-job`
- **Type:** Freestyle project

**Source Code Management:**
- Git
- Repository URL: `https://github.com/angelicab7/Rick-and-Morty-Web-Jenkins-Pipeline.git`
- Credentials: (select your GitHub App credentials)
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

This creates two jobs:
- `BOG001-data-lovers-pr-checks`
- `BOG001-data-lovers-main-pipeline`

---

## Step 8: Configure GitHub Webhook (5 min)

Go to: https://github.com/angelicab7/BOG001-data-lovers/settings/hooks

Add webhook:
- **Payload URL:** `http://localhost:8080/github-webhook/`
- **Content type:** `application/json`
- **Events:** Pushes, Pull requests
- **Active:** ✓

If Jenkins isn't publicly accessible, use ngrok:
```bash
ngrok http 8080
# Use: https://your-id.ngrok.io/github-webhook/
```

---

## Step 9: Push Configuration (3 min)

```bash
# Push this Jenkins repository
cd Rick-and-Morty-Web-Jenkins-Pipeline
git add .
git commit -m "Add Jenkins as Code configuration"
git push origin main

# Push requirements.txt to integration tests
cd ../Rick-and-Morty-API-testing
git add requirements.txt
git commit -m "Add requirements.txt"
git push origin main

# BOG001-data-lovers needs no changes
```

---

## Step 10: Test PR Pipeline (5 min)

```bash
cd ../BOG001-data-lovers
git checkout -b test-jenkins
echo "// Test" >> src/main.js
git add .
git commit -m "Test Jenkins CI"
git push origin test-jenkins
```

Create PR on GitHub.

**Expected result:**
- Jenkins job `BOG001-data-lovers-pr-checks` runs
- Unit tests execute
- Status posted to PR

Check: Jenkins → `BOG001-data-lovers-pr-checks` → Latest build

---

## Step 11: Test Main Pipeline (5 min)

Merge the PR on GitHub.

**Expected result:**
- Jenkins job `BOG001-data-lovers-main-pipeline` runs
- Deploys to GitHub Pages
- Runs Playwright tests
- Generates Allure Report

Check: Jenkins → `BOG001-data-lovers-main-pipeline` → Latest build → Allure Report

---

## Verification Checklist

- [ ] Docker images built
- [ ] Jenkins plugins installed
- [ ] Docker configured in Jenkins
- [ ] Shared library configured
- [ ] Seed job created and ran
- [ ] Two jobs created
- [ ] GitHub webhook configured
- [ ] Test PR ran successfully
- [ ] Main pipeline ran and Allure Report visible

---

## Troubleshooting

**Docker agent won't start:**
```bash
docker images | grep jenkins-agent
# If missing, rebuild: cd docker && ./build-agents.sh
```

**Seed job fails:**
- Check GitHub credentials
- Verify Job DSL plugin installed

**Webhook doesn't trigger:**
- Check webhook shows green checkmark in GitHub
- Use ngrok if testing locally

**Tests fail:**
- Check console output in Jenkins
- Verify Docker agents can start
- Check application server starts correctly

---

## Done!

You now have:
- PR checks running automatically
- Deployment pipeline to GitHub Pages
- Integration tests with Allure reports
- Everything as code

For modifications, see README.md "Updating Pipelines" section.
