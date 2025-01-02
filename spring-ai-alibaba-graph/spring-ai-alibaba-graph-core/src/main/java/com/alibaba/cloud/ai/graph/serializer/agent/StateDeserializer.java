package com.alibaba.cloud.ai.graph.serializer.agent;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.state.GraphState;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class StateDeserializer extends JsonDeserializer<GraphState> {

	@Override
	public GraphState deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
		JsonNode node = parser.getCodec().readTree(parser);

		Map<String, Object> data = new HashMap<>();

		var dataNode = node.has("data") ? node.get("data") : node;
		if (dataNode.has(GraphState.INPUT) && StringUtils.hasText(dataNode.get(GraphState.INPUT).asText())) {
			data.put(GraphState.INPUT, dataNode.get(GraphState.INPUT).asText());
		}
		if (dataNode.has(GraphState.OUTPUT)) {
			JsonNode outputNode = dataNode.get(GraphState.OUTPUT);
			if (StringUtils.hasText(outputNode.asText())) {
				data.put(GraphState.OUTPUT, outputNode.asText());
			}
			else {
				if (!outputNode.isNull()) {
					var agentOutcome = ctx.readValue(outputNode.traverse(parser.getCodec()), AgentOutcome.class);
					data.put("agent_outcome", agentOutcome);
				}
			}
		}
		if (dataNode.has(GraphState.SUB_GRAPH)) {
			JsonNode outputNode = dataNode.get(GraphState.SUB_GRAPH);
			var agentOutcome = ctx.readValue(outputNode.traverse(parser.getCodec()),
					CompiledGraph.AsyncNodeGenerator.class);
			data.put(GraphState.SUB_GRAPH, agentOutcome);
		}

		return new GraphState(data);
	}

}
