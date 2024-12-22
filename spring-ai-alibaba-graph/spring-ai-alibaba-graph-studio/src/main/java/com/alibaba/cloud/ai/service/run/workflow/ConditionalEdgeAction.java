package com.alibaba.cloud.ai.service.run.workflow;

import com.alibaba.cloud.ai.exception.NotImplementedException;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.alibaba.cloud.ai.graph.state.NodeState;
import com.alibaba.cloud.ai.model.VariableType;
import com.alibaba.cloud.ai.model.workflow.Case;
import com.alibaba.cloud.ai.model.workflow.Edge;

import java.util.ArrayList;
import java.util.List;

public class ConditionalEdgeAction implements EdgeAction{

    private Edge edge;

    private String source;

    public ConditionalEdgeAction(Edge edge, String source){
        this.edge = edge;
        this.source = source;
    }


    @Override
    public String apply(NodeState state) throws Exception {
        for (Case c : edge.getCases()) {
            String logicalOperator = c.getLogicalOperator();
            List<Boolean> conditionAsserts = new ArrayList<>();
            for (Case.Condition condition : c.getConditions()) {
                Case.ComparisonOperatorType comparisonOperator = Case.ComparisonOperatorType
                        .fromValue(condition.getComparisonOperator())
                        .orElseThrow(()-> new NotImplementedException("Unsupported comparison operator type: " +  condition.getComparisonOperator()));
                VariableType variableType = VariableType
                        .fromValue(condition.getVarType())
                        .orElseThrow(()-> new NotImplementedException("Unsupported variable type: " + condition.getVarType()));
                Object value = variableType.clazz().cast(condition.getValue());
                Object expectedValue = state.value(condition.getVariableSelector().variableKey()).orElse(null);
                conditionAsserts.add(comparisonOperator.assertFunc().apply(value, expectedValue));
            }
            boolean conditionResult;
            if (logicalOperator.equals(Case.LogicalOperatorType.AND.value())){
                conditionResult = conditionAsserts.stream().reduce(true, (a, b) -> a && b);

            }else {
                conditionResult = conditionAsserts.stream().reduce(false, (a, b) -> a || b);
            }
            if (conditionResult){
                return edge.getTargetMap().get(Edge.getTarget(source, c.getId()));
            }
        }
        return edge.getTargetMap().get(Edge.getTarget(source, Edge.DEFAULT_CASE_ID));
    }


}
