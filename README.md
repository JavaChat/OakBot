# OakBot

OakBot is a chat bot for [Stack Overflow Chat](http://chat.stackoverflow.com) that's written in Java.  It is named after the first name given to the Java programming language before it became "Java".

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

Contains various configuration settings for the bot. Open the sample "bot.properties" file at the root of this project for a description of each setting.

OakBot must be restarted if any of these settings are changed while OakBot is running.

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
