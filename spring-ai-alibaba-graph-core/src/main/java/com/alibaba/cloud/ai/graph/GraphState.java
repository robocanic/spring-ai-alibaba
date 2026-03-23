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
package com.alibaba.cloud.ai.graph;

import java.io.Serializable;

/**
 * Marker interface for all user-defined graph state classes. Implementing this interface
 * allows a POJO to be used as the type parameter {@code S} in
 * {@link StateGraph}, {@link CompiledGraph}, and all action interfaces.
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>{@code
 * public class ChatState implements GraphState {
 *
 *     private String query;
 *
 *     @StateField(strategy = AppendStrategy.class)
 *     private List<String> messages;
 *
 *     @StateField(streaming = true)
 *     private String answer;
 *
 *     public ChatState() {}
 *     // getters and setters ...
 * }
 * }</pre>
 *
 * <p>
 * Constraints:
 * </p>
 * <ul>
 *   <li>Must have a public no-arg constructor.</li>
 *   <li>Must extend {@link Serializable} (required by the framework for checkpointing).</li>
 * </ul>
 *
 * @author robocanic
 * @since 2.0
 */
public interface GraphState extends Serializable {

}
