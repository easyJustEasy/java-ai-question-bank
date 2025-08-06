package com.siyuan.siyuan.service;

import cn.hutool.core.io.FileUtil;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.siyuan.siyuan.config.AppConfig;
import com.siyuan.siyuan.consts.QuestionType;
import com.siyuan.siyuan.dto.QuestionGenerationRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

@Service
public class QuestionGenerationService {
    @Autowired
    private AppConfig appConfig;


    /**
     * 根据题型生成试题并返回可访问的URL
     */
    public String generateQuestions(@Valid QuestionGenerationRequest questionType) throws NoApiKeyException, InputRequiredException {
        String question = convertQuestion(questionType.getQuestionType());
        GenerationResult result = callWithMessage(questionType, question);
        System.out.println( result);
        String text = result.getOutput().getChoices().get(0).getMessage().getContent();
        String fileName = questionType.getQuestionType() + File.separator + UUID.randomUUID() + ".html";
        String file = appConfig.getFilePath()+File.separator+fileName;
        FileUtil.mkParentDirs( file);
        FileUtil.touch( file);
        FileUtil.writeString(text.replace("```html","").replace("```",""), file, "utf-8");
        return appConfig.getBaseUrl() + fileName;
    }

    private String convertQuestion(@NotBlank(message = "题型不能为空") Integer questionType) {
        return "写一个html网页，要求里面有10道java方面的" +
                QuestionType.formCodeName(questionType) +
                "，难度适中，先隐藏答案，等用户提交后显示答案，并给出对错判断。我只需要返回能直接运行的html部分，不需要额外的信息";
    }


    public GenerationResult callWithMessage(QuestionGenerationRequest request, String question) throws ApiException, NoApiKeyException, InputRequiredException {
        Generation gen = new Generation();
        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content("你是一个java专家，擅长所有java问题")
                .build();
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(question)
                .build();
        GenerationParam param = GenerationParam.builder()
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                .apiKey(request.getApiKey())
                // 此处以qwen-plus为例，可按需更换模型名称。模型列表：https://help.aliyun.com/zh/model-studio/getting-started/models
                .model(appConfig.getModelName())
                .messages(Arrays.asList(systemMsg, userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .enableThinking(false).enableSearch(false)
                .build();
        return gen.call(param);
    }
}