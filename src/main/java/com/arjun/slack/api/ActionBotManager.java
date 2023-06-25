package com.arjun.slack.api;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.slack.api.bolt.App;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.model.event.ReactionAddedEvent;

@Service
public class ActionBotManager {

	@Autowired
	private App app;

	@PostConstruct
	public void init() {
		reactionAdded();
		appMention();
	}

	public void reactionAdded() {
		app.event(ReactionAddedEvent.class, (payload, ctx) -> {
			return ctx.ack();
		});
	}

	public void appMention() {
		app.event(AppMentionEvent.class, (payload, ctx) -> {
			return ctx.ack();
		});
	}

}
