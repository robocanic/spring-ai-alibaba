package com.alibaba.cloud.ai.model;

import lombok.Data;
import lombok.experimental.Accessors;
import com.alibaba.cloud.ai.service.runner.RunnableEngine;
import com.alibaba.cloud.ai.service.runner.Runnable;
import java.util.Map;

/**
 * RunEvent defines a single event emitted by the {@link RunnableEngine} and {@link Runnable}.
 */
@Data
@Accessors(chain = true)
public class RunEvent {

	private String eventType;

	private String runId;

	private String runnableId;

	private Map<String, Object> data;

	public RunEvent(String eventType){
		this.eventType = eventType;
	}

	public enum EventType {

		RUNNABLE_BUILDING("runnable building"),

		RUNNABLE_BUILT("runnable built"),

		RUNNABLE_STARTED("runnable started"),

		RUNNABLE_FINISHED("runnable finished"),

		NODE_STARTED("node started"),

		NODE_FINISHED("node finished");

		private String value;

		public String value() {
			return value;
		}

		EventType(String value) {
			this.value = value;
		}
	}

}
