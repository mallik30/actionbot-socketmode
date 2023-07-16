package com.arjun.slack.api;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.slack.api.bolt.App;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.conversations.ConversationsRepliesResponse;
import com.slack.api.model.Message;
import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.model.event.MessageEvent;
import com.slack.api.model.event.ReactionAddedEvent;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ActionBotAppMentionManagerApproach2 {

	private static final String ACT_PREFIX = "ACT";

	private static final String REQUEST_RECEIVED = "Thank you we received your request ";

	private static final String THANK_YOU_STARTS_WITH = "Thank you";

	private static final String INVALID_COMMAND = "Invalid command, we support only following!";

	private static final String PRIORITY = "Priority level for the action item?";

	private static final String ETA = "What is the expected timeframe for resolving the action item?";

	private static final String DESCRIPTION = "Provide a description for the action item";

	@Value("${SLACK_BOT_TOKEN}")
	private String botToken;

	@Value("${SLACK_USER_TOKEN}")
	private String userToken;

	@Autowired
	private App app;

	@PostConstruct
	public void init() {
		reactionAdded();
		appMention();
		messageEvent();
	}

	public void reactionAdded() {
		app.event(ReactionAddedEvent.class, (payload, ctx) -> {
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

	public void appMention() {
		app.event(AppMentionEvent.class, (payload, ctx) -> {
			AppMentionEvent event = payload.getEvent();
			String typeOfCommand = event.getText().substring(event.getText().indexOf('>') + 1);

			switch (typeOfCommand.trim()) {
			case "create":
				chatPostMessage(ctx, event, DESCRIPTION);
				break;
			case "update":
				// add update code here
				break;
			default:
				chatPostMessage(ctx, event, INVALID_COMMAND);
			}
			return ctx.ack();
		});
	}

	private ChatPostMessageResponse chatPostMessage(EventContext ctx, AppMentionEvent event, String message)
			throws IOException, SlackApiException {
		return ctx.client()
				.chatPostMessage(create -> create.channel(event.getChannel()).threadTs(event.getTs()).text(message));
	}

	public void messageEvent() {
		app.event(MessageEvent.class, (payload, ctx) -> {
			MessageEvent event = payload.getEvent();
			// Listen only thread replies, if ThreadTs is present it is a reply
			if (event.getThreadTs() != null) {

				app.executorService().submit(() -> {
					try {
						// question and answer map
						Map<String, String> questionAndAnswerMap = qAndAmap();

						// get slack thread replies
						List<String> getThreadReplies = conversationReplies(ctx, event);

						String nextMessage = null;
						// update answers
						updateAnswers(questionAndAnswerMap, getThreadReplies);

						// ignore replies for threads not related to BOT && completed requests
						if (ignoreCompletedRequests(getThreadReplies)) {
							final String nextQuestion;
							// find next question
							nextMessage = findNextQuestion(questionAndAnswerMap, nextMessage);

							// send thank you message if there are no questions to ask
							if (null == nextMessage) {
								nextMessage = constructMongoPayload(event, questionAndAnswerMap);
								// add async mongo insert call here
							}

							nextQuestion = nextMessage;

							chatPostMessage(ctx, event, nextQuestion);
						}
					} catch (Exception ex) {
//						log.error("add valid exception message here-{}", ex);
					}

				});

			}

			return ctx.ack();
		});
	}

	private boolean ignoreCompletedRequests(List<String> getThreadReplies) {
		boolean ignoreReplies = getThreadReplies.stream().anyMatch(reply -> reply.startsWith(THANK_YOU_STARTS_WITH));
		return getThreadReplies.get(0).contains("U05ABC5BWVB") && !ignoreReplies;
	}

	private String findNextQuestion(Map<String, String> questionAndAnswerMap, String nextMessage) {
		// find next question
		if (null == nextMessage) {
			nextMessage = questionAndAnswerMap.keySet().stream()
					.filter(question -> questionAndAnswerMap.get(question) == null).findFirst().orElse(null);
		}
		return nextMessage;
	}

	private String constructMongoPayload(MessageEvent event, Map<String, String> questionAndAnswerMap) {
		String requestId = ACT_PREFIX + UUID.randomUUID().toString();
		String createdBy = "<@" + event.getParentUserId() + ">" + event.getParentUserId();
		String createdTimestamp = LocalDateTime.now().toString();
		String lastUpdatedTimestamp = createdTimestamp;
		String channelId = "<#" + event.getChannel() + ">" + event.getChannel();

		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(REQUEST_RECEIVED).append(createdBy).append("\n").append("Request Id: ").append(requestId)
				.append("\n").append("description: ").append(questionAndAnswerMap.get(DESCRIPTION)).append("\n")
				.append("priority: ").append(questionAndAnswerMap.get(PRIORITY)).append("\n").append("eta: ")
				.append(questionAndAnswerMap.get(ETA)).append("\n").append("createdBy: ").append(createdBy).append("\n")
				.append("createdTimestamp: ").append(createdTimestamp).append("\n").append("lastUpdatedTimestamp: ")
				.append(lastUpdatedTimestamp).append("\n").append("status: ").append("CREATED").append("\n")
				.append("channelId: ").append(channelId);

		return stringBuffer.toString();
	}

	private ChatPostMessageResponse chatPostMessage(EventContext ctx, MessageEvent event, final String nextQuestion)
			throws IOException, SlackApiException {
		return ctx.client()
				.chatPostMessage(r -> r.channel(event.getChannel()).threadTs(event.getTs()).text(nextQuestion));
	}

	private void updateAnswers(Map<String, String> questionAndAnswerMap, List<String> messages) {
		// iterate from the last to find out the answer and question
		for (int i = messages.size() - 1; i > 1; i--) {
			String question = messages.get(i - 1);
			if (questionAndAnswerMap.containsKey(question)) {
				questionAndAnswerMap.put(question, messages.get(i));
			}
		}
	}

	private Map<String, String> qAndAmap() {
		Map<String, String> questionAndAnswerMap = new LinkedHashMap<>();
		questionAndAnswerMap.put(DESCRIPTION, null);
		questionAndAnswerMap.put(ETA, null);
		questionAndAnswerMap.put(PRIORITY, null);
		return questionAndAnswerMap;
	}

	private List<String> conversationReplies(EventContext ctx, MessageEvent event)
			throws IOException, SlackApiException {
		ConversationsRepliesResponse conversationsReplies = ctx.client()
				.conversationsReplies(r -> r.channel(event.getChannel()).ts(event.getThreadTs()));
		return conversationsReplies.getMessages().stream().map(Message::getText).collect(Collectors.toList());
	}
}
