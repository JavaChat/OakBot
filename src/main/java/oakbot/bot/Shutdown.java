package oakbot.bot;

/**
 * Instructs the bot to terminate.
 * @author Michael Angstadt
 */
public class Shutdown implements ChatAction {
	@Override
	public ChatActions execute(ActionContext context) {
		context.getBot().stop();
		return ChatActions.doNothing();
	}
}
