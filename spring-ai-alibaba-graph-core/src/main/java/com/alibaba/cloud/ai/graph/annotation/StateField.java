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
package com.alibaba.cloud.ai.graph.annotation;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for declaring field-level update strategy, key mapping name, and streaming
 * flag on {@link com.alibaba.cloud.ai.graph.GraphState} POJO fields.
 *
 * <p>
 * Usage examples:
 * </p>
 *
 * <pre>{@code
 * public class ChatState implements GraphState {
 *
 *     // default ReplaceStrategy, key = "query"
 *     private String query;
 *
 *     // AppendStrategy, key = "messages"
 *     @StateField(strategy = AppendStrategy.class)
 *     private List<String> messages;
 *
 *     // ReplaceStrategy, key = "input" (overrides field name "userInput")
 *     @StateField(fieldName = "input")
 *     private String userInput;
 *
 *     // Streaming output field; at most one per State class
 *     @StateField(streaming = true)
 *     private String answer;
 * }
 * }</pre>
 *
 * <p>
 * Constraints validated at {@link com.alibaba.cloud.ai.graph.StateGraph} construction:
 * </p>
 * <ul>
 *   <li>At most one field may have {@code streaming = true}.</li>
 *   <li>Fields with {@code strategy = AppendStrategy.class} must be of type {@link java.util.List}.</li>
 *   <li>{@code fieldName} values must be unique within the same State class hierarchy.</li>
 *   <li>{@code transient} fields must not be annotated with {@code @StateField}.</li>
 * </ul>
 *
 * @author robocanic
 * @since 2.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface StateField {

	/**
	 * The {@link KeyStrategy} implementation class to use when merging this field into the
	 * graph state. Defaults to {@link ReplaceStrategy} (last-write-wins).
	 * @return the strategy class
	 */
	Class<? extends KeyStrategy> strategy() default ReplaceStrategy.class;

	/**
	 * Optional override for the Map key used in POJO ↔ Map conversion. If empty (the
	 * default), the Java field name is used as the key.
	 * @return the explicit key name, or empty string to use the field name
	 */
	String fieldName() default "";

	/**
	 * Marks this field as the target for streaming output. The framework will write
	 * incremental streaming content into this field during streaming node execution.
	 * At most one field per State class hierarchy may have {@code streaming = true}.
	 * @return {@code true} if this is the streaming output field
	 */
	boolean streaming() default false;

}
