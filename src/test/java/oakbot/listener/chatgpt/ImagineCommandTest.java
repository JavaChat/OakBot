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
class ImagineCommandTest {
	@Test
	void no_content() {
		var openAIClient = new OpenAIClient("KEY");
		var stabilityAIClient = new StabilityAIClient("KEY");
		var core = new ImagineCore(openAIClient, stabilityAIClient, 1);
		var command = new ImagineCommand(core);

		//@formatter:off
		var message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("")
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var response = command.onMessage(message, bot);
		assertMessage("Imagine what?", 1, response);
	}

	@Test
	void ImagineParameterParser() {
		var actual = new ImagineCore.ImagineParameterParser("dall-e-3 https://www.example.com/image.png prompt goes here").parse();
		assertEquals("dall-e-3", actual.model());
		assertEquals("https://www.example.com/image.png", actual.inputImage());
		assertEquals("prompt goes here", actual.prompt());

		actual = new ImagineCore.ImagineParameterParser("https://www.example.com/image.png prompt goes here").parse();
		assertNull(actual.model());
		assertEquals("https://www.example.com/image.png", actual.inputImage());
		assertEquals("prompt goes here", actual.prompt());

		actual = new ImagineCore.ImagineParameterParser("dall-e-3 prompt goes here").parse();
		assertEquals("dall-e-3", actual.model());
		assertNull(actual.inputImage());
		assertEquals("prompt goes here", actual.prompt());

		actual = new ImagineCore.ImagineParameterParser("prompt goes here").parse();
		assertNull(actual.model());
		assertNull(actual.inputImage());
		assertEquals("prompt goes here", actual.prompt());

		actual = new ImagineCore.ImagineParameterParser("one two").parse();
		assertNull(actual.model());
		assertNull(actual.inputImage());
		assertEquals("one two", actual.prompt());

		actual = new ImagineCore.ImagineParameterParser("one").parse();
		assertNull(actual.model());
		assertNull(actual.inputImage());
		assertEquals("one", actual.prompt());
	}

	@Test
	void validateParameters_image_variations() {
		var model = ImagineCore.MODEL_DALLE_2;
		assertValidationError(model, null, null);
		assertValidationPasses(model, null, "prompt");
		assertValidationPasses(model, "image", null);
		assertValidationError(model, "image", "prompt");

		model = ImagineCore.MODEL_DALLE_3;
		assertValidationError(model, null, null);
		assertValidationPasses(model, null, "prompt");
		assertValidationError(model, "image", null);
		assertValidationError(model, "image", "prompt");

		model = ImagineCore.MODEL_STABLE_IMAGE_CORE;
		assertValidationError(model, null, null);
		assertValidationPasses(model, null, "prompt");
		assertValidationError(model, "image", null);
		assertValidationError(model, "image", "prompt");

		model = ImagineCore.MODEL_STABLE_DIFFUSION;
		assertValidationError(model, null, null);
		assertValidationPasses(model, null, "prompt");
		assertValidationError(model, "image", null);
		assertValidationPasses(model, "image", "prompt");

		model = ImagineCore.MODEL_STABLE_DIFFUSION_TURBO;
		assertValidationError(model, null, null);
		assertValidationPasses(model, null, "prompt");
		assertValidationError(model, "image", null);
		assertValidationPasses(model, "image", "prompt");
	}

	private static void assertValidationError(String model, String image, String prompt) {
		var error = ImagineCore.validateParameters(model, image, prompt);
		assertNotNull(error);
	}

	private static void assertValidationPasses(String model, String image, String prompt) {
		var error = ImagineCore.validateParameters(model, image, prompt);
		assertNull(error);
	}

	@Test
	void chooseWhichModelToUse() {
		//no model provided
		assertEquals(ImagineCore.MODEL_DALLE_3, ImagineCore.chooseWhichModelToUse(null, null, "prompt"));

		//no model provided with input images
		assertEquals(ImagineCore.MODEL_DALLE_2, ImagineCore.chooseWhichModelToUse(null, "image", null));
		assertEquals(ImagineCore.MODEL_STABLE_DIFFUSION, ImagineCore.chooseWhichModelToUse(null, "image", "prompt"));

		//if a model was provided, use it
		for (var model : ImagineCore.supportedModels) {
			assertEquals(model, ImagineCore.chooseWhichModelToUse(model, null, "prompt"));
		}
	}
}
