package oakbot.listener.chatgpt;

import static java.util.function.Predicate.not;
import static oakbot.bot.ChatActions.create;
import static oakbot.bot.ChatActions.doNothing;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import com.github.mangstadt.sochat4j.ChatMessage;
import com.github.mangstadt.sochat4j.Content;
import com.github.mangstadt.sochat4j.SplitStrategy;

import oakbot.bot.ChatActions;
import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.command.HelpDoc;
import oakbot.listener.CatchAllMentionListener;
import oakbot.task.ScheduledTask;
import oakbot.util.CharIterator;
import oakbot.util.ChatBuilder;
import oakbot.util.HttpFactory;

/**
 * Periodically comment on the chat room's latest messages using ChatGPT. Posts
 * a response when a user mentions the bot in chat.
 * @author Michael Angstadt
 */
public class ChatGPT implements ScheduledTask, CatchAllMentionListener {
	private static final Logger logger = Logger.getLogger(ChatGPT.class.getName());
	private static final Collection<String> imageTypesSupportedByVisionModel = Set.of("image/png", "image/jpeg", "image/gif", "image/webp");

	private final OpenAIClient openAIClient;
	private final MoodCommand moodCommand;
	private final String model;
	private final String defaultPrompt;
	private final Duration timeBetweenSpontaneousPosts;
	private final int completionMaxTokens;
	private final int numLatestMessagesToIncludeInRequest;
	private final int latestMessageCharacterLimit;
	private final Map<Integer, String> promptsByRoom;
	private final Map<Integer, Instant> spontaneousPostTimesByRoom = new HashMap<>();
	private boolean ignoreNextMessage;
	private boolean firstRun = true;

	/**
	 * @param openAIClient the OpenAI client
	 * @param moodCommand the mood command object or null if not set
	 * @param model the model (e.g. "gpt-3.5-turbo")
	 * @param defaultPrompt one or more sentences that define the bot's
	 * personality (e.g. "You are a helpful assistant"). Counts against your
	 * usage quota.
	 * @param promptsByRoom room-specific prompts
	 * @param completionMaxTokens places a limit on the length of ChatGPT's
	 * completion (response). If this number is too short, then the completion
	 * may end abruptly (e.g. in an unfinished sentence).
	 * @param timeBetweenSpontaneousPosts the amount of time to wait before
	 * posting a message (e.g. "PT12H")
	 * @param numLatestMessagesToIncludeInRequest the number of chat room
	 * messages to include in the ChatGPT request to give the bot context of the
	 * conversation. Each message counts against your usage quota.
	 * @param latestMessageCharacterLimit each chat message that is sent to
	 * ChatGPT will be truncated to this number characters (0 to disable
	 * truncation). Each message counts against your usage quota.
	 */
	public ChatGPT(OpenAIClient openAIClient, MoodCommand moodCommand, String model, String defaultPrompt, Map<Integer, String> promptsByRoom, int completionMaxTokens, String timeBetweenSpontaneousPosts, int numLatestMessagesToIncludeInRequest, int latestMessageCharacterLimit) {
		this.openAIClient = openAIClient;
		this.moodCommand = moodCommand;
		this.model = model;
		this.defaultPrompt = defaultPrompt;
		this.promptsByRoom = promptsByRoom;
		this.completionMaxTokens = completionMaxTokens;
		this.timeBetweenSpontaneousPosts = Duration.parse(timeBetweenSpontaneousPosts);
		this.numLatestMessagesToIncludeInRequest = numLatestMessagesToIncludeInRequest;
		this.latestMessageCharacterLimit = latestMessageCharacterLimit;
	}

	@Override
	public long nextRun() {
		if (spontaneousPostTimesByRoom.isEmpty()) {
			if (firstRun) {
				firstRun = false;
				return 1;
			}

			return timeBetweenSpontaneousPosts.toMillis();
		}

		Instant now = Instant.now();

		//@formatter:off
		long lowest = spontaneousPostTimesByRoom.values().stream()
			.map(runTime -> Duration.between(now, runTime))
			.mapToLong(Duration::toMillis)
		.min().getAsLong();
		//@formatter:on

		return (lowest < 1) ? 1 : lowest;
	}

	@Override
	public void run(IBot bot) throws Exception {
		removeRoomsBotIsNotIn(bot);
		startTimerForNewRooms(bot);
		List<Integer> roomsToPostTo = findRoomsToSpontaneouslyPostTo();

		for (Integer roomId : roomsToPostTo) {
			resetSpontaneousPostTimer(roomId);

			/*
			 * Do not post anything if the room has no messages.
			 */
			List<ChatMessage> messages = bot.getLatestMessages(roomId, numLatestMessagesToIncludeInRequest);
			if (messages.isEmpty()) {
				continue;
			}

			/*
			 * Do not post anything if the latest message was not authored by a
			 * human.
			 */
			ChatMessage latestMessage = messages.get(messages.size() - 1);
			boolean latestMsgPostedBySystemBot = (latestMessage.getUserId() < 1);
			if (latestMsgPostedBySystemBot) {
				continue;
			}
			boolean latestMsgPostedByThisBot = (latestMessage.getUserId() == bot.getUserId());
			if (latestMsgPostedByThisBot) {
				continue;
			}

			String prompt = buildPrompt(roomId);
			prompt += " Nobody is talking to you directly; you are just sharing your thoughts.";

			ChatCompletionRequest request = buildChatCompletionRequest(prompt, messages, bot);
			String response = sendChatCompletionRequest(request);

			PostMessage postMessage = new PostMessage(response).splitStrategy(SplitStrategy.WORD);
			bot.sendMessage(roomId, postMessage);
		}
	}

	@Override
	public String name() {
		return "chatgpt";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder((CatchAllMentionListener)this)
			.summary("Allows the user to have a conversation with ChatGPT.")
			.detail("Mentioning the bot will invoke a response from ChatGPT. The last " + numLatestMessagesToIncludeInRequest + " messages in the chat room are sent to the ChatGPT servers for context. Under certain conditions, the bot will also periodically post a message from ChatGPT on its own.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatMessage message, IBot bot) {
		/*
		 * Ignore this message if another listener already responded to it.
		 */
		if (ignoreNextMessage) {
			ignoreNextMessage = false;
			return doNothing();
		}

		/*
		 * Ignore this message if the bot isn't mentioned.
		 */
		if (!message.getContent().isMentioned(bot.getUsername())) {
			return doNothing();
		}

		try {
			List<ChatMessage> prevMessages = bot.getLatestMessages(message.getRoomId(), numLatestMessagesToIncludeInRequest);

			String prompt = buildPrompt(message.getRoomId());
			ChatCompletionRequest request = buildChatCompletionRequest(prompt, prevMessages, bot);
			addParentMessageToRequest(message, prevMessages, request, bot);

			String response = sendChatCompletionRequest(request);

			resetSpontaneousPostTimer(message.getRoomId());

			//@formatter:off
			return create(
				new PostMessage(new ChatBuilder()
					.reply(message)
					.append(response)
				).splitStrategy(SplitStrategy.WORD)
			);
			//@formatter:on
		} catch (IOException e) {
			logger.log(Level.SEVERE, e, () -> "Problem communicating with ChatGPT.");
			return reply("I don't really feel like talking right now.", message);
		}
	}

	private void addParentMessageToRequest(ChatMessage message, List<ChatMessage> prevMessages, ChatCompletionRequest request, IBot bot) {
		long parentId = message.getParentMessageId();
		if (parentId == 0) {
			return;
		}

		//@formatter:off
		boolean parentMessageAlreadyInPrevMessages = prevMessages.stream()
			.mapToLong(ChatMessage::getMessageId)
		.anyMatch(id -> id == parentId);
		//@formatter:on

		if (parentMessageAlreadyInPrevMessages) {
			return;
		}

		boolean parentMessagePostedByBot = message.getContent().getContent().startsWith("@" + bot.getUsername());

		try {
			String parentMessageContent = bot.getOriginalMessageContent(parentId);

			List<String> imageUrls = extractImageUrlsIfModelSupportsVision(parentMessageContent);

			/*
			 * Insert the parent message right before the child message.
			 */
			int insertPos = request.getMessageCount() - 1;

			if (parentMessagePostedByBot) {
				request.addBotMessage(parentMessageContent, imageUrls, insertPos);
			} else {
				request.addHumanMessage(parentMessageContent, imageUrls, insertPos);
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE, e, () -> "Problem getting content of parent message.");
		}
	}

	@Override
	public void ignoreNextMessage() {
		ignoreNextMessage = true;
	}

	private String buildPrompt(int roomId) {
		String prompt = promptsByRoom.getOrDefault(roomId, defaultPrompt);

		if (moodCommand != null) {
			String mood = moodCommand.getMood(roomId);
			prompt = prompt.replace("$MOOD", mood);
		}

		return prompt;
	}

	private void removeRoomsBotIsNotIn(IBot bot) {
		List<Integer> roomsBotIsIn = bot.getRooms();
		spontaneousPostTimesByRoom.keySet().removeIf(not(roomsBotIsIn::contains));
	}

	private void startTimerForNewRooms(IBot bot) {
		//@formatter:off
		bot.getRooms().stream()
			.filter(not(spontaneousPostTimesByRoom::containsKey))
		.forEach(this::resetSpontaneousPostTimer);
		//@formatter:on
	}

	private List<Integer> findRoomsToSpontaneouslyPostTo() {
		//@formatter:off
		return spontaneousPostTimesByRoom.entrySet().stream()
			.filter(not(entry -> entry.getValue().isAfter(Instant.now())))
			.map(Map.Entry::getKey)
		.collect(Collectors.toList());
		//@formatter:on
	}

	private ChatCompletionRequest buildChatCompletionRequest(String prompt, List<ChatMessage> messages, IBot bot) throws IOException {
		ChatCompletionRequest request = new ChatCompletionRequest(prompt);
		request.setMaxTokensForCompletion(completionMaxTokens);
		request.setModel(model);

		for (ChatMessage message : messages) {
			Content content = message.getContent();

			boolean messageWasDeleted = (content == null);
			if (messageWasDeleted) {
				continue;
			}

			String contentStr = content.getContent();
			boolean fixedWidthFont = content.isFixedWidthFont();
			String contentMd = ChatBuilder.toMarkdown(contentStr, fixedWidthFont);

			List<String> imageUrls = extractImageUrlsIfModelSupportsVision(contentStr);

			String truncatedContentMd;
			if (latestMessageCharacterLimit > 0) {
				truncatedContentMd = SplitStrategy.WORD.split(contentMd, latestMessageCharacterLimit).get(0);
			} else {
				truncatedContentMd = contentMd;
			}

			boolean messagePostedByOak = (message.getUserId() == bot.getUserId());
			if (messagePostedByOak) {
				request.addBotMessage(truncatedContentMd, imageUrls);
			} else {
				request.addHumanMessage(truncatedContentMd, imageUrls);
			}
		}

		return request;
	}

	private List<String> extractImageUrlsIfModelSupportsVision(String content) throws IOException {
		if (!modelSupportsVision()) {
			return List.of();
		}

		List<String> allUrls = extractUrls(content);

		/*
		 * Create an HTTP client with a short request timeout, to avoid long bot
		 * response times.
		 */
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(5 * 1000).build();
		HttpClientBuilder builder = HttpClients.custom().setDefaultRequestConfig(requestConfig);

		try (CloseableHttpClient client = HttpFactory.connect(builder).getClient()) {
			//@formatter:off
			return allUrls.stream()
				.filter(url -> isSupportedImage(client, url))
			.collect(Collectors.toList());
			//@formatter:on
		}
	}

	private boolean isSupportedImage(CloseableHttpClient client, String url) {
		HttpResponse response;
		try {
			HttpHead request = new HttpHead(url);
			response = client.execute(request);
		} catch (IOException e) {
			return false;
		}

		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != 200) {
			return false;
		}

		Header header = response.getFirstHeader("Content-Type");
		if (header == null) {
			return false;
		}

		/*
		 * Example header value: "image/jpeg;charset=UTF-8"
		 */
		HeaderElement[] elements = header.getElements();
		if (elements.length == 0) {
			return false;
		}

		String contentType = elements[0].getName();
		return imageTypesSupportedByVisionModel.contains(contentType);
	}

	private boolean modelSupportsVision() {
		return "gpt-4-vision-preview".equals(model);
	}

	static List<String> extractUrls(String content) {
		List<String> urls = new ArrayList<>(); //do not use Set to preserve insertion order

		/*
		 * Do not include any punctuation at the end of the URL (e.g. "." or
		 * "?")
		 * Source:
		 * https://www.geeksforgeeks.org/extract-urls-present-in-a-given-string/
		 */
		Pattern p = Pattern.compile("\\bhttps?://[-a-z0-9+&@#/%?=~_|!:,.;]*[-a-z0-9+&@#/%=~_|]", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(content);
		while (m.find()) {
			String url = m.group(0);
			if (!urls.contains(url)) {
				urls.add(url);
			}
		}

		return urls;
	}

	private String sendChatCompletionRequest(ChatCompletionRequest request) throws IOException {
		try (CloseableHttpClient client = HttpFactory.connect().getClient()) {
			String response = openAIClient.chatCompletion(request);
			response = removeMentionsFromBeginningOfMessage(response);
			response = removeReplySyntaxFromBeginningOfMessage(response);
			return formatMessagesWithCodeBlocks(response);
		} catch (OpenAIException e) {
			//@formatter:off
			return new ChatBuilder()
				.code()
				.append("ERROR BEEP BOOP: ")
				.append(e.getMessage())
				.code()
			.toString();
			//@formatter:on
		}
	}

	/**
	 * <p>
	 * Remove all mentions from the beginning of the message.
	 * </p>
	 * <p>
	 * ChatGPT will sometimes put mentions at the beginning of the response,
	 * probably because the chat messages that we send to it have mentions in
	 * them. These mentions duplicate and build up over time for some reason, so
	 * the first response might have one mention, the next response might have
	 * two, and so on.
	 * </p>
	 * @param message the message
	 * @return the message without mentions at the beginning
	 */
	static String removeMentionsFromBeginningOfMessage(String message) {
		return removeFromBeginningOfMessage('@', message);
	}

	/**
	 * <p>
	 * Remove all reply syntax from the beginning of the message.
	 * </p>
	 * <p>
	 * ChatGPT may add these if any of the messages that were sent to ChatGPT
	 * were replies with fixed-width formatting (SO Chat ignores reply syntax on
	 * fixed-with messages--it does not convert them to mentions like with other
	 * messages).
	 * </p>
	 * @param message the message
	 * @return the message without reply syntax at the beginning
	 */
	static String removeReplySyntaxFromBeginningOfMessage(String message) {
		return removeFromBeginningOfMessage(':', message);
	}

	private static String removeFromBeginningOfMessage(char startingChar, String message) {
		CharIterator it = new CharIterator(message);
		boolean inString = false;
		while (it.hasNext()) {
			char c = it.next();

			if (c == startingChar) {
				inString = true;
				continue;
			}

			if (inString) {
				if (Character.isWhitespace(c)) {
					inString = false;
				}
				continue;
			}

			break;
		}

		return (inString || !it.hasNext()) ? "" : message.substring(it.index());
	}

	/**
	 * <p>
	 * Converts responses that contain code samples to fixed-width font, and
	 * removes the code block markdown syntax.
	 * </p>
	 * <p>
	 * SO Chat does not support code block markdown syntax. The entire message
	 * is converted to fixed-width font because SO Chat does not support
	 * formatting part of a message in fixed-width, only the entire message.
	 * </p>
	 * @param message the message
	 * @return the reformatted message
	 */
	static String formatMessagesWithCodeBlocks(String message) {
		if (!message.contains("```")) {
			return message;
		}

		//@formatter:off
		return ChatBuilder.FIXED_WIDTH_PREFIX + message
			.replaceAll("(?m)^ *```[^\\n]++\\n", "")
			.replaceAll("(?m)^ *```\\n?", "")
			.replace("\n", "\n" + ChatBuilder.FIXED_WIDTH_PREFIX);
		//@formatter:on
	}

	/**
	 * Resets the chat timer for the given room.
	 * @param roomId the room ID
	 */
	private void resetSpontaneousPostTimer(int roomId) {
		Instant nextRunTime = Instant.now().plus(timeBetweenSpontaneousPosts);
		spontaneousPostTimesByRoom.put(roomId, nextRunTime);
	}
}
