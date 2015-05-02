package oakbot.command.urban;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a word definition within an {@link UrbanResponse}.
 * @author Michael Angstadt
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UrbanDefinition {
	@JsonProperty("defid")
	private long id;

	private String word, author, permalink, definition, example;

	@JsonProperty("thumbs_up")
	private long thumbsUp;

	@JsonProperty("thumbs_down")
	private long thumbsDown;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getPermalink() {
		return permalink;
	}

	public void setPermalink(String permalink) {
		this.permalink = permalink;
	}

	public String getDefinition() {
		return definition;
	}

	public void setDefinition(String definition) {
		this.definition = definition;
	}

	public String getExample() {
		return example;
	}

	public void setExample(String example) {
		this.example = example;
	}

	public long getThumbsUp() {
		return thumbsUp;
	}

	public void setThumbsUp(long thumbsUp) {
		this.thumbsUp = thumbsUp;
	}

	public long getThumbsDown() {
		return thumbsDown;
	}

	public void setThumbsDown(long thumbsDown) {
		this.thumbsDown = thumbsDown;
	}
}
