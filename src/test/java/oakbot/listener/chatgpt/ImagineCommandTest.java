package oakbot.listener.chatgpt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * @author Michael Angstadt
 */
public class ImagineCommandTest {
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
	}
}
