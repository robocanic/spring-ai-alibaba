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
import io.opentelemetry.context.Context;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Represents an asynchronous node action that operates on a graph state and returns a
 * state update.
 *
 * @param <S> the concrete graph state type
 */
@FunctionalInterface
public interface AsyncNodeAction<S extends GraphState> extends Function<S, CompletableFuture<NodeActionResult<S>>> {

	/**
	 * Applies this action to the given agent state.
	 * @param state the agent state
	 * @return a CompletableFuture representing the result of the action
	 */
	CompletableFuture<NodeActionResult<S>> apply(S state);

	/**
	 * Creates an asynchronous node action from a synchronous node action.
	 * @param <S> the concrete graph state type
	 * @param syncAction the synchronous node action
	 * @return an asynchronous node action
	 */
	static <S extends GraphState> AsyncNodeAction<S> node_async(NodeAction<S> syncAction) {
		return state -> {
			Context context = Context.current();
			CompletableFuture<NodeActionResult<S>> result = new CompletableFuture<>();
			try {
				result.complete(syncAction.apply(state));
			}
			catch (Exception e) {
				result.completeExceptionally(e);
			}
			return result;
		};
	}

}
