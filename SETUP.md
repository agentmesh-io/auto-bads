# Auto-BADS Setup Guide

## 🎯 Quick Start

This guide will help you set up and run the Auto-BADS system.

## ✅ Prerequisites

- **Java 22** (OpenJDK or Oracle JDK)
- **Maven 3.9+**
- **OpenAI API Key** (required for LLM functionality)
- **IDE** (IntelliJ IDEA or VS Code with Java extensions recommended)

## 📦 Installation Steps

### 1. Clone and Navigate

```bash
cd /Users/univers/projects/agentmesh/Auto-BADS
```

### 2. Configure Environment

Create or update your environment variables:

```bash
# Add to ~/.zshrc or ~/.bash_profile
export OPENAI_API_KEY="sk-your-api-key-here"

# Reload shell
source ~/.zshrc
```

### 3. Configure Lombok (IDE-Specific)

#### IntelliJ IDEA

1. Install Lombok Plugin:
   - Go to `Preferences` → `Plugins`
   - Search for "Lombok"
   - Install and restart IntelliJ

2. Enable Annotation Processing:
   - Go to `Preferences` → `Build, Execution, Deployment` → `Compiler` → `Annotation Processors`
   - Check "Enable annotation processing"
   - Click Apply

#### VS Code

1. Install Extensions:
   - Java Extension Pack
   - Lombok Annotations Support for VS Code

2. Settings:
   - Enable annotation processing in Java settings

### 4. Build the Project

```bash
# Clean build
mvn clean install -DskipTests

# If you encounter Lombok errors, try:
mvn clean compile -Dmaven.compiler.forceJavacCompilerUse=true
```

### 5. Run the Application

```bash
# Option A: Using Maven
mvn spring-boot:run

# Option B: Using Java directly
java -jar target/Auto-BADS-1.0-SNAPSHOT.jar
```

### 6. Verify Installation

```bash
# Check health endpoint
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}
```

## 🐛 Troubleshooting

### Issue: Lombok Annotations Not Working

**Symptoms**: Compilation errors like "cannot find symbol: method builder()"

**Solution**:

1. **Check Lombok is in classpath**:
```bash
mvn dependency:tree | grep lombok
```

2. **Force recompile with annotation processing**:
```bash
mvn clean
mvn compiler:compile -Dmaven.compiler.forceJavacCompilerUse=true
```

3. **IDE-specific**: Ensure annotation processing is enabled (see step 3 above)

4. **Manual fix**: If all else fails, run the fix script:
```bash
./fix-lombok.sh
```

### Issue: OpenAI API Errors

**Symptoms**: HTTP 401 or authentication errors

**Solution**:
```bash
# Verify API key is set
echo $OPENAI_API_KEY

# Test API key
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer $OPENAI_API_KEY"
```

### Issue: Port 8080 Already in Use

**Solution**:
```bash
# Find process using port 8080
lsof -i :8080

# Kill the process or change port in application.yml:
server:
  port: 8081
```

### Issue: H2 Database Connection Errors

**Solution**:
```bash
# Access H2 Console
# URL: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:autobads
# Username: sa
# Password: (leave blank)
```

## 🧪 Testing the System

### Test 1: Submit a Business Idea

```bash
curl -X POST http://localhost:8080/api/v1/ideas \
  -H "Content-Type: application/json" \
  -d '{
    "idea": "A platform that connects freelance developers with startups needing MVP development"
  }'
```

Expected response:
```json
{
  "ideaId": "some-uuid",
  "status": "INGESTION_IN_PROGRESS",
  "message": "Your idea has been submitted..."
}
```

### Test 2: Check Idea Status

```bash
curl http://localhost:8080/api/v1/ideas/{ideaId}
```

### Test 3: Get Solution Recommendation

```bash
curl http://localhost:8080/api/v1/solutions/{ideaId}
```

## 📊 Monitoring

### View Application Metrics

```bash
# Health check
curl http://localhost:8080/actuator/health

# Application info
curl http://localhost:8080/actuator/info

# Metrics
curl http://localhost:8080/actuator/metrics

# Spring Modulith structure
curl http://localhost:8080/actuator/modulith
```

## 🔧 Development Mode

### Enable Hot Reload (Spring DevTools)

Add to pom.xml (optional):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

### Enable Debug Logging

Update application.yml:
```yaml
logging:
  level:
    com.therighthandapp.autobads: DEBUG
    org.springframework.ai: DEBUG
```

### Run Tests

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=IdeaIngestionServiceTest

# Run with coverage
mvn test jacoco:report
```

## 🚀 Production Deployment

### 1. Use PostgreSQL Instead of H2

```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/autobads
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
```

### 2. Configure Kafka (Optional)

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
```

### 3. Build Production JAR

```bash
mvn clean package -DskipTests -Pprod
```

### 4. Run with Production Profile

```bash
java -jar target/Auto-BADS-1.0-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --server.port=8080
```

## 📝 Configuration Reference

### Essential Configuration (application.yml)

```yaml
# OpenAI Configuration
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        model: gpt-4-turbo-preview  # or gpt-4, gpt-3.5-turbo
        temperature: 0.7
        max-tokens: 2000

# Auto-BADS Specific
auto-bads:
  recommendation:
    weights:
      strategic-alignment: 0.30
      technical-feasibility: 0.25
      market-opportunity: 0.25
      resource-cost: 0.20
    pmf-threshold: 40.0  # Product-Market Fit threshold
```

## 🆘 Getting Help

1. **Check logs**: `tail -f logs/spring.log`
2. **Enable debug mode**: Set `logging.level.root=DEBUG`
3. **Review errors**: Check compilation output in `/tmp/compile*.log`
4. **Documentation**: Read the full [README.md](README.md)

## 📚 Next Steps

After successful setup:

1. ✅ Review the architecture in [README.md](README.md)
2. ✅ Explore the API endpoints
3. ✅ Submit test business ideas
4. ✅ Review generated solution packages
5. ✅ Customize recommendation weights
6. ✅ Add custom analysis agents

---

**Success Indicators**:
- ✅ Application starts without errors
- ✅ Health endpoint returns `{"status":"UP"}`
- ✅ Can submit ideas via API
- ✅ LLM integration works (check logs for OpenAI calls)
- ✅ Database connections successful

Happy analyzing! 🎉

