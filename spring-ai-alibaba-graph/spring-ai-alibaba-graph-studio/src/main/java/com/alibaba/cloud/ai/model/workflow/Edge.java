package com.alibaba.cloud.ai.model.workflow;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * Edge defines the routes between node and node. There are two types of Edge: Direct and
 * Conditional
 */
@Data
@Accessors(chain = true)
public class Edge {

	public static final String DEFAULT_CASE_ID = "false";

	private String id;

	private String type;

	private String source;

	private String target;

	private List<Case> cases;

	// source+case -> target id
	private Map<String, String> targetMap;

	private Map<String, Object> data;

	private Integer zIndex = 0;

	private Boolean selected = false;

	public static String getTarget(String source, String caseId){
		return source + "&" + caseId;
	}

}
