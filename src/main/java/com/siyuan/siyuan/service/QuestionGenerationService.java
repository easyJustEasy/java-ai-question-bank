package com.siyuan.siyuan.service;

import cn.hutool.core.date.StopWatch;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;
@Slf4j
@Service
public class QuestionGenerationService {
    @Autowired
    private AppConfig appConfig;
private static final String KNOWLEDGE = "Java：语法、OOP、集合、异常、泛型、多线程、I/O、JVM、常用库。  \n" +
        "计算机基础：OS、网络、数据结构与算法、编译原理、组成原理。  \n" +
        "数据库：关系型/非关系型、SQL、设计、事务。  \n" +
        "开发工具：Git、Maven/Gradle、IDE、调试、测试。  \n" +
        "常用框架：Spring、MyBatis、Hibernate、Netty、其他。  \n" +
        "系统设计：架构、设计模式、API、高并发、安全。  \n" +
        "分布式：系统理论、消息队列、服务发现、缓存、事务。  \n" +
        "高性能：优化、缓存、异步、CDN。  \n" +
        "高可用：容错、监控、恢复、弹性伸缩。";

    /**
     * 根据题型生成试题并返回可访问的URL
     */
    public String generateQuestions(@Valid QuestionGenerationRequest questionType) throws NoApiKeyException, InputRequiredException {
        log.info("开始生成题型：{}",QuestionType.formCodeName(questionType.getQuestionType()));
        //增加耗时统计
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("convertQuestion");
        String question = convertQuestion(questionType.getQuestionType());
        log.info("开始生成题型：{}", question);
        stopWatch.stop();
        stopWatch.start("callWithMessage");
        GenerationResult result = callWithMessage(questionType, question);
        stopWatch.stop();
        log.info("耗时统计：{}", stopWatch.prettyPrint());
        log.info("生成成功 {}，开始保存文件",result);
        String text = result.getOutput().getChoices().get(0).getMessage().getContent();
        String fileName = questionType.getQuestionType() + File.separator + UUID.randomUUID() + ".html";
        String file = appConfig.getFilePath()+File.separator+fileName;
        FileUtil.mkParentDirs( file);
        FileUtil.touch( file);
        FileUtil.writeString(text.replace("```html","").replace("```",""), file, "utf-8");
        log.info("保存成功 {}",file);
        return appConfig.getBaseUrl() + fileName;
    }

    private String convertQuestion(@NotBlank(message = "题型不能为空") Integer questionType) {
        return "写一个html网页，要求里面有10道java方面的" +
                QuestionType.formCodeName(questionType) +
                "，难度偏难，先隐藏答案，用户提交后显示答案。我只需要返回能直接运行的html部分，不需要额外的信息";
    }


    public GenerationResult callWithMessage(QuestionGenerationRequest request, String question) throws ApiException, NoApiKeyException, InputRequiredException {
        Generation gen = new Generation();
        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content("你是一个java专家，擅长所有java问题,需要你帮用户生成一些题目，涉及的知识点有："+ KNOWLEDGE)
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