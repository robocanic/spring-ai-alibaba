package com.alibaba.cloud.ai.service.run.workflow;


import com.alibaba.cloud.ai.exception.NotImplementedException;
import com.alibaba.cloud.ai.exception.RunFailedException;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.model.App;
import com.alibaba.cloud.ai.model.workflow.*;
import com.alibaba.cloud.ai.service.dsl.NodeDataConverter;
import com.alibaba.cloud.ai.service.run.RunnableBuilder;
import com.alibaba.cloud.ai.service.run.Runnable;
import com.alibaba.cloud.ai.service.run.RunnableType;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class WorkflowRunnableBuilder implements RunnableBuilder<App> {

    private List<NodeDataConverter> nodeDataConverters;

    public WorkflowRunnableBuilder(List<NodeDataConverter> nodeDataConverters){
        this.nodeDataConverters = nodeDataConverters;
    }

    @Override
    public Boolean support(String runnableType) {
        return RunnableType.WORKFLOW.value().equals(runnableType);
    }

    @Override
    public Runnable build(App runnableModel, Map<String, Object> inputs) {
        String appId = runnableModel.id();
        Workflow workflow = (Workflow) runnableModel.getSpec();
        // nodeId -> node
        Map<String, Node> nodeMap = workflow.getGraph().getNodes().stream().collect(Collectors.toMap(Node::getId, node -> node));
        // edgeSource -> list(edge)
        Map<String, List<Edge>> edgeMap = new HashMap<>();
        for (Edge edge : workflow.getGraph().getEdges()) {
            if (edgeMap.containsKey(edge.getSource())){
                edgeMap.get(edge.getSource()).add(edge);
            }else {
                List<Edge> edgeList = new LinkedList<>();
                edgeList.add(edge);
                edgeMap.put(edge.getSource(), edgeList);
            }
        }
        CompiledGraph<WorkflowState> compiledGraph;

        try {
            StateGraph<WorkflowState> stateGraph = buildGraph(nodeMap, edgeMap);
            compiledGraph = stateGraph.compile();
        } catch (GraphStateException e) {
            throw new RunFailedException("Graph build error" + e.getMessage(), e);
        }
        return new WorkflowRunnable(compiledGraph);
    }

    private NodeDataConverter getNodeDataConverter(String nodeType){
        return nodeDataConverters.stream()
                .filter(nodeDataConverter -> nodeDataConverter.supportType(nodeType))
                .findFirst()
                .orElseThrow(()->new NotImplementedException(nodeType + "is not supported yet"));
    }

    private StateGraph<WorkflowState> buildGraph(Map<String, Node> nodeMap, Map<String, List<Edge>> edgeMap) throws GraphStateException{
        StateGraph<WorkflowState> graph = new StateGraph<>(WorkflowState.SCHEMA, new JSONStateSerializer());
        Map<String, NodeAction<WorkflowState>> nodeActionMap = constructNodeActions(nodeMap);
        for (Map.Entry<String, NodeAction<WorkflowState>> entry : nodeActionMap.entrySet()) {
            graph.addNode(entry.getKey(), AsyncNodeAction.node_async(entry.getValue()));
        }
        Node startNode = findStart(nodeMap.values());
        connectNodes(startNode, nodeMap, edgeMap, graph);
        return graph;
    }

    private Node findStart(Collection<Node> nodes){
        return nodes.stream().filter(node -> node.getType().equals(NodeType.START.value()))
                .findFirst().orElseThrow(() -> new RuntimeException("No start node found"));
    }

    private Map<String, NodeAction<WorkflowState>> constructNodeActions(Map<String, Node> nodeMap){
        Map<String, NodeAction<WorkflowState>> nodeActionMap = new HashMap<>();
        for (Map.Entry<String, Node> entry : nodeMap.entrySet()) {
            Node node = entry.getValue();
            String nodeType = node.getType();
            // skip start and node convert
            if (nodeType.equals(NodeType.START.value()) || nodeType.equals(NodeType.END.value())){
                continue;
            }
            NodeDataConverter nodeDataConverter = getNodeDataConverter(node.getType());
            NodeAction<WorkflowState> nodeAction = nodeDataConverter.constructNodeAction(node.getData());
            nodeActionMap.put(entry.getKey(), nodeAction);
        }
        return nodeActionMap;
    }

    // TODO parallel mode support
    private void connectNodes(Node current, Map<String, Node> nodeMap, Map<String, List<Edge>> edgeMap, StateGraph<WorkflowState> graph) throws GraphStateException{
        if (current.getType().equals(NodeType.END.value()) || !edgeMap.containsKey(current.getId())){
            return;
        }
        // need to remove node
        List<Edge> edgeList = edgeMap.remove(current.getId());
        if (edgeList == null || edgeList.isEmpty()){
            return;
        }
        Edge edge = edgeList.getFirst();
        if (edge.getType().equals(EdgeType.DIRECT.value())){
            Node next = nodeMap.get(edge.getTarget());
            graph.addEdge(current.getId(), next.getId());
            connectNodes(next, nodeMap, edgeMap, graph);
        }else {
            ConditionalEdgeAction<WorkflowState> conditionalEdgeAction = new ConditionalEdgeAction<>(edge);
            graph.addConditionalEdges(current.getId(), AsyncEdgeAction.edge_async(conditionalEdgeAction), edge.getTargetMap());
            for (String targetNodeId : edge.getTargetMap().values()) {
                Node next = nodeMap.get(targetNodeId);
                connectNodes(next, nodeMap, edgeMap, graph);
            }
        }
    }



}
