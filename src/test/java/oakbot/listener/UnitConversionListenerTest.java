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
	void onMessage_temperature() {
		assertResponse("it's 40f right now", "🌡 40°F = 4.44°C = 277.59°K");
		assertResponse("it's -40f right now", "🌡 -40°F = -40°C = 233.15°K");
		assertResponse("it's 40 f right now", "🌡 40°F = 4.44°C = 277.59°K");
		assertResponse("it's 40°F right now", "🌡 40°F = 4.44°C = 277.59°K");
		assertResponse("it's 40&#176;F right now", "🌡 40°F = 4.44°C = 277.59°K");
		assertResponse("it's 40 degrees f right now", "🌡 40°F = 4.44°C = 277.59°K");
		assertResponse("it's 40 deg f right now", "🌡 40°F = 4.44°C = 277.59°K");
		assertResponse("it's 40 fahrenheit right now", "🌡 40°F = 4.44°C = 277.59°K");

		assertResponse("it's 40c right now", "🌡 40°C = 104°F = 313.15°K");
		assertResponse("it's 40 c right now", "🌡 40°C = 104°F = 313.15°K");
		assertResponse("it's 40°C right now", "🌡 40°C = 104°F = 313.15°K");
		assertResponse("it's 40&#176;C right now", "🌡 40°C = 104°F = 313.15°K");
		assertResponse("it's 40 degrees c right now", "🌡 40°C = 104°F = 313.15°K");
		assertResponse("it's 40 deg c right now", "🌡 40°C = 104°F = 313.15°K");
		assertResponse("it's 40 celsius right now", "🌡 40°C = 104°F = 313.15°K");
		assertResponse("it's 40 centigrade right now", "🌡 40°C = 104°F = 313.15°K");

		assertResponse("the photosphere of the sun is 5800 kelvin", "🌡 5,800°K = 5,526.85°C = 9,980.33°F");
	}

	@Test
	void onMessage_temperature_zero() {
		assertResponse("0f", "🌡 0°F = -17.78°C = 255.37°K");
		assertResponse("0c", "🌡 0°C = 32°F = 273.15°K");
		assertResponse("0 kelvin", "🌡 0°K = -273.15°C = -459.67°F");
	}

	@Test
	void onMessage_length() {
		assertResponse("i ran 2 miles", "📏 2 miles = 3.22 km");
		assertResponse("i ran 2 mile", "📏 2 miles = 3.22 km");

		assertResponse("i ran 2 km today", "📏 2 km = 1.24 miles");
		assertResponse("i ran 2 kilometers today", "📏 2 km = 1.24 miles");
		assertResponse("i ran 2 kilometer today", "📏 2 km = 1.24 miles");

		assertResponse("it's 4 feet long", "📏 4 ft = 1.22 m");
		assertResponse("it's 4 foot long", "📏 4 ft = 1.22 m");
	}

	@Test
	void onMessage_length_zero() {
		assertNoResponse("0 miles");
		assertNoResponse("0 km");
		assertNoResponse("0 ft");
		assertNoResponse("0 m");
		assertNoResponse("0 in");
		assertNoResponse("0 cm");
	}

	@Test
	void onMessage_weight() {
		assertResponse("i weigh 160 lbs", "⚖️ 160 lbs = 72.73 kg = 11.43 st");
		assertResponse("i weigh 72 kg", "⚖️ 72 kg = 158.4 lbs = 11.34 st");
		assertResponse("i weigh 11 stone", "⚖️ 11 st = 69.85 kg = 154 lbs");
	}

	@Test
	void onMessage_weight_zero() {
		assertNoResponse("0 lbs");
		assertNoResponse("0 kg");
		assertNoResponse("0 stone");
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
		assertResponse("it's 40.6f right now", "🌡 40.6°F = 4.78°C = 277.93°K");
		assertResponse("it's 40,6f right now", "🌡 40.6°F = 4.78°C = 277.93°K");
	}

	@Test
	void onMessage_remove_duplicates() {
		assertResponse("it's 40f right now. yesterday it was 34f and the day before it was 40f", "🌡 40°F = 4.44°C = 277.59°K\n🌡 34°F = 1.11°C = 274.26°K");
	}

	@Test
	void onMessage_preserve_order() {
		assertResponse("i ran 3 miles on a 70f day", "📏 3 miles = 4.83 km\n🌡 70°F = 21.11°C = 294.26°K");
		assertResponse("on a 70f day, i ran 3 miles", "🌡 70°F = 21.11°C = 294.26°K\n📏 3 miles = 4.83 km");
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
