package oakbot.listener;

import static oakbot.bot.ChatActions.doNothing;
import static oakbot.bot.ChatActions.post;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.github.mangstadt.sochat4j.ChatMessage;

import oakbot.bot.ChatActions;
import oakbot.bot.IBot;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

/**
 * Automatically detects metric/imperial units in chat messages and performs
 * unit conversions.
 * @author Michael Angstadt
 */
public class UnitConversionListener implements Listener {
	//@formatter:off
	private final Pattern regex = Pattern.compile(
		"(?i)" +
		"\\b" +
		"(\\d+([\\.,]\\d+)?)\\s*" + //support comma as a decimal separator
		"(°|deg|degree|degrees|&#176;)?\\s*" +
		"(C|celsius|centigrade|F|fahrenheit)" + 
		"\\b"
	);
	//@formatter:on

	private final NumberFormat nf = DecimalFormat.getNumberInstance();
	{
		nf.setMaximumFractionDigits(2);
	}

	@Override
	public String name() {
		return "unitconversion";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Automatically detects metric/imperial units in chat messages and performs unit conversions.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatMessage message, IBot bot) {
		if (message.getContent().isOnebox()) {
			return doNothing();
		}

		var temperatures = findTemperatures(message.getContent().getContent());
		if (temperatures.isEmpty()) {
			return doNothing();
		}

		var cb = new ChatBuilder();

		for (var temperature : temperatures) {
			var line = switch (temperature.unit) {
			case CELSIUS -> {
				var f = convertCtoF(temperature.value());
				yield nf.format(temperature.value()) + "°C = " + nf.format(f) + "°F";
			}
			case FAHRENHEIT -> {
				var c = convertFtoC(temperature.value());
				yield nf.format(temperature.value()) + "°F = " + nf.format(c) + "°C";
			}
			};

			cb.append("🌡 ").append(line).nl();
		}

		return post(cb);
	}

	private double convertCtoF(double value) {
		return value * 9 / 5 + 32;
	}

	private double convertFtoC(double value) {
		return (value - 32) * 5 / 9;
	}

	private List<Temperature> findTemperatures(String content) {
		var temperatures = new ArrayList<Temperature>();

		var m = regex.matcher(content);
		while (m.find()) {
			var valueStr = m.group(1).replace(',', '.'); //support comma as a decimal separator
			var value = Double.parseDouble(valueStr);

			var unitStr = m.group(4).toLowerCase();
			var unit = unitStr.startsWith("c") ? TemperatureUnit.CELSIUS : TemperatureUnit.FAHRENHEIT;

			var temperature = new Temperature(unit, value);

			//do not spam the chat with duplicate numbers
			//do not use a set, insertion order required
			if (!temperatures.contains(temperature)) {
				temperatures.add(temperature);
			}
		}

		return temperatures;
	}

	private record Temperature(TemperatureUnit unit, double value) {
	}

	private enum TemperatureUnit {
		FAHRENHEIT, CELSIUS
	}
}
