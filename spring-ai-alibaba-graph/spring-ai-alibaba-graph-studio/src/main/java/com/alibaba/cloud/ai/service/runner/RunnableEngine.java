package com.alibaba.cloud.ai.service.runner;

import com.alibaba.cloud.ai.model.RunEvent;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Runner abstract the running action of a runnable model(could be an app, node, etc.)
 */
public interface RunnableEngine<T extends RunnableModel> {

	RunEvent invoke(T runnableModel, Map<String, Object> inputs);

	Flux<RunEvent> stream(T runnableModel, Map<String, Object> inputs);

}
