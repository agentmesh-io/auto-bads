#!/bin/bash

# Auto-BADS Startup Script
# Ensures Auto-BADS uses AgentMesh's shared infrastructure (Kafka, Postgres)

set -e

echo "🚀 Starting Auto-BADS with shared infrastructure..."

# Export environment variables to use AgentMesh's services
export KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/autobads"
export SPRING_DATASOURCE_USERNAME="agentmesh"
export SPRING_DATASOURCE_PASSWORD="agentmesh123"
export SPRING_DATASOURCE_DRIVER_CLASS_NAME="org.postgresql.Driver"
export SPRING_JPA_HIBERNATE_DDL_AUTO="update"
export SPRING_JPA_DATABASE_PLATFORM="org.hibernate.dialect.PostgreSQLDialect"
export AGENTMESH_API_URL="http://localhost:8080"
export OLLAMA_BASE_URL="http://localhost:11434"
export OLLAMA_MODEL="tinyllama:1.1b"

# Don't use profile - use default with env overrides
unset SPRING_PROFILES_ACTIVE

echo "📋 Configuration:"
echo "  Kafka: $KAFKA_BOOTSTRAP_SERVERS"
echo "  Database: $SPRING_DATASOURCE_URL"
echo "  AgentMesh API: $AGENTMESH_API_URL"
echo "  Ollama: $OLLAMA_BASE_URL"
echo ""

# Check if Auto-BADS is already running
if lsof -i :8083 > /dev/null 2>&1; then
    echo "⚠️  Auto-BADS is already running on port 8083"
    echo "   Stopping existing process..."
    pkill -f "autobads.AutoBadsApplication" || true
    sleep 2
fi

# Check if required services are available
echo "🔍 Checking dependencies..."

# Check Kafka
if ! nc -z localhost 9092 2>/dev/null; then
    echo "❌ Kafka is not running on localhost:9092"
    echo "   Please start AgentMesh services first:"
    echo "   cd /Users/univers/projects/agentmesh/AgentMesh"
    echo "   docker-compose up -d"
    exit 1
fi
echo "  ✅ Kafka is running"

# Check PostgreSQL
if ! nc -z localhost 5432 2>/dev/null; then
    echo "❌ PostgreSQL is not running on localhost:5432"
    echo "   Please start AgentMesh services first"
    exit 1
fi
echo "  ✅ PostgreSQL is running"

# Check Ollama (optional)
if ! nc -z localhost 11434 2>/dev/null; then
    echo "  ⚠️  Ollama is not running (optional for testing)"
else
    echo "  ✅ Ollama is running"
fi

echo ""
echo "🔧 Building Auto-BADS..."
cd /Users/univers/projects/agentmesh/Auto-BADS
mvn clean install -DskipTests -q

if [ $? -ne 0 ]; then
    echo "❌ Build failed"
    exit 1
fi

echo "✅ Build successful"
echo ""
echo "🎯 Starting Auto-BADS..."
echo "   Logs will be written to: logs/autobads.log"
echo "   Monitor with: tail -f logs/autobads.log"
echo ""

# Create logs directory
mkdir -p logs

# Start Auto-BADS in the background
nohup mvn spring-boot:run \
    -Dspring-boot.run.jvmArguments="-Xmx1g -Xms512m" \
    > logs/autobads.log 2>&1 &

AUTOBADS_PID=$!

echo "⏳ Waiting for Auto-BADS to start..."
sleep 5

# Check if process is still running
if ! ps -p $AUTOBADS_PID > /dev/null; then
    echo "❌ Auto-BADS failed to start. Check logs/autobads.log"
    tail -50 logs/autobads.log
    exit 1
fi

# Wait for health endpoint
MAX_ATTEMPTS=30
ATTEMPT=0
while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if curl -s http://localhost:8083/actuator/health > /dev/null 2>&1; then
        echo "✅ Auto-BADS is running!"
        echo ""
        echo "📊 Service endpoints:"
        echo "   Health: http://localhost:8083/actuator/health"
        echo "   API:    http://localhost:8083/api/v1"
        echo "   Test:   http://localhost:8083/api/v1/test/publish-srs"
        echo ""
        echo "🎉 Auto-BADS started successfully (PID: $AUTOBADS_PID)"
        exit 0
    fi
    ATTEMPT=$((ATTEMPT + 1))
    sleep 2
done

echo "❌ Auto-BADS health check timeout. Check logs/autobads.log"
tail -50 logs/autobads.log
exit 1
