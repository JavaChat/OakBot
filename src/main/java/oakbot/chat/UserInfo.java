package oakbot.chat;

import java.time.LocalDateTime;

/**
 * Contains information about a chat user.
 * @author Michael Angstadt
 */
public class UserInfo {
	private final int userId, roomId;
	private final String username;
	private final String profilePicture;
	private final int reputation;
	private final boolean moderator, owner;
	private final LocalDateTime lastSeen, lastPost;

	private UserInfo(Builder builder) {
		userId = builder.userId;
		roomId = builder.roomId;
		username = builder.username;
		profilePicture = builder.profilePicture;
		reputation = builder.reputation;
		moderator = builder.moderator;
		owner = builder.owner;
		lastSeen = builder.lastSeen;
		lastPost = builder.lastPost;
	}

	/**
	 * Gets the user ID.
	 * @return the user ID
	 */
	public int getUserId() {
		return userId;
	}

	/**
	 * Gets the room that this user information came from.
	 * @return the room ID
	 */
	public int getRoomId() {
		return roomId;
	}

	/**
	 * Gets the username.
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Gets the URL to the profile picture. For users without a profile picture,
	 * this will return a URL to the auto-generated picture (based on a hash of
	 * the user's email address).
	 * @return the URL to the profile picture
	 */
	public String getProfilePicture() {
		return profilePicture;
	}

	/**
	 * Gets the Stackoverflow reputation score.
	 * @return the reputation
	 */
	public int getReputation() {
		return reputation;
	}

	/**
	 * Gets whether the user is a moderator or not.
	 * @return true if the user is a moderator, false if not
	 */
	public boolean isModerator() {
		return moderator;
	}

	/**
	 * Gets whether the user is an owner of the room that this user information
	 * came from
	 * @return true if the user is a room owner, false if not
	 */
	public boolean isOwner() {
		return owner;
	}

	/**
	 * Gets the time the user was last seen in the room that this user
	 * information came from.
	 * @return the last seen time
	 */
	public LocalDateTime getLastSeen() {
		return lastSeen;
	}

	/**
	 * Gets the time the user last posted in the room that this user information
	 * came from.
	 * @return the last post time
	 */
	public LocalDateTime getLastPost() {
		return lastPost;
	}

	@Override
	public String toString() {
		return "UserInfo [userId=" + userId + ", username=" + username + ", profilePicture=" + profilePicture + ", reputation=" + reputation + ", moderator=" + moderator + ", owner=" + owner + ", lastSeen=" + lastSeen + ", lastPost=" + lastPost + "]";
	}

	public static class Builder {
		private int userId, roomId;
		private String username;
		private String profilePicture;
		private int reputation;
		private boolean moderator, owner;
		private LocalDateTime lastSeen, lastPost;

		public Builder userId(int userId) {
			this.userId = userId;
			return this;
		}

		public Builder roomId(int roomId) {
			this.roomId = roomId;
			return this;
		}

		public Builder username(String username) {
			this.username = username;
			return this;
		}

		public Builder reputation(int reputation) {
			this.reputation = reputation;
			return this;
		}

		public Builder profilePicture(String profilePicture) {
			this.profilePicture = profilePicture;
			return this;
		}

		public Builder moderator(boolean moderator) {
			this.moderator = moderator;
			return this;
		}

		public Builder owner(boolean owner) {
			this.owner = owner;
			return this;
		}

		public Builder lastSeen(LocalDateTime lastSeen) {
			this.lastSeen = lastSeen;
			return this;
		}

		public Builder lastPost(LocalDateTime lastPost) {
			this.lastPost = lastPost;
			return this;
		}

		public UserInfo build() {
			return new UserInfo(this);
		}
	}
}
