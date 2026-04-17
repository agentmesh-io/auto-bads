# Auto-BADS Setup Guide

## Quick Start

Auto-BADS runs **standalone** (not in Docker) on port 8083 to avoid ND4J native library issues.

### 1. Start AgentMesh Infrastructure

First, ensure all infrastructure services are running:

```bash
cd /Users/univers/projects/agentmesh/AgentMesh
./restart-agentmesh.sh
```

This starts:
- PostgreSQL (port 5432)
- Redis (port 6379)
- Kafka (port 9092)
- AgentMesh API (port 8080)
- AgentMesh UI (port 3001)

### 2. Start Auto-BADS

```bash
cd /Users/univers/projects/agentmesh/Auto-BADS
./start-autobads.sh
```

Or manually:
```bash
cd /Users/univers/projects/agentmesh/Auto-BADS
mvn spring-boot:run
```

Auto-BADS will be available at:
- **API:** http://localhost:8083
- **Health:** http://localhost:8083/actuator/health

### 3. Verify Integration

Check that the UI can reach Auto-BADS:

```bash
# From host
curl http://localhost:8083/actuator/health

# From UI container (should work with host.docker.internal)
docker exec agentmesh-ui wget -qO- http://host.docker.internal:8083/actuator/health
```

## Configuration

Auto-BADS uses the shared infrastructure:

| Service    | Endpoint             | Database     |
|------------|---------------------|--------------|
| PostgreSQL | localhost:5432      | `autobads`   |
| Kafka      | localhost:9092      | -            |
| Redis      | localhost:6379      | -            |
| Ollama     | localhost:11434     | -            |

### Database Setup

Auto-BADS automatically creates its database schema on startup (`ddl-auto: update`).

If you need to manually create the database:

```bash
docker exec -it agentmesh-postgres psql -U agentmesh -d agentmesh
```

```sql
CREATE DATABASE autobads;
GRANT ALL PRIVILEGES ON DATABASE autobads TO agentmesh;
```

## Troubleshooting

### Port 8083 Already in Use

```bash
# Find what's using the port
lsof -i :8083

# Kill the process
kill -9 <PID>

# Or use the script which handles this
./start-autobads.sh
```

### Cannot Connect to PostgreSQL

Make sure AgentMesh infrastructure is running:

```bash
cd ../AgentMesh
./check-status.sh
```

If PostgreSQL is not healthy:

```bash
./restart-agentmesh.sh
```

### Cannot Connect to Kafka

Check Kafka status:

```bash
docker logs agentmesh-kafka --tail 50
```

If Kafka has issues (NodeExistsException):

```bash
cd ../AgentMesh
./restart-agentmesh.sh --clean-kafka
```

### UI Shows "Failed to Fetch"

This means Auto-BADS is not running or not accessible:

1. **Check Auto-BADS is running:**
   ```bash
   curl http://localhost:8083/actuator/health
   ```

2. **Check UI can reach Auto-BADS:**
   ```bash
   docker exec agentmesh-ui wget -qO- http://host.docker.internal:8083/actuator/health
   ```

3. **Check docker-compose configuration:**
   ```bash
   docker exec agentmesh-ui env | grep AUTOBADS
   ```
   Should show: `NEXT_PUBLIC_AUTOBADS_API=http://host.docker.internal:8083`

4. **Restart UI if needed:**
   ```bash
   cd ../AgentMesh
   docker-compose restart agentmesh-ui
   ```

### Ollama Not Available

Auto-BADS expects Ollama on `localhost:11434` with the `tinyllama:1.1b` model.

**Install Ollama:**
```bash
# macOS
brew install ollama

# Start Ollama
ollama serve

# Pull the model (in another terminal)
ollama pull tinyllama:1.1b
```

**Or use different model:**

Edit `application.yml`:
```yaml
spring:
  ai:
    ollama:
      chat:
        model: llama2  # or another model
```

## Running in Background

### Option 1: Use screen

```bash
cd /Users/univers/projects/agentmesh/Auto-BADS
screen -S autobads
mvn spring-boot:run

# Detach: Ctrl+A, then D
# Reattach: screen -r autobads
```

### Option 2: Use nohup

```bash
cd /Users/univers/projects/agentmesh/Auto-BADS
nohup mvn spring-boot:run > logs/autobads.log 2>&1 &
echo $! > autobads.pid

# Stop later:
kill $(cat autobads.pid)
```

### Option 3: Build and run JAR

```bash
# Build
mvn clean package -DskipTests

# Run
java -jar target/Auto-BADS-0.0.1-SNAPSHOT.jar

# Or in background
nohup java -jar target/Auto-BADS-0.0.1-SNAPSHOT.jar > logs/autobads.log 2>&1 &
```

## Development vs Production

### Development (default)

```bash
mvn spring-boot:run
```

Uses `application.yml` with localhost connections.

### Production

```bash
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

Uses `application-prod.yml` with production settings.

## Logs

View Auto-BADS logs:

```bash
# If running with mvn
# Logs appear in terminal

# If running in background with nohup
tail -f logs/autobads.log

# If running as JAR
tail -f logs/spring.log
```

## Integration with AgentMesh UI

The UI "Submit Idea" feature sends requests to Auto-BADS at `http://host.docker.internal:8083/api/ideas`.

**Expected workflow:**

1. User fills out idea form in UI (http://localhost:3001/submit-idea)
2. UI sends POST request to Auto-BADS API
3. Auto-BADS analyzes the idea using:
   - Market analysis
   - Product innovation scoring
   - Financial forecasting (LSTM model)
   - PMF (Product-Market Fit) assessment
4. Auto-BADS publishes event to Kafka topic `business-idea-analyzed`
5. AgentMesh API consumes the event and creates workflow
6. UI polls AgentMesh API for workflow status

## API Endpoints

- `POST /api/ideas` - Submit new business idea
- `GET /api/ideas/{id}` - Get idea analysis
- `GET /api/ideas` - List all ideas
- `GET /actuator/health` - Health check
- `GET /actuator/metrics` - Metrics

## Quick Reference

```bash
# Start everything
cd /Users/univers/projects/agentmesh/AgentMesh && ./restart-agentmesh.sh
cd /Users/univers/projects/agentmesh/Auto-BADS && ./start-autobads.sh

# Check status
curl http://localhost:8083/actuator/health
curl http://localhost:3001
curl http://localhost:8080/actuator/health

# Stop Auto-BADS
# Press Ctrl+C in the terminal running mvn spring-boot:run
# Or kill the Java process:
pkill -f "Auto-BADS"
```
