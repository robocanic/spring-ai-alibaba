package com.alibaba.cloud.ai.model;

import com.alibaba.cloud.ai.exception.NotImplementedException;

import javax.swing.text.html.Option;
import java.util.Arrays;
import java.util.Optional;

public enum VariableType {

	STRING("String", String.class, "string", ""),

	NUMBER("Number", Number.class, "number", ""),

	BOOLEAN("Boolean", Boolean.class, "not supported", false),

	OBJECT("Object", Object.class, "object", new Object()),

	// FIXME find appropriate type
	FILE("File", Object.class, "file", new Object[]{}),

	ARRAY_STRING("String[]", String[].class, "array[string]", new String[]{}),

	ARRAY_NUMBER("Number[]", Number[].class, "array[number]", new Integer[]{}),

	ARRAY_OBJECT("Object[]", Object[].class, "array[object]", new Object[]{}),

	ARRAY_FILE("File[]", Object[].class, "file-list", new Object(){});

	private String value;

	private Class clazz;

	private String difyValue;

	private Object preset;

	VariableType(String value, Class clazz, String difyValue, Object preset) {
		this.value = value;
		this.clazz = clazz;
		this.difyValue = difyValue;
		this.preset = preset;
	}

	public String value() {
		return value;
	}

	public Class clazz() {
		return clazz;
	}

	public String difyValue() {
		return difyValue;
	}

	public Object preset() {
		return preset;
	}

	public static Optional<VariableType> fromValue(String value){
		return Arrays.stream(VariableType.values())
				.filter(type -> type.value.equals(value))
				.findFirst();
	}

	public static Optional<VariableType> fromDifyValue(String difyValue) {
		return Arrays.stream(VariableType.values())
				.filter(type -> type.difyValue.equals(difyValue))
				.findFirst();
	}

}
