package com.alibaba.cloud.ai.model.workflow;

import com.alibaba.cloud.ai.exception.NotImplementedException;

import java.util.Arrays;
import java.util.Optional;

public enum NamespaceType {
    ENV("env", "env"),

    SYS("system", "sys"),

    WORKFLOW("workflow", "conversation"),

    NODE("node", "node");

    private final String value;

    private final String difyValue;

    public String value(){
        return value;
    }

    public String difyValue(){
        return difyValue;
    }

    public static Optional<NamespaceType> fromDifyValue(String difyValue){
         return Arrays.stream(NamespaceType.values())
                .filter(n -> n.difyValue.equals(difyValue))
                .findFirst();
    }

    NamespaceType(String value, String difyValue) {
        this.value = value;
        this.difyValue = difyValue;
    }

}
