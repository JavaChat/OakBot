package oakbot.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * Formats dates relative to the current time.
 * @author Michael Angstadt
 */
public class RelativeDateFormat {
	private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);

	/**
	 * Formats a date
	 * @param date the date or format
	 * @return the formatted date (e.g. "Today at 1:00 PM")
	 */
	public String format(LocalDateTime date) {
		LocalDateTime now = LocalDateTime.now();
		Duration diff = Duration.between(date, now);

		if (diff.toMinutes() < 1) {
			return "A moment ago";
		}

		if (diff.toHours() < 1) {
			return diff.toMinutes() + " minutes ago";
		}

		long dayDiff = diff.toDays();
		if (dayDiff == 0) {
			return "Today at " + timeFormatter.format(date);
		}
		if (dayDiff == 1) {
			return "Yesterday at " + timeFormatter.format(date);
		}
		if (dayDiff < 7) {
			return "About " + dayDiff + " days ago.";
		}
		if (dayDiff < 14) {
			return "Over a week ago.";
		}
		if (dayDiff < 30) {
			return "Over " + (dayDiff / 7) + " weeks ago.";
		}
		if (dayDiff < 60) {
			return "Over a month ago.";
		}
		return "Over " + (dayDiff / 30) + " months ago.";
	}
}
