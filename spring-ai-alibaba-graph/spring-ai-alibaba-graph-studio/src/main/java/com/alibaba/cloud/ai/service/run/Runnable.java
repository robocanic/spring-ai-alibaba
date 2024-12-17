package com.alibaba.cloud.ai.service.run;

import com.alibaba.cloud.ai.model.RunEvent;
import reactor.core.publisher.Flux;

import java.util.Map;

public interface Runnable {

    RunEvent invoke() throws Exception;

    Flux<RunEvent> stream() throws Exception;

}
