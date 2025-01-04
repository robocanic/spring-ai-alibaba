package com.alibaba.cloud.ai.service.runner.workflow;


import com.alibaba.cloud.ai.exception.NotImplementedException;
import com.alibaba.cloud.ai.exception.RunFailedException;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.serializer.agent.JSONStateSerializer;
import com.alibaba.cloud.ai.model.App;
import com.alibaba.cloud.ai.model.workflow.*;
import com.alibaba.cloud.ai.model.workflow.nodedata.BranchNodeData;
import com.alibaba.cloud.ai.service.runner.RunnableBuilder;
import com.alibaba.cloud.ai.service.runner.Runnable;
import com.alibaba.cloud.ai.service.runner.RunnableType;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class WorkflowRunnableBuilder implements RunnableBuilder<App> {

    private static final String SEPARATOR = ".";

    public final String WORKFLOW_ID = stateKey(NamespaceType.SYS.value(), "workflowId");

    public final String RUN_ID = stateKey(NamespaceType.SYS.value(), "runId");

    public final String MESSAGES = stateKey(NamespaceType.WORKFLOW.value(), "messages");

    private List<NodeActionConverter<NodeData>> nodeActionConverters;

    public WorkflowRunnableBuilder(List<NodeActionConverter<NodeData>> nodeActionConverters){
        this.nodeActionConverters = nodeActionConverters;
    }

    @Override
    public Boolean support(RunnableType runnableType) {
        return RunnableType.WORKFLOW.equals(runnableType);
    }

    // TODO runnable cache
    @Override
    public Runnable build(App runnableModel, String runId, Map<String, Object> rawInputs) throws Exception{
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
        // build graph
        StateGraph stateGraph = buildGraph(nodeMap, edgeMap);
        CompileConfig compileConfig = CompileConfig.builder().saverConfig(
                SaverConfig.builder().register(SaverConstant.MEMORY, new MemorySaver()).build()
        ).build();
        CompiledGraph compiledGraph = stateGraph.compile(compileConfig);
        // build graph inputs
        Map<String, Object> inputs = buildInputs(rawInputs, runnableModel, runId);
        return new WorkflowRunnable(compiledGraph, inputs);
    }

    private Map<String, Object> buildInputs(Map<String, Object> rawInputs, App app, String runId){
        Map<String, Object> inputs = new HashMap<>(rawInputs);
        rawInputs.put(WORKFLOW_ID, app.id());
        rawInputs.put(RUN_ID, runId);
        return inputs;
    }

    private String stateKey(String namespace, String name){
        return namespace + SEPARATOR + name;
    }


    private StateGraph buildGraph(Map<String, Node> nodeMap, Map<String, List<Edge>> edgeMap) throws GraphStateException{
        StateGraph graph = new StateGraph(new JSONStateSerializer());
        Map<String, NodeAction> nodeActionMap = constructNodeActions(nodeMap);
        for (Map.Entry<String, NodeAction> entry : nodeActionMap.entrySet()) {
            graph.addNode(entry.getKey(), AsyncNodeAction.node_async(entry.getValue()));
        }
        Node startNode = findStart(nodeMap.values());
        graph.addEdge(StateGraph.START, startNode.getId());
        connectNextNode(startNode, nodeMap, edgeMap, graph);
        Node endNode = findEnd(nodeMap.values());
        graph.addEdge(endNode.getId(), StateGraph.END);
        return graph;
    }

    private Node findStart(Collection<Node> nodes){
        return nodes.stream().filter(node -> node.getType().equals(NodeType.START.value()))
                .findFirst().orElseThrow(() -> new RuntimeException("No start node found"));
    }

    private Node findEnd(Collection<Node> nodes){
        return nodes.stream()
                .filter(node -> node.getType().equals(NodeType.END.value()) || node.getType().equals(NodeType.ANSWER.value()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No end node found"));
    }

    private Map<String, NodeAction> constructNodeActions(Map<String, Node> nodeMap){
        Map<String, NodeAction> nodeActionMap = new HashMap<>();
        for (Map.Entry<String, Node> entry : nodeMap.entrySet()) {
            Node node = entry.getValue();
            NodeType nodeType = NodeType.fromValue(node.getType()).orElseThrow(
                    ()-> new NotImplementedException("Unsupported NodeType: " + node.getType())
            );
            if (nodeType.equals(NodeType.BRANCH)){
                continue;
            }
            NodeActionConverter<NodeData> nodeDataConverter = getNodeActionConverter(nodeType);
            NodeAction nodeAction = nodeDataConverter.constructNodeAction(node.getId(), node.getData());
            nodeActionMap.put(entry.getKey(), nodeAction);
        }
        return nodeActionMap;
    }

    private NodeActionConverter<NodeData> getNodeActionConverter(NodeType nodeType){
        return nodeActionConverters.stream()
                .filter(c -> c.supportType(nodeType))
                .findFirst()
                .orElseThrow(()->new NotImplementedException(nodeType + "is not supported yet"));
    }

    private EdgeValue connectNextNode(Node current, Map<String, Node> nodeMap, Map<String, List<Edge>> edgeMap, StateGraph graph) throws GraphStateException{
        if (current.getType().equals(NodeType.END.value())
                || !edgeMap.containsKey(current.getId())){
            return new EdgeValue(current.id(), null);
        }
        // remove edge to avoid cycle connect
        List<Edge> edgeList = edgeMap.remove(current.getId());
        if (edgeList == null || edgeList.isEmpty()){
            return new EdgeValue(current.id(), null);
        }
        // TODO parallel mode support
        if (edgeList.size() > 1 && !current.getType().equals(NodeType.BRANCH.value())){
            throw new RunFailedException("Parallel mode is not supported yet");
        }
        if (current.getType().equals(NodeType.BRANCH.value())){
            BranchNodeData branchNodeData = (BranchNodeData) current.getData();
            Map<String, String> targetMap = edgeList.stream().collect(Collectors.toMap(Edge::getSourceHandle, Edge::getTarget));
            ConditionalEdgeAction conditionalEdgeAction = new ConditionalEdgeAction(branchNodeData.getCases(), targetMap);
            for (Edge edge : edgeList) {
                connectNextNode(nodeMap.get(edge.getTarget()),nodeMap,edgeMap,graph);
            }
            return new EdgeValue(null, new EdgeCondition(AsyncEdgeAction.edge_async(conditionalEdgeAction), targetMap));
        }else {
            for (Edge edge : edgeList) {
                Node next = nodeMap.get(edge.getTarget());
                EdgeValue edgeValue = connectNextNode(next, nodeMap, edgeMap, graph);
                if (edgeValue.value() == null){
                    graph.addEdge(current.getId(), next.getId());
                }else {
                    graph.addConditionalEdges(current.getId(), edgeValue.value().action(), edgeValue.value().mappings());
                }
            }
            return new EdgeValue(current.id(), null);
        }
    }




}
