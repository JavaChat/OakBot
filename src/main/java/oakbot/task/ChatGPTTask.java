package oakbot.task;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.chat.ChatMessage;
import oakbot.chat.SplitStrategy;
import oakbot.util.ChatBuilder;

/**
 * Periodically comment on the chat room's latest messages using ChatGPT.
 * @author Michael Angstadt
 */
public class ChatGPTTask implements ScheduledTask {
	private final ChatGPTParameters chatGPTParameters;
	private final Duration timeToWaitBeforePosting;
	private final int latestMessageCount, latestMessageCharacterLimit;
	private final Map<Integer, Instant> runTimes;

	/**
	 * @param chatGPTParameters parameters for connecting to ChatGPT
	 * @param timeToWaitBeforePosting the amount of time to wait before posting
	 * a message (duration string)
	 * @param latestMessagesCount the number of chat room messages to send to
	 * ChatGPT (each message counts against the usage quota)
	 * @param latestMessagesCharacterLimit each chat message that is sent to
	 * ChatGPT will not exceed this number of characters (includes markdown
	 * syntax). Chat messages that do will be truncated (without cutting off
	 * words). 0 to disable truncation (each message counts against the usage
	 * quota).
	 * @param roomIds the rooms to initially start this task in
	 */
	public ChatGPTTask(ChatGPTParameters chatGPTParameters, String timeToWaitBeforePosting, int latestMessageCount, int latestMessageCharacterLimit, List<Integer> roomIds) {
		this.chatGPTParameters = chatGPTParameters;
		this.timeToWaitBeforePosting = Duration.parse(timeToWaitBeforePosting);
		this.latestMessageCount = latestMessageCount;
		this.latestMessageCharacterLimit = latestMessageCharacterLimit;

		runTimes = new HashMap<>();
		roomIds.stream().forEach(this::resetTimer);
	}

	@Override
	public long nextRun() {
		long lowest;

		synchronized (runTimes) {
			if (runTimes.isEmpty()) {
				return timeToWaitBeforePosting.toMillis();
			}

			Instant now = Instant.now();

			//@formatter:off
			lowest = runTimes.values().stream()
				.map(runTime -> Duration.between(now, runTime))
				.mapToLong(Duration::toMillis)
			.min().getAsLong();
			//@formatter:on
		}

		return (lowest < 1) ? 1 : lowest;
	}

	@Override
	public void run(IBot bot) throws Exception {
		List<Integer> roomsToPostTo = findRoomsToPostTo(bot);

		for (Integer roomId : roomsToPostTo) {
			resetTimer(roomId);

			/*
			 * Do not post anything if the room has no messages.
			 */
			List<ChatMessage> messages = bot.getLatestMessages(roomId, latestMessageCount);
			if (messages.isEmpty()) {
				continue;
			}

			/*
			 * Do not post anything if the latest message was not authored by a
			 * human.
			 */
			ChatMessage latestMessage = messages.get(messages.size() - 1);
			boolean latestMsgPostedByBot = (latestMessage.getUserId() < 1);
			boolean latestMsgPostedByOak = (latestMessage.getUserId() == bot.getUserId());
			if (latestMsgPostedByBot || latestMsgPostedByOak) {
				continue;
			}

			ChatGPTRequest request = buildChatGPTRequest(messages, bot);

			String completion;
			try (CloseableHttpClient client = createClient()) {
				completion = request.send(client);
			}

			PostMessage postMessage = new PostMessage(completion).splitStrategy(SplitStrategy.WORD);
			bot.sendMessage(roomId, postMessage);
		}
	}

	private List<Integer> findRoomsToPostTo(IBot bot) {
		List<Integer> roomIds = new ArrayList<>();

		synchronized (runTimes) {
			for (Map.Entry<Integer, Instant> entry : runTimes.entrySet()) {
				Integer roomId = entry.getKey();
				boolean oakIsNotInTheRoom = !bot.getRooms().contains(roomId);
				if (oakIsNotInTheRoom) {
					continue;
				}

				Instant runTime = entry.getValue();
				if (runTime.isAfter(Instant.now())) {
					continue;
				}

				roomIds.add(roomId);
			}
		}

		return roomIds;
	}

	private ChatGPTRequest buildChatGPTRequest(List<ChatMessage> messages, IBot bot) {
		ChatGPTRequest request = new ChatGPTRequest(chatGPTParameters);

		for (ChatMessage message : messages) {
			String content = message.getContent().getContent();
			boolean fixedFont = message.getContent().isFixedFont();
			String contentMd = ChatBuilder.toMarkdown(content, fixedFont);

			String truncatedContentMd;
			if (latestMessageCharacterLimit > 0) {
				truncatedContentMd = SplitStrategy.WORD.split(contentMd, latestMessageCharacterLimit).get(0);
			} else {
				truncatedContentMd = contentMd;
			}

			boolean messagePostedByOak = (message.getUserId() == bot.getUserId());
			if (messagePostedByOak) {
				request.addBotMessage(truncatedContentMd);
			} else {
				request.addHumanMessage(truncatedContentMd);
			}
		}

		return request;
	}

	/**
	 * Resets the chat timer for the given room.
	 * @param roomId the room ID
	 */
	public void resetTimer(int roomId) {
		Instant nextRunTime = Instant.now().plus(timeToWaitBeforePosting);
		synchronized (runTimes) {
			runTimes.put(roomId, nextRunTime);
		}
	}

	/**
	 * Creates an HTTP client. This method is for unit testing.
	 * @return the HTTP client
	 */
	CloseableHttpClient createClient() {
		return HttpClients.createDefault();
	}
}
