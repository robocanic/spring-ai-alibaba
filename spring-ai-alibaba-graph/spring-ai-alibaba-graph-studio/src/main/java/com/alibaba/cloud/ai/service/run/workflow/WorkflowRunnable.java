package com.alibaba.cloud.ai.service.run.workflow;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.model.RunEvent;
import com.alibaba.cloud.ai.service.run.Runnable;
import org.bsc.async.AsyncGenerator;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class WorkflowRunnable implements Runnable {

    private final CompiledGraph<WorkflowState> graph;

    private final Map<String, Object> inputs;

    public WorkflowRunnable(CompiledGraph<WorkflowState> graph, Map<String, Object> inputs){
        this.graph = graph;
        this.inputs = inputs;
    }


    @Override
    public RunEvent invoke() throws Exception{
        Optional<WorkflowState> workflowState = graph.invoke(inputs);
        return null;
    }

    @Override
    public Flux<RunEvent> stream() throws Exception{
        AsyncGenerator<NodeOutput<WorkflowState>> generator = graph.stream(inputs);

        Stream<RunEvent> runEventStream = generator.stream().map(output -> {
            Map<String, Object> data = Map.of("nodeId", output.node(), "output", output.state());
            return new RunEvent(RunEvent.EventType.NODE_FINISHED.value())
                    .setData(data);
        });
        return Flux.fromStream(runEventStream);
    }
}
