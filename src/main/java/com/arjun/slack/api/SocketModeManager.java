package com.arjun.slack.api;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.model.event.AppMentionEvent;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SocketModeManager {

	private SocketModeApp socketModeApp;

	@PostConstruct
	public void initialize() throws Exception {
		String botToken = System.getenv("SLACK_BOT_TOKEN");
		App app = new App(AppConfig.builder().singleTeamBotToken(botToken).build());

		// you can use the injected service inside the lambda
		app.event(AppMentionEvent.class, (req, ctx) -> {
			log.info("AppMentionEvent", req.getEvent());
			log.info("AppMentionEventText", req.getEvent().getText());
			return ctx.ack();
		});

		app.command("/hello", (req, ctx) -> {
			log.info("command/hello", req.getPayload().getText());
			return ctx.ack();
		});

		String appToken = System.getenv("SLACK_APP_TOKEN");
		this.socketModeApp = new SocketModeApp(appToken, app);
		this.socketModeApp.startAsync();
	}

	// cleaning up the instance when terminating the app is highly recommended
	@PreDestroy
	public void destroy() {
		if (this.socketModeApp != null && !this.socketModeApp.isClientStopped()) {
			try {
				this.socketModeApp.close();
			} catch (Exception e) {
				log.error("Failed to close the underlying Socket Mode client", e);
			}
		}
	}

}
