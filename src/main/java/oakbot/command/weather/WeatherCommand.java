package oakbot.command.weather;

import static oakbot.bot.ChatActions.error;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;

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
		var location = chatCommand.getContent();
		if (location.isEmpty()) {
			return reply("Specify a location.", chatCommand);
		}

		WttrInResponse<String> responseUs;
		WttrInResponse<String> responseMetric;

		try {
			responseUs = sendRequest(location, Unit.US, true);
			responseMetric = sendRequest(location, Unit.METRIC, false);
		} catch (IOException | WttrInException e) {
			return error("Error querying wttr.in: ", e, chatCommand);
		}

		//@formatter:off
		var cb = new ChatBuilder()
			.append(responseUs.content().trim()) //trim to remove trailing newline
			.append("  |  ")
			.append(responseMetric.content().trim())
			.append(" (").link("source", responseUs.requestUri().toString()).append(")");
		//@formatter:on

		return reply(cb, chatCommand);
	}

	private WttrInResponse<String> sendRequest(String location, Unit unit, boolean showLocation) throws IOException, WttrInException {
		var request = new SingleLineRequest();
		request.setLocation(location);
		request.setUnit(unit);
		request.setShowLocation(showLocation);
		request.setShowWind(true);

		return client.fetch(request);
	}
}
