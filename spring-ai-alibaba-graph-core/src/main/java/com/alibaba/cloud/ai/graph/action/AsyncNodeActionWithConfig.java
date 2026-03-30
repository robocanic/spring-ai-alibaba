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
import io.opentelemetry.context.Context;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * Asynchronous node action with access to {@link RunnableConfig}.
 *
 * @param <S> the concrete graph state type
 */
public interface AsyncNodeActionWithConfig<S extends GraphState>
		extends BiFunction<S, RunnableConfig, CompletableFuture<NodeActionResult<S>>> {

	/**
	 * Applies this action to the given agent state.
	 * @param state the agent state
	 * @param config the runnable configuration
	 * @return a CompletableFuture representing the result of the action
	 */
	CompletableFuture<NodeActionResult<S>> apply(S state, RunnableConfig config);

	/**
	 * Creates an async node action with config from a synchronous one.
	 * @param <S> the concrete graph state type
	 * @param syncAction the synchronous action
	 * @return an async wrapper
	 */
	static <S extends GraphState> AsyncNodeActionWithConfig<S> node_async(NodeActionWithConfig<S> syncAction) {
		return (state, config) -> {
			Context context = Context.current();
            CompletableFuture<NodeActionResult<S>> result = new CompletableFuture<>();
			try {
				result.complete(syncAction.apply(state, config));
			}
			catch (Exception e) {
				result.completeExceptionally(e);
			}
			return result;
		};
	}

	/**
	 * Adapts a simple {@link AsyncNodeAction} to an {@link AsyncNodeActionWithConfig}.
	 * @param <S> the concrete graph state type
	 * @param action the simple action to be adapted
	 * @return an {@link AsyncNodeActionWithConfig} that wraps the given action
	 */
	static <S extends GraphState> AsyncNodeActionWithConfig<S> of(AsyncNodeAction<S> action) {
		if (action instanceof InterruptableAction) {
			return new InterruptableAsyncNodeActionWrapper<>(action, (InterruptableAction<S>) action);
		}
		return (t, config) -> action.apply(t);
	}

	class InterruptableAsyncNodeActionWrapper<S extends GraphState>
			implements AsyncNodeActionWithConfig<S>, InterruptableAction<S> {

		private final AsyncNodeAction<S> delegate;

		private final InterruptableAction<S> interruptable;

		public InterruptableAsyncNodeActionWrapper(AsyncNodeAction<S> delegate, InterruptableAction<S> interruptable) {
			this.delegate = delegate;
			this.interruptable = interruptable;
		}

		@Override
		public CompletableFuture<NodeActionResult<S>> apply(S state, RunnableConfig config) {
			return delegate.apply(state);
		}

		@Override
		public Optional<InterruptionMetadata<S>> interrupt(String nodeId, S state, RunnableConfig config) {
			return interruptable.interrupt(nodeId, state, config);
		}

		@Override
		public Optional<InterruptionMetadata<S>> interruptAfter(String nodeId, S state,
				NodeActionResult<S> actionResult, RunnableConfig config) {
			return interruptable.interruptAfter(nodeId, state, actionResult, config);
		}
	}

}
