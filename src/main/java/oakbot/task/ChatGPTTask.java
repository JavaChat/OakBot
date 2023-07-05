package oakbot.task;

import java.util.List;

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
	private final long runFrequency;
	private final String prompt, apiKey;
	private final int maxTokens, pastMessages;
	private final List<Integer> roomIds;

	/**
	 * @param runFrequency how often this task runs (in milliseconds)
	 * @param prompt the prompt to define the bot's personality (e.g. "You are a
	 * helpful assistant")
	 * @param maxTokens defines how long ChatGPT's response will be (one word is
	 * about 0.75 tokens)
	 * @param pastMessages how many of the latest messages from the chat room to
	 * send to ChatGPT
	 * @param apiKey the ChatGPT API key
	 * @param roomIds the rooms to do this with
	 */
	public ChatGPTTask(long runFrequency, String prompt, int maxTokens, int pastMessages, String apiKey, List<Integer> roomIds) {
		this.runFrequency = runFrequency;
		this.prompt = prompt;
		this.maxTokens = maxTokens;
		this.pastMessages = pastMessages;
		this.apiKey = apiKey;
		this.roomIds = roomIds;
	}

	@Override
	public long nextRun() {
		return runFrequency;
	}

	@Override
	public void run(Bot bot) throws Exception {
		for (Integer roomId : roomIds) {
			if (!bot.getRooms().contains(roomId)) {
				continue;
			}

			List<ChatMessage> messages = bot.getLatestMessages(roomId, pastMessages);
			ChatGPTRequest request = new ChatGPTRequest(apiKey, prompt, maxTokens);
			for (ChatMessage message : messages) {
				String content = message.getContent().getContent();
				boolean fixedFont = message.getContent().isFixedFont();
				String contentMd = ChatBuilder.toMarkdown(content, fixedFont);
				String truncatedMessage = SplitStrategy.WORD.split(contentMd, 200).get(0);

				if (message.getUserId() == bot.getUserId()) {
					request.addBotMessage(truncatedMessage);
				} else {
					request.addHumanMessage(truncatedMessage);
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
	 * Creates an HTTP client. This method is for unit testing.
	 * @return the HTTP client
	 */
	CloseableHttpClient createClient() {
		return HttpClients.createDefault();
	}

	//@formatter:off
	/*
	{"model":"gpt-3.5-turbo","messages":[{"role":"system","content":"You are knowledgable, but grumpy, Java software developer."},{"role":"user","content":"When was Java 17 released?"}],"max_tokens":50,"temperature":1.0}
	{
	  "id" : "chatcmpl-7Yi4sFx7sq4hfA3huekbWN04TVZAN",
	  "object" : "chat.completion",
	  "created" : 1688506942,
	  "model" : "gpt-3.5-turbo-0613",
	  "choices" : [ {
	    "index" : 0,
	    "message" : {
	      "role" : "assistant",
	      "content" : "Java 17 was released on September 14, 2021. Finally, they managed to make it work, but who knows for how long..."
	    },
	    "finish_reason" : "stop"
	  } ],
	  "usage" : {
	    "prompt_tokens" : 32,
	    "completion_tokens" : 30,
	    "total_tokens" : 62
	  }
	}
	*/
	//@formatter:on
}
