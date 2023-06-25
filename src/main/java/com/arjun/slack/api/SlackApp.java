package com.arjun.slack.api;

import javax.servlet.annotation.WebServlet;

import com.slack.api.bolt.App;
import com.slack.api.bolt.servlet.SlackAppServlet;

@WebServlet("/slack/events")
public class SlackApp extends SlackAppServlet {
	private static final long serialVersionUID = 1L;

	public SlackApp(App app) {
		super(app);
	}
}
