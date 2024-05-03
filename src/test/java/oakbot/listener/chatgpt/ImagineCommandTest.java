package oakbot.listener.chatgpt;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import oakbot.ai.openai.OpenAIClient;
import oakbot.ai.stabilityai.StabilityAIClient;
import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.util.ChatCommandBuilder;

/**
 * @author Michael Angstadt
 */
public class ImagineCommandTest {
	@Test
	public void no_content() {
		OpenAIClient openAIClient = new OpenAIClient("KEY");
		StabilityAIClient stabilityAIClient = new StabilityAIClient("KEY");
		ImagineCommand command = new ImagineCommand(openAIClient, stabilityAIClient, 1);

		//@formatter:off
		ChatCommand message = new ChatCommandBuilder(command)
			.messageId(1)
			.content("")
		.build();
		//@formatter:on

		IBot bot = mock(IBot.class);

		ChatActions response = command.onMessage(message, bot);
		assertMessage(":1 Imagine what?", response);
	}

	@Test
	public void parseContent() {
		ImagineCommand.ImagineCommandParameters actual = ImagineCommand.parseContent("");
		assertNull(actual);

		actual = ImagineCommand.parseContent("dall-e-3 https://www.example.com/image.png prompt goes here");
		assertEquals("dall-e-3", actual.getModel());
		assertEquals("https://www.example.com/image.png", actual.getInputImage());
		assertEquals("prompt goes here", actual.getPrompt());

		actual = ImagineCommand.parseContent("https://www.example.com/image.png prompt goes here");
		assertNull(actual.getModel());
		assertEquals("https://www.example.com/image.png", actual.getInputImage());
		assertEquals("prompt goes here", actual.getPrompt());

		actual = ImagineCommand.parseContent("dall-e-3 prompt goes here");
		assertEquals("dall-e-3", actual.getModel());
		assertNull(actual.getInputImage());
		assertEquals("prompt goes here", actual.getPrompt());

		actual = ImagineCommand.parseContent("prompt goes here");
		assertNull(actual.getModel());
		assertNull(actual.getInputImage());
		assertEquals("prompt goes here", actual.getPrompt());

		actual = ImagineCommand.parseContent("one two");
		assertNull(actual.getModel());
		assertNull(actual.getInputImage());
		assertEquals("one two", actual.getPrompt());

		actual = ImagineCommand.parseContent("one");
		assertNull(actual.getModel());
		assertNull(actual.getInputImage());
		assertEquals("one", actual.getPrompt());
	}

	@Test
	public void validateParameters_image_variations() {
		String model = ImagineCommand.MODEL_DALLE_2;
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
		String error = ImagineCommand.validateParameters(model, image, prompt);
		assertNotNull(error);
	}

	private static void assertValidationPasses(String model, String image, String prompt) {
		String error = ImagineCommand.validateParameters(model, image, prompt);
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
		for (String model : ImagineCommand.supportedModels) {
			assertEquals(model, ImagineCommand.chooseWhichModelToUse(model, null, "prompt"));
		}
	}
}
