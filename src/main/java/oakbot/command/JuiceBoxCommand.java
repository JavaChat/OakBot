package oakbot.command;

import static oakbot.bot.ChatActions.error;
import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.util.List;

import org.apache.http.client.utils.URIBuilder;
import org.jsoup.nodes.Document;

import com.github.mangstadt.sochat4j.PingableUser;
import com.github.mangstadt.sochat4j.UserInfo;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.util.HttpFactory;

/**
 * Posts a modified version of a user's profile picture, showing them drinking
 * from a juice box.
 * @see "https://juiceboxify.me"
 * @author Michael Angstadt
 */
public class JuiceBoxCommand implements Command {
	@Override
	public String name() {
		return "juicebox";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Posts a modified version of a user's profile picture showing them drinking from a juice box.")
			.detail("Images from juiceboxify.me.")
			.example("", "Juiceboxifies the picture of the user who sent the command.")
			.example("Michael", "Finds a user named \"Michael\" in the current chat room and juiceboxifies their picture.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var content = chatCommand.getContent();
		final var targetUser = content.isEmpty() ? chatCommand.getMessage().username() : content;

		var currentRoom = bot.getRoom(chatCommand.getMessage().roomId());

		PingableUser matchingUser;
		try {
			//@formatter:off
			matchingUser = currentRoom.getPingableUsers().stream()
				.filter(user -> user.username().equalsIgnoreCase(targetUser))
				.findFirst()
			.orElse(null);
			//@formatter:on
		} catch (IOException e) {
			return error("Problem getting the room's list of pingable users: ", e, chatCommand);
		}
		if (matchingUser == null) {
			return reply("User not found (they must be in this room).", chatCommand);
		}

		UserInfo matchingUserInfo;
		try {
			var list = currentRoom.getUserInfo(List.of(matchingUser.userId()));
			matchingUserInfo = list.isEmpty() ? null : list.get(0);
		} catch (IOException e) {
			return error("Problem getting user info: ", e, chatCommand);
		}
		if (matchingUserInfo == null) {
			return reply("Couldn't get the user info for that user.", chatCommand);
		}

		String juicifiedPhotoUrl;
		try {
			juicifiedPhotoUrl = juicifyPhoto(matchingUserInfo.profilePicture());
		} catch (IOException e) {
			return error("Problem juicifying user: ", e, chatCommand);
		}
		if (juicifiedPhotoUrl == null) {
			return reply("User has no face.", chatCommand);
		}

		return reply(juicifiedPhotoUrl, chatCommand);
	}

	/**
	 * Adds a juice box to the given photo.
	 * @param photo the URL to the photo
	 * @return the URL to the edited photo or null if it could not be juicified
	 * @throws IOException if there's a network problem
	 */
	private String juicifyPhoto(String photo) throws IOException {
		var url = url(photo);

		Document document;
		try (var http = HttpFactory.connect()) {
			document = http.get(url).getBodyAsHtml();
		}

		var element = document.selectFirst("section[class='result'] img");
		return (element == null) ? null : element.absUrl("src");
	}

	private String url(String photo) {
		//@formatter:off
		return new URIBuilder()
			.setScheme("https")
			.setHost("juiceboxify.me")
			.setParameter("url", photo)
		.toString();
		//@formatter:on
	}
}
