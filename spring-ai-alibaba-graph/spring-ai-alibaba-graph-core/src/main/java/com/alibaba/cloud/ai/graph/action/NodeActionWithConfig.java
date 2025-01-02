package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.state.GraphState;

import java.util.Map;

@FunctionalInterface
public interface NodeActionWithConfig<T,R> {

	R apply(T t, RunnableConfig config) throws Exception;

}
