package oakbot.command.weather;

import static oakbot.bot.ChatActions.error;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.util.stream.Collectors;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.command.Command;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;

/**
 * Displays weather information.
 * @author Michael Angstadt
 */
public class WeatherCommand implements Command {
	private final WttrInClient client = new WttrInClient();

	@Override
	public String name() {
		return "weather";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Displays weather information using wttr.in.")
			.example("paris", "Displays current weather in a given city.")
			.example("paris c", "Use metric.")
			.example("paris f", "Use fahrenheit.")
			.example("eiffel tower", "Landmarks supported.")
			.example("muc", "Three-letter airport codes supported.")
			.example("94107", "Area codes supported.")
			.example("@stackoverflow.com", "Domain names supported.")
			.example("-78.46,106.79", "GPS coordinates supported.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var content = chatCommand.getContentAsArgs();
		if (content.isEmpty()) {
			return reply("Specify a location.", chatCommand);
		}

		var last = content.get(content.size() - 1);
		var unit = switch (last) {
		case "c", "C" -> Unit.METRIC;
		case "f", "F" -> Unit.US;
		default -> null;
		};

		var location = (unit == null) ? chatCommand.getContent() : content.stream().limit(content.size() - 1L).collect(Collectors.joining(" "));

		var request = new SingleLineRequest();
		request.setLocation(location);
		request.setShowLocation(true);
		request.setShowWind(true);
		request.setUnit(unit);

		String response;
		try {
			response = client.fetch(request);
		} catch (IOException e) {
			return error("Error querying wttr.in.", e, chatCommand);
		}

		//remove trailing newline
		response = response.trim();

		var cb = new ChatBuilder().append(response).append(" (").link("source", "https://wttr.in").append(")");
		return reply(cb, chatCommand);
	}
}
