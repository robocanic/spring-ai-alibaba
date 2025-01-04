package com.alibaba.cloud.ai.service.runner;

import com.alibaba.cloud.ai.model.RunEvent;
import reactor.core.publisher.Flux;

public interface Runnable {

    RunEvent invoke() throws Exception;

    Flux<RunEvent> stream() throws Exception;

}
