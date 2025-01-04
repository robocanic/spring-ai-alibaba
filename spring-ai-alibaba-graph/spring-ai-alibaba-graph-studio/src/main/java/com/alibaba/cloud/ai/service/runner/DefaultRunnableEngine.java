package com.alibaba.cloud.ai.service.runner;

import com.alibaba.cloud.ai.exception.NotImplementedException;
import com.alibaba.cloud.ai.exception.RunFailedException;
import com.alibaba.cloud.ai.model.RunEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class DefaultRunnableEngine<T extends RunnableModel> implements RunnableEngine<T> {

    private final List<RunnableBuilder<T>> builders;

    public DefaultRunnableEngine(List<RunnableBuilder<T>> builders){
        this.builders = builders;
    }

    private RunnableBuilder<T> getRunnableBuilder(RunnableType runnableType){
        return builders.stream().
                filter(builder -> builder.support(runnableType))
                .findFirst()
                .orElseThrow(() -> new NotImplementedException("No runnable builder found for type " + runnableType));
    }

    @Override
    public RunEvent invoke(T runnableModel, Map<String, Object> inputs) {
        RunnableBuilder<T> builder = getRunnableBuilder(runnableModel.runnableType());
        Runnable runnable;
        String runId = UUID.randomUUID().toString();
        try {
            runnable = builder.build(runnableModel, runId, inputs);
        } catch (Exception e) {
            throw new RunFailedException("Runnable build error" + e.getMessage(), e);
        }

        try {
            return runnable.invoke();
        }catch (Exception e){
            throw new RunFailedException("Runnable run error" + e.getMessage(), e);
        }
    }

    @Override
    public Flux<RunEvent> stream(T runnableModel, Map<String, Object> inputs) {
        RunnableBuilder<T> builder = getRunnableBuilder(runnableModel.runnableType());
        Runnable runnable;
        String runId = UUID.randomUUID().toString();
        Flux<RunEvent> buildingFlux = Flux.just(new RunEvent(RunEvent.EventType.RUNNABLE_BUILDING.value())
                .setRunId(runId).setRunnableId(runnableModel.id()));
        try {
            runnable = builder.build(runnableModel, runId, inputs);
        }catch (Exception e) {
            throw new RunFailedException("Graph build error: " + e.getMessage(), e);
        }
        Flux<RunEvent> builtFlux = Flux.just(new RunEvent(RunEvent.EventType.RUNNABLE_BUILT.value())
                .setRunId(runId).setRunnableId(runnableModel.id()));
        Flux<RunEvent> startedFlux = Flux.just(new RunEvent(RunEvent.EventType.RUNNABLE_STARTED.value())
                .setRunId(runId).setRunnableId(runnableModel.id()));
        Flux<RunEvent> runEventFlux;
        try {
            runEventFlux = runnable.stream().map(runEvent -> runEvent.setRunId(runId).setRunnableId(runnableModel.id()));
        }catch (Exception e){
            throw new RunFailedException("Graph run error: " + e.getMessage(), e);
        }
        Flux<RunEvent> finisedFlux = Flux.just(new RunEvent(RunEvent.EventType.RUNNABLE_FINISHED.value())
                .setRunId(runId).setRunnableId(runnableModel.id()));
        return Flux.concat(buildingFlux, builtFlux, startedFlux, runEventFlux, finisedFlux);
    }


}
