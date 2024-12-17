package com.alibaba.cloud.ai.service.run.workflow;

import com.alibaba.cloud.ai.graph.state.AgentState;
import com.alibaba.cloud.ai.graph.state.AppenderChannel;
import com.alibaba.cloud.ai.graph.state.Channel;
import com.alibaba.cloud.ai.model.workflow.NamespaceType;

import java.util.ArrayList;
import java.util.Map;

public class WorkflowState extends AgentState {

    private static final String SEPARATOR = ".";

    public static final String WORKFLOW_ID = stateKey(NamespaceType.SYS.value(), "workflowId");

    public static final String RUN_ID = stateKey(NamespaceType.SYS.value(), "runId");

    public static final String MESSAGES = stateKey(NamespaceType.WORKFLOW.value(), "messages");

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            WORKFLOW_ID, Channel.of(()-> ""),
            RUN_ID, Channel.of(()-> ""),
            MESSAGES, AppenderChannel.of(ArrayList::new)
    );

    public WorkflowState(Map<String, Object> initData){
        super(initData);
    }

    public static String stateKey(String namespace, String name){
        return namespace + SEPARATOR + name;
    }
}
