package oakbot.bot;

import java.util.List;

/**
 * Security-related configuration for the Bot.
 * Manages user permissions and access control.
 */
public class SecurityConfiguration {
	private final List<Integer> admins;
	private final List<Integer> bannedUsers;
	private final List<Integer> allowedUsers;

	public SecurityConfiguration(List<Integer> admins, List<Integer> bannedUsers, List<Integer> allowedUsers) {
		this.admins = List.copyOf(admins);
		this.bannedUsers = List.copyOf(bannedUsers);
		this.allowedUsers = List.copyOf(allowedUsers);
	}

	public List<Integer> getAdmins() {
		return admins;
	}

	public List<Integer> getBannedUsers() {
		return bannedUsers;
	}

	public List<Integer> getAllowedUsers() {
		return allowedUsers;
	}

	public boolean isAdmin(Integer userId) {
		return admins.contains(userId);
	}

	public boolean isBanned(Integer userId) {
		return bannedUsers.contains(userId);
	}

	public boolean isAllowed(Integer userId) {
		return allowedUsers.isEmpty() || allowedUsers.contains(userId);
	}
}
