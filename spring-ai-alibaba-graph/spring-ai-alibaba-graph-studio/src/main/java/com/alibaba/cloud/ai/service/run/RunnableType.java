package com.alibaba.cloud.ai.service.run;

public enum RunnableType {

    WORKFLOW("WORKFLOW"),

    CHATBOT("CHATBOT"),

    NODE("NODE");

    private final String value;

    public String value(){
        return value;
    }

    RunnableType(String value) {
        this.value = value;
    }
}
