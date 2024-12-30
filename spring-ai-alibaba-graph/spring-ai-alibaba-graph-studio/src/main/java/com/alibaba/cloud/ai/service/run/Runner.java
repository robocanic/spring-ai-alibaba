package com.alibaba.cloud.ai.service.run;

import com.alibaba.cloud.ai.model.RunEvent;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Runner abstract the running action of a runnable model(could be an app, node, etc.)
 */
public interface Runner<T extends RunnableModel> {

	RunEvent invoke(T runnerModel, Map<String, Object> inputs);

	Flux<RunEvent> stream(T runnerModel, Map<String, Object> inputs);

}
