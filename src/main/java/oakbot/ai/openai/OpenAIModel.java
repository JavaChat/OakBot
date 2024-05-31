package oakbot.ai.openai;

import java.time.Instant;

/**
 * Represents an OpenAI model.
 * @author Michael Angstadt
 * @see OpenAIClient#listModels
 * @see "https://platform.openai.com/docs/api-reference/models/list"
 */
public record OpenAIModel(String id, Instant created, String owner) {
}
