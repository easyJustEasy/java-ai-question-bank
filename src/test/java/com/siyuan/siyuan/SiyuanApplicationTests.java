package com.siyuan.siyuan;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.siyuan.siyuan.consts.QuestionType;
import com.siyuan.siyuan.dto.QuestionGenerationRequest;
import com.siyuan.siyuan.service.QuestionGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SiyuanApplicationTests {
	@Autowired
	private QuestionGenerationService questionGenerationService;

	@Test
	void contextLoads() throws NoApiKeyException, InputRequiredException {
		QuestionGenerationRequest questionGenerationRequest = new QuestionGenerationRequest();
		questionGenerationRequest.setQuestionType(QuestionType.SELECTION.getValue());
		questionGenerationRequest.setApiKey("sk-e509549ef737448dac317a0e4b0f57f9");

		String s = questionGenerationService.generateQuestions(questionGenerationRequest);
		System.out.println(s);
	}

}
