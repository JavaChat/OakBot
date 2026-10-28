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
		var core = new ImagineCore(openAIClient, stabilityAIClient, null, 1);
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
		var actual = new ImagineCore.ImagineParameterParser("gpt-image-1 https://www.example.com/image.png prompt goes here").parse();
		assertEquals(ImagineCore.Model.GPT_IMAGE_1, actual.model());
		assertEquals("https://www.example.com/image.png", actual.inputImage());
		assertEquals("prompt goes here", actual.prompt());

		actual = new ImagineCore.ImagineParameterParser("https://www.example.com/image.png prompt goes here").parse();
		assertNull(actual.model());
		assertEquals("https://www.example.com/image.png", actual.inputImage());
		assertEquals("prompt goes here", actual.prompt());

		actual = new ImagineCore.ImagineParameterParser("gpt-image-1 prompt goes here").parse();
		assertEquals(ImagineCore.Model.GPT_IMAGE_1, actual.model());
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
		var model = ImagineCore.Model.STABLE_IMAGE_CORE;
		assertValidationError(model, null, null);
		assertValidationPasses(model, null, "prompt");
		assertValidationError(model, "input image", null);
		assertValidationError(model, "input image", "prompt");

		model = ImagineCore.Model.STABLE_DIFFUSION;
		assertValidationError(model, null, null);
		assertValidationPasses(model, null, "prompt");
		assertValidationError(model, "input image", null);
		assertValidationPasses(model, "input image", "prompt");

		model = ImagineCore.Model.STABLE_DIFFUSION_TURBO;
		assertValidationError(model, null, null);
		assertValidationPasses(model, null, "prompt");
		assertValidationError(model, "input image", null);
		assertValidationPasses(model, "input image", "prompt");
	}

	private static void assertValidationError(ImagineCore.Model model, String image, String prompt) {
		var error = ImagineCore.validateParameters(model, image, prompt);
		assertNotNull(error);
	}

	private static void assertValidationPasses(ImagineCore.Model model, String image, String prompt) {
		var error = ImagineCore.validateParameters(model, image, prompt);
		assertNull(error);
	}

	@Test
	void chooseWhichModelToUse() {
		//no model provided
		assertEquals(ImagineCore.Model.GPT_IMAGE_1_MINI, ImagineCore.chooseWhichModelToUse(null, null, "prompt"));

		//no model provided with input images
		assertEquals(ImagineCore.Model.STABLE_DIFFUSION, ImagineCore.chooseWhichModelToUse(null, "image", "prompt"));

		//if a model was provided, use it
		for (var model : ImagineCore.Model.values()) {
			assertEquals(model, ImagineCore.chooseWhichModelToUse(model, null, "prompt"));
		}
	}
}
