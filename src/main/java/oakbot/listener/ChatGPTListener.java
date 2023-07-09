package oakbot.listener;

import static oakbot.bot.ChatActions.doNothing;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import oakbot.bot.ChatActions;
import oakbot.bot.IBot;
import oakbot.chat.ChatMessage;
import oakbot.chat.SplitStrategy;
import oakbot.command.HelpDoc;
import oakbot.task.ChatGPTParameters;
import oakbot.task.ChatGPTRequest;
import oakbot.task.ChatGPTTask;
import oakbot.util.ChatBuilder;

/**
 * Responds when a user mentions the bot's name.
 * @author Michael Angstadt
 */
public class ChatGPTListener implements CatchAllMentionListener {
	private static final Logger logger = Logger.getLogger(ChatGPTListener.class.getName());

	private final ChatGPTTask task;
	private final ChatGPTParameters chatGPTParameters;
	private final int latestMessagesCount, latestMessagesCharacterLimit;
	private boolean ignoreNextMessage;

	/**
	 * @param task the task that posts a ChatGPT message every so-often. The
	 * timer on this task is reset every time someone mentions the bot and the
	 * message is handled by this listener. Can be null.
	 * @param chatGPTParameters parameters for connecting to ChatGPT
	 * @param latestMessagesCount the number of chat room messages to send to
	 * ChatGPT (each message counts against the usage quota)
	 * @param latestMessagesCharacterLimit each chat message that is sent to
	 * ChatGPT will not exceed this number of characters (includes markdown
	 * syntax). 0 to disable truncation (each message counts against the usage
	 * quota).
	 */
	public ChatGPTListener(ChatGPTTask task, ChatGPTParameters chatGPTParameters, int latestMessagesCount, int latestMessagesCharacterLimit) {
		this.task = task;
		this.chatGPTParameters = chatGPTParameters;
		this.latestMessagesCount = latestMessagesCount;
		this.latestMessagesCharacterLimit = latestMessagesCharacterLimit;
	}

	@Override
	public String name() {
		return "chatgpt";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Allows the user to have a conversation with ChatGPT.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatMessage message, IBot bot) {
		if (ignoreNextMessage) {
			ignoreNextMessage = false;
			return doNothing();
		}

		if (!message.getContent().isMentioned(bot.getUsername())) {
			return doNothing();
		}

		try {
			List<ChatMessage> prevMessages = bot.getLatestMessages(message.getRoomId(), latestMessagesCount);
			ChatGPTRequest request = new ChatGPTRequest(chatGPTParameters);
			for (ChatMessage prevMessage : prevMessages) {
				String content = prevMessage.getContent().getContent();
				boolean fixedFont = prevMessage.getContent().isFixedFont();
				String contentMd = ChatBuilder.toMarkdown(content, fixedFont);

				String truncatedContentMd;
				if (latestMessagesCharacterLimit > 0) {
					truncatedContentMd = SplitStrategy.WORD.split(contentMd, latestMessagesCharacterLimit).get(0);
				} else {
					truncatedContentMd = contentMd;
				}

				if (prevMessage.getUserId() == bot.getUserId()) {
					request.addBotMessage(truncatedContentMd);
				} else {
					request.addHumanMessage(truncatedContentMd);
				}
			}

			String completion;
			try (CloseableHttpClient client = createClient()) {
				completion = request.send(client);
			}

			if (task != null) {
				task.resetTimer(message.getRoomId());
			}

			return reply(completion, message);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Problem communicating with ChatGPT.", e);
			return reply("I don't really feel like talking right now.", message);
		}
	}

	@Override
	public void ignoreNextMessage() {
		ignoreNextMessage = true;
	}

	/**
	 * Creates an HTTP client. This method is for unit testing.
	 * @return the HTTP client
	 */
	CloseableHttpClient createClient() {
		return HttpClients.createDefault();
	}
}
