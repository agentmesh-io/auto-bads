#!/bin/bash
# Auto-BADS Docker Deployment - Patient Mode
# This script waits properly for image builds and container startup

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "╔════════════════════════════════════════════════════════════╗"
echo "║     Auto-BADS Docker Deployment - Patient Mode            ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

cd "$(dirname "$0")"

# Step 1: Check Docker Desktop
echo "📋 Step 1: Checking Docker Desktop..."
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed${NC}"
    exit 1
fi

if ! docker info &> /dev/null; then
    echo -e "${YELLOW}⚠️  Docker is not running. Starting...${NC}"
    open -a Docker
    echo "⏳ Waiting for Docker (max 2 minutes)..."
    COUNTER=0
    while ! docker info &> /dev/null && [ $COUNTER -lt 120 ]; do
        sleep 2
        COUNTER=$((COUNTER + 2))
        echo -n "."
    done
    echo ""
    if ! docker info &> /dev/null; then
        echo -e "${RED}❌ Docker failed to start${NC}"
        exit 1
    fi
fi
echo -e "${GREEN}✅ Docker is running${NC}"

# Step 2: Check Docker Compose
echo ""
echo "📋 Step 2: Checking Docker Compose..."
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose not found${NC}"
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
echo "   - Fast startup (~2-5 minutes first build)"
echo "   - Debug port enabled (5005)"
echo ""
echo "2) 🏢 Production Mode"
echo "   - 6 services (App, PostgreSQL, Kafka, Prometheus, Grafana)"
echo "   - Full monitoring stack"
echo "   - Production-grade (~5-10 minutes first build)"
echo ""
read -p "Enter choice [1-2] (default: 1): " CHOICE
CHOICE=${CHOICE:-1}

echo ""

# Step 5: Deploy with proper build time
if [ "$CHOICE" = "1" ]; then
    echo "🚀 Deploying in Development Mode..."
    echo "═══════════════════════════════════"
    echo ""

    COMPOSE_FILE="docker-compose.dev.yml"
    SERVICE_NAME="autobads-dev"

    echo "📦 Building Docker image (this may take 3-5 minutes)..."
    echo "Please be patient while Maven downloads dependencies and compiles..."
    echo ""

    # Build with output
    docker-compose -f $COMPOSE_FILE build --progress=plain 2>&1 | grep -E "Step|Building|Successfully|ERROR" || docker-compose -f $COMPOSE_FILE build

    echo ""
    echo "🎬 Starting Auto-BADS container..."
    docker-compose -f $COMPOSE_FILE up -d

    WAIT_TIME=60

elif [ "$CHOICE" = "2" ]; then
    echo "🏢 Deploying in Production Mode..."
    echo "═══════════════════════════════════"
    echo ""

    COMPOSE_FILE="docker-compose.yml"
    SERVICE_NAME="autobads"

    echo "📦 Building/pulling Docker images (this may take 5-10 minutes)..."
    echo "Please be patient while:"
    echo "  - Maven downloads dependencies"
    echo "  - Application compiles"
    echo "  - Supporting services are pulled"
    echo ""

    # Build with output
    docker-compose -f $COMPOSE_FILE build --progress=plain 2>&1 | grep -E "Step|Building|Successfully|ERROR" || docker-compose -f $COMPOSE_FILE build

    echo ""
    echo "🎬 Starting all services..."
    docker-compose -f $COMPOSE_FILE up -d

    WAIT_TIME=90

else
    echo -e "${RED}❌ Invalid choice${NC}"
    exit 1
fi

# Step 6: Wait for startup with progress
echo ""
echo "⏳ Waiting for services to start (checking every 5 seconds for up to 3 minutes)..."
echo ""

MAX_WAIT=180  # 3 minutes
ELAPSED=0
HEALTH_CHECKS=0

while [ $ELAPSED -lt $MAX_WAIT ]; do
    # Check if container is running
    if docker-compose -f $COMPOSE_FILE ps | grep -q "Up"; then
        echo -e "${GREEN}✓${NC} Container is running (${ELAPSED}s)"

        # Try health check
        if curl -s -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
            HEALTH_CHECKS=$((HEALTH_CHECKS + 1))
            if [ $HEALTH_CHECKS -ge 2 ]; then
                echo -e "${GREEN}✓${NC} Health check passed!"
                break
            fi
            echo -e "${GREEN}✓${NC} Health check responding (${ELAPSED}s)"
        else
            echo -e "${YELLOW}⏳${NC} Waiting for application startup... (${ELAPSED}s)"
        fi
    else
        echo -e "${YELLOW}⏳${NC} Waiting for container to start... (${ELAPSED}s)"
    fi

    sleep 5
    ELAPSED=$((ELAPSED + 5))
done

echo ""

# Step 7: Final health check
if curl -s -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Application is healthy and ready!${NC}"
else
    echo -e "${YELLOW}⚠️  Application is still starting...${NC}"
    echo ""
    echo "Check logs with:"
    echo "  docker-compose -f $COMPOSE_FILE logs -f $SERVICE_NAME"
    echo ""
    echo "Check container status:"
    echo "  docker-compose -f $COMPOSE_FILE ps"
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
echo '     -d '"'"'{"idea": "AI-powered fitness app"}'"'"
echo ""
echo "📋 Useful Commands:"
echo "   View logs:    docker-compose -f $COMPOSE_FILE logs -f"
echo "   Check status: docker-compose -f $COMPOSE_FILE ps"
echo "   Stop:         docker-compose -f $COMPOSE_FILE stop"
echo "   Remove:       docker-compose -f $COMPOSE_FILE down"
echo "   Rebuild:      docker-compose -f $COMPOSE_FILE build --no-cache"
echo ""
echo "📚 Documentation:"
echo "   Complete guide: cat DOCKER.md"
echo "   Quick start:    cat START-HERE.md"
echo ""
echo "🎉 Auto-BADS is deployed on Docker!"

