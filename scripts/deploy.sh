#!/bin/bash
# Auto-BADS Final Deployment Script
# Handles all scenarios and provides clear guidance

set -e

echo "╔════════════════════════════════════════════════════════════╗"
echo "║        Auto-BADS Deployment Assistant                      ║"
echo "║        Complete AI Business Analysis System                ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

cd "$(dirname "$0")"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_success() { echo -e "${GREEN}✅ $1${NC}"; }
print_error() { echo -e "${RED}❌ $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
print_info() { echo -e "${BLUE}ℹ️  $1${NC}"; }

# Check prerequisites
echo "📋 Checking prerequisites..."
echo ""

# Check Docker
if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed"
    echo "   Please install Docker Desktop from: https://www.docker.com/products/docker-desktop"
    exit 1
fi

# Check if Docker daemon is running
if ! docker info &> /dev/null; then
    print_warning "Docker daemon is not running"
    echo ""
    echo "   Please start Docker:"
    echo "   - macOS: Open Docker Desktop application"
    echo "   - Linux: sudo systemctl start docker"
    echo ""
    echo "   Then run this script again."
    exit 1
fi

print_success "Docker is installed and running"

# Check Docker Compose
if ! command -v docker-compose &> /dev/null; then
    print_error "Docker Compose is not installed"
    echo "   Please install Docker Compose"
    exit 1
fi

print_success "Docker Compose is installed"
echo ""

# Check for .env file
if [ ! -f .env ]; then
    print_info "Creating .env file..."
    cp .env.example .env
    print_success ".env file created"
    print_warning "Update OPENAI_API_KEY in .env for production use"
    echo ""
fi

# Present options
echo "╔════════════════════════════════════════════════════════════╗"
echo "║  Choose Deployment Mode:                                   ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
echo "1) 🚀 Development Mode (Recommended for Testing)"
echo "   - Single container with H2 database"
echo "   - Fast startup (~30 seconds)"
echo "   - Debug port enabled (5005)"
echo "   - Minimal resource usage"
echo ""
echo "2) 🏢 Production Mode (Full Stack)"
echo "   - 6 services (App, PostgreSQL, Kafka, Prometheus, Grafana)"
echo "   - Complete monitoring and observability"
echo "   - Production-grade setup"
echo "   - ~60 second startup"
echo ""
echo "3) 📊 Status Check (View current deployment)"
echo ""
echo "4) 🛑 Stop All Services"
echo ""
echo "5) 🗑️  Remove Everything (including data)"
echo ""
echo "6) 📚 Show Documentation Links"
echo ""
read -p "Enter your choice [1-6]: " choice
echo ""

case $choice in
    1)
        echo "🚀 Starting Development Mode..."
        echo "════════════════════════════════"
        echo ""

        docker-compose -f docker-compose.dev.yml up -d

        echo ""
        print_info "Waiting for application to start (30 seconds)..."
        sleep 30

        # Check health
        if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
            print_success "Application is running!"
            echo ""
            echo "╔════════════════════════════════════════════════════════════╗"
            echo "║  Deployment Successful!                                    ║"
            echo "╚════════════════════════════════════════════════════════════╝"
            echo ""
            echo "📡 Access Points:"
            echo "   Application:  http://localhost:8080"
            echo "   Health Check: http://localhost:8080/actuator/health"
            echo "   Metrics:      http://localhost:8080/actuator/metrics"
            echo "   Debug Port:   localhost:5005"
            echo ""
            echo "🧪 Test the API:"
            echo "   curl -X POST http://localhost:8080/api/v1/ideas \\"
            echo "     -H \"Content-Type: application/json\" \\"
            echo "     -d '{\"idea\": \"Your business idea here\"}'"
            echo ""
            echo "📋 Useful Commands:"
            echo "   View logs:    docker-compose -f docker-compose.dev.yml logs -f"
            echo "   Stop:         docker-compose -f docker-compose.dev.yml stop"
            echo "   Remove:       docker-compose -f docker-compose.dev.yml down"
        else
            print_warning "Application is starting... Check logs with:"
            echo "   docker-compose -f docker-compose.dev.yml logs -f"
        fi
        ;;

    2)
        echo "🏢 Starting Production Mode..."
        echo "══════════════════════════════"
        echo ""

        print_info "This will start 6 services:"
        echo "   1. Auto-BADS Application"
        echo "   2. PostgreSQL Database"
        echo "   3. Apache Kafka"
        echo "   4. Zookeeper"
        echo "   5. Prometheus"
        echo "   6. Grafana"
        echo ""

        read -p "Continue? [y/N]: " confirm
        if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
            echo "Cancelled."
            exit 0
        fi

        docker-compose up -d

        echo ""
        print_info "Waiting for services to start (60 seconds)..."
        sleep 60

        # Check health
        if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
            print_success "All services are running!"
            echo ""
            echo "╔════════════════════════════════════════════════════════════╗"
            echo "║  Full Stack Deployment Successful!                         ║"
            echo "╚════════════════════════════════════════════════════════════╝"
            echo ""
            echo "📡 Access Points:"
            echo "   Auto-BADS:    http://localhost:8080"
            echo "   Prometheus:   http://localhost:9090"
            echo "   Grafana:      http://localhost:3000 (admin/admin)"
            echo "   PostgreSQL:   localhost:5432"
            echo ""
            echo "📊 View Status:"
            echo "   docker-compose ps"
            echo ""
            echo "📋 View Logs:"
            echo "   docker-compose logs -f autobads"
        else
            print_warning "Services are starting... Check with:"
            echo "   docker-compose ps"
            echo "   docker-compose logs -f"
        fi
        ;;

    3)
        echo "📊 Current Deployment Status"
        echo "════════════════════════════"
        echo ""

        # Check dev mode
        if docker-compose -f docker-compose.dev.yml ps | grep -q "Up"; then
            echo "🚀 Development Mode: RUNNING"
            docker-compose -f docker-compose.dev.yml ps
        else
            echo "🚀 Development Mode: NOT RUNNING"
        fi

        echo ""

        # Check production mode
        if docker-compose ps | grep -q "Up"; then
            echo "🏢 Production Mode: RUNNING"
            docker-compose ps
        else
            echo "🏢 Production Mode: NOT RUNNING"
        fi

        echo ""

        # Try health check
        if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
            print_success "Application health: UP"
            echo "   http://localhost:8080/actuator/health"
        else
            print_info "Application not responding on port 8080"
        fi
        ;;

    4)
        echo "🛑 Stopping All Services..."
        echo "════════════════════════════"
        echo ""

        docker-compose -f docker-compose.dev.yml stop 2>/dev/null || true
        docker-compose stop 2>/dev/null || true

        print_success "All services stopped"
        echo ""
        echo "To start again, run this script and choose option 1 or 2"
        ;;

    5)
        echo "🗑️  Remove Everything"
        echo "═══════════════════════"
        echo ""
        print_warning "This will remove all containers, volumes, and data!"
        read -p "Are you sure? [y/N]: " confirm

        if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
            docker-compose -f docker-compose.dev.yml down -v 2>/dev/null || true
            docker-compose down -v 2>/dev/null || true
            print_success "Everything removed"
        else
            echo "Cancelled."
        fi
        ;;

    6)
        echo "📚 Documentation Links"
        echo "═══════════════════════"
        echo ""
        echo "Available Documentation Files:"
        echo ""
        echo "1. README.md"
        echo "   - Complete architecture overview"
        echo "   - Features and capabilities"
        echo "   - Technology stack"
        echo ""
        echo "2. DOCKER.md"
        echo "   - Complete Docker deployment guide"
        echo "   - Troubleshooting steps"
        echo "   - Production deployment"
        echo ""
        echo "3. BUILD-STATUS.md"
        echo "   - Current build status"
        echo "   - Known issues and workarounds"
        echo "   - Deployment options"
        echo ""
        echo "4. DEPLOYMENT-CHECKLIST.md"
        echo "   - Pre-deployment checklist"
        echo "   - Post-deployment verification"
        echo "   - Security checklist"
        echo ""
        echo "5. SETUP.md"
        echo "   - Local development setup"
        echo "   - IDE configuration"
        echo "   - Troubleshooting"
        echo ""
        echo "To read any file:"
        echo "   cat [filename]"
        echo "   Or open in your editor"
        ;;

    *)
        print_error "Invalid choice"
        exit 1
        ;;
esac

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "For more help, see:"
echo "  - BUILD-STATUS.md (current status and options)"
echo "  - DOCKER.md (complete Docker guide)"
echo "  - README.md (architecture overview)"
echo "═══════════════════════════════════════════════════════════"

