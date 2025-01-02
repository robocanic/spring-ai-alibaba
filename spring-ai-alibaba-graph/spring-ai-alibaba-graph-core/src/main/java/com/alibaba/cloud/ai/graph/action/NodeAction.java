package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.NodeActionDescriptor;


@FunctionalInterface
public interface NodeAction<InputState, OutputState>{

	OutputState apply(InputState inputState) throws Exception;

	default NodeActionDescriptor getNodeActionDescriptor(){
		return NodeActionDescriptor.EMPTY;
	}

}
