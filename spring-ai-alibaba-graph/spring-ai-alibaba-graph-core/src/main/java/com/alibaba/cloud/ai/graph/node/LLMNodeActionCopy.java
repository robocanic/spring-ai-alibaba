package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.NodeAttributes;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.state.AgentState;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;

import java.util.*;
import java.util.stream.Collectors;

public class LLMNodeActionCopy <State extends AgentState> extends AbstractNode implements NodeAction<State> {

    /**
     * each llm node has their own state
     */
    public static final String MESSAGES_KEY = "sys.messages";

    public ChatClient chatClient;

    private final NodeAttributes nodeAttributes;

    private LLMNodeActionCopy (ChatClient chatClient, NodeAttributes nodeAttributes) {
        this.chatClient = chatClient;
        this.nodeAttributes = nodeAttributes;
    }

    public static LLMNodeActionCopy.Builder builder (ChatModel chatModel) {

        return new LLMNodeActionCopy.Builder(chatModel);
    }

    @Override
    public NodeAttributes getNodeAttributes() {
        return this.nodeAttributes;
    }

    /**
     * @param state
     * @return Map<String, Object>
     * @throws Exception
     */
    @Override
    public Map<String, Object> apply (State state) throws Exception {
        Map<String, Object> partialState;
        // if input schema is not empty, use input schema to get input from state as partialState
        if (!nodeAttributes.getInputSchema().isEmpty()){
            partialState = nodeAttributes.getInputSchema().stream()
                    .collect(Collectors.toMap(inputKey -> inputKey, inputKey -> state.value(inputKey, "")));
        }else {
            partialState = state.data();
        }
        List<Message> messages = state.value(MESSAGES_KEY, new ArrayList<>());
        List<Generation> generations = chatClient.prompt().user(s -> s.params(partialState)).messages(messages).call().chatResponse().getResults();
        List<Message> output = generations.stream().map(Generation::getOutput).collect(Collectors.toList());
        // if output schema is not empty, use output schema to put output into state
        if (!nodeAttributes.getOutputSchema().isEmpty()){
            return Map.of(
                    MESSAGES_KEY,
                    output,
                    nodeAttributes.getOutputSchema().get(0),
                    output.stream()
                            .map(Message::getContent)
                            .reduce("", (a, b) -> a + "\n" + b)
            );
        }

        return Map.of(MESSAGES_KEY, output);

    }

    public static class Builder {

        protected ChatModel chatModel;

        protected String sysPrompt;

        protected String[] functions;

        protected NodeAttributes nodeAttributes;

        public Builder (ChatModel chatModel) {
            this.chatModel = chatModel;
            this.nodeAttributes = new NodeAttributes();
            this.nodeAttributes.addProperty("chatOptions", chatModel.getDefaultOptions());
        }

        public <State extends AgentState> LLMNodeActionCopy<AgentState> build () {
            ChatClient.Builder builder = ChatClient.builder(this.chatModel);
            String sysPr = Optional.ofNullable(sysPrompt).orElse("{'role': 'system', 'content': 'You are a helpful assistant.'}");
            builder.defaultSystem(sysPr);
            if (functions != null && functions.length > 0) builder.defaultFunctions(functions);
            return new LLMNodeActionCopy<>(builder.build(), nodeAttributes);
        }

        public LLMNodeActionCopy.Builder withSysPrompt (String prompt) {
            this.sysPrompt = prompt;
            nodeAttributes.addProperty("sysPrompt", prompt);
            return this;
        }

        public LLMNodeActionCopy.Builder withFunctions (String... functionNames) {
            if (functionNames == null || functionNames.length == 0) return this;
            this.functions = functionNames;
            nodeAttributes.addProperty("functions", functionNames);
            return this;
        }

        public LLMNodeActionCopy.Builder withInputSchema (String ...inputSchema){
            Arrays.stream(inputSchema).forEach(input -> nodeAttributes.addInputSchema(input));
            return this;
        }

        public LLMNodeActionCopy.Builder withOutputSchema (String ...outputSchema){
            Arrays.stream(outputSchema).forEach(output -> nodeAttributes.addOutputSchema(output));
            return this;
        }
    }
}

