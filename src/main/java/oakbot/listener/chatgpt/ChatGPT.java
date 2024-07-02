package oakbot.listener.chatgpt;

import static java.util.function.Predicate.not;
import static oakbot.bot.ChatActions.create;
import static oakbot.bot.ChatActions.doNothing;
import static oakbot.bot.ChatActions.reply;
import static oakbot.util.StringUtils.plural;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.UncheckedIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mangstadt.sochat4j.ChatMessage;
import com.github.mangstadt.sochat4j.SplitStrategy;

import oakbot.ai.openai.ChatCompletionRequest;
import oakbot.ai.openai.ChatCompletionResponse;
import oakbot.ai.openai.OpenAIClient;
import oakbot.ai.openai.OpenAIException;
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
	private static final Logger logger = LoggerFactory.getLogger(ChatGPT.class);
	private static final Collection<String> imageTypesSupportedByVisionModel = Set.of("image/png", "image/jpeg", "image/gif", "image/webp");
	private static final Collection<String> visionModels = Set.of("gpt-4-vision-preview", "gpt-4o");

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
	private final UsageQuota usageQuota;

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
	 * @param requestsPer24Hours requests allowed per user per 24 hours, or
	 * {@literal <= 0} for no limit
	 */
	public ChatGPT(OpenAIClient openAIClient, MoodCommand moodCommand, String model, String defaultPrompt, Map<Integer, String> promptsByRoom, int completionMaxTokens, String timeBetweenSpontaneousPosts, int numLatestMessagesToIncludeInRequest, int latestMessageCharacterLimit, int requestsPer24Hours) {
		this.openAIClient = openAIClient;
		this.moodCommand = moodCommand;
		this.model = model;
		this.defaultPrompt = defaultPrompt;
		this.promptsByRoom = promptsByRoom;
		this.completionMaxTokens = completionMaxTokens;
		this.timeBetweenSpontaneousPosts = Duration.parse(timeBetweenSpontaneousPosts);
		this.numLatestMessagesToIncludeInRequest = numLatestMessagesToIncludeInRequest;
		this.latestMessageCharacterLimit = latestMessageCharacterLimit;
		usageQuota = (requestsPer24Hours > 0) ? new UsageQuota(Duration.ofDays(1), requestsPer24Hours) : UsageQuota.allowAll();
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

		var now = Instant.now();

		//@formatter:off
		var lowest = spontaneousPostTimesByRoom.values().stream()
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
		var roomsToPostTo = findRoomsToSpontaneouslyPostTo();

		for (var roomId : roomsToPostTo) {
			resetSpontaneousPostTimer(roomId);

			/*
			 * Do not post anything if the room has no messages.
			 */
			var messages = bot.getLatestMessages(roomId, numLatestMessagesToIncludeInRequest);
			if (messages.isEmpty()) {
				continue;
			}

			/*
			 * Do not post anything if the latest message was not authored by a
			 * human.
			 */
			var latestMessage = messages.get(messages.size() - 1);
			var latestMsgPostedBySystemBot = (latestMessage.getUserId() < 1);
			if (latestMsgPostedBySystemBot) {
				continue;
			}
			var latestMsgPostedByThisBot = (latestMessage.getUserId() == bot.getUserId());
			if (latestMsgPostedByThisBot) {
				continue;
			}

			var prompt = buildPrompt(roomId);
			prompt += " Nobody is talking to you directly; you are just sharing your thoughts.";

			var apiMessages = buildChatCompletionMessages(prompt, messages, bot);

			var request = buildChatCompletionRequest(apiMessages);

			var response = sendChatCompletionRequest(request);

			var postMessage = new PostMessage(response).splitStrategy(SplitStrategy.WORD);
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

		/**
		 * Has the user exceeded quota?
		 */
		var userId = message.getUserId();
		var timeUntilNextRequest = usageQuota.getTimeUntilUserCanMakeRequest(userId);
		if (!timeUntilNextRequest.isZero()) {
			var hours = timeUntilNextRequest.toHours() + 1;
			return reply("Bad human! You are over quota. Try again in " + hours + " " + plural("hour", hours) + ".", message);
		}

		try {
			var prevMessages = bot.getLatestMessages(message.getRoomId(), numLatestMessagesToIncludeInRequest);

			var prompt = buildPrompt(message.getRoomId());
			var apiMessages = buildChatCompletionMessages(prompt, prevMessages, bot);
			addParentMessage(message, prevMessages, apiMessages, bot);

			var request = buildChatCompletionRequest(apiMessages);

			var response = sendChatCompletionRequest(request);

			resetSpontaneousPostTimer(message.getRoomId());

			if (!bot.isAdminUser(userId)) {
				usageQuota.logRequest(userId);
			}

			//@formatter:off
			return create(
				new PostMessage(new ChatBuilder()
					.reply(message)
					.append(response)
				).splitStrategy(SplitStrategy.WORD)
			);
			//@formatter:on
		} catch (IOException e) {
			logger.atError().setCause(e).log(() -> "Problem communicating with ChatGPT.");
			return reply("I don't really feel like talking right now.", message);
		}
	}

	private ChatCompletionRequest buildChatCompletionRequest(List<ChatCompletionRequest.Message> apiMessages) {
		//@formatter:off
		return new ChatCompletionRequest.Builder()
			.messages(apiMessages)
			.model(model)
			.maxTokens(completionMaxTokens)
		.build();
		//@formatter:on
	}

	private void addParentMessage(ChatMessage message, List<ChatMessage> prevMessages, List<ChatCompletionRequest.Message> apiMessages, IBot bot) {
		var parentId = message.getParentMessageId();
		if (parentId == 0) {
			return;
		}

		//@formatter:off
		var parentMessageAlreadyInPrevMessages = prevMessages.stream()
			.mapToLong(ChatMessage::getMessageId)
		.anyMatch(id -> id == parentId);
		//@formatter:on

		if (parentMessageAlreadyInPrevMessages) {
			return;
		}

		var parentMessagePostedByBot = message.getContent().getContent().startsWith("@" + bot.getUsername());

		try {
			var parentMessageContent = bot.getOriginalMessageContent(parentId);

			var imageUrls = parentMessagePostedByBot ? List.<String> of() : extractImageUrlsIfModelSupportsVision(parentMessageContent);

			/*
			 * Insert the parent message right before the child message.
			 */
			var insertPos = apiMessages.size() - 1;

			//@formatter:off
			apiMessages.add(insertPos, new ChatCompletionRequest.Message.Builder()
				.role(parentMessagePostedByBot ? "assistant" : "user")
				.text(parentMessageContent)
				.imageUrls(imageUrls, "low")
			.build());
			//@formatter:on
		} catch (IOException e) {
			logger.atError().setCause(e).log(() -> "Problem getting content of parent message.");
		}
	}

	@Override
	public void ignoreNextMessage() {
		ignoreNextMessage = true;
	}

	private String buildPrompt(int roomId) {
		var prompt = promptsByRoom.getOrDefault(roomId, defaultPrompt);

		if (moodCommand != null) {
			var mood = moodCommand.getMood(roomId);
			prompt = prompt.replace("$MOOD", mood);
		}

		return prompt;
	}

	private void removeRoomsBotIsNotIn(IBot bot) {
		var roomsBotIsIn = bot.getRooms();
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
		.toList();
		//@formatter:on
	}

	private List<ChatCompletionRequest.Message> buildChatCompletionMessages(String prompt, List<ChatMessage> chatMessages, IBot bot) {
		var apiMessages = new ArrayList<ChatCompletionRequest.Message>();

		//@formatter:off
		apiMessages.add(new ChatCompletionRequest.Message.Builder()
			.role("system")
			.text(prompt)
		.build());

		chatMessages.stream()
			.filter(chatMessage -> chatMessage.getContent() != null) //skip deleted messages
			.map(chatMessage -> {
				var content = chatMessage.getContent();
				var contentStr = content.getContent();
				var fixedWidthFont = content.isFixedWidthFont();
				var contentMd = ChatBuilder.toMarkdown(contentStr, fixedWidthFont);
				var messagePostedByOak = (chatMessage.getUserId() == bot.getUserId());

				/*
				 * GPT-4o model does not allow "assistant" messages to contain image URLs.
				 */
				var imageUrls = messagePostedByOak ? List.<String>of() : extractImageUrlsIfModelSupportsVision(contentStr);

				String truncatedContentMd;
				if (latestMessageCharacterLimit > 0) {
					truncatedContentMd = SplitStrategy.WORD.split(contentMd, latestMessageCharacterLimit).get(0);
				} else {
					truncatedContentMd = contentMd;
				}

				return new ChatCompletionRequest.Message.Builder()
					.role(messagePostedByOak ? "assistant" : "user")
					.name(chatMessage.getUsername())
					.text(truncatedContentMd)
					.imageUrls(imageUrls, "low")
				.build();
			})
		.forEach(apiMessages::add);
		//@formatter:on

		return apiMessages;
	}

	private List<String> extractImageUrlsIfModelSupportsVision(String content) {
		if (!visionModels.contains(model)) {
			return List.of();
		}

		var allUrls = extractUrls(content);
		if (allUrls.isEmpty()) {
			return List.of();
		}

		/*
		 * Create an HTTP client with a short request timeout, to avoid long bot
		 * response times.
		 */
		var requestConfig = RequestConfig.custom().setConnectTimeout(5 * 1000).build();
		var builder = HttpClients.custom().setDefaultRequestConfig(requestConfig);

		try (var client = HttpFactory.connect(builder).getClient()) {
			//@formatter:off
			return allUrls.stream()
				.filter(url -> isSupportedImage(client, url))
			.toList();
			//@formatter:on
		} catch (IOException e) {
			//only thrown on HTTP client close
			throw new UncheckedIOException(e);
		}
	}

	private boolean isSupportedImage(CloseableHttpClient client, String url) {
		HttpResponse response;
		try {
			var request = new HttpHead(url);
			response = client.execute(request);
		} catch (IOException e) {
			return false;
		}

		var statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != 200) {
			return false;
		}

		var header = response.getFirstHeader("Content-Type");
		if (header == null) {
			return false;
		}

		/*
		 * Example header value: "image/jpeg;charset=UTF-8"
		 */
		var elements = header.getElements();
		if (elements.length == 0) {
			return false;
		}

		var contentType = elements[0].getName();
		return imageTypesSupportedByVisionModel.contains(contentType);
	}

	static List<String> extractUrls(String content) {
		var urls = new ArrayList<String>(); //do not use Set to preserve insertion order

		/*
		 * Do not include any punctuation at the end of the URL (e.g. "." or
		 * "?")
		 * Source:
		 * https://www.geeksforgeeks.org/extract-urls-present-in-a-given-string/
		 */
		var p = Pattern.compile("\\bhttps?://[-a-z0-9+&@#/%?=~_|!:,.;]*[-a-z0-9+&@#/%=~_|]", Pattern.CASE_INSENSITIVE);
		var m = p.matcher(content);
		while (m.find()) {
			var url = m.group(0);
			if (!urls.contains(url)) {
				urls.add(url);
			}
		}

		return urls;
	}

	private String sendChatCompletionRequest(ChatCompletionRequest apiRequest) throws IOException {
		ChatCompletionResponse apiResponse;
		try {
			apiResponse = openAIClient.chatCompletion(apiRequest);
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

		if (apiResponse.getChoices().isEmpty()) {
			//@formatter:off
			return new ChatBuilder()
				.code("ERROR BEEP BOOP: Choices array in response is empty.")
			.toString();
			//@formatter:on
		}

		var response = apiResponse.getChoices().get(0).getContent();
		response = removeMentionsFromBeginningOfMessage(response);
		response = removeReplySyntaxFromBeginningOfMessage(response);
		return formatMessagesWithCodeBlocks(response);
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
		var it = new CharIterator(message);
		var inString = false;
		while (it.hasNext()) {
			var c = it.next();

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
		var nextRunTime = Instant.now().plus(timeBetweenSpontaneousPosts);
		spontaneousPostTimesByRoom.put(roomId, nextRunTime);
	}
}
