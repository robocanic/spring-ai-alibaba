package com.alibaba.cloud.ai.model;


import java.util.Arrays;
import java.util.Optional;

public enum AppMode {

    CHATBOT("chatbot"),

    WORKFLOW("workflow");

    private final String value;

    public String value(){
        return this.value;
    }

    public static Optional<AppMode> fromValue(String value){
        return Arrays.stream(AppMode.values())
                .filter(appMode -> appMode.value.equals(value))
                .findFirst();
    }

    AppMode(String value){
        this.value = value;
    }
}
