package oakbot.listener;

import static oakbot.bot.ChatActions.doNothing;
import static oakbot.bot.ChatActions.post;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
	private final NumberFormat nf = NumberFormat.getNumberInstance();
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
		var units = Arrays.stream(Unit.values())
			.map(Unit::name)
			.map(String::toLowerCase)
		.collect(Collectors.joining(", "));

		return new HelpDoc.Builder(this)
			.summary("Checks every message posted to the chat for units of measurement and then converts those units to other units. Supports the following units: " + units)
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatMessage message, IBot bot) {
		if (message.content().isOnebox()) {
			return doNothing();
		}

		var conversions = new ArrayList<Conversion>();
		var content = message.content().getContent();
		for (var unit : Unit.values()) {
			var processedValues = new HashSet<Double>();

			var m = unit.regex.matcher(content);
			while (m.find()) {
				var origValue = unit.parse(m);
				if (processedValues.contains(origValue)) {
					continue;
				}

				var convertedValues = unit.convert(origValue);
				var line = new StringBuilder();
				line.append(unit.emoticon).append(" ");
				line.append(nf.format(origValue)).append(unit.suffix);
				for (var convertedValue : convertedValues) {
					line.append(" = ").append(nf.format(convertedValue.value)).append(convertedValue.unit.suffix);
				}
				var conversion = new Conversion(m.start(), line.toString());
				conversions.add(conversion);
				processedValues.add(origValue);
			}
		}

		if (conversions.isEmpty()) {
			return doNothing();
		}

		var cb = new ChatBuilder();

		//@formatter:off
		conversions.stream()
			.sorted(Comparator.comparingInt(Conversion::index)) //sort by where the value appeared in the message
			.map(Conversion::line)
		.forEach(line -> cb.append(line).nl());
		//@formatter:on

		return post(cb);
	}

	private enum Unit {
		//@formatter:off
		CELCIUS("(°|deg|degrees?|&#176;)?\\s*(C|celsius|centigrade)", "°C", "🌡") {
			@Override
			Collection<UnitValue> convert(double value) {
				return List.of(
					new UnitValue(value * 9 / 5 + 32, Unit.FAHRENHEIT),
					new UnitValue(value + 273.15, Unit.KELVIN)
				);
			}
		},

		FAHRENHEIT("(°|deg|degrees?|&#176;)?\\s*(F|fahrenheit)", "°F", "🌡") {
			@Override
			Collection<UnitValue> convert(double value) {
				return List.of(
					new UnitValue((value - 32) * 5 / 9, Unit.CELCIUS),
					new UnitValue((value + 459.67) * 5 / 9, Unit.KELVIN)
				);
			}
		},

		KELVIN("(°|deg|degrees?|&#176;)?\\s*(kelvin)", "°K", "🌡") {
			@Override
			Collection<UnitValue> convert(double value) {
				return List.of(
					new UnitValue(value - 273.15, Unit.CELCIUS),
					new UnitValue((value - 273.15) * 9 / 5 + 32, Unit.FAHRENHEIT)
				);
			}
		},

		KILOMETERS("(km|kilometers?)", " km", "📏") {
			@Override
			Collection<UnitValue> convert(double value) {
				return List.of(
					new UnitValue(value * 0.621371, Unit.MILES)
				);
			}
		},

		MILES("(miles?)", " miles", "📏") {
			@Override
			Collection<UnitValue> convert(double value) {
				return List.of(
					new UnitValue(value * 1.60934, Unit.KILOMETERS)
				);
			}
		},

		METERS("(m|meters?)", " m", "📏") {
			@Override
			Collection<UnitValue> convert(double value) {
				return List.of(
					new UnitValue(value * 3.28, Unit.FEET)
				);
			}
		},

		FEET("(ft|feet|foot)", " ft", "📏") {
			@Override
			Collection<UnitValue> convert(double value) {
				return List.of(
					new UnitValue(value / 3.28, Unit.METERS)
				);
			}
		},

		CENTIMETERS("(cm|centimeters?)", " cm", "📏") {
			@Override
			Collection<UnitValue> convert(double value) {
				return List.of(
					new UnitValue(value / 2.54, Unit.INCHES)
				);
			}
		},

		INCHES("(in|inch(es)?)", " in", "📏") {
			@Override
			Collection<UnitValue> convert(double value) {
				return List.of(
					new UnitValue(value * 2.54, Unit.CENTIMETERS)
				);
			}
		},

		KILOGRAMS("(kg|kilos?|kilograms?)", " kg", "⚖️") {
			@Override
			Collection<UnitValue> convert(double value) {
				return List.of(
					new UnitValue(value * 2.2, Unit.POUNDS),
					new UnitValue(value / 6.35029, Unit.STONE)
				);
			}
		},

		POUNDS("(lbs?|pounds?)", " lbs", "⚖️") {
			@Override
			Collection<UnitValue> convert(double value) {
				return List.of(
					new UnitValue(value / 2.2, Unit.KILOGRAMS),
					new UnitValue(value / 14, Unit.STONE)
				);
			}
		},

		STONE("(stone)", " st", "⚖️") {
			@Override
			Collection<UnitValue> convert(double value) {
				return List.of(
					new UnitValue(value * 6.35029, Unit.KILOGRAMS),
					new UnitValue(value * 14, Unit.POUNDS)
				);
			}
		};
		//@formatter:on

		private final Pattern regex;
		private final String suffix;
		private final String emoticon;

		private Unit(String suffixRegex, String suffix, String emoticon) {
			//@formatter:off
			this.regex = Pattern.compile(
				"(?i)" +
				"(-|\\b)" +
				"(\\d+([\\.,]\\d+)?)\\s*" + //treat comma as a decimal separator
				suffixRegex + 
				"\\b");
			//@formatter:on
			this.suffix = suffix;
			this.emoticon = emoticon;
		}

		public double parse(Matcher m) {
			var s = m.group(2).replace(',', '.'); //treat comma as a decimal separator
			var value = Double.parseDouble(s);

			if ("-".equals(m.group(1))) {
				value *= -1;
			}

			return value;
		}

		abstract Collection<UnitValue> convert(double d);
	}

	private record Conversion(int index, String line) {
	}

	private record UnitValue(double value, Unit unit) {
	}
}
