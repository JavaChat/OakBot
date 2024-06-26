package oakbot.command;

import static oakbot.bot.ChatActionsUtils.assertMessage;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.mangstadt.sochat4j.IRoom;
import com.github.mangstadt.sochat4j.PingableUser;
import com.github.mangstadt.sochat4j.UserInfo;

import oakbot.bot.IBot;
import oakbot.util.ChatCommandBuilder;
import oakbot.util.Gobble;
import oakbot.util.HttpFactory;
import oakbot.util.MockHttpClientBuilder;

/**
 * @author Michael Angstadt
 */
class JuiceBoxCommandTest {
	@AfterEach
	void after() {
		HttpFactory.restore();
	}

	@Test
	void juicify_self() throws Exception {
		var face = new Gobble(getClass(), "juiceboxify-face.html").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://juiceboxify.me?url=https%3A%2F%2Fwww.gravatar.com%2Favatar%2F29d7c49f6f174710788c79011219bae1")
			.responseOk(face)
		.build());
		//@formatter:on

		var command = new JuiceBoxCommand();

		var room = mock(IRoom.class);
		when(room.getPingableUsers()).thenReturn(Arrays
				.asList( //@formatter:off
			new PingableUser(1, 100, "Michael", LocalDateTime.now()),
			new PingableUser(1, 200, "OakBot", LocalDateTime.now())
		));
		when(room.getUserInfo(List.of(100))).thenReturn(List.of(
			new UserInfo.Builder().profilePicture("https://www.gravatar.com/avatar/29d7c49f6f174710788c79011219bae1").build()
		));
		when(room.getUserInfo(List.of(200))).thenReturn(List.of(
			new UserInfo.Builder().profilePicture("https://i.stack.imgur.com/SmeIn.jpg").build()
		)); //@formatter:on

		var bot = mock(IBot.class);
		when(bot.getRoom(anyInt())).thenReturn(room);

		var message = new ChatCommandBuilder(command).username("Michael").build();
		var response = command.onMessage(message, bot);

		assertMessage(":0 https://juiceboxify.me/images/8176425e07bbe2caf82c90e82ac07dc445013e68.jpg", response);
	}

	@Test
	void juicify_someone_else() throws Exception {
		var face = new Gobble(getClass(), "juiceboxify-face.html").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://juiceboxify.me?url=https%3A%2F%2Fi.stack.imgur.com%2FSmeIn.jpg")
			.responseOk(face)
		.build());
		//@formatter:on

		var command = new JuiceBoxCommand();

		var room = mock(IRoom.class);
		when(room.getPingableUsers()).thenReturn(Arrays
				.asList( //@formatter:off
			new PingableUser(1, 100, "Michael", LocalDateTime.now()),
			new PingableUser(1, 200, "OakBot", LocalDateTime.now())
		));
		when(room.getUserInfo(List.of(100))).thenReturn(List.of(
			new UserInfo.Builder().profilePicture("https://www.gravatar.com/avatar/29d7c49f6f174710788c79011219bae1").build()
		));
		when(room.getUserInfo(List.of(200))).thenReturn(List.of(
			new UserInfo.Builder().profilePicture("https://i.stack.imgur.com/SmeIn.jpg").build()
		)); //@formatter:on

		var bot = mock(IBot.class);
		when(bot.getRoom(anyInt())).thenReturn(room);

		var message = new ChatCommandBuilder(command).username("Michael").content("oakbot").build();
		var response = command.onMessage(message, bot);

		assertMessage(":0 https://juiceboxify.me/images/8176425e07bbe2caf82c90e82ac07dc445013e68.jpg", response);
	}

	@Test
	void no_face() throws Exception {
		var face = new Gobble(getClass(), "juiceboxify-no-face.html").asString();

		//@formatter:off
		HttpFactory.inject(new MockHttpClientBuilder()
			.requestGet("https://juiceboxify.me?url=https%3A%2F%2Fi.stack.imgur.com%2FSmeIn.jpg")
			.responseOk(face)
		.build());
		//@formatter:on

		var command = new JuiceBoxCommand();

		var room = mock(IRoom.class);
		when(room.getPingableUsers()).thenReturn(Arrays
				.asList( //@formatter:off
			new PingableUser(1, 100, "Michael", LocalDateTime.now()),
			new PingableUser(1, 200, "OakBot", LocalDateTime.now())
		));
		when(room.getUserInfo(List.of(100))).thenReturn(List.of(
			new UserInfo.Builder().profilePicture("https://www.gravatar.com/avatar/29d7c49f6f174710788c79011219bae1").build()
		));
		when(room.getUserInfo(List.of(200))).thenReturn(List.of(
			new UserInfo.Builder().profilePicture("https://i.stack.imgur.com/SmeIn.jpg").build()
		)); //@formatter:on

		var bot = mock(IBot.class);
		when(bot.getRoom(anyInt())).thenReturn(room);

		var message = new ChatCommandBuilder(command).username("Michael").content("oakbot").build();
		var response = command.onMessage(message, bot);

		assertMessage(":0 User has no face.", response);
	}

	@Test
	void user_not_in_room() throws Exception {
		var command = new JuiceBoxCommand();

		var room = mock(IRoom.class);
		when(room.getPingableUsers()).thenReturn(Arrays
				.asList( //@formatter:off
			new PingableUser(1, 100, "Michael", LocalDateTime.now()),
			new PingableUser(1, 200, "OakBot", LocalDateTime.now())
		)); //@formatter:on

		var bot = mock(IBot.class);
		when(bot.getRoom(anyInt())).thenReturn(room);

		var message = new ChatCommandBuilder(command).username("Michael").content("JonSkeet").build();
		var response = command.onMessage(message, bot);

		assertMessage(":0 User not found (they must be in this room).", response);
	}
}
