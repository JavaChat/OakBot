package oakbot.command.weather;

/**
 * Gets the raw weather data in JSON format.
 * @author Michael Angstadt
 */
public class JsonRequest extends WttrInRequest {
	private boolean includeHourlyData;

	public JsonRequest(boolean includeHourlyData) {
		this.includeHourlyData = includeHourlyData;
	}

	public boolean isIncludeHourlyData() {
		return includeHourlyData;
	}

	public void setIncludeHourlyData(boolean includeHourlyData) {
		this.includeHourlyData = includeHourlyData;
	}
}
