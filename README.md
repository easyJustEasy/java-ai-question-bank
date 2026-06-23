# 📝 Java 试题生成器（AI 驱动）

> 基于 DeepSeek V4 模型的 Java 试题自动生成工具。选择题 / 判断题 / 问答题，一键生成可运行的 HTML 试卷。

[English](#english) | [简体中文](#简体中文)

---

## 简体中文

### 📖 简介

轻量级 Web 服务，通过调用 DeepSeek API 自动生成 Java 试题。无需数据库、无需 nginx、零配置启动。

### ✨ 功能

| 功能 | 说明 |
|------|------|
| **三种题型** | 选择题、判断题、问答题 |
| **双模型可选** | DeepSeek V4 Flash / V4 Pro |
| **即时预览** | 生成后自动在页面内预览 HTML 试卷 |
| **暗色/亮色主题** | 右上角 🌙/☀️ 一键切换，偏好自动保存 |
| **API Key 显隐** | 👁️ 点击可查看/隐藏密钥 |
| **本地持久化** | API Key、模型、题型选择自动保存到浏览器 |
| **零依赖** | 仅需 JDK 17，无需 Spring Boot / Maven / nginx |

### 🚀 快速开始

```bash
# 下载 JAR 或自行编译
java -jar java-question-generator.jar 8001
```

打开浏览器访问 `http://localhost:8001`

#### 手动编译

```bash
# 需要 JDK 17 + Maven
mvn clean package -DskipTests
java -jar target/java-question-generator.jar 8001
```

### 🔧 技术架构

```
src/main/java/com/questionbank/
├── Main.java        # 入口，启动 HTTP 服务器
└── WebServer.java   # 路由 + DeepSeek API 调用 + 文件服务

src/main/resources/
└── index.html       # 单页前端（HTML + CSS + JS）
```

- **HTTP 服务**：`com.sun.net.httpserver.HttpServer`（JDK 内置）
- **AI 接口**：DeepSeek Chat Completions API（OpenAI 兼容格式）
- **前端**：原生 JS，无框架依赖
- **JAR 大小**：仅 **15KB**

### 📂 文件存储

生成的 HTML 试卷保存在配置的 `file-path` 目录（默认 `/mnt/f/files/java`），通过 `/files/` 路径提供访问。

### ☁️ 在线地址

👉 [https://java.2u1.cn](https://java.2u1.cn)

### 📄 许可证

MIT License

---

## English

### 📖 Introduction

A lightweight Java question generator powered by DeepSeek V4 API. Generates multiple-choice, true/false, and essay questions as standalone HTML pages.

### ✨ Features

- **3 question types**: Multiple choice, True/False, Essay
- **2 models**: DeepSeek V4 Flash / V4 Pro
- **Instant preview**: Generated HTML displayed in-page
- **Dark/Light theme**: Toggle with 🌙/☀️, preference saved
- **Zero dependencies**: JDK 17 only, no Spring Boot / Maven / nginx
- **15KB JAR**: Ultra lightweight

### 🚀 Quick Start

```bash
java -jar java-question-generator.jar 8001
```

Open `http://localhost:8001`

### 📄 License

MIT License
