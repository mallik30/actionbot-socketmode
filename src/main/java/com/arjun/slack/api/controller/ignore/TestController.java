package com.arjun.slack.api.controller.ignore;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

	@GetMapping("/get")
	public String myMethod(@RequestParam String val) {
		return val;
	}
}
