# Auto-BADS Docker Deployment Guide

## 🐳 Overview

This guide covers deploying Auto-BADS using Docker and Docker Compose, with full support for PostgreSQL, Kafka, Prometheus, and Grafana.

## 📦 What's Included

The Docker setup includes:

- **Auto-BADS Application** - Main Spring Boot application
- **PostgreSQL** - Production database with initialization scripts
- **Apache Kafka + Zookeeper** - Event streaming platform
- **Prometheus** - Metrics collection
- **Grafana** - Metrics visualization and dashboards

## 🚀 Quick Start

### Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- OpenAI API Key

### 1. Set Up Environment

```bash
# Copy environment template
cp .env.example .env

# Edit .env and add your OpenAI API key
# OPENAI_API_KEY=sk-your-api-key-here
```

### 2. Start All Services

```bash
# Build and start all services
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f autobads
```

### 3. Verify Deployment

```bash
# Check application health
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP","groups":["liveness","readiness"]}
```

## 🛠️ Development Mode

For development with minimal dependencies:

```bash
# Start only the application with H2 database
docker-compose -f docker-compose.dev.yml up -d

# Enable remote debugging on port 5005
```

## 📊 Access Services

| Service | URL | Credentials |
|---------|-----|-------------|
| **Auto-BADS API** | http://localhost:8080 | - |
| **H2 Console** | http://localhost:8080/h2-console | `jdbc:h2:mem:autobads` / sa / (no password) |
| **PostgreSQL** | localhost:5432 | autobads / autobads_password |
| **Prometheus** | http://localhost:9090 | - |
| **Grafana** | http://localhost:3000 | admin / admin |
| **Kafka** | localhost:9092 | - |

## 🧪 Testing the Deployment

### Submit a Business Idea

```bash
curl -X POST http://localhost:8080/api/v1/ideas \
  -H "Content-Type: application/json" \
  -d '{
    "idea": "An AI-powered platform for automated business analysis"
  }'
```

### Check Idea Status

```bash
# Get idea by ID
curl http://localhost:8080/api/v1/ideas/{ideaId}
```

### View Metrics

```bash
# Application metrics
curl http://localhost:8080/actuator/metrics

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

## 🔧 Docker Commands Reference

### Container Management

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose stop

# Restart a service
docker-compose restart autobads

# View logs
docker-compose logs -f autobads

# Execute command in container
docker-compose exec autobads sh

# Remove all containers and volumes
docker-compose down -v
```

### Build and Update

```bash
# Rebuild application image
docker-compose build autobads

# Rebuild and restart
docker-compose up -d --build autobads

# Pull latest images
docker-compose pull
```

### Database Operations

```bash
# Access PostgreSQL
docker-compose exec postgres psql -U autobads -d autobads

# Backup database
docker-compose exec postgres pg_dump -U autobads autobads > backup.sql

# Restore database
docker-compose exec -T postgres psql -U autobads autobads < backup.sql

# View database logs
docker-compose logs -f postgres
```

## 📈 Monitoring and Observability

### Prometheus

1. Access Prometheus UI: http://localhost:9090
2. Query metrics:
   - `http_server_requests_seconds_count` - Request count
   - `jvm_memory_used_bytes` - JVM memory usage
   - `system_cpu_usage` - CPU usage

### Grafana Dashboards

1. Access Grafana: http://localhost:3000
2. Login with admin/admin
3. Import dashboards:
   - Spring Boot 2.1 Statistics (ID: 10280)
   - JVM (Micrometer) (ID: 4701)

### Application Logs

```bash
# Tail logs in real-time
docker-compose logs -f autobads

# View last 100 lines
docker-compose logs --tail=100 autobads

# Save logs to file
docker-compose logs autobads > autobads.log
```

## 🔒 Security Configuration

### Production Settings

Update `.env` file:

```bash
# Use strong passwords
DB_PASSWORD=your-strong-password-here
GRAFANA_ADMIN_PASSWORD=your-strong-password-here

# Use production OpenAI API key
OPENAI_API_KEY=sk-your-production-key

# Optional: Configure additional security
```

### Network Security

```bash
# Expose only necessary ports
# Edit docker-compose.yml and remove port mappings for internal services
```

## 🚀 Production Deployment

### Using Docker Compose (Production)

```bash
# Use production compose file
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Or set production environment
export SPRING_PROFILES_ACTIVE=prod
docker-compose up -d
```

### Using Docker Swarm

```bash
# Initialize swarm
docker swarm init

# Deploy stack
docker stack deploy -c docker-compose.yml autobads

# Check services
docker stack services autobads

# Scale application
docker service scale autobads_autobads=3
```

### Using Kubernetes

See `k8s/` directory for Kubernetes manifests (to be added).

## 🐛 Troubleshooting

### Application Won't Start

```bash
# Check logs
docker-compose logs autobads

# Common issues:
# 1. OpenAI API key not set
echo $OPENAI_API_KEY

# 2. Port already in use
lsof -i :8080

# 3. Database connection issues
docker-compose logs postgres
```

### Out of Memory

```bash
# Increase JVM memory in docker-compose.yml:
environment:
  - JAVA_OPTS=-Xmx2g -Xms1g

# Restart services
docker-compose restart autobads
```

### Database Connection Issues

```bash
# Check if PostgreSQL is healthy
docker-compose ps postgres

# Test connection
docker-compose exec postgres pg_isready -U autobads

# Check connection string
docker-compose exec autobads env | grep DATASOURCE
```

### Kafka Connection Issues

```bash
# Check Kafka status
docker-compose logs kafka

# Check Zookeeper
docker-compose logs zookeeper

# Restart Kafka services
docker-compose restart zookeeper kafka
```

## 🔄 Updates and Maintenance

### Update Application

```bash
# Pull latest code
git pull

# Rebuild and restart
docker-compose up -d --build autobads
```

### Backup and Restore

```bash
# Backup everything
docker-compose exec postgres pg_dump -U autobads autobads > backup-$(date +%Y%m%d).sql

# Restore
docker-compose exec -T postgres psql -U autobads autobads < backup-20231031.sql
```

### Clean Up

```bash
# Remove stopped containers
docker-compose down

# Remove all data (WARNING: deletes volumes)
docker-compose down -v

# Remove unused images
docker image prune -a

# Complete cleanup
docker system prune -a --volumes
```

## 📊 Performance Tuning

### JVM Tuning

Edit `docker-compose.yml`:

```yaml
environment:
  - JAVA_OPTS=-Xmx2g -Xms1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

### Database Tuning

Edit `docker-compose.yml`:

```yaml
postgres:
  command: postgres -c max_connections=200 -c shared_buffers=256MB
```

### Container Resources

```yaml
autobads:
  deploy:
    resources:
      limits:
        cpus: '2'
        memory: 2G
      reservations:
        cpus: '1'
        memory: 1G
```

## 🌐 Environment Variables Reference

| Variable | Description | Default |
|----------|-------------|---------|
| `OPENAI_API_KEY` | OpenAI API key (required) | - |
| `SPRING_PROFILES_ACTIVE` | Spring profile | `docker` |
| `SPRING_DATASOURCE_URL` | Database URL | `jdbc:postgresql://postgres:5432/autobads` |
| `SPRING_DATASOURCE_USERNAME` | Database user | `autobads` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `autobads_password` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka servers | `kafka:9092` |
| `AUTO_BADS_RECOMMENDATION_PMF_THRESHOLD` | PMF threshold | `40.0` |
| `JAVA_OPTS` | JVM options | `-Xmx1g -Xms512m` |

## 📝 Next Steps

1. ✅ Configure monitoring alerts in Grafana
2. ✅ Set up automated backups
3. ✅ Configure SSL/TLS certificates
4. ✅ Set up CI/CD pipeline
5. ✅ Configure log aggregation (ELK stack)
6. ✅ Add health check endpoints
7. ✅ Configure auto-scaling

## 🆘 Getting Help

- **Logs**: `docker-compose logs -f`
- **Health**: `curl http://localhost:8080/actuator/health`
- **Container shell**: `docker-compose exec autobads sh`
- **Database shell**: `docker-compose exec postgres psql -U autobads`

---

**Success Indicators**:
- ✅ All services show "Up" in `docker-compose ps`
- ✅ Health endpoint returns UP status
- ✅ Can submit ideas via API
- ✅ Database connections successful
- ✅ Metrics visible in Prometheus/Grafana

Happy Deploying! 🚀

