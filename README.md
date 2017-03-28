# OakBot

OakBot is a chat bot for [Stackoverflow Chat](http://chat.stackoverflow.com) that's written in Java.  It is named after the first name given to the Java programming language before it became "Java".

# Build Instructions

OakBot requires Java 1.8 and uses the [Maven](http://maven.apache.org) build system.

The easiest way to build it for production use is to create a fat JAR like so:

`mvn test assembly:single`

# Deploy Instructions

OakBot requires Java 1.8 to run.

1. Copy the following files to the server.  Put them in the same directory:
   1. **The OakBot fat JAR**
   1. **bot.properties** - Configures the bot.  A sample file is located in the root of this project.  See the section below descriptions of the fields that make up this file.
   1. **logging.properties** - (optional) A config file for the Java Logging API.  A sample file is located in the root of this project.
1. Run OakBot: `java -jar oakbot.jar &`  
   1. The "&" at the end of the command launches the program in the background.  This is useful if you are ssh-ing into a server and need to logout after launching OakBot.

# db.json

This is the file OakBot uses to persist information, such as how many commands it has responded to and what rooms it has joined. This file will automatically be created if it doesn't exist.

# bot.properties

Note that OakBot must be restarted in order to pick up any changes you make to these settings.

Property | Description
-------- | -----------
login.email | The email address of the StackOverflow user that the bot will login as. It must be a StackExchange login.  It cannot be a login from another provider, like Google or Facebook.
login.password | The password of the StackOverflow user (yes, it's in plain text, so sue me).
bot.userName | The username associated with the StackOverflow user.
bot.userId | The user ID of the bot's StackOverflow account. You can get this from the URL of your profile page.
trigger | The character that prefixes all commands.  For example, if the trigger was "/", then posting "/about" in the chat room will cause the bot to display information about itself.
homeRooms | A comma-separated list of room IDs that OakBot will join and which OakBot cannot be unsummoned from.  A chat room's room ID can be found in its URL.
heartbeat | How often OakBot will poll each chat room to look for new messages (in milliseconds).  Unfortunately, OakBot does not use websockets, like your browser does.
admins | (optional) A comma-separated list of user IDs that can run admin-level commands against the bot (notably, the "shutdown" command).
javadoc.folder | (optional) The folder that contains the Javadoc information used with the "javadoc" command.  The Javadoc info for various libraries (including the Java 8 API) are stored on a [public Dropbox folder](https://www.dropbox.com/sh/xkf7kua3hzd8xvo/AAC1sOkVTNUE2MKPAXTm28bna?dl=0) (they are ZIP files).  You can also build these ZIP files yourself using the [oakbot-doclet](https://github.com/mangstadt/oakbot-doclet) tool. If this property is not defined, the "javadoc" command will not be activated.
dictionary.key | (optional) This is used by the "define" command to lookup dictionary definitions from the [dictionaryapi.com](http://www.dictionaryapi.com/) website. If this property is not defined, the "define" command will not be activated.
greeting | (optional) The message OakBot will post when it joins a room. If this property is not defined, OakBot will not say anything when it joins a room.
about.host | (optional) The name of the server that is hosting this bot.  Displayed in the "about" command.

# Adding/Removing Commands

Currently, the adding and removal of commands is done in the [source code](https://github.com/JavaChat/OakBot/blob/master/src/main/java/oakbot/Main.java).  The command system is pluggable--simply create an instance of [Command](https://github.com/JavaChat/OakBot/blob/master/src/main/java/oakbot/command/Command.java) and add it to the bot.

# CLI Arguments

Argument | Description
-------- | -----------
--settings=PATH | The properties file that contains the bot's configuration settings, such as login credentials (defaults to "bot.properties").
--db=PATH | The path to a JSON file for storing all persistant data (defaults to "db.json").
--quiet | If specified, the bot will not output a greeting message when it starts up.
--version | Prints the version of this program.
--help | Prints descriptions of each argument.

# Questions/Feedback

Feel free to post questions to the [Java chat room](http://chat.stackoverflow.com/rooms/139).  Be sure to mention my name (@Michael) when doing so.

Please submit bug reports and feature requests to the [issue tracker](https://github.com/mangstadt/OakBot/issues).
