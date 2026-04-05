# 部署指南

## 1. 本地开发环境

### Python版本

```bash
cd python-impl
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env
# 编辑 .env 填入你的API Key
python -m api.main
```

访问: http://localhost:8000/docs (Swagger UI)

### Java版本

```bash
cd java-impl
mvn clean package -DskipTests
java -jar target/smart-cs-agent-1.0.0.jar
```

访问: http://localhost:8080/api/health

### Go版本

```bash
cd go-impl
go mod tidy
go run main.go
```

访问: http://localhost:8090/health

## 2. Docker部署

### 单服务启动

```bash
# Python
cd python-impl
docker build -t smart-cs-python .
docker run -p 8000:8000 --env-file .env smart-cs-python

# Java
cd java-impl
mvn clean package -DskipTests
docker build -t smart-cs-java .
docker run -p 8080:8080 -e OPENAI_API_KEY=xxx smart-cs-java

# Go
cd go-impl
docker build -t smart-cs-go .
docker run -p 8090:8090 smart-cs-go
```

### Docker Compose 一键启动

```bash
docker-compose up -d
```

## 3. API接口说明

所有三个版本提供统一的REST API：

### POST /api/chat — 聊天接口

```json
// Request
{
  "message": "我想了解一下理财产品A",
  "user_id": "user_001",
  "session_id": "optional-session-id"
}

// Response
{
  "response": "关于理财产品A...",
  "session_id": "xxx",
  "intent": "knowledge_rag",
  "compliance_passed": true
}
```

### GET /api/history/{session_id} — 对话历史

### GET /api/tools — MCP工具列表

### GET /api/metrics — 系统指标

### GET /health — 健康检查

## 4. 环境变量说明

| 变量 | 说明 | 默认值 |
|------|------|--------|
| OPENAI_API_KEY | LLM API密钥 | 无 |
| OPENAI_BASE_URL | API端点 | https://api.openai.com/v1 |
| MODEL_NAME | 模型名称 | gpt-4o |
| REDIS_URL | Redis地址 | redis://localhost:6379/0 |
| OTEL_SERVICE_NAME | 追踪服务名 | smart-cs-multi-agent |
| OTEL_EXPORTER_OTLP_ENDPOINT | OTLP端点 | http://localhost:4317 |
