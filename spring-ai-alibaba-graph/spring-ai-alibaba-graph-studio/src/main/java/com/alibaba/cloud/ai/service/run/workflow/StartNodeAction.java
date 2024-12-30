package com.alibaba.cloud.ai.service.run.workflow;

import com.alibaba.cloud.ai.graph.NodeActionDescriptor;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.state.NodeState;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.nodedata.StartNodeData;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class StartNodeAction implements NodeAction {

    private final StartNodeData nodeData;

    private final String nodeId;

    private NodeActionDescriptor descriptor;

    public StartNodeAction(String nodeId, StartNodeData nodeData){
        this.nodeId = nodeId;
        this.nodeData = nodeData;
        this.buildNodeDescriptor();
    }

    private void buildNodeDescriptor(){
        this.descriptor = new NodeActionDescriptor();
        String[] inputs = nodeData.getStartInputs().stream().map(StartNodeData.StartInput::getVariable).toArray(String[]::new);
        String[] outputs = Arrays.stream(inputs).map(inputKey -> VariableSelector.variableKey(nodeId, inputKey)).toArray(String[]::new);
        this.descriptor.addInputKey(inputs);
        this.descriptor.addOutputKey(outputs);
    }

    @Override
    public NodeActionDescriptor getNodeActionDescriptor() {
        return NodeAction.super.getNodeActionDescriptor();
    }

    @Override
    public Map<String, Object> apply(NodeState state) throws Exception {
        Map<String, Object> partialState = reduceState(state);
        return formattedOutput(partialState);
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
        return nodeData.getStartInputs().stream()
                .map(StartNodeData.StartInput::getVariable)
                .collect(Collectors.toMap(inputKey->VariableSelector.variableKey(nodeId, inputKey), partialState::get));
    }

}
