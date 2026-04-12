package oakbot.listener;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Locale;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.mangstadt.sochat4j.ChatMessage;

import oakbot.bot.IBot;

/**
 * @author Michael Angstadt
 */
class UnitConversionListenerTest {
	private static Locale defaultLocale;

	@BeforeAll
	static void beforeAll() {
		/*
		 * Ensure NumberFormat uses periods for decimal separators and commas
		 * for thousands separators.
		 */
		defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.US);
	}

	@AfterAll
	static void afterAll() {
		Locale.setDefault(defaultLocale);
	}

	@Test
	void onMessage_temperature() {
		var expected40f = "🌡 40°F = 4.44°C = 277.59K = 499.67°Ra = 9.83°Rø = 1.47°N = 143.33°D = 3.56°Ré = cold";
		assertResponse("it's 40f right now", expected40f);
		assertResponse("it's 40 f right now", expected40f);
		assertResponse("it's 40°F right now", expected40f);
		assertResponse("it's 40&#176;F right now", expected40f);
		assertResponse("it's 40 degrees f right now", expected40f);
		assertResponse("it's 40 deg f right now", expected40f);
		assertResponse("it's 40 fahrenheit right now", expected40f);
		
		var expectedMinus40f = "🌡 -40°F = -40°C = 233.15K = 419.67°Ra = -13.5°Rø = -13.2°N = 210°D = -32°Ré = cold af";
		assertResponse("it's -40f right now", expectedMinus40f);

		var expected40c = "🌡 40°C = 104°F = 313.15K = 563.67°Ra = 28.5°Rø = 13.2°N = 90°D = 32°Ré = hot af";
		assertResponse("it's 40c right now", expected40c);
		assertResponse("it's 40 c right now", expected40c);
		assertResponse("it's 40°C right now", expected40c);
		assertResponse("it's 40&#176;C right now", expected40c);
		assertResponse("it's 40 degrees c right now", expected40c);
		assertResponse("it's 40 deg c right now", expected40c);
		assertResponse("it's 40 celsius right now", expected40c);
		assertResponse("it's 40 centigrade right now", expected40c);

		var expected5800K = "🌡 5,800K = 5,526.85°C = 9,980.33°F = 10,440°Ra = 2,909.1°Rø = 1,823.86°N = -8,140.28°D = 4,421.48°Ré = hot af";  
		assertResponse("the photosphere of the sun is 5800 kelvin", expected5800K);
	}

	@Test
	void onMessage_temperature_zero() {
		assertResponse("0f", "🌡 0°F = -17.78°C = 255.37K = 459.67°Ra = -1.83°Rø = -5.87°N = 176.67°D = -14.22°Ré = cold af");
		assertResponse("0c", "🌡 0°C = 32°F = 273.15K = 491.67°Ra = 7.5°Rø = 0°N = 150°D = 0°Ré = cold");
		assertResponse("0 kelvin", "🌡 0K = -273.15°C = -459.67°F = 0°Ra = -135.9°Rø = -90.14°N = 559.72°D = -218.52°Ré = cold af");
	}

	@Test
	void onMessage_length() {
		assertResponse("i ran 2 miles", "📏 2 miles = 3.22 km");
		assertResponse("i ran 2 mile", "📏 2 miles = 3.22 km");

		assertResponse("i ran 2 km today", "📏 2 km = 1.24 miles");
		assertResponse("i ran 2 kilometers today", "📏 2 km = 1.24 miles");
		assertResponse("i ran 2 kilometres today", "📏 2 km = 1.24 miles");
		assertResponse("i ran 2 kilometer today", "📏 2 km = 1.24 miles");

		assertResponse("it's 4 ft long", "📏 4 ft = 1.22 m");
		assertResponse("it's 4 feet long", "📏 4 ft = 1.22 m");
		assertResponse("it's 4 foot long", "📏 4 ft = 1.22 m");

		assertResponse("it's 4 m long", "📏 4 m = 13.12 ft");
		assertResponse("it's 4 meters long", "📏 4 m = 13.12 ft");
		assertResponse("it's 4 metres long", "📏 4 m = 13.12 ft");

		assertResponse("i'm 5'11\" tall", "📏 5'11\" = 1.8 m");
		assertResponse("i'm 5&#39;11&quot; tall", "📏 5'11\" = 1.8 m");
		assertResponse("i'm 5' 11\" tall", "📏 5'11\" = 1.8 m");
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
		var expected = "🌡 40.6°F = 4.78°C = 277.93K = 500.27°Ra = 10.01°Rø = 1.58°N = 142.83°D = 3.82°Ré = cold";
		assertResponse("it's 40.6f right now", expected);
		assertResponse("it's 40,6f right now", expected);
	}

	@Test
	void onMessage_remove_duplicates() {
		assertResponse("it's 40f right now. yesterday it was 34f and the day before it was 40f", "🌡 40°F = 4.44°C = 277.59K = 499.67°Ra = 9.83°Rø = 1.47°N = 143.33°D = 3.56°Ré = cold\n🌡 34°F = 1.11°C = 274.26K = 493.67°Ra = 8.08°Rø = 0.37°N = 148.33°D = 0.89°Ré = cold");
	}

	@Test
	void onMessage_preserve_order() {
		assertResponse("i ran 3 miles on a 70f day", "📏 3 miles = 4.83 km\n🌡 70°F = 21.11°C = 294.26K = 529.67°Ra = 18.58°Rø = 6.97°N = 118.33°D = 16.89°Ré = comfy");
		assertResponse("on a 70f day, i ran 3 miles", "🌡 70°F = 21.11°C = 294.26K = 529.67°Ra = 18.58°Rø = 6.97°N = 118.33°D = 16.89°Ré = comfy\n📏 3 miles = 4.83 km");
	}

	@Test
	void onMessage_range() {
		assertResponse("we got 6-8 inches of snow", "📏 6 in = 15.24 cm\n📏 8 in = 20.32 cm");
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
