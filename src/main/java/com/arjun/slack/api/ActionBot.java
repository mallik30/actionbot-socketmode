package com.arjun.slack.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
public class ActionBot {
	public static void main(String[] args) {
		SpringApplication.run(ActionBot.class, args);
	}
}
