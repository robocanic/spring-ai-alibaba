package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.api.RunnerAPI;
import com.alibaba.cloud.ai.saver.AppSaver;
import com.alibaba.cloud.ai.service.runner.RunnableEngine;
import com.alibaba.cloud.ai.service.runner.RunnableModel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("graph-studio/api/run")
public class RunnerController implements RunnerAPI {

	private final AppSaver appSaver;

	private final RunnableEngine<?> runnableEngine;

	public  RunnerController(AppSaver appSaver, RunnableEngine<?> runnableEngine) {
		this.appSaver = appSaver;
		this.runnableEngine = runnableEngine;
	}


	@Override
	public <T extends RunnableModel> RunnableEngine<T> getRunnableEngine() {
		return (RunnableEngine<T>) runnableEngine;
	}

	@Override
	public AppSaver getAppSaver() {
		return appSaver;
	}

}
