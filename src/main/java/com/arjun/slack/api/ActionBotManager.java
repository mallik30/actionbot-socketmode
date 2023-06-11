package com.arjun.slack.api;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.slack.api.Slack;
import com.slack.api.app_backend.slash_commands.payload.SlashCommandPayload;
import com.slack.api.bolt.App;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;

@Component
public class ActionBotManager {

	@Value("${SLACK_BOT_TOKEN}")
	private String botToken;

	@Value("${SLACK_USER_TOKEN}")
	private String userToken;

	private Slack slack;

	public ActionBotManager() {
		this.slack = Slack.getInstance();
	}

	public App slashCommand(App app) {
		// slack needs to return request within 3 seconds or else request will fail
		app.command("/hello", (req, ctx) -> {
			SlashCommandPayload payload = req.getPayload();

//			Parse the text here amd throw the error
			String userText = payload.getText();

			String requestId = "ACT-" + UUID.randomUUID()
					.toString();

			String channelId = payload.getChannelId();
			String userName = payload.getUserName();

			String message = "Your request has been received " + userName + " :+1: request id: " + requestId;

			ChatPostMessageResponse chatPostMessage = chatPostMessage(message, channelId, userName);

			// do something with chatPostMessage

			// "Something went wrong, please try in following format: /create --description
			// myDescription --eta 15:00 --priority p1"
			return ctx.ack();
		});
		return app;
	}

	public ChatPostMessageResponse chatPostMessage(String message, String channelId, String userName)
			throws IOException, SlackApiException {
		return slack.methods(botToken)
				.chatPostMessage(req -> req.channel(channelId)
						.text(message)
						.username(userName));
	}

}
