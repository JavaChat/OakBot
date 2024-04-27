package oakbot.ai.openai;

import java.util.Objects;

/**
 * Generates audio from the input text.
 * @see "https://platform.openai.com/docs/api-reference/audio/createSpeech"
 */
public class CreateSpeechRequest {
	private final String model;
	private final String input;
	private final String voice;
	private final String responseFormat;
	private final Double speed;

	private CreateSpeechRequest(CreateSpeechRequest.Builder builder) {
		model = Objects.requireNonNull(builder.model);
		input = Objects.requireNonNull(builder.input);
		voice = Objects.requireNonNull(builder.voice);
		responseFormat = builder.responseFormat;
		speed = builder.speed;
	}

	public String getModel() {
		return model;
	}

	public String getInput() {
		return input;
	}

	public String getVoice() {
		return voice;
	}

	public String getResponseFormat() {
		return responseFormat;
	}

	public Double getSpeed() {
		return speed;
	}

	public static class Builder {
		private String model;
		private String input;
		private String voice;
		private String responseFormat;
		private Double speed;

		/**
		 * <p>
		 * Sets the model to use.
		 * </p>
		 * <p>
		 * Supported values: tts-1, tts-1-hd
		 * </p>
		 * @param model the model
		 * @return this
		 */
		public Builder model(String model) {
			this.model = model;
			return this;
		}

		/**
		 * Sets the text to generate audio for. The maximum length is 4096
		 * characters.
		 * @param input the input text
		 * @return this
		 */
		public Builder input(String input) {
			this.input = input;
			return this;
		}

		/**
		 * <p>
		 * Sets the voice to use when generating the audio.
		 * </p>
		 * <p>
		 * Supported values: alloy, echo, fable, onyx, nova, shimmer
		 * </p>
		 * @see <a href=
		 * "https://platform.openai.com/docs/guides/text-to-speech/voice-options">Previews</a>
		 * @param voice the voice
		 * @return this
		 */
		public Builder voice(String voice) {
			this.voice = voice;
			return this;
		}

		/**
		 * <p>
		 * Sets the format of the generated audio.
		 * </p>
		 * <p>
		 * Default value: mp3
		 * </p>
		 * <p>
		 * Supported values: mp3, opus, aac, flac, wav, pcm
		 * </p>
		 * @param responseFormat the format
		 * @return this
		 */
		public Builder responseFormat(String responseFormat) {
			this.responseFormat = responseFormat;
			return this;
		}

		/**
		 * <p>
		 * Sets the speed of the generated audio.
		 * </p>
		 * <p>
		 * Default value: 1.0
		 * </p>
		 * <p>
		 * Valid values: [ 0.25 .. 4.0 ]
		 * </p>
		 * @param speed the speed
		 * @return this
		 */
		public Builder speed(Double speed) {
			this.speed = speed;
			return this;
		}

		public CreateSpeechRequest build() {
			return new CreateSpeechRequest(this);
		}
	}
}