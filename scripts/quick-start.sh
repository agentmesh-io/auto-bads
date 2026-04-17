#!/bin/bash
# Quick Start Script for Auto-BADS Docker Deployment

echo "🚀 Auto-BADS Quick Start"
echo "========================"
echo ""

# Check if .env exists
if [ ! -f .env ]; then
    echo "Creating .env file from template..."
    cp .env.example .env
    echo ""
    echo "⚠️  Please edit .env file and add your OpenAI API key:"
    echo "   OPENAI_API_KEY=sk-your-api-key-here"
    echo ""
    echo "Then run this script again."
    exit 1
fi

# Check if OpenAI API key is set
source .env
if [ -z "$OPENAI_API_KEY" ] || [ "$OPENAI_API_KEY" = "sk-your-openai-api-key-here" ]; then
    echo "❌ OpenAI API key not configured in .env file"
    echo "   Please edit .env and add your API key"
    exit 1
fi

echo "✅ Configuration looks good!"
echo ""

# Ask user which deployment mode
echo "Choose deployment mode:"
echo "1) Development (minimal - app only with H2)"
echo "2) Production (full stack with PostgreSQL, Kafka, monitoring)"
echo ""
read -p "Enter choice [1-2]: " choice

case $choice in
    1)
        echo ""
        echo "Starting development environment..."
        docker-compose -f docker-compose.dev.yml up -d
        echo ""
        echo "✅ Development environment started!"
        echo "   Access: http://localhost:8080"
        echo "   Debug: localhost:5005"
        ;;
    2)
        echo ""
        echo "Starting production environment..."
        docker-compose up -d
        echo ""
        echo "✅ Production environment started!"
        echo ""
        echo "Access Points:"
        echo "  📡 Auto-BADS API:    http://localhost:8080"
        echo "  📊 Prometheus:       http://localhost:9090"
        echo "  📈 Grafana:          http://localhost:3000 (admin/admin)"
        echo "  💾 PostgreSQL:       localhost:5432"
        ;;
    *)
        echo "Invalid choice"
        exit 1
        ;;
esac

echo ""
echo "View logs:        docker-compose logs -f autobads"
echo "Check status:     docker-compose ps"
echo "Stop services:    docker-compose stop"
echo "Remove all:       docker-compose down -v"
echo ""

