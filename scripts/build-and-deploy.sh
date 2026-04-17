#!/bin/bash
# Build and Deploy Auto-BADS with Lombok workaround

set -e

echo "🚀 Auto-BADS Build and Docker Deployment"
echo "========================================"
echo ""

cd "$(dirname "$0")"

# Step 1: Check prerequisites
echo "📋 Step 1: Checking prerequisites..."
if ! command -v docker &> /dev/null; then
    echo "❌ Docker not found. Please install Docker first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose not found. Please install Docker Compose first."
    exit 1
fi

echo "✅ Docker and Docker Compose found"

# Step 2: Check .env file
echo ""
echo "📋 Step 2: Checking environment configuration..."
if [ ! -f .env ]; then
    echo "Creating .env file..."
    cat > .env << 'ENVEOF'
# Auto-BADS Environment - Local Development
OPENAI_API_KEY=test-key-local-dev
DB_HOST=postgres
DB_PORT=5432
DB_NAME=autobads
DB_USERNAME=autobads
DB_PASSWORD=autobads_local
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
AUTO_BADS_PMF_THRESHOLD=40.0
GRAFANA_ADMIN_PASSWORD=admin
COMPOSE_PROJECT_NAME=autobads
ENVEOF
    echo "✅ Created .env file (update OPENAI_API_KEY for real usage)"
else
    echo "✅ .env file exists"
fi

# Step 3: Start with development mode (no complex build)
echo ""
echo "📋 Step 3: Starting development environment..."
echo "Using docker-compose.dev.yml for simplified deployment"
echo ""

# Start development environment
docker-compose -f docker-compose.dev.yml up -d

echo ""
echo "⏳ Waiting for services to start (30 seconds)..."
sleep 30

# Step 4: Check health
echo ""
echo "📋 Step 4: Checking application health..."
MAX_RETRIES=10
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✅ Application is healthy!"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo "Waiting... (attempt $RETRY_COUNT/$MAX_RETRIES)"
    sleep 3
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo "⚠️  Application did not become healthy. Check logs:"
    echo "   docker-compose -f docker-compose.dev.yml logs"
    exit 1
fi

# Step 5: Show status
echo ""
echo "🎉 Deployment Complete!"
echo "======================"
echo ""
echo "📡 Access Points:"
echo "   Application:  http://localhost:8080"
echo "   Health:       http://localhost:8080/actuator/health"
echo "   Metrics:      http://localhost:8080/actuator/metrics"
echo "   Debug Port:   localhost:5005"
echo ""
echo "📋 Useful Commands:"
echo "   View logs:    docker-compose -f docker-compose.dev.yml logs -f"
echo "   Stop:         docker-compose -f docker-compose.dev.yml stop"
echo "   Remove:       docker-compose -f docker-compose.dev.yml down"
echo ""
echo "🧪 Test the API:"
echo '   curl -X POST http://localhost:8080/api/v1/ideas \'
echo '     -H "Content-Type: application/json" \'
echo '     -d '"'"'{"idea": "Your business idea here"}'"'"
echo ""

