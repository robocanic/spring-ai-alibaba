package com.alibaba.cloud.ai.service.runner.workflow;

import com.alibaba.cloud.ai.exception.NotImplementedException;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.alibaba.cloud.ai.graph.state.NodeState;
import com.alibaba.cloud.ai.model.VariableType;
import com.alibaba.cloud.ai.model.workflow.Case;
import com.alibaba.cloud.ai.model.workflow.Edge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConditionalEdgeAction implements EdgeAction{

    public static final String DEFAULT_CASE_ID = "false";
    private List<Case> cases;

    private Map<String, String> targetMap;

    public ConditionalEdgeAction(List<Case> cases, Map<String, String> targetMap){
        this.targetMap = targetMap;
        this.cases = cases;
    }


    @Override
    public String apply(NodeState state) throws Exception {
        for (Case c : cases) {
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
                return targetMap.get(c.getId());
            }
        }
        return targetMap.get(DEFAULT_CASE_ID);
    }
}
