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
import reactor.core.publisher.Flux;


/**
 * Unified return type for all {@link NodeAction} implementations.
 *
 * <p>
 * A {@code NodeActionResult} encapsulates:
 * </p>
 * <ul>
 *   <li>A state update (the POJO after the node has finished its work).</li>
 *   <li>An optional streaming {@link Flux} for streaming nodes that emit incremental output.</li>
 * </ul>
 *
 * <p>
 * Construction examples:
 * </p>
 *
 * <pre>{@code
 * // Non-streaming result
 * return NodeActionResult.of(updatedState);
 *
 * // Streaming result – framework detects hasStreamingFlux() == true
 * return NodeActionResult.ofStreaming(updatedState, chatModelFlux);
 * }</pre>
 *
 * @param <S> the concrete graph state type
 * @author spring-ai-alibaba
 * @since 1.0
 */
public final class NodeActionResult<S extends GraphState> {

	private final S state;

	private final Flux<?> streamingFlux;


	private NodeActionResult(S state, Flux<?> streamingFlux) {
		this.state = state;
		this.streamingFlux = streamingFlux;
	}

	/**
	 * Creates an empty {@code NodeActionResult} with a null state, streaming flux, and legacy delta.
	 * This is useful for indicating that no state update is required.
	 * @param <S> the concrete graph state type
	 * @return an empty result
	 */
	public static <S extends GraphState> NodeActionResult<S> empty() {
		return new NodeActionResult<>(null, null);
	}

	/**
	 * Creates a non-streaming {@code NodeActionResult} with only a state update.
	 * @param <S> the concrete graph state type
	 * @param state the updated state returned by the node; must not be {@code null}
	 * @return a non-streaming result
	 */
	public static <S extends GraphState> NodeActionResult<S> of(S state) {
		if (state == null) {
			throw new IllegalArgumentException("state must not be null");
		}
		return new NodeActionResult<>(state, null);
	}

	/**
	 * Creates a streaming {@code NodeActionResult} that carries both a state update and a
	 * streaming {@link Flux}.
	 *
	 * <p>
	 * The framework will call {@link #hasStreamingFlux()} to detect this path, then use
	 * {@link com.alibaba.cloud.ai.graph.utils.StateFieldScanner#getStreamingFieldKey} to
	 * determine which field receives the streaming output.
	 * </p>
	 * @param <S> the concrete graph state type
	 * @param <T> the type of items emitted by the flux
	 * @param state the updated state returned by the node; must not be {@code null}
	 * @param streamingFlux the flux of streaming items; must not be {@code null}
	 * @return a streaming result
	 */
	public static <S extends GraphState, T> NodeActionResult<S> ofStreaming(S state, Flux<T> streamingFlux) {
		if (state == null) {
			throw new IllegalArgumentException("state must not be null");
		}
		if (streamingFlux == null) {
			throw new IllegalArgumentException("streamingFlux must not be null");
		}
		return new NodeActionResult<>(state, streamingFlux);
	}

	/**
	 * Returns the updated state.
	 * @return the state; never {@code null}
	 */
	public S state() {
		return state;
	}

	/**
	 * Returns {@code true} if this result carries a streaming {@link Flux}.
	 * @return {@code true} for streaming results
	 */
	public boolean hasStreamingFlux() {
		return streamingFlux != null;
	}

	/**
	 * Returns the streaming {@link Flux}, or {@code null} for non-streaming results.
	 * @return the flux, or {@code null}
	 */
	public Flux<?> streamingFlux() {
		return streamingFlux;
	}

}
