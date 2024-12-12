package com.alibaba.cloud.ai.model.workflow;

import com.alibaba.cloud.ai.exception.NotImplementedException;
import com.alibaba.cloud.ai.model.VariableSelector;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * A Case represents a condition in ConditionalEdge
 */
@Data
@Accessors(chain = true)
public class Case {

	private String id;

	private String logicalOperator;

	private List<Condition> conditions;

	public enum LogicalOperatorType{
		AND("and"),
		OR("or");

		private String value;

		public String value(){
			return value;
		}

		public LogicalOperatorType fromValue(String value){
			for (LogicalOperatorType logicalOperatorType : LogicalOperatorType.values()) {
				if (Objects.equals(logicalOperatorType.value, value)){
					return logicalOperatorType;
				}
			}
			throw new NotImplementedException("Unsupported logicalOperator type" + value);
		}

		LogicalOperatorType(String value){
			this.value = value;
		}
	}

	public enum ComparisonOperatorType{
		CONTAINS("contains", (value, expectedValue) -> {
            if (value instanceof String) {
                return ((String) value).contains((String) expectedValue);
            } else if (value instanceof List<?>) {
                return ((List<?>) value).contains(expectedValue);
            } else {
				throw new IllegalArgumentException(typeErrorString("contains",
						"String, List", value.getClass().getTypeName()));
            }
        }),

		NOT_CONTAINS("not contains",(value, expectedValue)->{
			if (value instanceof String) {
				return !((String) value).contains((String) expectedValue);
			} else if (value instanceof List<?>) {
				return !((List<?>) value).contains(expectedValue);
			} else {
				throw new IllegalArgumentException(typeErrorString("not contains",
						"String, List", value.getClass().getTypeName()));
			}
		}),

		START_WITH("start with", (value, expectedValue)->{
			if (value instanceof String){
				return ((String) value).startsWith((String) expectedValue);
			}else {
				throw new IllegalArgumentException(typeErrorString("start with",
						"String", value.getClass().getTypeName()));
			}
		}),

		END_WITH("end with", (value, expectedValue)->{
			if (value instanceof String){
				return ((String) value).endsWith((String) expectedValue);
			}else {
				throw new IllegalArgumentException(typeErrorString("end with",
						"String", value.getClass().getTypeName()));
			}
		}),

		IS("is", (value, expectedValue)->{
			if (value instanceof String){
				return value.equals(expectedValue);
			}else {
				throw new IllegalArgumentException(typeErrorString("is",
						"String", value.getClass().getTypeName()));
			}
		}),

		IS_NOT("is not", (value, expectedValue)->{
			if (value instanceof String){
				return !value.equals(expectedValue);
			}else {
				throw new IllegalArgumentException(typeErrorString("is not",
						"String", value.getClass().getTypeName()));
			}
		}),

		EMPTY("empty", (value, expectedValue)->{
			if (value instanceof List<?>){
				return ((List<?>) value).isEmpty();
			}else {
				throw new IllegalArgumentException(typeErrorString("empty",
						"List", value.getClass().getTypeName()));
			}
		}),

		NOT_EMPTY("not empty", (value, expectedValue)->{
			if (value instanceof List<?>){
				return !((List<?>) value).isEmpty();
			}else {
				throw new IllegalArgumentException(typeErrorString("not empty",
						"List", value.getClass().getTypeName()));
			}
		}),

		IN("in", (value, expectedValue)->{
			if (expectedValue instanceof List<?>){
                return ((List<?>) expectedValue).contains(value);
			}else {
				throw new IllegalArgumentException(typeErrorString("in",
						"List", value.getClass().getTypeName()));
			}
		}),

		NOT_IN("not in", (value, expectedValue)->{
			if (expectedValue instanceof List<?>){
				return !((List<?>) expectedValue).contains(value);
			}else {
				throw new IllegalArgumentException(typeErrorString("not in",
						"List", value.getClass().getTypeName()));
			}
		}),

		ALL_OF("all of", (value, expectedValue)->{
			if (expectedValue instanceof List<?> && value instanceof String){
				List<String> list = new ArrayList<>();
				for (Object item : (List<?>)expectedValue){
					if (!((String) value).contains((String)item)){
						return false;
					}
				}
				return true;
			}else {
				throw new IllegalArgumentException(typeErrorString("all of",
						"List", value.getClass().getTypeName()));
			}
		}),

		EQUALS("=", (value, expectedValue)->{
			if (value instanceof Number){
				return ((Number) value).doubleValue() == ((Number) expectedValue).doubleValue();
			}else {
				throw new IllegalArgumentException(typeErrorString("=",
						"Number", value.getClass().getTypeName()));
			}
		}),

		NOT_EQUALS("≠", (value, expectedValue)->{
			if (value instanceof Number){
				return !(((Number) value).doubleValue() == ((Number) expectedValue).doubleValue());
			}else {
				throw new IllegalArgumentException(typeErrorString("≠",
						"Number", value.getClass().getTypeName()));
			}
		}),

		GREATER_THAN(">", (value, expectedValue)->{
			if (value instanceof Number){
				return ((Number) value).doubleValue() > ((Number) expectedValue).doubleValue();
			}else {
				throw new IllegalArgumentException(typeErrorString(">",
						"Number", value.getClass().getTypeName()));
			}
		}),

		LESS_THAN("<", (value, expectedValue)->{
			if (value instanceof Number){
				return ((Number) value).doubleValue() < ((Number) expectedValue).doubleValue();
			}else {
				throw new IllegalArgumentException(typeErrorString("<",
						"Number", value.getClass().getTypeName()));
			}
		}),

		GREATER_EQUAL("≥", (value, expectedValue)->{
			if (value instanceof Number){
				return ((Number) value).doubleValue() >= ((Number) expectedValue).doubleValue();
			}else {
				throw new IllegalArgumentException(typeErrorString("≥",
						"Number", value.getClass().getTypeName()));
			}
		}),

		LESS_EQUAL("≤", (value, expectedValue)->{
			if (value instanceof Number){
				return ((Number) value).doubleValue() <= ((Number) expectedValue).doubleValue();
			}else {
				throw new IllegalArgumentException(typeErrorString("≤",
						"Number", value.getClass().getTypeName()));
			}
		}),

		NULL("null", (value, expectedValue)->{
			return value == null;
		}),

		NOT_NULL("not null", (value, expectedValue)->{
			return value != null;
		}),

		EXISTS("exists", (value, expectedValue)->{
			return value != null;
		}),

		NOT_EXISTS("not exists", (value, expectedValue)->{
			return value == null;
		});

		private String value;

		private BiFunction<Object, Object, Boolean> assertFunc;

		public String value(){
			return value;
		}

		public BiFunction<Object, Object, Boolean> assertFunc(){
			return assertFunc;
		}
		
		private static String typeErrorString(String operator, String supportType, String type){
            return "comparison operator `" +
                    operator +
                    "` only support " +
                    supportType +
                    "but value type is: " +
                    type;
		}

		public static ComparisonOperatorType fromValue(String value){
			for (ComparisonOperatorType type : ComparisonOperatorType.values()){
				if (type.value().equals(value)){
					return type;
				}
			}
			return null;
		}
		
		ComparisonOperatorType(String value, BiFunction<Object, Object, Boolean> assertFunc){
			this.value = value;
			this.assertFunc = assertFunc;
		}

	}

	@Data
	@Accessors(chain = true)
	public static class Condition {

		private String value;

		private String varType;

		private String comparisonOperator;

		private VariableSelector variableSelector;

	}



}
