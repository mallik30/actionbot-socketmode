package com.arjun.slack.api.controller.ignore;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.reminders.RemindersAddResponse;
import com.slack.api.methods.response.users.UsersListResponse;
import com.slack.api.model.User;

@RestController
public class SlackAPIClient {

	@Value("${SLACK_BOT_TOKEN}")
	private String botToken;

	@Value("${SLACK_USER_TOKEN}")
	private String userToken;

	Slack slack = Slack.getInstance();

	@PostMapping("/post")
	public ChatPostMessageResponse chatPostMessage(@RequestParam String value) throws IOException, SlackApiException {
		return slack.methods(botToken)
				.chatPostMessage(req -> req.channel("#random")
						.text(":wave: Hi from a bot written in Java!" + " " + value));
	}

	@PostMapping("/reminder")
	public RemindersAddResponse chatPostMessages(@RequestParam String time) throws IOException, SlackApiException {
		// TODO assign to the user from user.list
		return slack.methods(botToken)
				.remindersAdd(rem -> rem.text("!random Please check this is a reminder " + time)
						.time(time)
						.token(userToken));
	}

	@GetMapping("/list")
	public UsersListResponse chatPostMessages() throws IOException, SlackApiException {
		UsersListResponse response = slack.methods(botToken)
				.usersList(req -> req);
		List<User> users = response.getMembers();
		System.out.println("users: " + users);
//		for (User user : users) {
//			String userId = user.getId();
//			String userName = user.getName();
//			// Output or store the user ID and name for reference
//			System.out.println("User ID: " + userId + ", User Name: " + userName);
//		}
		return response;
	}

}
