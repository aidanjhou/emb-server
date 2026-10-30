# emb-server (Spring AI 官方规范接口)

基于 **Java 17** 与最新安全维护补丁（**Spring Boot 4.1.2** / **Spring AI 2.0.1**，2026年10月安全版本）构建。

接口严格遵循大模型**官方标准路由规范**：
- **`POST /v1/chat/completions`**
- **`GET /v1/models`**

---

## 🌟 核心文件

- `pom.xml`：锁定 Java 17 编译规范与安全补丁依赖
- `src/main/resources/application.yml`：大模型 API 连接配置
- `src/main/java/com/emb/server/EmbServerApplication.java`：应用启动入口
- `src/main/java/com/emb/server/controller/ChatCompletionController.java`：官方标准路由实现（内聚流式/非流式）

---

## 🚀 启动与使用

### 1. 配置环境变量 (以 DeepSeek 为例)
```bash
export OPENAI_BASE_URL=https://api.deepseek.com
export OPENAI_API_KEY=sk-your-deepseek-api-key
export OPENAI_MODEL=deepseek-chat
```

### 2. 启动服务
```bash
mvn spring-boot:run
```

默认端口：`8080`。

---

## 📡 官方路由调用示例

### 1. 官方 SSE 流式输出（`stream: true`）
```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "deepseek-chat",
    "messages": [
      {"role": "user", "content": "你好，请用一句话自我介绍！"}
    ],
    "stream": true
  }'
```

### 2. 官方同步输出（`stream: false`）
```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "deepseek-chat",
    "messages": [
      {"role": "user", "content": "你好，请用一句话自我介绍！"}
    ],
    "stream": false
  }'
```

### 3. 查看可用模型列表
```bash
curl http://localhost:8080/v1/models
```
