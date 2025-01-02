package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.RunnableConfig;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public interface AsyncNodeActionWithConfig<InputState, OutputState>
		extends BiFunction<InputState, RunnableConfig, CompletableFuture<OutputState>> {

	/**
	 * Applies this action to the given agent state.
	 * @param inputState the agent state
	 * @return a CompletableFuture representing the result of the action
	 */
	CompletableFuture<OutputState> apply(InputState inputState, RunnableConfig config);

	static<T,R> AsyncNodeActionWithConfig<T,R> node_async(NodeActionWithConfig<T,R> syncAction) {
		return (t, config) -> {
			CompletableFuture<R> result = new CompletableFuture<>();
			try {
				result.complete(syncAction.apply(t, config));
			}
			catch (Exception e) {
				result.completeExceptionally(e);
			}
			return result;
		};
	}

	/**
	 * Adapts a simple AsyncNodeAction to an AsyncNodeActionWithConfig.
	 * @param action the simple AsyncNodeAction to be adapted
	 * @return an AsyncNodeActionWithConfig that wraps the given AsyncNodeAction
	 */
	static <T,R> AsyncNodeActionWithConfig<T,R> of(AsyncNodeAction<T,R> action) {
		return (t, config) -> action.apply(t);
	}

}
