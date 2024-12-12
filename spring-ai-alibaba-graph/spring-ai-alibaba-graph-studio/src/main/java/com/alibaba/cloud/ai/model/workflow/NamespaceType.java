package com.alibaba.cloud.ai.model.workflow;

import java.util.Arrays;

public enum NamespaceType {
    ENV("ENV", "env"),

    SYS("SYS", "sys"),

    WORKFLOW("WORKFLOW", "conversation"),

    NODE("Node", "node");

    private final String value;

    private final String difyValue;

    public String value(){
        return value;
    }

    public String difyValue(){
        return difyValue;
    }

    public static NamespaceType difyValueOf(String difyValue){
        return Arrays.stream(NamespaceType.values())
                .filter(n -> n.difyValue.equals(difyValue))
                .findFirst()
                .orElse(null);
    }

    NamespaceType(String value, String difyValue) {
        this.value = value;
        this.difyValue = difyValue;
    }

}
