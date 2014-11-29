package oakbot;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
	public static final String VERSION, URL;
	public static final Date BUILT;
	static {
		Properties props = new Properties();
		try (InputStream in = Main.class.getResourceAsStream("/info.properties")){
			props.load(in);
		} catch (IOException e){
			throw new RuntimeException(e);
		}

		VERSION = props.getProperty("version");
		URL = props.getProperty("url");

		Date built;
		try {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
			built = df.parse(props.getProperty("built"));
		} catch (ParseException e) {
			//this could happen during development if the properties file is not filtered by Maven
			built = new Date();
		}
		BUILT = built;
	}
	
	public static void main(String[] args) throws Exception {
		BotProperties props = loadProperties();
		DateFormat builtFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm Z");

		//TODO add "=javadoc TimeUnit 2" for second para
		//TODO add max message size limit
		//TODO re-download fkeys after 1 hour of inactivity
		//@formatter:off
		Bot bot = new Bot.Builder(props.getLoginEmail(), props.getLoginPassword())
		.commands(createJavadocCommand())
		.heartbeat(props.getHeartbeat())
		.admins(props.getAdmins().toArray(new Integer[0]))
		.trigger(props.getTrigger())
		.rooms(props.getRooms().toArray(new Integer[0]))
		.about("**Oak Bot** v" + VERSION + " by `Michael` | [source code](" + URL + ") | Built: " + builtFormat.format(BUILT))
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
