/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.example.deepresearch.agents;

import com.alibaba.cloud.ai.example.deepresearch.config.PythonCoderProperties;
import com.alibaba.cloud.ai.example.deepresearch.tool.McpClientToolCallbackProvider;
import com.alibaba.cloud.ai.example.deepresearch.tool.PythonReplTool;
import lombok.SneakyThrows;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;

import java.nio.charset.Charset;
import java.util.Set;

@Configuration
public class AgentsConfiguration {

	@Value("classpath:prompts/researcher.md")
	private Resource researcherPrompt;

	@Value("classpath:prompts/coder.md")
	private Resource coderPrompt;

	/**
	 * Create Research Agent ChatClient Bean
	 * @param chatClientBuilder ChatClientBuilder McpAsyncClient and the locally configure
	 * ToolCallbackProviders.
	 * @return ChatClient
	 */
	@SneakyThrows
	@Bean

	public ChatClient researchAgent(ChatClient.Builder chatClientBuilder,
			McpClientToolCallbackProvider mcpClientToolCallbackProvider) {
		Set<ToolCallback> defineCallback = mcpClientToolCallbackProvider.findToolCallbacks("researchAgent");
		return chatClientBuilder.defaultSystem(researcherPrompt.getContentAsString(Charset.defaultCharset()))
			.defaultToolNames("tavilySearch")
			.defaultToolCallbacks(defineCallback.toArray(ToolCallback[]::new))
			// .defaultToolNames("tavilySearch", "firecrawlFunction") todo 待调整
			.build();
	}

	/**
	 * Create Coder Agent ChatClient Bean
	 * @param chatClientBuilder ChatClientBuilder McpAsyncClient and the locally configure
	 * ToolCallbackProviders.
	 * @return ChatClient
	 */
	@SneakyThrows
	@Bean
	public ChatClient coderAgent(ChatClient.Builder chatClientBuilder, PythonCoderProperties coderProperties,
			McpClientToolCallbackProvider mcpClientToolCallbackProvider) {
		Set<ToolCallback> defineCallback = mcpClientToolCallbackProvider.findToolCallbacks("coderAgent");
		return chatClientBuilder.defaultSystem(coderPrompt.getContentAsString(Charset.defaultCharset()))
			.defaultTools(new PythonReplTool(coderProperties))
			.defaultToolCallbacks(defineCallback.toArray(ToolCallback[]::new))
			.build();
	}

}
