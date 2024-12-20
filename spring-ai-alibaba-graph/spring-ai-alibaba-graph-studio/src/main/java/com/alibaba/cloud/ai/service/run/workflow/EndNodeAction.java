package com.alibaba.cloud.ai.service.run.workflow;

import com.alibaba.cloud.ai.graph.NodeActionDescriptor;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.state.NodeState;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeData;

import java.util.Map;
import java.util.stream.Collectors;

public class EndNodeAction implements NodeAction {

    private final NodeData nodeData;

    private final String nodeId;

    private NodeActionDescriptor descriptor;

    public EndNodeAction(String nodeId, NodeData nodeData){
        this.nodeId = nodeId;
        this.nodeData = nodeData;
        this.buildNodeDescriptor();
    }

    private void buildNodeDescriptor(){
        this.descriptor = new NodeActionDescriptor();
        String[] inputs = nodeData.getInputs().stream().map(VariableSelector::variableKey).toArray(String[]::new);
        String[] outputs = nodeData.getInputs().stream().map(VariableSelector::getLabel).toArray(String[]::new);
        this.descriptor.addInputKey(inputs);
        this.descriptor.addOutputKey(outputs);
    }

    @Override
    public Map<String, Object> apply(NodeState state) throws Exception {
        Map<String, Object> partialState = reduceState(state);
        return formattedOutput(partialState);
    }

    @Override
    public NodeActionDescriptor getNodeDescriptor() {
        return this.descriptor;
    }

    private Map<String, Object> reduceState(NodeState state){
        if (descriptor.getInputSchema().isEmpty()){
            return state.data();
        }
        return descriptor.getInputSchema()
                .stream()
                .collect(Collectors.toMap(inputKey -> inputKey, inputKey -> state.value(inputKey, () -> "")));
    }

    private Map<String, Object> formattedOutput(Map<String, Object> partialState){
        return nodeData.getInputs()
                .stream()
                .collect(Collectors.toMap(selector -> VariableSelector.variableKey(nodeId, selector.getLabel()),
                        selector-> partialState.get(selector.variableKey())));
    }
}
