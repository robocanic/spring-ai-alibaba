package com.alibaba.cloud.ai.service.run;

import com.alibaba.cloud.ai.exception.NotImplementedException;
import com.alibaba.cloud.ai.exception.RunFailedException;
import com.alibaba.cloud.ai.model.RunEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class DefaultRunEngine<T extends RunnableModel> implements Runner<T> {

    private final List<RunnableBuilder<T>> builders;

    public DefaultRunEngine(List<RunnableBuilder<T>> builders){
        this.builders = builders;
    }

    private RunnableBuilder<T> getRunnerBuilder(String runnableType){
        return builders.stream().
                filter(builder -> builder.support(runnableType))
                .findFirst()
                .orElseThrow(() -> new NotImplementedException("No runnable builder found for type " + runnableType));
    }

    @Override
    public RunEvent invoke(T runnerModel, Map<String, Object> inputs) {
        RunnableBuilder<T> builder = getRunnerBuilder(runnerModel.runnerType());
        Runnable runnable;
        String runId = UUID.randomUUID().toString();
        try {
            runnable = builder.build(runnerModel, runId, inputs);
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
    public Flux<RunEvent> stream(T runnerModel, Map<String, Object> inputs) {
        RunnableBuilder<T> builder = getRunnerBuilder(runnerModel.runnerType());
        Runnable runnable;
        String runId = UUID.randomUUID().toString();
        Flux<RunEvent> buildingFlux = Flux.just(new RunEvent(RunEvent.EventType.RUNNABLE_BUILT.value())
                .setRunId(runId).setRunnableId(runnerModel.id()));
        try {
            runnable = builder.build(runnerModel, runId, inputs);
        }catch (Exception e) {
            throw new RunFailedException("Graph build error" + e.getMessage(), e);
        }
        Flux<RunEvent> builtFlux = Flux.just(new RunEvent(RunEvent.EventType.RUNNABLE_BUILT.value())
                .setRunId(runId).setRunnableId(runnerModel.id()));
        Flux<RunEvent> startedFlux = Flux.just(new RunEvent(RunEvent.EventType.RUNNABLE_STARTED.value())
                .setRunId(runId).setRunnableId(runnerModel.id()));
        Flux<RunEvent> runEventFlux;
        try {
            runEventFlux = runnable.stream().map(runEvent -> runEvent.setRunId(runId).setRunnableId(runnerModel.id()));
        }catch (Exception e){
            throw new RunFailedException("Graph run error" + e.getMessage(), e);
        }
        Flux<RunEvent> finisedFlux = Flux.just(new RunEvent(RunEvent.EventType.RUNNABLE_FINISHED.value())
                .setRunId(runId).setRunnableId(runnerModel.id()));
        return Flux.concat(buildingFlux, builtFlux, startedFlux, runEventFlux, finisedFlux);
    }


}
