package oakbot.util;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Formats dates relative to the current time.
 * @author Michael Angstadt
 */
public class RelativeDateFormat {
	//private final DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
	private final DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT);

	/**
	 * Formats a date
	 * @param date the date or format
	 * @return the formatted date (e.g. "Today at 1:00 PM")
	 */
	public String format(Date date) {
		Date now = new Date();

		long diff = now.getTime() - date.getTime();
		if (diff <= 60 * 1000) {
			return "A moment ago";
		}

		if (diff <= 60 * 60 * 1000) {
			return (diff / (60 * 1000)) + " minutes ago";
		}

		Calendar dateCal = Calendar.getInstance();
		dateCal.setTime(date);
		Calendar nowCal = Calendar.getInstance();
		nowCal.setTime(now);
		int dayDiff = (nowCal.get(Calendar.YEAR) - dateCal.get(Calendar.YEAR)) * 365 + (nowCal.get(Calendar.DAY_OF_YEAR) - dateCal.get(Calendar.DAY_OF_YEAR));

		if (dayDiff == 0) {
			return "Today at " + tf.format(date);
		}
		if (dayDiff == 1) {
			return "Yesterday at " + tf.format(date);
		}
		if (dayDiff < 7) {
			return "About " + dayDiff + " days ago.";
		}
		if (dayDiff < 14) {
			return "About a week ago.";
		}
		if (dayDiff < 30) {
			return "About " + (dayDiff / 7) + " weeks ago.";
		}
		if (dayDiff < 60) {
			return "About a month ago.";
		}
		return "About " + (dayDiff / 30) + " months ago.";
	}
}
