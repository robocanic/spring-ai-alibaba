package com.alibaba.cloud.ai.graph.serializer.agent;

import com.alibaba.cloud.ai.graph.serializer.plain_text.PlainTextStateSerializer;
import com.alibaba.cloud.ai.graph.state.GraphState;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.NonNull;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

public class JSONStateSerializer extends PlainTextStateSerializer {

	public static final JSONStateSerializer INSTANCE = new JSONStateSerializer();

	final ObjectMapper objectMapper;

	public JSONStateSerializer() {
		this(new ObjectMapper());
	}

	public JSONStateSerializer(@NonNull ObjectMapper objectMapper) {
		super(GraphState::new);
		this.objectMapper = objectMapper;
		this.objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

		var module = new SimpleModule();
		module.addDeserializer(GraphState.class, new StateDeserializer());
		module.addDeserializer(AgentOutcome.class, new AgentOutcomeDeserializer());
		module.addDeserializer(AgentAction.class, new AgentActionDeserializer());
		module.addDeserializer(AgentFinish.class, new AgentFinishDeserializer());

		objectMapper.registerModule(module);
	}

	@Override
	public String mimeType() {
		return "application/json";
	}

	@Override
	public void write(GraphState object, ObjectOutput out) throws IOException {
		var json = objectMapper.writeValueAsString(object.data());
		out.writeUTF(json);
	}

	@Override
	public GraphState read(ObjectInput in) throws IOException, ClassNotFoundException {
		var json = in.readUTF();
		Map<String, Object> stateData = objectMapper.readValue(json, new TypeReference<>() {});
		return new GraphState(stateData);
	}

}
