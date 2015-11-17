# OakBot

OakBot is a chat bot for [Stackoverflow Chat](http://chat.stackoverflow.com) that's written in Java.  It is named after the first name given to the Java programming language before it became "Java".

# Build Instructions

OakBot requires Java 1.8 and uses the [Maven](http://maven.apache.org) build system.

The easiest way to build it for production use is to create a fat JAR like so:  
`mvn package assembly:single`

# Deploy Instructions

OakBot requires Java 1.8 to run.

1. Copy the following files to the server.  Put them in the same directory:
   1. **The OakBot fat JAR**
   1. **bot.properties** - Configures the bot.  A sample file is located in the root of this project.  See the section below descriptions of the fields that make up this file.
   1. **logging.properties** - (optional) A config file for the Java Logging API.  A sample file is located in the root of this project.
1. Run OakBot: `java -jar name-of-oakbot-fat-jar.jar &`  
   1. The "&" at the end of the command launches the program in the background.  This is useful if you are ssh-ing into a server and need to logout after launching OakBot.
   1. Adding a `-q` after the JAR filename will launch OakBot in "quiet mode", which means it will not send a notification message to each chat room it joins when it first starts up.

# bot.properties

Note that OakBot must be restarted in order to pick up any changes you make to these settings.

Property | Description
-------- | -----------
login.email | The email address of the StackOverflow user that the bot will login as. It must be a StackExchange login.  It cannot be a login from another provider, like Google or Facebook.
login.password | The password of the StackOverflow user (yes, it's in plain text, so sue me).
bot.name | The username assoicated with the StackOverflow user.
trigger | The character that prefixes all commands.  For example, if the trigger was "/", then posting "/about" in the chat room will cause the bot to display information about itself.
rooms | A comma-separated list of room IDs that OakBot will join.  A chat room's room ID can be found in its URL.
heartbeat | How often OakBot will poll each chat room to look for new messages (in milliseconds).  Unfortunately, OakBot does not use websockets, like your browser does.
admins | A comma-separated list of user IDs that can run admin-level commands against the bot (notably, the "shutdown" command).
javadoc.folder | The folder that contains the Javadoc information used with the "javadoc" command.  The Javadoc info for various libraries (including the Java 8 API) are stored on a [public Dropbox folder](https://www.dropbox.com/sh/xkf7kua3hzd8xvo/AAC1sOkVTNUE2MKPAXTm28bna?dl=0) (they are ZIP files).  You can also build these ZIP files yourself using the [oakbot-doclet](https://github.com/mangstadt/oakbot-doclet) tool.
dictionary.key | This is used by the "define" command to lookup dictionary definitions from the [dictionaryapi.com](http://www.dictionaryapi.com/) website. If this property is not defined, the "define" command will not be activated.

# Adding/Removing Commands

You can customize OakBot's commands by making changes to the `oakbot.Main#main()` method.  

# Questions/Feedback

Feel free to post questions to the [Java chat room](http://chat.stackoverflow.com/rooms/139/javachat-fish-and-chips).  Be sure to mention my name (@Michael) when doing so.

Please submit bug reports and feature requests to the [issue tracker](https://github.com/mangstadt/OakBot/issues).


