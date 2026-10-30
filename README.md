# emb-server (Spring AI 官方规范接口)

基于 **Java 17** 与最新安全维护补丁（**Spring Boot 4.1.2** / **Spring AI 2.0.1**，2026年10月安全版本）构建。

接口严格遵循大模型**官方标准路由规范**：
- **`POST /v1/chat/completions`**
- **`GET /v1/models`**

---

## 🌟 核心文件

- `pom.xml`：锁定 Java 17 编译规范与安全补丁依赖
- `src/main/resources/application.yml`：大模型 API 连接配置
- `src/main/java/com/emb/server/EmbServerApplication.java`：应用启动入口（支持自动加载 `.env` 与启动参数脱敏回显）
- `src/main/java/com/emb/server/controller/ChatCompletionController.java`：官方标准路由实现（内聚流式/非流式）
- `src/main/java/com/emb/server/dto/ApiErrorResponse.java`：对齐官方标准的错误响应协议 DTO
- `src/main/java/com/emb/server/exception/GlobalExceptionHandler.java`：全局异常拦截（401/429/400/500 标准化输出）

---

## 🚀 启动与配置

支持以下两种配置方式（任选其一即可）：

### 方式 1：使用 `.env` 文件（开箱即用，推荐）
复制根目录下的示例文件：
```bash
cp .env.example .env
```
编辑 `.env` 填入真实 Key：
```properties
OPENAI_BASE_URL=https://api.deepseek.com
OPENAI_API_KEY=sk-your-deepseek-api-key
OPENAI_MODEL=deepseek-chat
```
服务启动时会自动检测并读取 `.env` 配置（已加入 `.gitignore`，防止秘钥泄露）。

### 方式 2：配置系统环境变量
```bash
export OPENAI_BASE_URL=https://api.deepseek.com
export OPENAI_API_KEY=sk-your-deepseek-api-key
export OPENAI_MODEL=deepseek-chat
```

### 启动服务
```bash
mvn spring-boot:run
```
默认服务端口：`8080`。

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
