package com.alibaba.cloud.ai.service.run.workflow;

import com.alibaba.cloud.ai.graph.state.AgentState;
import com.alibaba.cloud.ai.graph.state.Channel;
import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.workflow.NamespaceType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowState extends AgentState {

    public static final String SEPARATOR = ".";

    public static final String WORKFLOW_ID = "appId";

    public static final String MESSAGES = "messages";

    public static final Map<String, Channel<?>> SCHEMA = Map.of(

    );

    public WorkflowState(Map<String, Object> initData){
        super(initData);
    }

    public static class Builder{
        private Map<String, Object> initData;

        public Builder(){
            this.initData = new HashMap<>();
        }

        public Builder workflowVars(List<Variable> workflowVars){
            workflowVars.forEach(v -> initData.put(stateKey(NamespaceType.WORKFLOW.value(), v.getName()), v.getValue()));
            return this;
        }

        public Builder envVars(List<Variable> envVars){
            envVars.forEach(v -> initData.put(stateKey(NamespaceType.ENV.value(), v.getName()), v.getValue()));
            return this;
        }

        public WorkflowState build(){
            return new WorkflowState(initData);
        }
    }


    public static String stateKey(String namespace, String name){
        return namespace + SEPARATOR + name;
    }
}
