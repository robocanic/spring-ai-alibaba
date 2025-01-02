package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.state.GraphState;
import com.alibaba.cloud.ai.graph.utils.ObjectMapperSingleton;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bsc.async.AsyncGenerator;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.cloud.ai.graph.utils.CollectionsUtils.mapOf;

class SubgraphNodeAction implements AsyncNodeActionWithConfig<Map<String, Object>, Map<String, Object>> {

	final CompiledGraph subGraph;

	SubgraphNodeAction(CompiledGraph subGraph) {
		this.subGraph = subGraph;
	}

	@Override
	public CompletableFuture<Map<String, Object>> apply(Map<String, Object> inputState, RunnableConfig config) {
		CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
		try {
			AsyncGenerator<NodeOutput> generator = subGraph.stream(inputState, config);
			future.complete(mapOf(GraphState.SUB_GRAPH, generator));
		}
		catch (Exception e) {

			future.completeExceptionally(e);
		}

		return future;
	}
}
