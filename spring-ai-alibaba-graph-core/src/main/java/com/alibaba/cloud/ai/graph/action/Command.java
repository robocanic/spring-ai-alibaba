/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.GraphState;

import java.util.Objects;

/**
 * Represents the outcome of a {@link CommandAction} within a graph. A {@code Command}
 * encapsulates instructions for the graph's next step, including an optional target node
 * to transition to and an optional partial state update.
 *
 * <p>
 * The {@code update} field is a POJO of type {@code S} (or {@code null} for routing-only
 * commands). When {@code update} is non-null, the framework calls
 * {@link com.alibaba.cloud.ai.graph.utils.StateFieldScanner#toMap} skipping
 * {@code null}-valued fields to produce a partial {@code Map} update.
 * </p>
 *
 * @param <S> the concrete graph state type
 * @param gotoNode the name of the next node to execute
 * @param update optional partial state POJO; {@code null} means no state change
 */
public record Command<S extends GraphState>(String gotoNode, S update) {

	public Command {
		Objects.requireNonNull(gotoNode, "gotoNode cannot be null");
		// update may be null (routing-only command)
	}

	/**
	 * Constructs a routing-only {@code Command} with no state update.
	 * @param gotoNode The name of the next node to transition to. Can be null.
	 */
	public Command(String gotoNode) {
		this(gotoNode, null);
	}

}
