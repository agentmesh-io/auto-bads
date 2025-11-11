#!/bin/bash
# Auto-BADS Docker Deployment Script

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Functions
print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

check_prerequisites() {
    print_info "Checking prerequisites..."

    # Check Docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    print_success "Docker installed"

    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi
    print_success "Docker Compose installed"

    # Check .env file
    if [ ! -f .env ]; then
        print_error ".env file not found. Creating from .env.example..."
        if [ -f .env.example ]; then
            cp .env.example .env
            print_info "Please edit .env file and add your OpenAI API key"
            exit 1
        else
            print_error ".env.example not found"
            exit 1
        fi
    fi

    # Check OpenAI API key
    source .env
    if [ -z "$OPENAI_API_KEY" ] || [ "$OPENAI_API_KEY" = "sk-your-openai-api-key-here" ]; then
        print_error "OpenAI API key not configured in .env file"
        exit 1
    fi
    print_success "OpenAI API key configured"
}

build_application() {
    print_info "Building Docker image..."
    docker-compose build autobads
    print_success "Docker image built successfully"
}

start_services() {
    print_info "Starting all services..."
    docker-compose up -d
    print_success "Services started"
}

wait_for_health() {
    print_info "Waiting for application to be healthy..."

    MAX_RETRIES=30
    RETRY_COUNT=0

    while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
        if curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
            print_success "Application is healthy!"
            return 0
        fi

        RETRY_COUNT=$((RETRY_COUNT + 1))
        echo -n "."
        sleep 2
    done

    print_error "Application failed to become healthy"
    print_info "Check logs with: docker-compose logs autobads"
    return 1
}

show_status() {
    echo ""
    print_info "Service Status:"
    docker-compose ps

    echo ""
    print_info "Access Points:"
    echo "  📡 Auto-BADS API:    http://localhost:8080"
    echo "  📊 Prometheus:       http://localhost:9090"
    echo "  📈 Grafana:          http://localhost:3000 (admin/admin)"
    echo "  💾 PostgreSQL:       localhost:5432 (autobads/autobads_password)"
    echo ""

    print_info "Quick Test:"
    echo '  curl -X POST http://localhost:8080/api/v1/ideas \'
    echo '    -H "Content-Type: application/json" \'
    echo '    -d '"'"'{"idea": "Your business idea here"}'"'"
    echo ""
}

# Main deployment flow
main() {
    echo "🚀 Auto-BADS Docker Deployment"
    echo "================================"
    echo ""

    # Check prerequisites
    check_prerequisites

    # Build application
    build_application

    # Start services
    start_services

    # Wait for health
    if wait_for_health; then
        show_status
        print_success "Deployment completed successfully! 🎉"
    else
        print_error "Deployment failed. Check logs for details."
        exit 1
    fi
}

# Handle command line arguments
case "${1:-deploy}" in
    deploy)
        main
        ;;
    stop)
        print_info "Stopping all services..."
        docker-compose stop
        print_success "Services stopped"
        ;;
    down)
        print_info "Removing all containers..."
        docker-compose down
        print_success "Containers removed"
        ;;
    logs)
        docker-compose logs -f autobads
        ;;
    status)
        docker-compose ps
        ;;
    rebuild)
        print_info "Rebuilding and restarting..."
        docker-compose up -d --build autobads
        wait_for_health
        print_success "Rebuild completed"
        ;;
    *)
        echo "Usage: $0 {deploy|stop|down|logs|status|rebuild}"
        exit 1
        ;;
esac
# Multi-stage Dockerfile for Auto-BADS

# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-22 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application (skip tests for faster builds)
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:22-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S autobads && adduser -S autobads -G autobads

# Copy JAR from builder stage
COPY --from=builder /app/target/Auto-BADS-*.jar /app/auto-bads.jar

# Change ownership
RUN chown -R autobads:autobads /app

# Switch to non-root user
USER autobads

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM options for container
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/auto-bads.jar"]

