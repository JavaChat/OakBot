package oakbot;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import oakbot.bot.Bot;
import oakbot.bot.Command;
import oakbot.javadoc.Java8PageParser;
import oakbot.javadoc.JavadocCommand;
import oakbot.javadoc.PageLoader;
import oakbot.javadoc.PageParser;
import oakbot.javadoc.ZipPageLoader;

/**
 * @author Michael Angstadt
 */
public class Main {
	public static void main(String[] args) throws Exception {
		BotProperties props = loadProperties();

		//TODO make javadoc case-insensitive
		//TODO add "=javadoc TimeUnit 2" for second para
		//TODO add max message size limit
		//@formatter:off
		Bot bot = new Bot.Builder(props.getLoginEmail(), props.getLoginPassword())
		.commands(createJavadocCommand())
		.heartbeat(props.getHeartbeat())
		.admins(props.getAdmins().toArray(new Integer[0]))
		.trigger(props.getTrigger())
		.rooms(props.getRooms().toArray(new Integer[0]))
		.about("**Oak Bot** by `Michael` | [Source code](http://github.com/mangstadt/OakBot) | Built: TODO") //TODO add build date
		.build();
		//@formatter:on

		bot.connect();
	}
	
	private static BotProperties loadProperties() throws IOException{
    	Path file = Paths.get("bot.properties");
    	Properties properties = new Properties();
    	try (Reader reader = Files.newBufferedReader(file, Charset.forName("UTF-8"))){
    		properties.load(reader);
    	}
    	return new BotProperties(properties);
    }
	
	private static Command createJavadocCommand() throws IOException{
		JavadocCommand javadocCommand = new JavadocCommand();
		
		Path dir = Paths.get("javadocs");
		Path java8Api = dir.resolve("java8.zip");
		if (Files.exists(java8Api)) {
			PageLoader loader = new ZipPageLoader(java8Api);
			PageParser parser = new Java8PageParser();
			javadocCommand.addLibrary(loader, parser);
		} else {
			//for testing purposes
			//this ZIP only has the "java.lang.String" class
			Path sample = dir.resolve("sample.zip");
			if (Files.exists(sample)) {
				PageLoader loader = new ZipPageLoader(sample);
				PageParser parser = new Java8PageParser();
				javadocCommand.addLibrary(loader, parser);
			}
		}
		
		return javadocCommand;
	}
	
	private static Command createHttpCommand(){
		//TODO
		return null;
	}
}
