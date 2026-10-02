package oakbot.command.weather;

/**
 * Base class for all wttr.in requests.
 * @author Michael Angstadt
 */
public class WttrInRequest {
	protected String location;
	protected String language;

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public void setLocationToCurrentLocation() {
		setLocation(null);
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}
}
