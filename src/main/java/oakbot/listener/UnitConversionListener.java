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
import java.util.stream.Stream;

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

		var content = message.content().getContent();
		var conversions = new ConversionsParser(content).parse();
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

	private class ConversionsParser {
		private final NumberFormat nf = NumberFormat.getNumberInstance();
		private final String content;

		public ConversionsParser(String content) {
			this.content = replaceRanges(content);
			nf.setMaximumFractionDigits(2);
		}

		private String replaceRanges(String content) {
			return content.replaceAll("(\\d+)\\s*-\\s*(\\d+)(\\s*\\w+)", "$1$3 to $2$3");
		}

		public List<Conversion> parse() {
			//@formatter:off
			return Stream.of(Unit.values())
				.map(this::searchForConversions)
				.flatMap(List::stream)
			.toList();
			//@formatter:on
		}

		private List<Conversion> searchForConversions(Unit unit) {
			var conversions = new ArrayList<Conversion>();

			if (unit == Unit.FEET_AND_INCHES) {
				var processedValues = new HashSet<Integer>();
				var m = Pattern.compile("(\\d+)'\\s*(\\d+)\"").matcher(content);
				while (m.find()) {
					var feet = Integer.parseInt(m.group(1));
					var inches = Integer.parseInt(m.group(2));
					var totalInches = feet * 12 + inches;

					if (processedValues.contains(totalInches)) {
						continue;
					}

					var meters = totalInches * 0.0254;
					var line = unit.emoticon + " " + feet + "'" + inches + "\" = " + outputValue(Unit.METERS, meters);
					var conversion = new Conversion(m.start(), line);

					conversions.add(conversion);
					processedValues.add(totalInches);
				}

				return conversions;
			}

			if (unit.regex == null) {
				return conversions;
			}

			var processedValues = new HashSet<Double>();
			var m = unit.regex.matcher(content);
			while (m.find()) {
				var origValue = unit.parse(m);

				if (origValue == 0 && unit.ignoreZeroValues()) {
					continue;
				}

				if (processedValues.contains(origValue)) {
					continue;
				}

				var line = buildLine(unit, origValue);
				var conversion = new Conversion(m.start(), line);
				conversions.add(conversion);
				processedValues.add(origValue);
			}

			return conversions;
		}

		private String buildLine(Unit unit, double origValue) {
			var convertedValues = unit.convert(origValue);

			//@formatter:off
			return unit.emoticon + " " + outputValue(unit, origValue) + " = " +
			convertedValues.stream()
				.map(this::outputValue)
			.collect(Collectors.joining(" = "));
			//@formatter:on
		}

		private String outputValue(Unit unit, double value) {
			if (unit == Unit.HUMAN) {
				return fahrenheitToHuman(value);
			}

			return nf.format(value) + unit.suffix;
		}

		private String outputValue(UnitValue value) {
			return outputValue(value.unit, value.value);
		}

		private String fahrenheitToHuman(double f) {
			if (f < 20) {
				return "cold af";
			}
			if (f < 50) {
				return "cold";
			}
			if (f < 60) {
				return "chilly";
			}
			if (f < 70) {
				return "tepid";
			}
			if (f < 75) {
				return "comfy";
			}
			if (f < 80) {
				return "warm";
			}
			if (f < 100) {
				return "hot";
			}
			return "hot af";
		}
	}

	private enum Unit {
		//@formatter:off
		CELCIUS("(°|deg|degrees?|&#176;)?\\s*(C|celsius|centigrade)", "°C", "🌡") {
			@Override
			Collection<UnitValue> convert(double value) {
				var f = new UnitValue(value * 9 / 5 + 32, Unit.FAHRENHEIT);
			
				return List.of(
					f,
					new UnitValue(value + 273.15, Unit.KELVIN),
					new UnitValue((value + 273.15) * 9 / 5, Unit.RANKINE),
					new UnitValue(f.value(), Unit.HUMAN)
				);
			}

			@Override
			boolean ignoreZeroValues() {
				return false;
			}
		},

		FAHRENHEIT("(°|deg|degrees?|&#176;)?\\s*(F|fahrenheit)", "°F", "🌡") {
			@Override
			Collection<UnitValue> convert(double value) {
				return List.of(
					new UnitValue((value - 32) * 5 / 9, Unit.CELCIUS),
					new UnitValue((value + 459.67) * 5 / 9, Unit.KELVIN),
					new UnitValue(value + 459.67, Unit.RANKINE),
					new UnitValue(value, Unit.HUMAN)
				);
			}

			@Override
			boolean ignoreZeroValues() {
				return false;
			}
		},

		KELVIN("(°|deg|degrees?|&#176;)?\\s*(kelvin)", "K", "🌡") {
			@Override
			Collection<UnitValue> convert(double value) {
				var f = new UnitValue((value - 273.15) * 9 / 5 + 32, Unit.FAHRENHEIT);
				
				return List.of(
					new UnitValue(value - 273.15, Unit.CELCIUS),
					f,
					new UnitValue(value * 9 / 5, Unit.RANKINE),
					new UnitValue(f.value(), Unit.HUMAN)
				);
			}

			@Override
			boolean ignoreZeroValues() {
				return false;
			}
		},
		
		RANKINE("(°|deg|degrees?|&#176;)?\\s*(rankine)", "°R", "🌡") {
			@Override
			Collection<UnitValue> convert(double value) {
				var f = new UnitValue(value - 459.67, Unit.FAHRENHEIT);
				
				return List.of(
					new UnitValue((value - 491.67) * 5 / 9, Unit.CELCIUS),
					f,
					new UnitValue(value * 5 / 9, Unit.KELVIN),
					new UnitValue(f.value(), Unit.HUMAN)
				);
			}

			@Override
			boolean ignoreZeroValues() {
				return false;
			}
		},
		
		HUMAN("", "🌡") {
			@Override
			Collection<UnitValue> convert(double value) {
				throw new UnsupportedOperationException();
			}

			@Override
			boolean ignoreZeroValues() {
				return false;
			}
		},

		KILOMETERS("(km|kilometers?|kilometres?)", " km", "📏") {
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

		METERS("(m|meters?|metres?)", " m", "📏") {
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

		FEET_AND_INCHES("", "📏") {
			@Override
			Collection<UnitValue> convert(double value) {
				throw new UnsupportedOperationException();
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

		TONS("(t|tons?)", " US short tons", "⚖️") {
			@Override
			Collection<UnitValue> convert(double value) {
				return List.of(
					new UnitValue(value * 2000, Unit.POUNDS),
					new UnitValue(value * 907.185, Unit.KILOGRAMS),
					new UnitValue(value * 2000 / 14, Unit.STONE)
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

		private Unit(String suffix, String emoticon) {
			this(null, suffix, emoticon);
		}

		private Unit(String suffixRegex, String suffix, String emoticon) {
			//@formatter:off
			this.regex = (suffixRegex == null) ? null : Pattern.compile(
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

		boolean ignoreZeroValues() {
			return true;
		}

		abstract Collection<UnitValue> convert(double d);
	}

	private record Conversion(int index, String line) {
	}

	private record UnitValue(double value, Unit unit) {
	}
}
