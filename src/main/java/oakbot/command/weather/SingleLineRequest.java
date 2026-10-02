package oakbot.command.weather;

/**
 * Gets the current weather information as a small, single-line snippet of text.
 * @author Michael Angstadt
 */
public class SingleLineRequest extends WttrInRequest {
	private boolean showWind;
	private boolean showLocation;
	private String customFormat;

	protected Unit unit;
	private boolean useMetricAndShowWindSpeedInMetersPerSec;
	private boolean ignoreUserAgentAndForceANSIOutput;

	public boolean isShowWind() {
		return showWind;
	}

	public void setShowWind(boolean showWind) {
		this.showWind = showWind;
	}

	public boolean isShowLocation() {
		return showLocation;
	}

	public void setShowLocation(boolean showLocation) {
		this.showLocation = showLocation;
	}

	public String getCustomFormat() {
		return customFormat;
	}

	public void setCustomFormat(String customFormat) {
		this.customFormat = customFormat;
	}

	public Unit getUnit() {
		return unit;
	}

	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public boolean isUseMetricAndShowWindSpeedInMetersPerSec() {
		return useMetricAndShowWindSpeedInMetersPerSec;
	}

	public void setUseMetricAndShowWindSpeedInMetersPerSec(boolean useMetricAndShowWindSpeedInMetersPerSec) {
		this.useMetricAndShowWindSpeedInMetersPerSec = useMetricAndShowWindSpeedInMetersPerSec;
	}

	public boolean isIgnoreUserAgentAndForceANSIOutput() {
		return ignoreUserAgentAndForceANSIOutput;
	}

	public void setIgnoreUserAgentAndForceANSIOutput(boolean ignoreUserAgentAndForceANSIOutput) {
		this.ignoreUserAgentAndForceANSIOutput = ignoreUserAgentAndForceANSIOutput;
	}
}
