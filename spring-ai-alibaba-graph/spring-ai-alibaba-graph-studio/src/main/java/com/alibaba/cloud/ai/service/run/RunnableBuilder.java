package com.alibaba.cloud.ai.service.run;

import java.util.Map;

public interface RunnableBuilder<T> {

    Boolean support(String runnableType);

    Runnable build(T runnableModel, Map<String, Object> inputs);

}
