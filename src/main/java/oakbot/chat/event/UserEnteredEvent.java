package oakbot.chat.event;

/**
 * Represents an event that is triggered when a user enters a room.
 * @author Michael Angstadt
 */
public class UserEnteredEvent extends Event {
	private final int userId;
	private final String username;
	private final int roomId;
	private final String roomName;

	private UserEnteredEvent(Builder builder) {
		super(builder);
		userId = builder.userId;
		username = builder.username;
		roomId = builder.roomId;
		roomName = builder.roomName;
	}

	/**
	 * Gets the user ID of the user
	 * @return the user ID
	 */
	public int getUserId() {
		return userId;
	}

	/**
	 * Gets the username of the user.
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Gets the ID of the room.
	 * @return the room ID
	 */
	public int getRoomId() {
		return roomId;
	}

	/**
	 * Gets the name of the room.
	 * @return the room name
	 */
	public String getRoomName() {
		return roomName;
	}

	/**
	 * Used for constructing {@link UserEnteredEvent} instances.
	 * @author Michael Angstadt
	 */
	public static class Builder extends Event.Builder<UserEnteredEvent, Builder> {
		private int userId;
		private String username;
		private int roomId;
		private String roomName;

		/**
		 * Creates an empty builder.
		 */
		public Builder() {
			super();
		}

		/**
		 * Initializes the builder from an existing event.
		 * @param original the original event
		 */
		public Builder(UserEnteredEvent original) {
			super(original);
			userId = original.userId;
			username = original.username;
			roomId = original.roomId;
			roomName = original.roomName;
		}

		/**
		 * Sets the user ID of the message author.
		 * @param userId the user ID
		 * @return this
		 */
		public Builder userId(int userId) {
			this.userId = userId;
			return this;
		}

		/**
		 * Sets the username of the message author.
		 * @param username the username
		 * @return this
		 */
		public Builder username(String username) {
			this.username = username;
			return this;
		}

		/**
		 * Sets the ID of the room the message was posted in.
		 * @param roomId the room ID
		 * @return this
		 */
		public Builder roomId(int roomId) {
			this.roomId = roomId;
			return this;
		}

		/**
		 * Sets the name of the room the message was posted in.
		 * @param roomName the room name
		 * @return this
		 */
		public Builder roomName(String roomName) {
			this.roomName = roomName;
			return this;
		}

		@Override
		public UserEnteredEvent build() {
			return new UserEnteredEvent(this);
		}
	}
}
