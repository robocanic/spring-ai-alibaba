package com.alibaba.cloud.ai.service.run;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.model.workflow.NodeData;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import  com.alibaba.cloud.ai.model.workflow.nodedata.LLMNodeData;

/**
 * NodeActionConverter defined the interface of converting NodeAction to Node and vice versa
 * @param <T> subclass of NodeData, e.g. {@link LLMNodeData}
 */
public interface NodeActionConverter<T extends NodeData> {

    /**
     * Whether the converter supports the given nodeType
     * @return true if support
     */
    Boolean supportType(NodeType nodeType);

    /**
     * Construct {@link NodeAction} from {@link NodeData}
     */
    NodeAction constructNodeAction(T nodeData, String nodeId);

    /**
     * Deconstruct {@link NodeAction} to {@link NodeData}
     */
    T deconstructNodeAction(NodeAction nodeAction);

}
