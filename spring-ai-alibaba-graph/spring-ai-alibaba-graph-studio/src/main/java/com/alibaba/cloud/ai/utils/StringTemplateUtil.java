package com.alibaba.cloud.ai.utils;

import com.alibaba.cloud.ai.model.VariableSelector;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.alibaba.cloud.ai.model.VariableSelector.DEFAULT_SEPARATOR;

public class StringTemplateUtil {

	/**
	 * turn dify string template into spring-ai template
	 * @param template e.g. "the output is {{#output}}"
	 * @param variables e.g. {{#output#}} -> context
	 * @return spring-ai template e.g. {output}
	 */
	public static String fromDifyTmpl(String template, List<String> variables) {
		String regex = "\\{\\{#(.*?)#\\}\\}";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(template);
		StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			String matchedText = matcher.group(1);
			String[] parts = matchedText.split("\\.", 2);
			String variable = parts[0] + DEFAULT_SEPARATOR + parts[1];
			variables.add(variable);
			matcher.appendReplacement(result, "{" + variable + "}");
		}
		matcher.appendTail(result);
		return result.toString();
	}

	/**
	 * turn spring-ai template into dify string template
	 * @param template e.g. "the output is {output}"
	 * @return dify template e.g. "the output is {{#output#}}"
	 */
	public static String toDifyTmpl(String template) {
		String regex = "\\{(.*?)}";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(template);
		StringBuilder result = new StringBuilder();
		while (matcher.find()){
			String matchedText = matcher.group(1);
			String[] parts = matchedText.split(DEFAULT_SEPARATOR, 2);
			String variable = parts[0] + "." + parts[1];
			matcher.appendReplacement(result, "{{#" + variable + "#}}");
		}
		matcher.appendTail(result);
		return result.toString();
	}

}
