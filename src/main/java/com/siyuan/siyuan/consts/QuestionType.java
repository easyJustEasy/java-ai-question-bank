package com.siyuan.siyuan.consts;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public enum QuestionType {
//        <option value="1">选择题</option>
//                    <option value="2">判断题</option>
//                    <option value="3">问答题</option>
    SELECTION(1, "选择题"),
    JUDGMENT(2, "判断题"),
    QUESTION_ANSWERING(3, "问答题");

    private final Integer value;
    private final String name;
    QuestionType(Integer value, String name) {
        this.value = value;
        this.name = name;
    }

    public static String formCodeName(@NotBlank(message = "题型不能为空") Integer questionType) {
        for (QuestionType type : QuestionType.values()) {
            if (type.value.equals(questionType)) {
                return type.name;
            }
        }
        return null;
    }
}
