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
import com.alibaba.cloud.ai.graph.RunnableConfig;

/**
 * Synchronous version of MultiCommandAction.
 *
 * @param <S> the concrete graph state type
 * @see AsyncMultiCommandAction for the async version
 */
@FunctionalInterface
public interface MultiCommandAction<S extends GraphState> {

	/**
	 * Applies this action to the given state and config.
	 * @param state the current graph state
	 * @param config the runnable configuration
	 * @return a {@link MultiCommand} for routing to multiple nodes
	 * @throws Exception if an error occurs
	 */
	MultiCommand<S> apply(S state, RunnableConfig config) throws Exception;

}

