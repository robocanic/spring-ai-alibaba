package com.alibaba.cloud.ai.model;

import com.alibaba.cloud.ai.exception.NotImplementedException;
import com.alibaba.cloud.ai.service.run.RunnableModel;
import com.alibaba.cloud.ai.service.run.RunnableType;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified app model definition.
 */
@Data
@NoArgsConstructor
public class App implements RunnableModel {

	private AppMetadata metadata;

	/**
	 * Spec has different implementations depending on the type of application. e.g.
	 * Workflow
	 */
	private Object spec;

	public App(AppMetadata metadata, Object spec) {
		this.metadata = metadata;
		this.spec = spec;
	}

	@Override
	public String id() {
		return metadata.getId();
	}

	@Override
	public RunnableType runnableType() {
		return RunnableType.fromValue(metadata.getMode())
				.orElseThrow(()->new NotImplementedException("Unsupported runnable type: " + metadata.getMode()));
	}

}
