package com.siyuan.siyuan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;


@Data
@ToString
public class QuestionGenerationRequest {
    @NotNull(message = "题型不能为空")
    private Integer questionType;
    @NotBlank(message = "API密钥不能为空")
    private String apiKey;
    @NotBlank(message = "模型名称不能为空")
    private String modelName;
}