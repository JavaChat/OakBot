package oakbot.task;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import oakbot.bot.Bot;
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
	private final long timeToWaitBeforePosting;
	private final int latestMessageCount, latestMessageCharacterLimit;
	private final Map<Integer, Instant> runTimes;

	/**
	 * @param chatGPTParameters parameters for connecting to ChatGPT
	 * @param timeToWaitBeforePosting the amount of time to wait before posting
	 * a message (in milliseconds)
	 * @param latestMessagesCount the number of chat room messages to send to
	 * ChatGPT (each message counts against the usage quota)
	 * @param latestMessagesCharacterLimit each chat message that is sent to
	 * ChatGPT will not exceed this number of characters (includes markdown
	 * syntax). Chat messages that do will be truncated (without cutting off
	 * words). 0 to disable truncation (each message counts against the usage
	 * quota).
	 * @param roomIds the rooms to initially start this task in
	 */
	public ChatGPTTask(ChatGPTParameters chatGPTParameters, long timeToWaitBeforePosting, int latestMessageCount, int latestMessageCharacterLimit, List<Integer> roomIds) {
		this.chatGPTParameters = chatGPTParameters;
		this.timeToWaitBeforePosting = timeToWaitBeforePosting;
		this.latestMessageCount = latestMessageCount;
		this.latestMessageCharacterLimit = latestMessageCharacterLimit;

		runTimes = new HashMap<>();
		for (Integer roomId : roomIds) {
			resetTimer(roomId);
		}
	}

	@Override
	public long nextRun() {
		long lowest = timeToWaitBeforePosting;
		synchronized (runTimes) {
			for (Instant instant : runTimes.values()) {
				long diff = instant.toEpochMilli() - Instant.now().toEpochMilli();
				if (diff < lowest) {
					lowest = diff;
				}
			}
		}

		if (lowest < 1) {
			lowest = 1;
		}
		return lowest;
	}

	@Override
	public void run(Bot bot) throws Exception {
		List<Integer> roomsToPostTo = new ArrayList<>();
		synchronized (runTimes) {
			for (Map.Entry<Integer, Instant> entry : runTimes.entrySet()) {
				Integer roomId = entry.getKey();
				if (!bot.getRooms().contains(roomId)) {
					continue;
				}

				Instant runTime = entry.getValue();
				if (runTime.isAfter(Instant.now())) {
					continue;
				}

				roomsToPostTo.add(roomId);
			}
		}

		for (Integer roomId : roomsToPostTo) {
			resetTimer(roomId);

			List<ChatMessage> messages = bot.getLatestMessages(roomId, latestMessageCount);
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

				if (message.getUserId() == bot.getUserId()) {
					request.addBotMessage(truncatedContentMd);
				} else {
					request.addHumanMessage(truncatedContentMd);
				}
			}

			String message;
			try (CloseableHttpClient client = createClient()) {
				message = request.send(client);
			}

			PostMessage postMessage = new PostMessage(message);
			postMessage.splitStrategy(SplitStrategy.WORD);
			bot.sendMessage(roomId, postMessage);
		}
	}

	/**
	 * Resets the chat timer for the given room.
	 * @param roomId the room ID
	 */
	public void resetTimer(int roomId) {
		Instant nextRunTime = Instant.now().plusMillis(timeToWaitBeforePosting);
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
