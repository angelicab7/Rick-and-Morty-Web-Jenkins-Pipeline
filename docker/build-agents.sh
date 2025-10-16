#!/bin/bash
# build-agents.sh
# Build Jenkins agent Docker images for Node.js and Playwright

set -e

echo "Building Jenkins agent images..."

# Build Node.js agent
echo "📦 Building Node.js agent image..."
docker build -t jenkins-agent-nodejs:latest -f Dockerfile.nodejs .

# Build Playwright agent
echo "📦 Building Playwright agent image..."
docker build -t jenkins-agent-playwright:latest -f Dockerfile.playwright .

echo "✅ All agent images built successfully!"
echo ""
echo "Available images:"
docker images | grep jenkins-agent

echo ""
echo "To use these agents in Jenkins pipelines, they will be automatically pulled"
echo "when the pipeline runs with the Docker agent configuration."
