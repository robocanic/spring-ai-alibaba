package com.alibaba.cloud.ai.service.dsl;

import com.alibaba.cloud.ai.exception.NotImplementedException;

import java.util.Arrays;
import java.util.Optional;

/**
 * DSLDialectType represent a specific dsl format
 */
public enum DSLDialectType {
    DIFY("dify", ".yml"),

    CUSTOM("custom", ".yml")
    ;
    private String value;

    private String fileExtension;

    public String value(){
        return value;
    }

    public String fileExtension(){
        return fileExtension;
    }

    public static Optional<DSLDialectType> fromValue(String value){
        return Arrays.stream(DSLDialectType.values())
                .filter(t -> t.value.equals(value))
                .findFirst();
    }

    DSLDialectType(String value, String fileExtension){
        this.value = value;
        this.fileExtension = fileExtension;
    }
}
