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

	@Value("${SLACK_BOT_TOKEN}")
	private String botToken;

	@Value("${SLACK_USER_TOKEN}")
	private String userToken;

//	@Autowired // not listening
	private App app;

	public ActionBotManager(ApplicationContext applicationContext) {
//		displayAllBeans(applicationContext);
		app = applicationContext.getBean("app", App.class);
//		app = ApplicationContextUtils.getApplicationContext().getBean("app", App.class);
	}

	public static void displayAllBeans(ApplicationContext applicationContext) {
		String[] allBeanNames = applicationContext.getBeanDefinitionNames();
		for (String beanName : allBeanNames) {
			System.out.println("bean: " + beanName);
		}
	}

	public App reactionAdded(/* App app */) {
		return app.event(ReactionAddedEvent.class, (payload, ctx) -> {
			ReactionAddedEvent event = payload.getEvent();
			if (event.getReaction().equals("white_check_mark")) {
				ChatPostMessageResponse message = ctx.client().chatPostMessage(r -> r
						.channel(event.getItem().getChannel()).threadTs(event.getItem().getTs())
						.text("<@" + event.getUser() + "> Thank you! We greatly appreciate your efforts :two_hearts:"));
				if (!message.isOk()) {
					ctx.logger.error("chat.postMessage failed: {}", message.getError());
				}
			}
			return ctx.ack();
		});
	}

	public App appMention(/* App app */) {
		return app.event(AppMentionEvent.class, (payload, ctx) -> {
			AppMentionEvent event = payload.getEvent();
			String typeOfCommand = event.getText().substring(event.getText().indexOf('>') + 1);
			if (StringUtils.isNotBlank(typeOfCommand) && typeOfCommand.trim().equals("create")) {
				ChatPostMessageResponse message = ctx.client()
						.chatPostMessage(r -> r.channel(event.getChannel()).threadTs(event.getTs())// event.getThreadTs()
								.text("please provide description")); // "<@" + event.getUser() +

				if (!message.isOk()) {
					ctx.logger.error("chat.postMessage failed: {}", message.getError());
				}
			} else {
				ctx.logger.info("only supports create!");
			}
			return ctx.ack();
		});
	}

}
