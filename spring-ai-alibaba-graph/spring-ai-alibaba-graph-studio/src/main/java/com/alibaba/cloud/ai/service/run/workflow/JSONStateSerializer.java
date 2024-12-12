package com.alibaba.cloud.ai.service.run.workflow;

import com.alibaba.cloud.ai.graph.serializer.plain_text.PlainTextStateSerializer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.NonNull;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class JSONStateSerializer extends PlainTextStateSerializer<WorkflowState> {

	final ObjectMapper objectMapper;

	public JSONStateSerializer() {
		this(new ObjectMapper());
	}

	public JSONStateSerializer(@NonNull ObjectMapper objectMapper) {
        super(WorkflowState::new);
        this.objectMapper = objectMapper;
		this.objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
	}

	@Override
	public String mimeType() {
		return "application/json";
	}

	@Override
	public void write(WorkflowState object, ObjectOutput out) throws IOException {
		var json = objectMapper.writeValueAsString(object);
		out.writeUTF(json);
	}

	@Override
	public WorkflowState read(ObjectInput in) throws IOException, ClassNotFoundException {
		var json = in.readUTF();
		return objectMapper.readValue(json, WorkflowState.class);
	}

}
