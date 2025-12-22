# Postman 测试指南

本文档说明如何使用 Postman 测试问答服务的各个接口，包括多轮对话功能。

## 基础配置

- **服务地址**: `http://localhost:8081`
- **基础路径**: `/api/chat`

---

## 1. 健康检查接口

### 测试端点
```
GET http://localhost:8081/api/chat/health
```

### Postman 配置
- **方法**: GET
- **URL**: `http://localhost:8081/api/chat/health`
- **Headers**: 无需特殊配置

### 预期响应
```
问答服务运行正常 | 时间：Mon Jan 15 10:30:00 CST 2024
```

---

## 2. 测试端点

### 测试端点
```
GET http://localhost:8081/api/chat/test
```

### Postman 配置
- **方法**: GET
- **URL**: `http://localhost:8081/api/chat/test`

### 预期响应
```json
{
    "status": "success",
    "message": "问答服务API已就绪",
    "endpoints": [
        "POST /api/chat/completions - 问答接口",
        "GET /api/chat/history - 获取历史",
        "GET /api/chat/health - 健康检查"
    ],
    "timestamp": 1705290600000
}
```

---

## 3. 核心问答接口（单轮对话）

### 测试端点
```
POST http://localhost:8081/api/chat/completions
```

### Postman 配置
- **方法**: POST
- **URL**: `http://localhost:8081/api/chat/completions`
- **Headers**:
  ```
  Content-Type: application/json
  ```
- **Body** (raw, JSON):
```json
{
    "question": "什么是人工智能？",
    "sessionId": "test_session_001",
    "userId": "user_001"
}
```

### 预期响应
```json
{
    "answer": "您的问题 \"什么是人工智能？\" 已收到。我正在为您查找相关信息...",
    "sessionId": "test_session_001",
    "timestamp": 1705290600000,
    "citations": [
        {
            "source": "知识库文档",
            "content": "相关产业知识内容",
            "page": 1
        }
    ]
}
```

---

## 4. 多轮对话测试（指代词解析）

这是多轮对话功能的核心测试场景。我们需要测试系统能否识别并替换指代词。

### 测试流程

#### 第一轮：初始问题

**请求**:
```json
POST http://localhost:8081/api/chat/completions

{
    "question": "什么是人工智能？",
    "sessionId": "multiturn_test_001",
    "userId": "user_001"
}
```

**预期响应**:
```json
{
    "answer": "您的问题 \"什么是人工智能？\" 已收到。我正在为您查找相关信息...",
    "sessionId": "multiturn_test_001",
    "timestamp": 1705290600000,
    "citations": [...]
}
```

#### 第二轮：使用指代词"它"

**请求**:
```json
POST http://localhost:8081/api/chat/completions

{
    "question": "它的应用领域有哪些？",
    "sessionId": "multiturn_test_001",
    "userId": "user_001"
}
```

**说明**: 
- 系统会自动将"它"解析为"人工智能"
- 实际处理的问题会变成："人工智能的应用领域有哪些？"

**预期响应**:
```json
{
    "answer": "您的问题 \"人工智能的应用领域有哪些？\" 已收到。我正在为您查找相关信息...",
    "sessionId": "multiturn_test_001",
    "timestamp": 1705290600000,
    "citations": [...]
}
```

#### 第三轮：使用"这个"

**请求**:
```json
POST http://localhost:8081/api/chat/completions

{
    "question": "这个技术的优势是什么？",
    "sessionId": "multiturn_test_001",
    "userId": "user_001"
}
```

**说明**: "这个"会被解析为最近的问题主题（人工智能）

#### 第四轮：使用"上述"、"之前提到的"

**请求**:
```json
POST http://localhost:8081/api/chat/completions

{
    "question": "之前提到的内容能详细解释一下吗？",
    "sessionId": "multiturn_test_001",
    "userId": "user_001"
}
```

### 支持的指代词列表

系统可以识别并替换以下指代词：
- `它`、`它们`、`它的`、`它们的`
- `这个`、`这些`、`那个`、`那些`
- `上述`、`之前`、`刚才`、`之前提到的`、`刚才说的`、`刚才问的`
- `其`、`其中`、`上述问题`、`这个问题`、`那个问题`
- `之前的内容`、`上述内容`

---

## 5. 查看对话历史

### 测试端点
```
GET http://localhost:8081/api/chat/history?sessionId=multiturn_test_001
```

### Postman 配置
- **方法**: GET
- **URL**: `http://localhost:8081/api/chat/history`
- **Params**:
  - Key: `sessionId`
  - Value: `multiturn_test_001`

### 预期响应
```json
[
    {
        "id": 3,
        "sessionId": "multiturn_test_001",
        "question": "之前提到的内容能详细解释一下吗？",
        "answer": "...",
        "createdAt": "2024-01-15T10:33:00"
    },
    {
        "id": 2,
        "sessionId": "multiturn_test_001",
        "question": "这个技术的优势是什么？",
        "answer": "...",
        "createdAt": "2024-01-15T10:32:00"
    },
    {
        "id": 1,
        "sessionId": "multiturn_test_001",
        "question": "什么是人工智能？",
        "answer": "...",
        "createdAt": "2024-01-15T10:31:00"
    }
]
```

**注意**: 返回的数据按时间倒序排列（最新的在前）

---

## 6. 查看上下文信息（调试用）

### 测试端点
```
GET http://localhost:8081/api/chat/context?sessionId=multiturn_test_001
```

### Postman 配置
- **方法**: GET
- **URL**: `http://localhost:8081/api/chat/context`
- **Params**:
  - Key: `sessionId`
  - Value: `multiturn_test_001`

### 预期响应
```json
{
    "historyCount": 3,
    "history": [
        {
            "id": 3,
            "sessionId": "multiturn_test_001",
            "question": "之前提到的内容能详细解释一下吗？",
            "answer": "...",
            "createdAt": "2024-01-15T10:33:00"
        },
        ...
    ],
    "contextSummary": "问题：之前提到的内容能详细解释一下吗？ 答案：... 问题：这个技术的优势是什么？ 答案：... 问题：什么是人工智能？ 答案：..."
}
```

这个接口可以帮助你了解系统如何构建上下文摘要。

---

## 7. 简单问答接口（不保存记录）

### 测试端点
```
POST http://localhost:8081/api/chat/simple
```

### Postman 配置
- **方法**: POST
- **URL**: `http://localhost:8081/api/chat/simple`
- **Headers**:
  ```
  Content-Type: application/json
  ```
- **Body** (raw, JSON):
```json
{
    "question": "这是一个测试问题"
}
```

### 预期响应
```json
{
    "question": "这是一个测试问题",
    "answer": "这是对问题的简单回答：这是一个测试问题",
    "timestamp": "1705290600000"
}
```

**注意**: 此接口不会保存对话记录，也不会进行上下文解析。

---

## 完整的多轮对话测试场景

### 场景：了解人工智能相关知识

#### 步骤 1: 询问基础概念
```json
POST /api/chat/completions
{
    "question": "什么是人工智能？",
    "sessionId": "ai_demo_001"
}
```

#### 步骤 2: 使用"它"询问应用
```json
POST /api/chat/completions
{
    "question": "它主要应用在哪些领域？",
    "sessionId": "ai_demo_001"
}
```
**系统解析**: "它" → "人工智能"

#### 步骤 3: 使用"这个"询问优势
```json
POST /api/chat/completions
{
    "question": "这个技术有什么优势？",
    "sessionId": "ai_demo_001"
}
```
**系统解析**: "这个" → "人工智能"

#### 步骤 4: 使用"之前提到的"继续询问
```json
POST /api/chat/completions
{
    "question": "之前提到的应用领域能举例说明吗？",
    "sessionId": "ai_demo_001"
}
```
**系统解析**: "之前提到的" → 最近的问题主题

#### 步骤 5: 查看完整对话历史
```json
GET /api/chat/history?sessionId=ai_demo_001
```

#### 步骤 6: 查看上下文信息
```json
GET /api/chat/context?sessionId=ai_demo_001
```

---

## 测试技巧

### 1. 使用环境变量
在 Postman 中可以创建环境变量：
- `base_url`: `http://localhost:8081`
- `test_session_id`: `test_session_001`

然后使用 `{{base_url}}/api/chat/...` 和 `{{test_session_id}}`

### 2. 保存请求到Collection
创建一个 Collection，将上述所有请求保存，方便后续测试。

### 3. 使用Pre-request Script自动生成SessionId
在请求的 Pre-request Script 中添加：
```javascript
pm.environment.set("sessionId", "session_" + Date.now());
```

### 4. 验证响应
在 Tests 标签中添加验证脚本：
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has answer field", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('answer');
});
```

---

## 常见问题

### Q1: 如何确认指代词已经被替换？
A: 查看服务器控制台输出，系统会打印：
```
原始问题: 它的应用领域有哪些？
解析后的问题: 人工智能的应用领域有哪些？
```

### Q2: 为什么指代词没有被替换？
A: 检查以下几点：
1. sessionId 是否一致
2. 之前是否有对话历史
3. 指代词是否在支持列表中
4. 查看 `/api/chat/context` 接口确认上下文是否正确

### Q3: 上下文窗口大小是多少？
A: 默认是最近 5 轮对话。可以在 `ContextResolutionService` 中修改 `CONTEXT_WINDOW` 常量。

---

## 注意事项

1. **SessionId 必须保持一致**：多轮对话需要在同一个 sessionId 下进行
2. **数据库连接**：确保数据库服务已启动，并且连接配置正确
3. **服务启动**：确保 Spring Boot 应用已启动并运行在 8081 端口
4. **时间顺序**：对话历史按创建时间倒序返回（最新的在前）

---

## 快速测试清单

- [ ] 健康检查接口正常
- [ ] 单轮问答接口正常
- [ ] 使用"它"的指代词被正确替换
- [ ] 使用"这个"的指代词被正确替换
- [ ] 使用"之前提到的"的指代词被正确替换
- [ ] 历史记录接口返回正确数据
- [ ] 上下文信息接口返回完整上下文
- [ ] 不同 sessionId 的对话互不干扰
