package com.arjun.slack.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;

@Configuration
public class SlackConfig {

	@Value("${SLACK_BOT_TOKEN}")
	private String botToken;

	@Value("${SLACK_SIGNING_SECRET}")
	private String signingSecret;

	@Bean
	AppConfig appConfig() {
		return AppConfig.builder().singleTeamBotToken(botToken).signingSecret(signingSecret).build();
	}

	@Bean
	App app(AppConfig appConfig) {
		return new App(appConfig);
	}
	
}
