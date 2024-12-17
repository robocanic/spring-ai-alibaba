package com.alibaba.cloud.ai.service.run;

import com.alibaba.cloud.ai.exception.NotImplementedException;

import java.util.Arrays;

public enum RunnableType {

    WORKFLOW("workflow"),

    CHATBOT("chatbot"),

    NODE("node");

    private final String value;

    public String value(){
        return value;
    }

    public static RunnableType fromValue(String value){
        return Arrays.stream(RunnableType.values())
                .filter(v -> v.value.equals(value))
                .findFirst()
                .orElseThrow(()->new NotImplementedException("Runnable Type" + value + "is not supported yet"));
    }

    RunnableType(String value) {
        this.value = value;
    }
}
