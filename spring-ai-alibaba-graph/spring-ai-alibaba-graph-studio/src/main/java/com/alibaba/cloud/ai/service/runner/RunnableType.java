package com.alibaba.cloud.ai.service.runner;

import java.util.Arrays;
import java.util.Optional;

public enum RunnableType {

    WORKFLOW("workflow"),

    CHATBOT("chatbot"),

    NODE("node");

    private final String value;

    public String value(){
        return value;
    }

    public static Optional<RunnableType> fromValue(String value){
        return Arrays.stream(RunnableType.values())
                .filter(v -> v.value.equals(value))
                .findFirst();
    }

    RunnableType(String value) {
        this.value = value;
    }
}
