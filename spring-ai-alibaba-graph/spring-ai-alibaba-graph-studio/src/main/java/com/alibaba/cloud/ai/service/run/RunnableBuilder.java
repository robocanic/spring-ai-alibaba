package com.alibaba.cloud.ai.service.run;

import java.util.Map;

public interface RunnableBuilder<T> {

    Boolean support(String runnableType);

    Runnable build(T runnableModel, String runId, Map<String, Object> rawInputs) throws Exception;

}
