package com.alibaba.cloud.ai.service.run;

import com.alibaba.cloud.ai.model.RunEvent;
import reactor.core.publisher.Flux;

import java.util.Map;

public interface Runnable {

    RunEvent invoke(Map<String, Object> inputs);

    Flux<RunEvent> stream(Map<String, Object> outputs);

}
