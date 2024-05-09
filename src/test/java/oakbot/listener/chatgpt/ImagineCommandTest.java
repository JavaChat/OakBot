package oakbot.listener.chatgpt;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import oakbot.ai.openai.OpenAIClient;
import oakbot.ai.stabilityai.StabilityAIClient;
import oakbot.bot.IBot;
import oakbot.util.ChatCommandBuilder;

/**
 * @author Michael Angstadt
 */
public class ImagineCommandTest {
	@Test
	public void no_content() {
		var openAIClient = new OpenAIClient("KEY");
		var stabilityAIClient = new StabilityAIClient("KEY");
		var command = new ImagineCommand(openAIClient, stabilityAIClient, 1);

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
		assertMessage(":1 Imagine what?", response);
	}

	@Test
	public void parseContent() {
		var actual = ImagineCommand.parseContent("");
		assertNull(actual);

		actual = ImagineCommand.parseContent("dall-e-3 https://www.example.com/image.png prompt goes here");
		assertEquals("dall-e-3", actual.model());
		assertEquals("https://www.example.com/image.png", actual.inputImage());
		assertEquals("prompt goes here", actual.prompt());

		actual = ImagineCommand.parseContent("https://www.example.com/image.png prompt goes here");
		assertNull(actual.model());
		assertEquals("https://www.example.com/image.png", actual.inputImage());
		assertEquals("prompt goes here", actual.prompt());

		actual = ImagineCommand.parseContent("dall-e-3 prompt goes here");
		assertEquals("dall-e-3", actual.model());
		assertNull(actual.inputImage());
		assertEquals("prompt goes here", actual.prompt());

		actual = ImagineCommand.parseContent("prompt goes here");
		assertNull(actual.model());
		assertNull(actual.inputImage());
		assertEquals("prompt goes here", actual.prompt());

		actual = ImagineCommand.parseContent("one two");
		assertNull(actual.model());
		assertNull(actual.inputImage());
		assertEquals("one two", actual.prompt());

		actual = ImagineCommand.parseContent("one");
		assertNull(actual.model());
		assertNull(actual.inputImage());
		assertEquals("one", actual.prompt());
	}

	@Test
	public void validateParameters_image_variations() {
		var model = ImagineCommand.MODEL_DALLE_2;
		assertValidationError(model, null, null);
		assertValidationPasses(model, null, "prompt");
		assertValidationPasses(model, "image", null);
		assertValidationError(model, "image", "prompt");

		model = ImagineCommand.MODEL_DALLE_3;
		assertValidationError(model, null, null);
		assertValidationPasses(model, null, "prompt");
		assertValidationError(model, "image", null);
		assertValidationError(model, "image", "prompt");

		model = ImagineCommand.MODEL_STABLE_IMAGE_CORE;
		assertValidationError(model, null, null);
		assertValidationPasses(model, null, "prompt");
		assertValidationError(model, "image", null);
		assertValidationError(model, "image", "prompt");

		model = ImagineCommand.MODEL_STABLE_DIFFUSION;
		assertValidationError(model, null, null);
		assertValidationPasses(model, null, "prompt");
		assertValidationError(model, "image", null);
		assertValidationPasses(model, "image", "prompt");

		model = ImagineCommand.MODEL_STABLE_DIFFUSION_TURBO;
		assertValidationError(model, null, null);
		assertValidationPasses(model, null, "prompt");
		assertValidationError(model, "image", null);
		assertValidationPasses(model, "image", "prompt");
	}

	private static void assertValidationError(String model, String image, String prompt) {
		var error = ImagineCommand.validateParameters(model, image, prompt);
		assertNotNull(error);
	}

	private static void assertValidationPasses(String model, String image, String prompt) {
		var error = ImagineCommand.validateParameters(model, image, prompt);
		assertNull(error);
	}

	@Test
	public void chooseWhichModelToUse() {
		//no model provided
		assertEquals(ImagineCommand.MODEL_DALLE_3, ImagineCommand.chooseWhichModelToUse(null, null, "prompt"));

		//no model provided with input images
		assertEquals(ImagineCommand.MODEL_DALLE_2, ImagineCommand.chooseWhichModelToUse(null, "image", null));
		assertEquals(ImagineCommand.MODEL_STABLE_DIFFUSION, ImagineCommand.chooseWhichModelToUse(null, "image", "prompt"));

		//if a model was provided, use it
		for (var model : ImagineCommand.supportedModels) {
			assertEquals(model, ImagineCommand.chooseWhichModelToUse(model, null, "prompt"));
		}
	}
}
