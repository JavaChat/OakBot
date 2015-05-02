package oakbot.command.urban;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a response from the Urban Dictionary API.
 * @author Michael Angstadt
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UrbanResponse {
	private List<String> tags;

	@JsonProperty("result_type")
	private String resultType;

	@JsonProperty("list")
	private List<UrbanDefinition> definitions;

	private List<String> sounds;

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public String getResultType() {
		return resultType;
	}

	public void setResultType(String resultType) {
		this.resultType = resultType;
	}

	public List<UrbanDefinition> getDefinitions() {
		return definitions;
	}

	public void setDefinitions(List<UrbanDefinition> definitions) {
		this.definitions = definitions;
	}

	public List<String> getSounds() {
		return sounds;
	}

	public void setSounds(List<String> sounds) {
		this.sounds = sounds;
	}
}
