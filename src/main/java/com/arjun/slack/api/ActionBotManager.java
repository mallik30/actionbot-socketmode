package com.arjun.slack.api;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.slack.api.bolt.App;
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
