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
package com.alibaba.cloud.ai.graph.utils;

import com.alibaba.cloud.ai.graph.GraphState;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.annotation.StateField;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for scanning {@link GraphState} POJO fields and performing bidirectional
 * conversion between POJO instances and {@code Map<String, Object>}.
 *
 * <p>
 * <b>Inheritance chain scanning:</b> All methods traverse the full class hierarchy (from
 * the given class up to—but not including—{@link Object}). When the same field name
 * appears in both a subclass and a superclass, the subclass declaration takes precedence.
 * </p>
 *
 * @author robocanic
 * @since 2.0
 */
public final class StateFieldScanner {

	/** Cache for resolved field lists to avoid repeated reflection on the same class. */
	private static final ConcurrentHashMap<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

	/** Cache for key-strategy mappings. */
	private static final ConcurrentHashMap<Class<?>, Map<String, KeyStrategy>> KEY_STRATEGY_CACHE =
			new ConcurrentHashMap<>();

	private StateFieldScanner() {
	}

	// -------------------------------------------------------------------------
	// Internal: inheritance-chain field collection
	// -------------------------------------------------------------------------

	/**
	 * Collects all fields from the full inheritance chain (current class and all
	 * superclasses, excluding {@link Object}). Subclass fields take precedence over
	 * superclass fields with the same name.
	 * @param clazz the class to scan
	 * @return ordered list of effective fields (subclass fields first, then parent fields
	 * that are not shadowed)
	 */
	static List<Field> collectFields(Class<?> clazz) {
		return FIELD_CACHE.computeIfAbsent(clazz, c -> {
			Map<String, Field> byName = new LinkedHashMap<>();
			// Walk from the most-derived class upward; first declaration wins.
			Class<?> current = c;
			while (current != null && current != Object.class) {
				for (Field field : current.getDeclaredFields()) {
					// Skip synthetic fields (e.g., inner-class references)
					if (field.isSynthetic()) {
						continue;
					}
					// Subclass declaration wins – only insert if not already present
					byName.putIfAbsent(field.getName(), field);
				}
				current = current.getSuperclass();
			}
			return List.copyOf(byName.values());
		});
	}

	/**
	 * Resolves the effective Map key for a field: uses {@link StateField#fieldName()} when
	 * non-empty, otherwise falls back to the Java field name.
	 */
	private static String resolveKey(Field field) {
		StateField ann = field.getAnnotation(StateField.class);
		if (ann != null && !ann.fieldName().isEmpty()) {
			return ann.fieldName();
		}
		return field.getName();
	}

	// -------------------------------------------------------------------------
	// 1. toMap – POJO → Map
	// -------------------------------------------------------------------------

	/**
	 * Converts a {@link GraphState} POJO to a {@code Map<String, Object>},
	 * {@code null}-valued fields. Covers the full inheritance
	 * chain.
	 * @param state the state instance to convert; must not be {@code null}
	 * @return a mutable map of non-null field values
	 */
	public static <S extends GraphState> Map<String, Object> toMap(S state) {
		Objects.requireNonNull(state, "state must not be null");
		Map<String, Object> result = new HashMap<>();
		for (Field field : collectFields(state.getClass())) {
			field.setAccessible(true);
			try {
				Object value = field.get(state);
				if (value != null) {
					result.put(resolveKey(field), value);
				}
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(
						"Cannot access field '" + field.getName() + "' on " + state.getClass().getName(), e);
			}
		}
		return result;
	}

	// -------------------------------------------------------------------------
	// 2. fromMap – Map → POJO
	// -------------------------------------------------------------------------

	/**
	 * Creates a new instance of {@code clazz} using its public no-arg constructor and
	 * injects values from {@code map}. Fields not present in the map retain their default
	 * values. Covers the full inheritance chain (including {@code private} superclass
	 * fields via {@link Field#setAccessible(boolean)}).
	 * @param map the source map; must not be {@code null}
	 * @param clazz the target class; must have a public no-arg constructor
	 * @param <S> the state type
	 * @return a newly created and populated POJO instance
	 */
	public static <S extends GraphState> S fromMap(Map<String, Object> map, Class<S> clazz) {
		Objects.requireNonNull(map, "map must not be null");
		Objects.requireNonNull(clazz, "clazz must not be null");
		S instance;
		try {
			instance = clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalStateException(
					"Cannot instantiate " + clazz.getName() + " – ensure a public no-arg constructor exists", e);
		}
		// Build reverse lookup: key → field
		Map<String, Field> keyToField = new HashMap<>();
		for (Field field : collectFields(clazz)) {
			keyToField.put(resolveKey(field), field);
		}
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			Field field = keyToField.get(entry.getKey());
			if (field != null && entry.getValue() != null) {
				field.setAccessible(true);
				try {
					field.set(instance, entry.getValue());
				}
				catch (IllegalAccessException e) {
					throw new IllegalStateException(
							"Cannot set field '" + field.getName() + "' on " + clazz.getName(), e);
				}
			}
		}
		return instance;
	}

	// -------------------------------------------------------------------------
	// 3. getKeyStrategies – extract KeyStrategy map
	// -------------------------------------------------------------------------

	/**
	 * Extracts a {@code Map<String, KeyStrategy>} from the POJO class by scanning
	 * {@link StateField} annotations. Fields without the annotation default to
	 * {@link ReplaceStrategy}. Covers the full inheritance chain.
	 * @param clazz the state class to scan
	 * @param <S> the state type
	 * @return an unmodifiable map from field key to its {@link KeyStrategy}
	 */
	public static <S extends GraphState> Map<String, KeyStrategy> getKeyStrategies(Class<S> clazz) {
		Objects.requireNonNull(clazz, "clazz must not be null");
		return KEY_STRATEGY_CACHE.computeIfAbsent(clazz, c -> {
			Map<String, KeyStrategy> result = new LinkedHashMap<>();
			for (Field field : collectFields(c)) {
				String key = resolveKey(field);
				StateField ann = field.getAnnotation(StateField.class);
				KeyStrategy strategy;
				if (ann != null) {
					Class<? extends KeyStrategy> strategyClass = ann.strategy();
					try {
						strategy = strategyClass.getDeclaredConstructor().newInstance();
					}
					catch (Exception e) {
						throw new IllegalStateException(
								"Cannot instantiate strategy " + strategyClass.getName() + " for field '"
										+ field.getName() + "'",
								e);
					}
				}
				else {
					strategy = new ReplaceStrategy();
				}
				result.put(key, strategy);
			}
			return Map.copyOf(result);
		});
	}

	// -------------------------------------------------------------------------
	// 4. diff – compute delta between snapshot and current state
	// -------------------------------------------------------------------------

	/**
	 * Computes the delta between a {@code snapshot} (taken before node execution) and the
	 * {@code current} state (after node execution).
	 *
	 * <ul>
	 *   <li>For {@link ReplaceStrategy} fields: if the value changed, includes the new value.</li>
	 *   <li>For {@link AppendStrategy} fields: includes only the <em>newly appended</em> elements
	 *       (i.e., {@code newList.subList(snapshotSize, newList.size())}).</li>
	 * </ul>
	 *
	 * <p>
	 * Covers the full inheritance chain.
	 * </p>
	 * @param snapshot the deep-copy taken before node execution
	 * @param current the state returned by the node action
	 * @param <S> the state type
	 * @return a delta map suitable for passing to {@code OverAllState.updateState()}
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <S extends GraphState> Map<String, Object> diff(S snapshot, S current) {
		Objects.requireNonNull(snapshot, "snapshot must not be null");
		Objects.requireNonNull(current, "current must not be null");
		Map<String, Object> delta = new HashMap<>();
		Map<String, KeyStrategy> strategies = getKeyStrategies((Class<S>) current.getClass());

		for (Field field : collectFields(current.getClass())) {
			field.setAccessible(true);
			String key = resolveKey(field);
			try {
				Object snapshotVal = field.get(snapshot);
				Object currentVal = field.get(current);

				KeyStrategy strategy = strategies.getOrDefault(key, new ReplaceStrategy());

				if (strategy instanceof AppendStrategy) {
					// AppendStrategy: emit only newly appended elements
					List snapshotList = (snapshotVal instanceof List) ? (List) snapshotVal : List.of();
					List currentList = (currentVal instanceof List) ? (List) currentVal : List.of();
					if (currentList.size() > snapshotList.size()) {
						List newElements = new ArrayList<>(
								currentList.subList(snapshotList.size(), currentList.size()));
						delta.put(key, newElements);
					}
				}
				else {
					// ReplaceStrategy (and others): emit if value changed
					if (!Objects.equals(snapshotVal, currentVal)) {
						if (currentVal != null) {
							delta.put(key, currentVal);
						}
					}
				}
			}
			catch (IllegalAccessException e) {
				throw new IllegalStateException(
						"Cannot access field '" + field.getName() + "' on " + current.getClass().getName(), e);
			}
		}
		return delta;
	}

	// -------------------------------------------------------------------------
	// 5. getStreamingFieldKey – find streaming=true field key
	// -------------------------------------------------------------------------

	/**
	 * Returns the Map key of the field annotated with {@code @StateField(streaming = true)}
	 * in the given class hierarchy. Subclass declarations are checked first (subclass
	 * takes precedence). Returns {@code null} if no streaming field is declared.
	 * @param clazz the state class to scan
	 * @param <S> the state type
	 * @return the key of the streaming field, or {@code null}
	 */
	public static <S extends GraphState> String getStreamingFieldKey(Class<S> clazz) {
		Objects.requireNonNull(clazz, "clazz must not be null");
		for (Field field : collectFields(clazz)) {
			if (Modifier.isTransient(field.getModifiers())) {
				continue;
			}
			StateField ann = field.getAnnotation(StateField.class);
			if (ann != null && ann.streaming()) {
				return resolveKey(field);
			}
		}
		return null;
	}

	/**
	 * Invalidates the internal caches for the given class. Primarily for testing.
	 * @param clazz the class whose cache entries should be removed
	 */
	public static void invalidateCache(Class<?> clazz) {
		FIELD_CACHE.remove(clazz);
		KEY_STRATEGY_CACHE.remove(clazz);
	}

}
