#!/bin/bash
# Auto-BADS Docker Deployment - Complete Automation
# This script will guide you through Docker deployment

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "╔════════════════════════════════════════════════════════════╗"
echo "║     Auto-BADS Docker Deployment - Starting Now            ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

cd "$(dirname "$0")"

# Step 1: Check Docker Desktop
echo "📋 Step 1: Checking Docker Desktop..."
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed${NC}"
    echo ""
    echo "Please install Docker Desktop:"
    echo "  1. Visit: https://www.docker.com/products/docker-desktop"
    echo "  2. Download for macOS"
    echo "  3. Install and start Docker Desktop"
    echo "  4. Run this script again"
    exit 1
fi

# Check if Docker daemon is running
if ! docker info &> /dev/null; then
    echo -e "${YELLOW}⚠️  Docker is installed but not running${NC}"
    echo ""
    echo "Starting Docker Desktop..."
    open -a Docker
    echo ""
    echo "⏳ Waiting for Docker to start (this may take 30-60 seconds)..."

    # Wait for Docker to be ready (max 2 minutes)
    COUNTER=0
    MAX_WAIT=120
    while ! docker info &> /dev/null && [ $COUNTER -lt $MAX_WAIT ]; do
        sleep 2
        COUNTER=$((COUNTER + 2))
        echo -n "."
    done
    echo ""

    if docker info &> /dev/null; then
        echo -e "${GREEN}✅ Docker is now running${NC}"
    else
        echo -e "${RED}❌ Docker failed to start within 2 minutes${NC}"
        echo ""
        echo "Please:"
        echo "  1. Manually open Docker Desktop"
        echo "  2. Wait for it to finish starting"
        echo "  3. Run this script again"
        exit 1
    fi
else
    echo -e "${GREEN}✅ Docker is running${NC}"
fi

# Step 2: Check Docker Compose
echo ""
echo "📋 Step 2: Checking Docker Compose..."
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose is not installed${NC}"
    echo ""
    echo "Docker Compose should come with Docker Desktop."
    echo "Please reinstall Docker Desktop if it's missing."
    exit 1
fi
echo -e "${GREEN}✅ Docker Compose is ready${NC}"

# Step 3: Check .env file
echo ""
echo "📋 Step 3: Checking environment configuration..."
if [ ! -f .env ]; then
    echo "Creating .env file..."
    cp .env.example .env
    echo -e "${GREEN}✅ .env file created${NC}"
    echo -e "${YELLOW}⚠️  Note: Update OPENAI_API_KEY in .env for production use${NC}"
else
    echo -e "${GREEN}✅ .env file exists${NC}"
fi

# Step 4: Choose deployment mode
echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║  Choose Deployment Mode:                                   ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
echo "1) 🚀 Development Mode (Recommended)"
echo "   - Single container"
echo "   - H2 in-memory database"
echo "   - Fast startup (~30 seconds)"
echo "   - Debug port enabled (5005)"
echo ""
echo "2) 🏢 Production Mode"
echo "   - 6 services (App, PostgreSQL, Kafka, Prometheus, Grafana)"
echo "   - Full monitoring stack"
echo "   - Production-grade (~60 seconds)"
echo ""
read -p "Enter choice [1-2] (default: 1): " CHOICE
CHOICE=${CHOICE:-1}

echo ""

# Step 5: Deploy
if [ "$CHOICE" = "1" ]; then
    echo "🚀 Deploying in Development Mode..."
    echo "═══════════════════════════════════"
    echo ""

    # Pull or build images
    echo "📦 Preparing Docker image..."
    docker-compose -f docker-compose.dev.yml pull 2>/dev/null || true

    # Start services
    echo "🎬 Starting Auto-BADS..."
    docker-compose -f docker-compose.dev.yml up -d

    COMPOSE_FILE="docker-compose.dev.yml"
    SERVICE_NAME="autobads-dev"
    WAIT_TIME=30

elif [ "$CHOICE" = "2" ]; then
    echo "🏢 Deploying in Production Mode..."
    echo "═══════════════════════════════════"
    echo ""

    # Pull or build images
    echo "📦 Preparing Docker images..."
    docker-compose pull 2>/dev/null || true

    # Start all services
    echo "🎬 Starting all services..."
    docker-compose up -d

    COMPOSE_FILE="docker-compose.yml"
    SERVICE_NAME="autobads"
    WAIT_TIME=60

else
    echo -e "${RED}❌ Invalid choice${NC}"
    exit 1
fi

# Step 6: Wait for startup
echo ""
echo "⏳ Waiting for services to start ($WAIT_TIME seconds)..."
sleep $WAIT_TIME

# Step 7: Check health
echo ""
echo "📋 Checking application health..."
MAX_RETRIES=10
RETRY=0

while [ $RETRY -lt $MAX_RETRIES ]; do
    if curl -s -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Application is healthy!${NC}"
        break
    fi
    RETRY=$((RETRY + 1))
    if [ $RETRY -lt $MAX_RETRIES ]; then
        echo "Waiting... (attempt $RETRY/$MAX_RETRIES)"
        sleep 3
    fi
done

if [ $RETRY -eq $MAX_RETRIES ]; then
    echo -e "${YELLOW}⚠️  Application is still starting...${NC}"
    echo ""
    echo "Check logs with:"
    echo "  docker-compose -f $COMPOSE_FILE logs -f $SERVICE_NAME"
    echo ""
fi

# Step 8: Show results
echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║               🎉 Deployment Complete!                      ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

if [ "$CHOICE" = "1" ]; then
    echo "📡 Access Points:"
    echo "   Application:  http://localhost:8080"
    echo "   Health Check: http://localhost:8080/actuator/health"
    echo "   Metrics:      http://localhost:8080/actuator/metrics"
    echo "   Debug Port:   localhost:5005"
else
    echo "📡 Access Points:"
    echo "   Auto-BADS:    http://localhost:8080"
    echo "   Health:       http://localhost:8080/actuator/health"
    echo "   Prometheus:   http://localhost:9090"
    echo "   Grafana:      http://localhost:3000 (admin/admin)"
    echo "   PostgreSQL:   localhost:5432"
fi

echo ""
echo "🧪 Test the API:"
echo "   curl http://localhost:8080/actuator/health"
echo ""
echo '   curl -X POST http://localhost:8080/api/v1/ideas \'
echo '     -H "Content-Type: application/json" \'
echo '     -d '"'"'{"idea": "Your business idea here"}'"'"
echo ""
echo "📋 Useful Commands:"
echo "   View logs:    docker-compose -f $COMPOSE_FILE logs -f"
echo "   Check status: docker-compose -f $COMPOSE_FILE ps"
echo "   Stop:         docker-compose -f $COMPOSE_FILE stop"
echo "   Remove:       docker-compose -f $COMPOSE_FILE down"
echo ""
echo "📚 Documentation:"
echo "   Complete guide: cat DOCKER.md"
echo "   Quick start:    cat START-HERE.md"
echo ""
echo "🎉 Auto-BADS is now running on Docker!"

