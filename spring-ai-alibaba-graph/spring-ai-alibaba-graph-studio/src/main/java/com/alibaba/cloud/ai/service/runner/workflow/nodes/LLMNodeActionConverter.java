package com.alibaba.cloud.ai.service.runner.workflow.nodes;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.node.llm.LLMNodeAction;
import com.alibaba.cloud.ai.graph.node.llm.LLMNodeActionDescriptor;
import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.VariableType;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.LLMNodeData;
import com.alibaba.cloud.ai.service.runner.workflow.NodeActionConverter;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.AssistantPromptTemplate;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LLMNodeActionConverter implements NodeActionConverter<LLMNodeData> {

    private final ChatModel chatModel;

    public LLMNodeActionConverter(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public Boolean supportType(NodeType nodeType) {
        return NodeType.LLM.equals(nodeType);
    }

    @Override
    public NodeAction constructNodeAction(String nodeId, LLMNodeData nodeData) {
//        chatModel = buildModel(nodeData.getModel());
        LLMNodeAction.Builder builder = LLMNodeAction.builder(chatModel);
        List<LLMNodeData.PromptTemplate> promptTemplates = nodeData.getPromptTemplate();
        List<PromptTemplate> promptTmpls = promptTemplates.stream().map(tmpl -> switch (tmpl.getRole()) {
            case "system" -> new SystemPromptTemplate(tmpl.getText());
            case "user" -> new PromptTemplate(tmpl.getText());
            case "assistant" -> new AssistantPromptTemplate(tmpl.getText());
            default -> throw new IllegalArgumentException("Unsupported role in prompt template:" + tmpl.getRole());
        }).toList();
        String[] inputKeys = nodeData.getInputs().stream()
                .map(VariableSelector::variableKey)
                .toArray(String[]::new);
        String[] outputKeys = nodeData.getOutputs().stream()
                .map(output -> VariableSelector.variableKey(nodeId, output.getName()))
                .toArray(String[]::new);
        builder.withPromptTemplates(promptTmpls).withInputKey(inputKeys).withOutputKey(outputKeys);
        return builder.build();
    }

    @Override
    public LLMNodeData deconstructNodeAction(NodeAction nodeAction) {
        LLMNodeActionDescriptor nodeActionDescriptor = (LLMNodeActionDescriptor)nodeAction.getNodeActionDescriptor();
        List<VariableSelector> inputs = nodeActionDescriptor.getInputSchema().stream().map(inputKey -> {
            String[] splits = inputKey.split(VariableSelector.DEFAULT_SEPARATOR, 2);
            return new VariableSelector(splits[0], splits[1]);
        }).toList();
        List<Variable> outputs = nodeActionDescriptor
                .getOutputSchema()
                .stream()
                .map(outputKey -> new Variable(outputKey, VariableType.STRING.value()))
                .toList();
        List<LLMNodeData.PromptTemplate> promptTemplates = nodeActionDescriptor.getPromptTemplates().stream().map(promptTemplate -> {
            if (promptTemplate instanceof SystemPromptTemplate) {
                return new LLMNodeData.PromptTemplate("system", promptTemplate.getTemplate());
            } else if (promptTemplate instanceof AssistantPromptTemplate) {
                return new LLMNodeData.PromptTemplate("assistant", promptTemplate.getTemplate());
            } else {
                return new LLMNodeData.PromptTemplate("user", promptTemplate.getTemplate());
            }
        }).toList();
        ChatOptions chatOptions = nodeActionDescriptor.getChatOptions();
        LLMNodeData.ModelConfig modelConfig = new LLMNodeData.ModelConfig()
                .setMode("chat")
                .setName(chatOptions.getModel())
                .setCompletionParams(
                        new LLMNodeData.CompletionParams()
                                .setMaxTokens(chatOptions.getMaxTokens())
                                .setTopK(chatOptions.getTopK())
                                .setTopP(chatOptions.getTopP())
                                .setStop(chatOptions.getStopSequences())
                                .setTemperature(chatOptions.getTemperature())
                                .setRepetitionPenalty(chatOptions.getFrequencyPenalty())
                );
        return new LLMNodeData(inputs, outputs).setModel(modelConfig).setPromptTemplate(promptTemplates);
    }
}
