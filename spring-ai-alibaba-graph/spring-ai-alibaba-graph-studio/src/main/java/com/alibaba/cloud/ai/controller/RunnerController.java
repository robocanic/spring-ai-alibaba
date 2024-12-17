package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.api.RunnerAPI;
import com.alibaba.cloud.ai.saver.AppSaver;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("graph-studio/api/run")
public class RunnerController implements RunnerAPI {


	private final AppSaver appSaver;

	public RunnerController(AppSaver appSaver) {
		this.appSaver = appSaver;
	}

	@Override
	public AppSaver getAppSaver() {
		return appSaver;
	}

}
