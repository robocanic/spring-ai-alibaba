package com.alibaba.cloud.ai.graph;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class NodeAttributes {

    public static final NodeAttributes EMPTY = new NodeAttributes(List.of(), List.of(), Map.of());

    // list of input key
    private List<String> inputSchema;

    // list of output key
    private List<String> outputSchema;

    private Map<String, Object> properties;

    public NodeAttributes() {
        this.inputSchema = new ArrayList<>();
        this.outputSchema = new ArrayList<>();
        this.properties = new HashMap<>();
    }

    public NodeAttributes(List<String> inputSchema, List<String> outputSchema, Map<String, Object> properties) {
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
        this.properties = properties;
    }

    public void addProperty(String key, Object value){
        properties.put(key, value);
    }

    public void addInputSchema(String input){
        inputSchema.add(input);
    }

    public void addOutputSchema(String output){
        outputSchema.add(output);
    }


}
