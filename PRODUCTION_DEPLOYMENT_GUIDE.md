# Auto-BADS Production Deployment Guide

## Overview
This guide covers deploying Auto-BADS to production environments with Docker, Kubernetes, or standalone deployment.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Environment Configuration](#environment-configuration)
3. [Docker Deployment](#docker-deployment)
4. [Kubernetes Deployment](#kubernetes-deployment)
5. [Health Checks](#health-checks)
6. [Monitoring](#monitoring)
7. [Security](#security)
8. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Services
- **PostgreSQL 14+** - Production database
- **Kafka 3.0+** - Event streaming
- **OpenAI API** - LLM service access
- **AgentMesh** - Orchestration layer (optional)

### System Requirements
- **CPU**: 2+ cores recommended
- **RAM**: 2GB minimum, 4GB recommended
- **Disk**: 10GB+ available space
- **Java**: 21+ (if not using Docker)

---

## Environment Configuration

### Required Environment Variables

```bash
# Database Configuration
export DB_HOST=postgres.example.com
export DB_PORT=5432
export DB_NAME=autobads
export DB_USERNAME=autobads_user
export DB_PASSWORD=<secure-password>

# Kafka Configuration
export KAFKA_BOOTSTRAP_SERVERS=kafka1:9092,kafka2:9092,kafka3:9092

# OpenAI Configuration
export OPENAI_API_KEY=sk-...

# AgentMesh Integration (optional)
export AGENTMESH_API_URL=http://agentmesh:8080
export AGENTMESH_API_KEY=<api-key>

# Application Configuration
export SERVER_PORT=8083
export SPRING_PROFILES_ACTIVE=prod
```

### Optional Configuration

```bash
# Database Pool Sizing
export DB_POOL_MAX_SIZE=20
export DB_POOL_MIN_IDLE=5

# JVM Memory
export JAVA_OPTS="-Xmx2g -Xms1g"

# Logging
export LOG_LEVEL_ROOT=WARN
export LOG_LEVEL_APP=INFO
```

---

## Docker Deployment

### 1. Build Docker Image

```bash
cd /path/to/Auto-BADS

# Build with tests
docker build -t autobads:latest .

# Build without tests (faster)
docker build --build-arg SKIP_TESTS=true -t autobads:latest .
```

### 2. Run with Docker

```bash
docker run -d \
  --name autobads \
  -p 8083:8083 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=postgres \
  -e DB_PASSWORD=secret \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  -e OPENAI_API_KEY=sk-... \
  --restart unless-stopped \
  autobads:latest
```

### 3. Docker Compose

```yaml
version: '3.8'

services:
  autobads:
    image: autobads:latest
    ports:
      - "8083:8083"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_HOST: postgres
      DB_NAME: autobads
      DB_USERNAME: autobads
      DB_PASSWORD: ${DB_PASSWORD}
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      OPENAI_API_KEY: ${OPENAI_API_KEY}
    depends_on:
      - postgres
      - kafka
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost:8083/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    restart: unless-stopped
    networks:
      - autobads-network

  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: autobads
      POSTGRES_USER: autobads
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - autobads-network

  kafka:
    image: confluentinc/cp-kafka:latest
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
    depends_on:
      - zookeeper
    networks:
      - autobads-network

  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    networks:
      - autobads-network

volumes:
  postgres-data:

networks:
  autobads-network:
    driver: bridge
```

Run with:
```bash
docker-compose up -d
```

---

## Kubernetes Deployment

### 1. Create Namespace

```bash
kubectl create namespace autobads
```

### 2. Create Secrets

```bash
kubectl create secret generic autobads-secrets \
  --from-literal=db-password=<password> \
  --from-literal=openai-api-key=sk-... \
  --from-literal=agentmesh-api-key=<key> \
  -n autobads
```

### 3. Deployment Manifest

```yaml
# autobads-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: autobads
  namespace: autobads
  labels:
    app: autobads
spec:
  replicas: 3
  selector:
    matchLabels:
      app: autobads
  template:
    metadata:
      labels:
        app: autobads
    spec:
      containers:
      - name: autobads
        image: autobads:latest
        ports:
        - containerPort: 8083
          name: http
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DB_HOST
          value: "postgres-service"
        - name: DB_NAME
          value: "autobads"
        - name: DB_USERNAME
          value: "autobads"
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: autobads-secrets
              key: db-password
        - name: KAFKA_BOOTSTRAP_SERVERS
          value: "kafka-service:9092"
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: autobads-secrets
              key: openai-api-key
        - name: JAVA_OPTS
          value: "-Xmx1g -Xms512m"
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8083
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8083
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
---
apiVersion: v1
kind: Service
metadata:
  name: autobads-service
  namespace: autobads
spec:
  type: ClusterIP
  selector:
    app: autobads
  ports:
  - port: 80
    targetPort: 8083
    protocol: TCP
    name: http
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: autobads-hpa
  namespace: autobads
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: autobads
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

Deploy:
```bash
kubectl apply -f autobads-deployment.yaml
```

---

## Health Checks

### Endpoints

#### Liveness Probe
```bash
curl http://localhost:8083/actuator/health/liveness
```

Response:
```json
{
  "status": "UP"
}
```

#### Readiness Probe
```bash
curl http://localhost:8083/actuator/health/readiness
```

Response:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

#### Full Health Check
```bash
curl http://localhost:8083/actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "database": {
      "status": "UP",
      "details": {
        "database": "operational",
        "total_ideas": 1234
      }
    },
    "llm": {
      "status": "UP",
      "details": {
        "llm_service": "operational",
        "provider": "openai"
      }
    },
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "agentmesh": "CLOSED",
        "openai": "CLOSED"
      }
    }
  }
}
```

---

## Monitoring

### Prometheus Metrics

Auto-BADS exposes Prometheus metrics at:
```
http://localhost:8083/actuator/prometheus
```

### Key Metrics to Monitor

1. **Application Health**
   - `health_status` - Overall application health
   - `jvm_memory_used_bytes` - JVM memory usage
   - `jvm_threads_live_threads` - Active thread count

2. **Business Metrics**
   - `ideas_ingested_total` - Total ideas ingested
   - `ideas_analyzed_total` - Total ideas analyzed
   - `llm_calls_total` - Total LLM API calls
   - `llm_call_duration_seconds` - LLM call latency

3. **Database**
   - `hikaricp_connections_active` - Active DB connections
   - `hikaricp_connections_pending` - Pending connections
   - `repository_operations_seconds` - Repository operation time

4. **Circuit Breakers**
   - `resilience4j_circuitbreaker_state` - Circuit breaker states
   - `resilience4j_retry_calls` - Retry attempts

### Grafana Dashboard

Import the provided dashboard:
```bash
# Coming soon: autobads-grafana-dashboard.json
```

### Alerts

Recommended alerts:
```yaml
- alert: AutoBADSDown
  expr: up{job="autobads"} == 0
  for: 1m
  labels:
    severity: critical

- alert: HighMemoryUsage
  expr: jvm_memory_used_bytes{job="autobads"} / jvm_memory_max_bytes > 0.9
  for: 5m
  labels:
    severity: warning

- alert: CircuitBreakerOpen
  expr: resilience4j_circuitbreaker_state{state="open"} == 1
  for: 2m
  labels:
    severity: warning
```

---

## Security

### Best Practices

1. **API Keys & Secrets**
   - Never commit secrets to version control
   - Use Kubernetes Secrets or external secret managers (AWS Secrets Manager, HashiCorp Vault)
   - Rotate keys regularly

2. **Network Security**
   - Use TLS/HTTPS in production
   - Implement network policies in Kubernetes
   - Restrict database access to application only

3. **Application Security**
   - Run as non-root user (already configured in Dockerfile)
   - Keep dependencies up to date
   - Enable security headers

4. **Database Security**
   - Use strong passwords
   - Enable SSL connections
   - Implement least-privilege access

### Enable HTTPS

Add to `application-prod.yml`:
```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: autobads
```

---

## Troubleshooting

### Common Issues

#### 1. Application Won't Start

**Check logs:**
```bash
docker logs autobads
# or
kubectl logs -n autobads deployment/autobads
```

**Common causes:**
- Database connection failed
- Missing environment variables
- Port already in use

#### 2. High Memory Usage

**Check JVM settings:**
```bash
# Adjust JAVA_OPTS
export JAVA_OPTS="-Xmx2g -Xms1g -XX:+HeapDumpOnOutOfMemoryError"
```

#### 3. Database Connection Issues

**Test connection:**
```bash
psql -h $DB_HOST -U $DB_USERNAME -d $DB_NAME
```

**Check Flyway migrations:**
```bash
# View migration status
kubectl exec -it autobads-pod -- java -jar /app/auto-bads.jar flyway info
```

#### 4. LLM Service Failures

**Check health:**
```bash
curl http://localhost:8083/actuator/health/llm
```

**Verify API key:**
```bash
# Test OpenAI API
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer $OPENAI_API_KEY"
```

#### 5. Circuit Breaker Open

**Check status:**
```bash
curl http://localhost:8083/actuator/health
```

**Reset if needed:**
- Wait for `waitDurationInOpenState` to elapse
- Fix underlying issue
- Circuit breaker will auto-recover

### Debug Mode

Enable debug logging:
```bash
export LOG_LEVEL_APP=DEBUG
```

Or at runtime:
```bash
curl -X POST http://localhost:8083/actuator/loggers/com.therighthandapp.autobads \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```

---

## Performance Tuning

### JVM Options

```bash
# Production JVM settings
export JAVA_OPTS="
  -Xmx2g
  -Xms1g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+UseContainerSupport
  -XX:MaxRAMPercentage=75.0
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/var/log/autobads
"
```

### Database Tuning

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### Kafka Tuning

```yaml
spring:
  kafka:
    producer:
      acks: all
      batch-size: 16384
      buffer-memory: 33554432
      compression-type: lz4
```

---

## Backup & Recovery

### Database Backups

```bash
# Daily backup
pg_dump -h $DB_HOST -U $DB_USERNAME $DB_NAME > autobads_backup_$(date +%Y%m%d).sql

# Restore
psql -h $DB_HOST -U $DB_USERNAME $DB_NAME < autobads_backup_20251110.sql
```

### Application State

Application state is persisted in PostgreSQL. Ensure database backups are automated and tested regularly.

---

## Support

For issues and questions:
- GitHub Issues: [Auto-BADS Repository]
- Documentation: [FINAL_TEST_REPORT.md](./FINAL_TEST_REPORT.md)
- Test Status: 127/128 tests passing (99.2%)

---

**Last Updated**: November 10, 2025  
**Version**: 1.0.0  
**Status**: Production Ready ✅
