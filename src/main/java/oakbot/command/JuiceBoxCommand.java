package oakbot.command;

import static oakbot.bot.ChatActions.reply;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.client.utils.URIBuilder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.chat.IRoom;
import oakbot.chat.PingableUser;
import oakbot.chat.UserInfo;
import oakbot.util.ChatBuilder;
import oakbot.util.Http;
import oakbot.util.HttpFactory;

/**
 * Posts a modified version of a user's profile picture, showing them drinking
 * from a juice box.
 * @see https://juiceboxify.me
 * @author Michael Angstadt
 */
public class JuiceBoxCommand implements Command {
	@Override
	public String name() {
		return "juicebox";
	}

	@Override
	public HelpDoc help() {
		return new HelpDoc.Builder(this) //@formatter:off
			.summary("Posts a modified version of a user's profile picture, showing them drinking from a juice box (https://juiceboxify.me).")
			.example("", "Posts the picture of the user who sent the command.")
			.example("Michael", "Finds a user named \"Michael\" in the current chat room and posts their picture.")
		.build(); //@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		String content = chatCommand.getContent();
		final String targetUser = content.isEmpty() ? chatCommand.getMessage().getUsername() : content;

		IRoom currentRoom = bot.getRoom(chatCommand.getMessage().getRoomId());

		PingableUser matchingUser;
		try {
			matchingUser = currentRoom.getPingableUsers().stream() //@formatter:off
				.filter(user -> user.getUsername().equalsIgnoreCase(targetUser))
				.findFirst()
			.orElse(null); //@formatter:on
		} catch (IOException e) {
			return reply(new ChatBuilder("Problem getting the room's list of pingable users: ").code(e.getMessage()), chatCommand);
		}
		if (matchingUser == null) {
			return reply("User not found (they must be in this room).", chatCommand);
		}

		UserInfo matchingUserInfo;
		try {
			List<UserInfo> list = currentRoom.getUserInfo(matchingUser.getUserId());
			matchingUserInfo = list.isEmpty() ? null : list.get(0);
		} catch (IOException e) {
			return reply(new ChatBuilder("Problem getting user info: ").code(e.getMessage()), chatCommand);
		}
		if (matchingUserInfo == null) {
			return reply("Couldn't get the user info for that user.", chatCommand);
		}

		String juicifiedPhotoUrl;
		try {
			juicifiedPhotoUrl = juicifyPhoto(matchingUserInfo.getProfilePicture());
		} catch (IOException e) {
			return reply(new ChatBuilder("Problem juicifying user: ").code(e.getMessage()), chatCommand);
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
		String baseUrl = "https://juiceboxify.me";

		String uri = null;
		try {
			//@formatter:off
			uri = new URIBuilder(baseUrl)
				.addParameter("url", photo)
			.build().toString();
			//@formatter:on
		} catch (URISyntaxException ignore) {
			//base URL is hard-coded
		}

		Document document;
		try (Http http = HttpFactory.connect()) {
			document = http.get(uri).getBodyAsHtml();
		}

		Element element = document.selectFirst("section[class='result'] img");
		return (element == null) ? null : element.absUrl("src");
	}
}
