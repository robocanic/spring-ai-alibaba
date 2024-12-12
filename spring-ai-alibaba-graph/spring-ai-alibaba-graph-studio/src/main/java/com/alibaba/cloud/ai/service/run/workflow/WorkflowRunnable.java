package com.alibaba.cloud.ai.service.run.workflow;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.model.RunEvent;
import com.alibaba.cloud.ai.service.run.Runnable;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;

public class WorkflowRunnable implements Runnable {

    private final CompiledGraph<WorkflowState> graph;
    public WorkflowRunnable(CompiledGraph<WorkflowState> graph){
        this.graph = graph;
    }
    @Override
    public RunEvent invoke(Map<String, Object> inputs) {
        try {
            Optional<WorkflowState> workflowState = graph.invoke(inputs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public Flux<RunEvent> stream(Map<String, Object> outputs) {

        return null;
    }
}
