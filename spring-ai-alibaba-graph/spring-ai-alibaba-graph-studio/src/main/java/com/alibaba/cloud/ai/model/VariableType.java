package com.alibaba.cloud.ai.model;

public enum VariableType {

	STRING("STRING", String.class, "string", ""),

	NUMBER("NUMBER", Number.class, "number", ""),

	BOOLEAN("BOOLEAN", Boolean.class, "not supported", false),

	OBJECT("OBJECT", Object.class, "object", new Object()),

	// FIXME find appropriate type
	FILE("FILE", Object.class, "file", new Object[]{}),

	ARRAY_STRING("ARRAY_STRING", String[].class, "array[string]", new String[]{}),

	ARRAY_NUMBER("ARRAY_NUMBER", Number[].class, "array[number]", new Integer[]{}),

	ARRAY_OBJECT("ARRAY_OBJECT", Object[].class, "array[object]", new Object[]{}),

	ARRAY_FILE("ARRAY_FILE", Object[].class, "file-list", new Object(){});

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

	public static VariableType difyValueOf(String difyValue) {
		for (VariableType type : VariableType.values()) {
			if (type.difyValue.equals(difyValue)) {
				return type;
			}
		}
		return null;
	}

}
