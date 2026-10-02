package oakbot.command.weather;

/**
 * Gets an ASCII art view of the weather.
 * @author Michael Angstadt
 */
public class AsciiArtRequest extends WttrInRequest {
	private boolean showWindSpeedInMetersPerSec;
	private View view;
	protected Unit unit;
	private boolean forceANSIOutput;
	private boolean onlyUseStandardConsoleFontGlyphs;
	private boolean hideSocialMedia;
	private boolean onlyShowDayAndNight;
	private boolean hideWeatherReportHeading;
	private boolean hideWeatherReportHeadingAndCityName;
	private boolean disableTerminalColorSequences;

	public boolean isShowWindSpeedInMetersPerSec() {
		return showWindSpeedInMetersPerSec;
	}

	public void setShowWindSpeedInMetersPerSec(boolean showWindSpeedInMetersPerSec) {
		this.showWindSpeedInMetersPerSec = showWindSpeedInMetersPerSec;
	}

	public View getView() {
		return view;
	}

	public void setView(View view) {
		this.view = view;
	}

	public Unit getUnit() {
		return unit;
	}

	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public boolean isForceANSIOutput() {
		return forceANSIOutput;
	}

	public void setForceANSIOutput(boolean forceANSIOutput) {
		this.forceANSIOutput = forceANSIOutput;
	}

	public boolean isOnlyUseStandardConsoleFontGlyphs() {
		return onlyUseStandardConsoleFontGlyphs;
	}

	public void setOnlyUseStandardConsoleFontGlyphs(boolean onlyUseStandardConsoleFontGlyphs) {
		this.onlyUseStandardConsoleFontGlyphs = onlyUseStandardConsoleFontGlyphs;
	}

	public boolean isHideSocialMedia() {
		return hideSocialMedia;
	}

	public void setHideSocialMedia(boolean hideSocialMedia) {
		this.hideSocialMedia = hideSocialMedia;
	}

	public boolean isOnlyShowDayAndNight() {
		return onlyShowDayAndNight;
	}

	public void setOnlyShowDayAndNight(boolean onlyShowDayAndNight) {
		this.onlyShowDayAndNight = onlyShowDayAndNight;
	}

	public boolean isHideWeatherReportHeading() {
		return hideWeatherReportHeading;
	}

	public void setHideWeatherReportHeading(boolean hideWeatherReportHeading) {
		this.hideWeatherReportHeading = hideWeatherReportHeading;
	}

	public boolean isHideWeatherReportHeadingAndCityName() {
		return hideWeatherReportHeadingAndCityName;
	}

	public void setHideWeatherReportHeadingAndCityName(boolean hideWeatherReportHeadingAndCityName) {
		this.hideWeatherReportHeadingAndCityName = hideWeatherReportHeadingAndCityName;
	}

	public boolean isDisableTerminalColorSequences() {
		return disableTerminalColorSequences;
	}

	public void setDisableTerminalColorSequences(boolean disableTerminalColorSequences) {
		this.disableTerminalColorSequences = disableTerminalColorSequences;
	}

	public enum View {
		CURRENT, CURRENT_TODAY_FORECAST, CURRENT_TODAY_TOMORROW_FORECAST, CURRENT_TODAY_TOMORROW_OVERMORROW_FORECAST
	}
}
