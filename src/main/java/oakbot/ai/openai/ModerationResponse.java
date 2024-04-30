package oakbot.ai.openai;

import java.util.Map;
import java.util.Set;

/**
 * A moderation response.
 * @author Michael Angstadt
 * @see OpenAIClient#moderate
 * @see "https://platform.openai.com/docs/api-reference/moderations/object"
 */
public class ModerationResponse {
	private final String id;
	private final String model;
	private final Set<String> flaggedCategories;
	private final Map<String, Double> categoryScores;

	private ModerationResponse(ModerationResponse.Builder builder) {
		id = builder.id;
		model = builder.model;
		flaggedCategories = (builder.flaggedCategories == null) ? Set.of() : Set.copyOf(builder.flaggedCategories);
		categoryScores = (builder.categoryScores == null) ? Map.of() : Map.copyOf(builder.categoryScores);
	}

	/**
	 * Gets the unique identifier for the moderation request.
	 * @return the ID
	 */
	public String getId() {
		return id;
	}

	/**
	 * Gets the model used to generate the moderation results.
	 * @return the model
	 */
	public String getModel() {
		return model;
	}

	/**
	 * Gets the categories that were flagged by the system due to their score
	 * being too high. If at least one category is flagged, then the entire
	 * input message is rejected by the moderation system.
	 * @return the flagged categories
	 */
	public Set<String> getFlaggedCategories() {
		return flaggedCategories;
	}

	/**
	 * Gets the score of each category.
	 * @return the scores
	 */
	public Map<String, Double> getCategoryScores() {
		return categoryScores;
	}

	public static class Builder {
		private String id;
		private String model;
		private Set<String> flaggedCategories;
		private Map<String, Double> categoryScores;

		public Builder id(String id) {
			this.id = id;
			return this;
		}

		public Builder model(String model) {
			this.model = model;
			return this;
		}

		public Builder flaggedCategories(Set<String> flaggedCategories) {
			this.flaggedCategories = flaggedCategories;
			return this;
		}

		public Builder categoryScores(Map<String, Double> categoryScores) {
			this.categoryScores = categoryScores;
			return this;
		}

		public ModerationResponse build() {
			return new ModerationResponse(this);
		}
	}
}
