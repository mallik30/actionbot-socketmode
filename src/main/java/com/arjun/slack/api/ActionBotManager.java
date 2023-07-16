package com.arjun.slack.api;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
//import com.mongodb.client.result.UpdateResult;
import com.slack.api.app_backend.slash_commands.payload.SlashCommandPayload;
import com.slack.api.bolt.App;
import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostEphemeralResponse;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.reminders.RemindersAddResponse;
import com.slack.api.methods.response.reminders.RemindersCompleteResponse;
import com.slack.api.methods.response.reminders.RemindersDeleteResponse;
import com.slack.api.methods.response.users.UsersInfoResponse;
import com.slack.api.model.Reminder;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ActionBotManager {

	@Value("${SLACK_USER_TOKEN}")
	private String userToken;

	private static final String DESCRIPTION = "description";
	private static final String ETA = "eta";
	private static final String PRIORITY = "priority";
	private static final String INVALID_FORMAT_UPDATE_EXCEPTION_MESSAGE = "Invalid format. please send a request in following format /updateItem ACT-myuniquecode --description my description, --eta my eta, --priority my priority";
	private static final String ERROR = "Error: {}";
	private SimpleDateFormat formatNoYear = new SimpleDateFormat("MM/dd h:mm a, z");
	private SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd h:mm a, z");
	@Autowired
	private App app;

	@PostConstruct
	public void init() {
		createCommand();
		updateCommand();
		addReminder();
		deleteReminder();
		completeReminder();
	}

	public void addReminder() {
		app.command("/addreminder", (req, ctx) -> {
			SlashCommandPayload payload = req.getPayload();
			app.executorService().submit(() -> {
				try {
					RemindersAddResponse remindersAdd = ctx.client()
							.remindersAdd(r -> r.text("This is reminder").time("in 1 hour").token(userToken));
					Reminder reminderId = remindersAdd.getReminder();
					String id = reminderId.getId();

					chatPostMessage(ctx, "reminderid: " + id);
					System.out.println("id- " + id);

					RemindersDeleteResponse remindersDelete = ctx.client()
							.remindersDelete(d -> d.reminder(id).token(userToken));

					boolean ok = remindersDelete.isOk();

					System.out.println("test- " + ok);

				} catch (Exception e) {
					log.error(ERROR, e.getMessage(), e);
					System.out.println("errror: " + e.getLocalizedMessage());
				}
			});
			return ctx.ack("<@" + payload.getUserId() + ">" + ", add and delete reminder");
		});
	}

	public void deleteReminder() {
		app.command("/deletereminder", (req, ctx) -> {
			SlashCommandPayload payload = req.getPayload();
			app.executorService().submit(() -> {
				String payloadText = payload.getText().strip();
				// Do anything asynchronously here
				try {
					RemindersDeleteResponse remindersDelete = ctx.client()
							.remindersDelete(d -> d.reminder(payloadText).token(userToken));

					boolean ok = remindersDelete.isOk();

					chatPostMessage(ctx, "delete reminderid: " + payloadText + " isDeleted:" + ok + " reason: "
							+ remindersDelete.getError());

					System.out.println("test- " + ok);

				} catch (Exception e) {
					log.error(ERROR, e.getMessage(), e);
					System.out.println("errror: " + e.getLocalizedMessage());
				}
			});
			return ctx.ack("<@" + payload.getUserId() + ">" + ", delete reminder");
		});
	}

	public void completeReminder() {
		app.command("/completereminder", (req, ctx) -> {
			SlashCommandPayload payload = req.getPayload();
			app.executorService().submit(() -> {
				String payloadText = payload.getText().strip();
				// Do anything asynchronously here
				try {
					RemindersCompleteResponse remindersComplete = ctx.client()
							.remindersComplete(d -> d.reminder(payloadText).token(userToken));

					boolean ok = remindersComplete.isOk();

					chatPostMessage(ctx, "complete reminderid: " + payloadText + " isCompleted:" + ok + " reason: "
							+ remindersComplete.getError());

					System.out.println("test- " + ok);

				} catch (Exception e) {
					log.error(ERROR, e.getMessage(), e);
					System.out.println("errror: " + e.getLocalizedMessage());
				}
			});
			return ctx.ack("<@" + payload.getUserId() + ">" + ", delete reminder");
		});
	}

	public void createCommand() {
		app.command("/createitem", (req, ctx) -> {
			SlashCommandPayload payload = req.getPayload();
			app.executorService().submit(() -> {
				String payloadText = payload.getText();
				String createdBy = payload.getUserId();
				String channelId = ctx.getChannelId();
				String responseURL = payload.getResponseUrl();
				log.info("Request received for payload: {}", payload);
				// Do anything asynchronously here
				try {
					ActionItem itemToAdd = new ActionItem();
					itemToAdd = createParseString(payloadText, createdBy, channelId, responseURL);
					// get user timezone
					UsersInfoResponse user = getUser(ctx, payload.getUserId());
					String json = new Gson().toJson(user);
					String tz = user.getUser().getTzLabel();
					// add year to eta
					String eta = determineYear(itemToAdd.getEta(), tz) + "/" + itemToAdd.getEta() + ", " + tz;
					itemToAdd.setEta(eta);
//							mongoTemplate.insertActionItem(itemToAdd);
					String message = "<@" + payload.getUserId() + ">"
							+ ", your request has been successfully created :+1: with ID: " + itemToAdd.getRequestId();
					chatPostMessage(ctx, message);
				} catch (Exception e) {
					log.error(ERROR, e.getMessage(), e);
				}
			});
			return ctx.ack("<@" + payload.getUserId() + ">"
					+ ", we have received your request and will notify you once it has been processed.");
		});
	}

	public void updateCommand() {
		app.command("/updateitem", (req, ctx) -> {
			app.executorService().submit(() -> {
				try {
					SlashCommandPayload payload = req.getPayload();
					String payloadText = payload.getText();
					ActionItem actionItem = new ActionItem();

					// payload string parsing
					String[] payloadArr = payloadText.split("--");

					if (payloadArr[0].startsWith("ACT-")) {
//								log.info("ID retrieved: {}", payloadArr[0]);

						actionItem.setRequestId(payloadArr[0].strip());
					} else {
						return chatPostEphemeralMessage(ctx, payload.getUserId(),
								INVALID_FORMAT_UPDATE_EXCEPTION_MESSAGE);
					}

					for (int i = 1; i < payloadArr.length; i++) {
						String str = payloadArr[i];

						if (str.startsWith(DESCRIPTION)) {
							actionItem.setDescription(str.substring(DESCRIPTION.length()).strip());
						} else if (str.startsWith(ETA)) {
							String etaToUpdate = str.substring(ETA.length()).strip();
							// 07/05 10:30 am
							// get user timezone
							UsersInfoResponse user = getUser(ctx, payload.getUserId());
							String tz = user.getUser().getTzLabel();
							// add year to eta
							// 2023/07/05 10:30 am, Central Daylight Time
							etaToUpdate = determineYear(etaToUpdate, tz) + "/" + etaToUpdate + ", " + tz;
							actionItem.setEta(etaToUpdate);
						} else if (str.startsWith(PRIORITY)) {
							actionItem.setPriority(str.substring(PRIORITY.length()).strip());
						} else {
							chatPostEphemeralMessage(ctx, payload.getUserId(), INVALID_FORMAT_UPDATE_EXCEPTION_MESSAGE);
							return ctx.ack("Invalid command.");
						}
					}
					actionItem.setStatus(Status.UPDATED);

					// validate id and update action item in mongo
//							UpdateResult updateResult = mongoTemplate.updateActionItemProperty(actionItem);

					long val = 1;

					if (val > 0) {
						chatPostMessage(ctx,
								"Your request, " + actionItem.getRequestId() + ", has been successfully updated.");
					} else {
						chatPostEphemeralMessage(ctx, payload.getUserId(), "Your request, " + actionItem.getRequestId()
								+ ", could not be found. Double check to make sure the ID is valid.");
					}

				} catch (Exception e) {
					log.error(ERROR, e.getMessage(), e);
				}
				return ctx.ack();
			});
			return ctx.ack();
		});
	}

	private ChatPostMessageResponse chatPostMessage(SlashCommandContext ctx, String message)
			throws IOException, SlackApiException {
		return ctx.client().chatPostMessage(r -> r.channel(ctx.getChannelId()).text(message));
	}

	private ChatPostEphemeralResponse chatPostEphemeralMessage(SlashCommandContext ctx, String userID, String message)
			throws IOException, SlackApiException {
		return ctx.client().chatPostEphemeral(r -> r.channel(ctx.getChannelId()).user(userID).text(message));
	}

	private UsersInfoResponse getUser(SlashCommandContext ctx, String userID) throws IOException, SlackApiException {
		return ctx.client().usersInfo(r -> r.user(userID));
	}

	private String dateFormattedString(Long unixTimeStamp, String fallback) {
		return "<!date^" + unixTimeStamp + "^{date_long_pretty} at {time} (local time)|" + fallback + ">";
	}

	public ActionItem createParseString(String payload, String createdBy, String channelId, String getResponseUrl)
			throws SlackApiException {
		log.info("Request received for payload:-{}", payload);
		String[] rawData = payload.split("--");
		if (payload != null && payload.contains("--description") && payload.contains("--eta")
				&& payload.contains("--priority") && rawData[1].substring(0, 12).equals("description ")
				&& rawData[2].substring(0, 4).equals("eta ") && rawData[3].substring(0, 9).equals("priority ")) {
			String description = rawData[1].substring(12);
			String eta = rawData[2].substring(4).strip();
			String priority = rawData[3].substring(9);
			String requestId = "ACT-" + UUID.randomUUID();
			LocalDateTime createdTimestamp = LocalDateTime.now();
			LocalDateTime lastUpdatedTimestamp = createdTimestamp;

			ActionItem actionItem = new ActionItem();
			actionItem.setRequestId(requestId);
			actionItem.setDescription(description);
			actionItem.setPriority(priority);
			actionItem.setEta(eta);
			actionItem.setCreatedBy("<@" + createdBy + ">");
			actionItem.setCreatedTimestamp(createdTimestamp);
			actionItem.setLastUpdatedTimestamp(lastUpdatedTimestamp);
			actionItem.setStatus(Status.CREATED);
			actionItem.setChannelId("<#" + channelId + ">");
			actionItem.setResponseURL(getResponseUrl);

			return actionItem;
		} else {
			throw new SlackApiException(null, "Invalid String");
		}
	}

	private String determineYear(String timestamp, String timezone) throws ParseException {
		Calendar cal = Calendar.getInstance();
		// Sun Jul 05 10:30:00 CDT 1970
		Date temp = formatNoYear.parse(timestamp + ", " + timezone);
		// 07/05 10:30 AM, CDT
		String ts = formatNoYear.format(temp);
		cal.setTime(formatNoYear.parse(ts));
		LocalDateTime now = LocalDateTime.now();
		// note that Calendar.MONTH starts from 0 so we need to +1
		if (now.getMonthValue() < (cal.get(Calendar.MONTH) + 1)) {
			return Integer.toString(now.getYear());
		} else if (now.getMonthValue() == (cal.get(Calendar.MONTH) + 1)
				&& now.getDayOfMonth() < cal.get(Calendar.DAY_OF_MONTH)) {
			return Integer.toString(now.getYear());
		} else if (now.getMonthValue() == (cal.get(Calendar.MONTH) + 1)
				&& now.getDayOfMonth() == cal.get(Calendar.DAY_OF_MONTH)
				&& now.getHour() < cal.get(Calendar.HOUR_OF_DAY)) {
			return Integer.toString(now.getYear());
		} else if (now.getMonthValue() == (cal.get(Calendar.MONTH) + 1)
				&& now.getDayOfMonth() == cal.get(Calendar.DAY_OF_MONTH)
				&& now.getHour() == cal.get(Calendar.HOUR_OF_DAY) && now.getMinute() <= cal.get(Calendar.MINUTE)) {
			return Integer.toString(now.getYear());
		} else {
			return Integer.toString(now.getYear() + 1);
		}
	}

	private long convertToUnixTimestamp(String timestamp) throws ParseException {
		Date temp = formatter.parse(timestamp);
		String ts = formatter.format(temp);
		Calendar cal = Calendar.getInstance();
		cal.setTime(formatter.parse(ts));
		// conversion to unix
		return cal.getTimeInMillis() / 1000;
	}

}
