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

import com.alibaba.cloud.ai.graph.OverAllState;

import java.util.Map;

/**
 * Backward-compatibility shim for existing code that uses the old
 * {@code Map<String, Object>}-returning node action signature.
 *
 * <p>
 * Before the state-generics change, node actions had the signature:
 * </p>
 *
 * <pre>{@code
 * Map<String, Object> apply(OverAllState state) throws Exception;
 * }</pre>
 *
 * <p>
 * After the change, the canonical signature is:
 * </p>
 *
 * <pre>{@code
 * NodeActionResult<S> apply(S state) throws Exception;
 * }</pre>
 *
 * <p>
 * This interface preserves the old signature and provides a {@link #wrap(OverallStateNodeAction)}
 * factory method to bridge the old implementation into the new generic API:
 * </p>
 *
 * <pre>{@code
 * OverallStateNodeAction legacyAction = state -> Map.of("result", compute(state));
 * NodeAction<OverAllState> wrapped = OverallStateNodeAction.wrap(legacyAction);
 * stateGraph.addNode("myNode", wrapped);
 * }</pre>
 *
 * @author spring-ai-alibaba
 * @since 1.0
 */
@FunctionalInterface
public interface OverallStateNodeAction {

	/**
	 * Applies this action to the given {@link OverAllState} and returns a partial state
	 * update map.
	 * @param state the current agent state
	 * @return a (possibly empty) map of partial state updates
	 * @throws Exception if an error occurs during the action
	 */
	Map<String, Object> apply(OverAllState state) throws Exception;

	/**
	 * Wraps an {@code OverallStateNodeAction} into a {@link NodeAction}{@code <OverAllState>}.
	 *
	 * <p>
	 * The returned action:
	 * </p>
	 * <ol>
	 *   <li>Passes the {@link OverAllState} instance into the legacy action.</li>
	 *   <li>Takes the returned partial map and merges it via
	 *       {@link OverAllState#updateState(Map)}.</li>
	 *   <li>Wraps the updated state in {@link NodeActionResult}.</li>
	 * </ol>
	 * @param action the legacy action to wrap; must not be {@code null}
	 * @return a {@link NodeAction}{@code <OverAllState>} backed by the legacy action
	 */
	static NodeAction<OverAllState> wrap(OverallStateNodeAction action) {
		if (action == null) {
			throw new IllegalArgumentException("action must not be null");
		}
		return state -> {
			Map<String, Object> partialUpdate = action.apply(state);
			// Store the delta in the result so the executor can apply it correctly
			// (respecting key strategies such as AppendStrategy).
			// Do NOT modify the state here to avoid double-merging.
			return NodeActionResult.ofLegacy(state, partialUpdate);
		};
	}

}
