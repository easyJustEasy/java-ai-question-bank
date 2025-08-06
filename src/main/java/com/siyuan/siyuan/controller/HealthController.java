// 可选：添加健康检查端点
package com.siyuan.siyuan.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "Java Test Generator API");
        status.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return status;
    }
}