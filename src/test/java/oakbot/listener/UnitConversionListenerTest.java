package oakbot.listener;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import com.github.mangstadt.sochat4j.ChatMessage;

import oakbot.bot.IBot;

/**
 * @author Michael Angstadt
 */
class UnitConversionListenerTest {
	@Test
	void onMessage() {
		assertResponse("it's 40f right now", "🌡 40°F = 4.44°C");
		assertResponse("it's 40 f right now", "🌡 40°F = 4.44°C");
		assertResponse("it's 40°F right now", "🌡 40°F = 4.44°C");
		assertResponse("it's 40&#176;F right now", "🌡 40°F = 4.44°C");
		assertResponse("it's 40 degrees f right now", "🌡 40°F = 4.44°C");
		assertResponse("it's 40 deg f right now", "🌡 40°F = 4.44°C");
		assertResponse("it's 40 fahrenheit right now", "🌡 40°F = 4.44°C");

		assertResponse("it's 40c right now", "🌡 40°C = 104°F");
		assertResponse("it's 40 c right now", "🌡 40°C = 104°F");
		assertResponse("it's 40°C right now", "🌡 40°C = 104°F");
		assertResponse("it's 40&#176;C right now", "🌡 40°C = 104°F");
		assertResponse("it's 40 degrees c right now", "🌡 40°C = 104°F");
		assertResponse("it's 40 deg c right now", "🌡 40°C = 104°F");
		assertResponse("it's 40 celsius right now", "🌡 40°C = 104°F");
		assertResponse("it's 40 centigrade right now", "🌡 40°C = 104°F");
	}

	@Test
	void onMessage_empty() {
		assertNoResponse("it's hot af right now");
		assertNoResponse("it's40f right now");
		assertNoResponse("40 fun");
		assertNoResponse("40 cats");
	}

	@Test
	void onMessage_decimal_separator() {
		assertResponse("it's 40.6f right now", "🌡 40.6°F = 4.78°C");
		assertResponse("it's 40,6f right now", "🌡 40.6°F = 4.78°C");
	}

	@Test
	void onMessage_remove_duplicates() {
		assertResponse("it's 40f right now. yesterday it was 34f and the day before it was 40f", "🌡 40°F = 4.44°C\n🌡 34°F = 1.11°C");
	}

	private static void assertNoResponse(String message) {
		assertResponse(message, null);
	}

	private static void assertResponse(String message, String response) {
		//@formatter:off
		var chatMessage = new ChatMessage.Builder()
			.roomId(1)
			.content(message)
		.build();
		//@formatter:on

		var bot = mock(IBot.class);

		var listener = new UnitConversionListener();

		var chatResponse = listener.onMessage(chatMessage, bot);
		if (response == null) {
			assertTrue(chatResponse.isEmpty());
		} else {
			assertMessage(response + "\n", chatResponse);
		}
	}
}
