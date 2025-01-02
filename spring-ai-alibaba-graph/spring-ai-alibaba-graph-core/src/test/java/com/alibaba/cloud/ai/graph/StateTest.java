package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.node.llm.LLMNodeAction;
import com.alibaba.cloud.ai.graph.serializer.agent.JSONStateSerializer;
import com.alibaba.cloud.ai.graph.state.ReduceStrategy;
import com.alibaba.cloud.ai.graph.state.ReduceStrategyType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.StateGraph.START;

public class StateTest {

    @Data
    @NoArgsConstructor
    class MessageState{
        @ReduceStrategy(value = ReduceStrategyType.APPEND_LIST)
        private List<Message> messages;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class RetrieverInputState {
        private String code;
        private String codeLanguage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class RetrieverOutputState {
        private List<Document> documents;
    }

    @Slf4j
    class RetrieverNodeAction implements NodeAction<RetrieverInputState, RetrieverOutputState>{

        @Override
        public RetrieverOutputState apply(RetrieverInputState retrieverInputState) throws Exception {
            List<Document> documents = List.of(new Document("GraalVM 能够提前将 Java 应用程序编译成独立的二进\n" +
                    "制文件。与在 Java 虚拟机上运行的应用程序相比，这些二进制文件更小，启动速度快 100\n" +
                    "倍，在没有预热的情况下提供峰值性能，并且使用更少的内存和 CPU"));
            return new RetrieverOutputState(documents);
        }
    }

    @Test
    public void testNodeAction(){
        StateGraph<MessageState> stateGraph = new StateGraph<>(MessageState.class, new JSONStateSerializer());
        RetrieverNodeAction retrieverNodeAction = new RetrieverNodeAction();
        LLMNodeAction llmNodeAction = new LLMNodeAction.Builder(new DashScopeChatModel(new DashScopeApi(""))).build();
        CompiledGraph compiledGraph;
        try {
            stateGraph.addNode("code", AsyncNodeAction.node_async(retrieverNodeAction))
                    .addNode("llm", AsyncNodeAction.node_async(llmNodeAction))
                    .addEdge(START, "code").addEdge("code", "llm");
            compiledGraph = stateGraph.compile();
        } catch (GraphStateException e) {
            throw new RuntimeException(e);
        }
        try {
            MessageState messageState = (MessageState) compiledGraph.invoke(Map.of("messages", new UserMessage("为什么dubbo3启动快10倍？"))).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
