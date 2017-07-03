# OakBot

[![](https://travis-ci.org/JavaChat/OakBot.svg?branch=master)](https://travis-ci.org/JavaChat/OakBot)
[![codecov.io](http://codecov.io/github/JavaChat/OakBot/coverage.svg?branch=master)](http://codecov.io/github/JavaChat/OakBot?branch=master)

OakBot is a chat bot for [Stack Overflow Chat](http://chat.stackoverflow.com) that's written in Java.  It is named after the first name given to the Java programming language before it became "Java".
 
OakBot is most active in the [Java](https://chat.stackoverflow.com/rooms/139) and [Java and Android era](https://chat.stackoverflow.com/rooms/19132) chat rooms.

# Requirements

* Java 1.8
* [Maven](http://maven.apache.org) (for building)

# Build Instructions

To build the project, run the command below.

`mvn package`

This command will build the project and package it into an executable, shaded JAR. A shaded JAR file contains all of the project's dependencies. The shaded JAR file is saved here: `target/OakBot-VERSION.jar`.

# Deploy Instructions

1. Copy the following files to the server. Put them in the same directory:
   1. `target/OakBot-VERSION.jar`: The executable, shaded JAR file that contains OakBot's code and dependencies.
   1. `bot.properties`: This file contains configuration data, such as the bot's login credentials. A sample file is located in the root of this project.
   1. `logging.properties` (optional): The configuration file for the Java Logging API.  A sample file is located in the root of this project.
1. Run OakBot: `java -jar OakBot-VERSION.jar &`  
   1. The "&" at the end of the command launches the program in the background.  This is useful if you are logged into a server remotely and need to logout after launching OakBot.

# db.json

This is the file OakBot uses to persist information, such as how many commands it has responded to and what rooms it has joined. It is located in the bot's working directory. The file will automatically be created if it doesn't exist.

# bot.properties

Contains various configuration settings for the bot. Open the sample "bot.properties" file at the root of this project for a description of each setting.

OakBot must be restarted if any of these settings are changed while OakBot is running.

# Adding/Removing Commands

To add a command, create an instance of the [Command](https://github.com/JavaChat/OakBot/blob/master/src/main/java/oakbot/command/Command.java) interface and add it to the bot in the [main method](https://github.com/JavaChat/OakBot/blob/master/src/main/java/oakbot/Main.java).

# CLI Arguments

Argument | Description
-------- | -----------
--settings=PATH | The properties file that contains the bot's configuration settings, such as login credentials (defaults to "bot.properties").
--db=PATH | The path to a JSON file for storing all persistant data (defaults to "db.json").
--quiet | If specified, the bot will not output a greeting message when it starts up.
--version | Prints the version of this program.
--help | Prints descriptions of each argument.

# Questions/Feedback

One way to reach me is in Stack Overflow's [Java chat room](https://chat.stackoverflow.com/rooms/139). Please mention my name (@Michael) so I will see your message.

You can also submit bug reports and feature requests to the [issue tracker](https://github.com/JavaChat/OakBot/issues).
