package com.alibaba.cloud.ai.service.run;

import com.alibaba.cloud.ai.exception.NotImplementedException;
import com.alibaba.cloud.ai.model.RunEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Component
public class DefaultRunner<T extends RunnableModel> implements Runner<T> {

    private final List<RunnableBuilder<T>> builders;

    public DefaultRunner(List<RunnableBuilder<T>> builders){
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
        Runnable runnable = builder.build(runnerModel, inputs);
        return runnable.invoke(inputs);
    }

    @Override
    public Flux<RunEvent> stream(T runnerModel, Map<String, Object> inputs) {
        RunnableBuilder<T> builder = getRunnerBuilder(runnerModel.runnerType());
        Runnable runnable = builder.build(runnerModel, inputs);
        return runnable.stream(inputs);
    }


}
