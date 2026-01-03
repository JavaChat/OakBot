package oakbot.task;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oakbot.bot.IBot;
import oakbot.bot.PostMessage;
import oakbot.command.HelpDoc;
import oakbot.util.ChatBuilder;
import oakbot.util.Rng;

/**
 * Regularly posts messages depending on how many security updates are
 * available.
 * @author Michael Angstadt
 */
public abstract class HealthMonitor implements ScheduledTask {
	private static final Logger logger = LoggerFactory.getLogger(HealthMonitor.class);
	private static final int MAX_PENDING_UPDATES_BEFORE_GETTING_SICK = 10;

	private final String[] responses = { "coughs", "sneezes", "clears throat", "expectorates", "sniffles", "wheezes", "groans", "moans" };
	private final List<Integer> roomIds;

	private boolean first = true;
	private int securityUpdates;

	@Override
	public String name() {
		return "healthmonitor";
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("Makes the bot \"cough\" when the server has pending security updates.")
			.detail("The bot will start coughing when there are " + MAX_PENDING_UPDATES_BEFORE_GETTING_SICK + " or more pending updates. It will cough more frequently the more updates there are.")
		.build();
		//@formatter:on
	}

	/**
	 * @param roomIds the rooms to post the messages to
	 */
	protected HealthMonitor(List<Integer> roomIds) {
		this.roomIds = roomIds;
	}

	@Override
	public Duration nextRun() {
		if (first) {
			first = false;

			logger.atInfo().log(() -> "Starting health monitor...");

			/*
			 * Do not start the health monitor if there is a problem checking
			 * for security updates.
			 */
			try {
				securityUpdates = getNumSecurityUpdates();
			} catch (Exception e) {
				logger.atInfo().setCause(e).log(() -> "Could not query system for security updates.  Health monitor disabled.");
				return null;
			}

			logger.atInfo().log(() -> "Health monitor started.  There are " + securityUpdates + " security updates available.");
		}

		/*
		 * Don't post anything if there are less than 10 updates. But check
		 * again tomorrow.
		 */
		if (securityUpdates < MAX_PENDING_UPDATES_BEFORE_GETTING_SICK) {
			return Duration.ofDays(1);
		}

		var timesToPostPerDay = securityUpdates / 30.0;
		if (timesToPostPerDay > 8) {
			timesToPostPerDay = 8;
		}

		var postEveryXMinutes = (24 / timesToPostPerDay) * 60;
		return Duration.ofMinutes((long) postEveryXMinutes);
	}

	@Override
	public void run(IBot bot) {
		try {
			securityUpdates = getNumSecurityUpdates();
		} catch (Exception e) {
			logger.atWarn().setCause(e).log(() -> "Problem querying for security updates.");
			securityUpdates = 0;
		}

		if (securityUpdates >= 10) {
			for (var roomId : roomIds) {
				var cb = new ChatBuilder();
				cb.italic(Rng.random(responses));
				var response = new PostMessage(cb).bypassFilters(true);
				try {
					bot.sendMessage(roomId, response);
				} catch (Exception e) {
					logger.atWarn().setCause(e).log(() -> "Problem sending cough message [room=" + roomId + "].");
				}
			}
		}
	}

	/**
	 * Queries the local operating system for the number of security updates
	 * that are available.
	 * @return the number of security updates or null if they couldn't be
	 * retrieved
	 * @throws Exception if there is any sort of problem
	 */
	public abstract int getNumSecurityUpdates() throws Exception;
}
