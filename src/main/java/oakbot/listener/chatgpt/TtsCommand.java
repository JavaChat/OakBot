package oakbot.listener.chatgpt;

import static oakbot.bot.ChatActions.error;
import static oakbot.bot.ChatActions.reply;
import static oakbot.util.StringUtils.plural;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import oakbot.ai.openai.CreateSpeechRequest;
import oakbot.ai.openai.OpenAIClient;
import oakbot.ai.openai.OpenAIException;
import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

/**
 * Generates text-to-speech audio using OpenAI.
 * @author Michael Angstadt
 * @see "https://platform.openai.com/docs/api-reference/audio/createSpeech"
 */
public class TtsCommand implements Command {
	private static final String defaultVoice = "alloy";
	private static final List<String> voices = List.of(defaultVoice, "echo", "fable", "onyx", "nova", "shimmer");

	private final OpenAIClient openAIClient;
	private final int requestsPer24Hours;
	private final UsageQuota usageQuota;

	/**
	 * @param openAIClient the OpenAI client
	 * @param requestsPer24Hours requests allowed per user per 24 hours, or
	 * {@literal <= 0} for no limit
	 */
	public TtsCommand(OpenAIClient openAIClient, int requestsPer24Hours) {
		this.openAIClient = openAIClient;
		this.requestsPer24Hours = requestsPer24Hours;
		usageQuota = (requestsPer24Hours > 0) ? new UsageQuota(Duration.ofDays(1), requestsPer24Hours) : UsageQuota.allowAll();
	}

	@Override
	public String name() {
		return "tts";
	}

	@Override
	public HelpDoc help() {
		var requestLimit = (requestsPer24Hours > 0) ? "Users can make " + requestsPer24Hours + " requests per day. " : "";

		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Generates text to speech using OpenAI.")
			.detail(requestLimit + "Voices: " + voices)
			.example("Four score and seven years ago.", "Generates audio using the \"" + defaultVoice + "\" voice.")
			.example("onyx Four score and seven years ago.", "Generates audio using the \"onyx\" voice.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var parameters = parseContent(chatCommand.getContent());
		var input = parameters.input();
		if (input.isEmpty()) {
			return reply("What should I say?", chatCommand);
		}

		var voice = (parameters.voice() == null) ? defaultVoice : parameters.voice();

		/*
		 * Check usage quota.
		 */
		var userId = chatCommand.getMessage().userId();
		var timeUntilNextRequest = usageQuota.getTimeUntilUserCanMakeRequest(userId);
		if (!timeUntilNextRequest.isZero()) {
			var hours = timeUntilNextRequest.toHours() + 1;
			return reply("Bad human! You are over quota and can't make any more requests right now. Try again in " + hours + " " + plural("hour", hours) + ".", chatCommand);
		}

		//@formatter:off
		var request = new CreateSpeechRequest.Builder()
			.model("tts-1")
			.voice(voice)
			.input(input)
		.build();
		//@formatter:on

		String url;
		try {
			var data = openAIClient.createSpeech(request);
			url = upload(data);
		} catch (OpenAIException e) {
			return reply(new ChatBuilder().code().append("ERROR BEEP BOOP: ").append(e.getMessage()).code(), chatCommand);
		} catch (IOException e) {
			return error("Network error: ", e, chatCommand);
		}

		return reply(url, chatCommand);
	}

	private String upload(byte[] data) throws IOException {
		//TODO upload the audio data somewhere
		return "Not implemented";
	}

	private TtsCommandParameters parseContent(String content) {
		content = content.trim();

		var split = content.split("\\s+", 2);
		var token1 = split[0];
		var rest = (split.length > 1) ? split[1] : "";

		if (voices.contains(token1.toLowerCase())) {
			return new TtsCommandParameters(token1.toLowerCase(), rest);
		}
		return new TtsCommandParameters(null, content);
	}

	private record TtsCommandParameters(String voice, String input) {
	}
}
