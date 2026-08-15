package oakbot.command;

import static oakbot.bot.ChatActions.post;

import java.util.ArrayList;
import java.util.List;

import oakbot.bot.ChatActions;
import oakbot.bot.ChatCommand;
import oakbot.bot.IBot;
import oakbot.listener.chatgpt.ChatGPT;
import oakbot.listener.chatgpt.ImagineCore;
import oakbot.util.ChatBuilder;
import oakbot.util.Rng;

/**
 * Displays the costs associated with running the bot, and how to support the
 * creator.
 * @author Michael Angstadt
 */
public class SupportCommand implements Command {
	private final List<String> coffees = List.of("Caramel frappuccinos", "Vanilla bean frappuccinos", "Vanilla lattes", "Iced white mocha lattes", "Iced chai lattes");
	private final List<String> costs;
	private final String paypalUrl;
	private final ChatGPT chatGPT;
	private final ImagineCore imagineCore;

	public SupportCommand(List<String> costs, String paypalUrl, ChatGPT chatGPT, ImagineCore imagineCore) {
		this.costs = costs;
		this.paypalUrl = paypalUrl;
		this.chatGPT = chatGPT;
		this.imagineCore = imagineCore;
	}

	@Override
	public String name() {
		return "support";
	}

	@Override
	public List<String> aliases() {
		return List.of("donate");
	}

	@Override
	public HelpDoc help() {
		//@formatter:off
		return new HelpDoc.Builder(this)
			.summary("How to support Oak.")
		.build();
		//@formatter:on
	}

	@Override
	public ChatActions onMessage(ChatCommand chatCommand, IBot bot) {
		var allCosts = injectCreamyCoffeeCost();
		var aiRateLimits = buildAiRateLimits();

		var cb = new ChatBuilder().append("Oak costs the following to run (USD):").nl();

		//@formatter:off
		allCosts.stream()
			.map(cost -> cost.replace("$AI_RATE_LIMITS", aiRateLimits))
		.forEach(cost -> cb.nl().append("• ").append(cost));
		//@formatter:on

		if (paypalUrl != null) {
			cb.nl().nl().append("Support Oak: ").append(paypalUrl);
		}

		return post(cb);
	}

	private String buildAiRateLimits() {
		var chatsPerDay = getChatsPerDay();
		var chatContextSize = getChatContextSize();
		var imagesPerDay = getImagesPerDay();

		var rateLimits = new ArrayList<String>();
		if (chatsPerDay != null) {
			rateLimits.add(chatsPerDay + " chats/user/day");
		}
		if (chatContextSize != null) {
			rateLimits.add(chatContextSize + " chat context size");
		}
		if (imagesPerDay != null) {
			rateLimits.add(imagesPerDay + " images/user/day");
		}

		return rateLimits.isEmpty() ? "" : "(rate-limited: " + String.join(", ", rateLimits) + ")";
	}

	private String getChatsPerDay() {
		if (chatGPT == null) {
			return null;
		}

		var requests = chatGPT.getUsageQuota().getRequestsPerPeriod();
		if (requests == 0) {
			return "unlimited";
		}

		return Integer.toString(requests);
	}

	private String getChatContextSize() {
		if (chatGPT == null) {
			return null;
		}

		return Integer.toString(chatGPT.getNumLatestMessagesToIncludeInRequest());
	}

	private String getImagesPerDay() {
		if (imagineCore == null) {
			return null;
		}

		var requests = imagineCore.getUsageQuota().getRequestsPerPeriod();
		if (requests == 0) {
			return "unlimited";
		}

		return Integer.toString(requests);
	}

	private List<String> injectCreamyCoffeeCost() {
		var allCosts = new ArrayList<>(costs);

		var coffee = Rng.random(coffees);
		var coffeeCost = Rng.next(220, 250);
		allCosts.add(coffee + ": $" + coffeeCost + "/mo");

		return allCosts;
	}
}
