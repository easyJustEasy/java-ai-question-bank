package com.siyuan.siyuan.controller;


import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.siyuan.siyuan.dto.QuestionGenerationRequest;
import com.siyuan.siyuan.dto.QuestionGenerationResponse;
import com.siyuan.siyuan.service.QuestionGenerationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@CrossOrigin(origins = "*") // 生产环境应配置具体的前端域名
@RequestMapping("/api")
public class QuestionGenerationController {

    @Autowired
    private QuestionGenerationService questionGenerationService;

    /**
     * 生成Java试题
     * @param request 包含题型和API Key的请求对象
     * @return 包含生成页面URL的响应
     */
    @PostMapping("/generate-java-questions")
    public ResponseEntity<QuestionGenerationResponse> generateQuestions(
            @Valid @RequestBody QuestionGenerationRequest request) throws NoApiKeyException, InputRequiredException {
        String generatedUrl = questionGenerationService.generateQuestions(request);

        QuestionGenerationResponse response = new QuestionGenerationResponse();
        response.setUrl(generatedUrl);
        response.setMessage("试题生成成功");

        return ResponseEntity.ok(response);
    }
}