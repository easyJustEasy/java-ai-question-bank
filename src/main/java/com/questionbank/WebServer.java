package com.questionbank;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * 试题生成器 Web 服务器
 */
public class WebServer {

    private static final Gson gson = new Gson();
    private static final String HTML;
    private static String FILE_PATH = "/mnt/f/files/java";
    private static String BASE_URL = "http://localhost/java/";

    static {
        try (InputStream is = WebServer.class.getResourceAsStream("/index.html")) {
            HTML = new String(Objects.requireNonNull(is, "index.html not found").readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load index.html", e);
        }
    }

    public static void start(int port) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", WebServer::handleRoot);
        server.createContext("/api/generate-java-questions", WebServer::handleGenerate);
        server.createContext("/files", WebServer::handleFiles);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("📝 Java 试题生成器已启动 → http://localhost:" + port);
    }

    /** 首页 */
    private static void handleRoot(HttpExchange ex) throws IOException {
        String uri = ex.getRequestURI().getPath();
        if ("/".equals(uri)) {
            byte[] bytes = HTML.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "text/html;charset=utf-8");
            ex.getResponseHeaders().add("Cache-Control", "no-cache, no-store, must-revalidate");
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
        } else {
            ex.sendResponseHeaders(204, -1);
        }
        ex.close();
    }

    /** 生成试题 API */
    private static void handleGenerate(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(405, -1);
            ex.close();
            return;
        }

        try {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> req = gson.fromJson(body);

            int questionType = ((Number) req.get("questionType")).intValue();
            String apiKey = (String) req.get("apiKey");
            String modelName = (String) req.get("modelName");

            if (apiKey == null || apiKey.isBlank()) {
                sendJson(ex, 400, Map.of("error", "API Key 不能为空"));
                return;
            }

            String typeName = typeName(questionType);
            System.out.println("生成试题: " + typeName + " 模型: " + modelName);

            // 调用 DeepSeek
            String htmlContent = callDeepSeek(modelName, apiKey, questionType);

            // 注入复制按钮 + 错题本按钮 + 自动错题检测 JS
            String injectedJs = "" +
                "<script>\n" +
                "(function(){\n" +
                "  var qType = '" + typeName + "';\n" +
                "  function saveMistake(txt){\n" +
                "    var arr=JSON.parse(localStorage.getItem('jqb_mistakes')||'[]');\n" +
                "    if(arr.some(function(x){return x.text===txt})) return;\n" +
                "    arr.push({id:Date.now()+'_'+Math.random().toString(36).slice(2,6),text:txt,type:qType,timestamp:Date.now()});\n" +
                "    localStorage.setItem('jqb_mistakes',JSON.stringify(arr));\n" +
                "    window.parent.postMessage({type:'mistake_updated'},'*');\n" +
                "  }\n" +
                "  function init(){\n" +
                "    var items = document.querySelectorAll('.question, .question-item, [class*=question], li, .qa-item, .problem');\n" +
                "    items.forEach(function(el){\n" +
                "      if(!el.textContent.trim()||el.querySelector('.q-extra-btn')) return;\n" +
                "      var txt = el.textContent.trim();\n" +
                "      if(txt.length<10||!/\\d/.test(txt.substr(0,10))) return;\n" +
                "      if(getComputedStyle(el).position==='static') el.style.position='relative';\n" +
                "      var wrap=document.createElement('div');\n" +
                "      wrap.className='q-extra-btn';\n" +
                "      Object.assign(wrap.style,{position:'absolute',top:'6px',right:'6px',display:'flex',gap:'6px',zIndex:'99'});\n" +
                "      // 复制\n" +
                "      var c=document.createElement('button');\n" +
                "      c.innerHTML='\\ud83d\\udccb';c.title='复制本题';\n" +
                "      Object.assign(c.style,{width:'26px',height:'26px',borderRadius:'50%',border:'none',background:'rgba(102,126,234,.25)',cursor:'pointer',fontSize:'12px',display:'flex',alignItems:'center',justifyContent:'center',transition:'all .2s'});\n" +
                "      c.onmouseenter=function(){this.style.background='rgba(102,126,234,.5)'};\n" +
                "      c.onmouseleave=function(){this.style.background='rgba(102,126,234,.25)'};\n" +
                "      c.onclick=function(e){e.stopPropagation();navigator.clipboard.writeText(txt).then(function(){c.innerHTML='\\u2713';setTimeout(function(){c.innerHTML='\\ud83d\\udccb'},1500)}).catch(function(){prompt('复制失败',txt)})};\n" +
                "      // 错题本按钮\n" +
                "      var m=document.createElement('button');\n" +
                "      m.innerHTML='\\ud83d\\udcdd';m.title='加入错题本';\n" +
                "      Object.assign(m.style,{width:'26px',height:'26px',borderRadius:'50%',border:'none',background:'rgba(239,68,68,.2)',cursor:'pointer',fontSize:'12px',display:'flex',alignItems:'center',justifyContent:'center',transition:'all .2s'});\n" +
                "      m.onmouseenter=function(){this.style.background='rgba(239,68,68,.4)'};\n" +
                "      m.onmouseleave=function(){this.style.background='rgba(239,68,68,.2)'};\n" +
                "      m.onclick=function(e){e.stopPropagation();saveMistake(txt);m.innerHTML='\\u2713';m.title='已加入';setTimeout(function(){m.innerHTML='\\ud83d\\udcdd';m.title='加入错题本'},1500)};\n" +
                "      wrap.appendChild(c);wrap.appendChild(m);\n" +
                "      el.appendChild(wrap);\n" +
                "    });\n" +
                "    // 自动错题检测：找提交按钮\n" +
                "    setTimeout(function(){\n" +
                "      var btns = document.querySelectorAll('button, input[type=submit], input[type=button]');\n" +
                "      btns.forEach(function(btn){\n" +
                "        var t = (btn.textContent||btn.value||'').toLowerCase();\n" +
                "        if(t.indexOf('提交')<0&&t.indexOf('检查')<0&&t.indexOf('submit')<0&&t.indexOf('check')<0&&t.indexOf('查看')<0&&t.indexOf('交卷')<0&&t.indexOf('完成')<0) return;\n" +
                "        btn.addEventListener('click',function(){\n" +
                "          setTimeout(function(){\n" +
                "            // 扫描错题：有 .wrong/.incorrect 子元素的 question\n" +
                "            var qs = document.querySelectorAll('.question, .question-item, [class*=question]');\n" +
                "            qs.forEach(function(q){\n" +
                "              var wrongs = q.querySelectorAll('.wrong, .incorrect, .error');\n" +
                "              if(wrongs.length>0) saveMistake(q.textContent.trim());\n" +
                "            });\n" +
                "          }, 600);\n" +
                "        });\n" +
                "      });\n" +
                "    }, 1000);\n" +
                "  }\n" +
                "  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',init);\n" +
                "  else init();\n" +
                "})();\n" +
                "</script>";

            // 在 </body> 前注入，如果没有 </body> 则追加到末尾
            int bodyEnd = htmlContent.lastIndexOf("</body>");
            if (bodyEnd >= 0) {
                htmlContent = htmlContent.substring(0, bodyEnd) + injectedJs + htmlContent.substring(bodyEnd);
            } else {
                htmlContent = htmlContent + injectedJs;
            }

            // 保存文件
            String fileName = questionType + "/" + UUID.randomUUID() + ".html";
            Path filePath = Path.of(FILE_PATH, fileName);
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, htmlContent, StandardCharsets.UTF_8);
            System.out.println("保存成功: " + filePath);

            String url = BASE_URL + fileName;
            sendJson(ex, 200, Map.of("url", url, "message", "试题生成成功"));

        } catch (Exception e) {
            System.err.println("生成失败: " + e.getMessage());
            sendJson(ex, 500, Map.of("error", "生成失败: " + e.getMessage()));
        }
        ex.close();
    }

    /** 提供生成的 HTML 文件 */
    private static void handleFiles(HttpExchange ex) throws IOException {
        String uri = ex.getRequestURI().getPath();
        String relativePath = uri.substring("/files/".length());
        Path file = Path.of(FILE_PATH, relativePath);
        if (Files.exists(file)) {
            byte[] bytes = Files.readAllBytes(file);
            ex.getResponseHeaders().add("Content-Type", "text/html;charset=utf-8");
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
        } else {
            ex.sendResponseHeaders(404, -1);
        }
        ex.close();
    }

    /** 调用 DeepSeek API */
    private static String callDeepSeek(String modelName, String apiKey, int questionType) throws Exception {
        String typeName = typeName(questionType);
        String knowledge = "Java：语法、OOP、集合、异常、泛型、多线程、I/O、JVM、常用库。  \n" +
            "计算机基础：OS、网络、数据结构与算法、编译原理、组成原理。  \n" +
            "数据库：关系型/非关系型、SQL、设计、事务。  \n" +
            "开发工具：Git、Maven/Gradle、IDE、调试、测试。  \n" +
            "常用框架：Spring、MyBatis、Hibernate、Netty、其他。  \n" +
            "系统设计：架构、设计模式、API、高并发、安全。  \n" +
            "分布式：系统理论、消息队列、服务发现、缓存、事务。  \n" +
            "高性能：优化、缓存、异步、CDN。  \n" +
            "高可用：容错、监控、恢复、弹性伸缩。";

        String prompt = "写一个html网页，要求里面有10道java方面的" + typeName +
            "，难度偏难，先隐藏答案，用户提交后显示答案。我只需要返回能直接运行的html部分，不需要额外的信息。" +
            "要求：每道题用一个带 class='question' 的 div 包裹。";

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", modelName);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
            "你是一个java专家，擅长所有java问题,需要你帮用户生成一些题目，涉及的知识点有：" + knowledge));
        messages.add(Map.of("role", "user", "content", prompt));
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 8192);

        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(60))
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.deepseek.com/chat/completions"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .timeout(java.time.Duration.ofSeconds(300))
            .POST(BodyPublishers.ofString(gson.toJson(requestBody)))
            .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        String respBody = response.body();

        if (response.statusCode() != 200) {
            throw new RuntimeException("DeepSeek API 返回 " + response.statusCode() + ": " + respBody);
        }

        Map<String, Object> respMap = gson.fromJson(respBody);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) respMap.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> choice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            String content = (String) message.get("content");
            return content.replace("```html", "").replace("```", "").trim();
        }

        throw new RuntimeException("DeepSeek 返回格式异常: " + respBody);
    }

    private static String typeName(int type) {
        return switch (type) {
            case 1 -> "选择题";
            case 2 -> "判断题";
            case 3 -> "问答题";
            default -> "选择题";
        };
    }

    private static void sendJson(HttpExchange ex, int status, Object data) throws IOException {
        byte[] bytes = gson.toJson(data).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
    }

    /** 简易 Gson 替代（无依赖） */
    private static class Gson {
        String toJson(Object obj) {
            if (obj instanceof Map) {
                StringBuilder sb = new StringBuilder("{");
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                boolean first = true;
                for (var e : map.entrySet()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append(quote(e.getKey())).append(":");
                    sb.append(toJsonValue(e.getValue()));
                }
                sb.append("}");
                return sb.toString();
            }
            if (obj instanceof List) {
                StringBuilder sb = new StringBuilder("[");
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) obj;
                boolean first = true;
                for (Object item : list) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append(toJsonValue(item));
                }
                sb.append("]");
                return sb.toString();
            }
            return toJsonValue(obj);
        }

        private String toJsonValue(Object v) {
            if (v == null) return "null";
            if (v instanceof Number || v instanceof Boolean) return v.toString();
            if (v instanceof Map || v instanceof List) return toJson(v);
            return quote(v.toString());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> fromJson(String json) {
            return (Map<String, Object>) parse(new StringReader(json));
        }

        private StringReader sr;
        private Object parse(StringReader reader) {
            sr = reader;
            skipWs();
            return parseValue();
        }

        private Object parseValue() {
            skipWs();
            int c = peek();
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == '-' || (c >= '0' && c <= '9')) return parseNumber();
            return parseLiteral();
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            read(); // {
            skipWs();
            if (peek() == '}') { read(); return map; }
            while (true) {
                skipWs();
                String key = (String) parseString();
                skipWs();
                read(); // :
                skipWs();
                map.put(key, parseValue());
                skipWs();
                int c = read();
                if (c == '}') break;
            }
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            read(); // [
            skipWs();
            if (peek() == ']') { read(); return list; }
            while (true) {
                list.add(parseValue());
                skipWs();
                int c = read();
                if (c == ']') break;
            }
            return list;
        }

        private String parseString() {
            read(); // "
            StringBuilder sb = new StringBuilder();
            while (true) {
                int c = read();
                if (c == '"') break;
                if (c == '\\') {
                    int next = read();
                    if (next == 'n') sb.append('\n');
                    else if (next == 't') sb.append('\t');
                    else if (next == 'r') sb.append('\r');
                    else if (next == '\\') sb.append('\\');
                    else if (next == '"') sb.append('"');
                    else if (next == '/') sb.append('/');
                    else if (next == 'u') {
                        int hex = 0;
                        for (int i = 0; i < 4; i++) hex = (hex << 4) + Character.digit((char) read(), 16);
                        sb.append((char) hex);
                    } else sb.append((char) next);
                } else {
                    sb.append((char) c);
                }
            }
            return sb.toString();
        }

        private Number parseNumber() {
            StringBuilder sb = new StringBuilder();
            while (true) {
                int c = peek();
                if (c >= '0' && c <= '9' || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                    sb.append((char) read());
                } else break;
            }
            String s = sb.toString();
            if (s.contains(".") || s.contains("e") || s.contains("E")) return Double.parseDouble(s);
            return Long.parseLong(s);
        }

        private Object parseLiteral() {
            StringBuilder sb = new StringBuilder();
            while (true) {
                int c = peek();
                if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z') sb.append((char) read());
                else break;
            }
            String s = sb.toString();
            if (s.equals("true")) return true;
            if (s.equals("false")) return false;
            return null;
        }

        private void skipWs() {
            while (true) {
                int c = peek();
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') read();
                else break;
            }
        }

        private int peek() { try { sr.mark(1); } catch (Exception e) {} int c = read(); try { sr.reset(); } catch (Exception e) {} return c; }
        private int read() { try { return sr.read(); } catch (Exception e) { return -1; } }

        private String quote(String s) {
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
        }
    }
}
