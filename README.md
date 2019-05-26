# OakBot

[![](https://travis-ci.org/JavaChat/OakBot.svg?branch=master)](https://travis-ci.org/JavaChat/OakBot)
[![codecov.io](http://codecov.io/github/JavaChat/OakBot/coverage.svg?branch=master)](http://codecov.io/github/JavaChat/OakBot?branch=master)
[![Known Vulnerabilities](https://snyk.io/test/github/JavaChat/OakBot/badge.svg)](https://snyk.io/test/github/JavaChat/OakBot)

OakBot is a chat bot for [Stack Overflow Chat](http://chat.stackoverflow.com) that's written in Java.  It is named after the first name given to the Java programming language before it became "Java".
 
OakBot is most active in the [Java](https://chat.stackoverflow.com/rooms/139) chat room.

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
   1. `bot-context.xml`: This file contains configuration data and command definitions. A sample file is located in the root of this project.
   1. `logging.properties` (optional): The configuration file for the Java Logging API.  A sample file is located in the root of this project.
1. Run OakBot: `java -jar OakBot-VERSION.jar`
1. Once the bot has fully started up, it will instruct you to press `Ctrl+Z`, then type the `bg` command. In Linux, this will move the program into the background and free up the shell.

# db.json

This is the file OakBot uses to persist information, such as how many commands it has responded to and what rooms it has joined. It is located in the bot's working directory. The file will automatically be created if it doesn't exist.

# bot-context.xml

Contains various configuration settings for the bot. Open the sample `bot-context.xml` file at the root of this project for a description of each setting.

OakBot must be restarted if any of these settings are changed while OakBot is running.

# Adding/Removing Commands

All of the bot's commands are defined in the `bot-context.xml` file.

Commands can be added by creating a class that extends the `Command` interface, and then adding the class to `bot-context.xml` (you must also, of course, include your class in the Java classpath when running the bot).

This file uses [Spring's IoC container](https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html) library.

All of OakBot's commands are [listed on the wiki](https://github.com/JavaChat/OakBot/wiki/Commands).

# CLI Arguments

Argument | Description
-------- | -----------
--context=PATH | The path to the Spring application context XML file that contains the bot's configuration settings and commands (defaults to "bot-context.xml"). Note: Absolute paths must be prefixed with "file:".
--mock | Runs the bot using a mock chat connection for testing purposes.<br><br>A text file will be created in the root of the project for each chat room the bot is configured to connect to. These files are used to "send" messages to the mock chat rooms. To send a message, type your message into the text file and save it.<br><br>Messages are entered one per line. Multi-line messages can be entered by ending each line with a backslash until you reach the last line. You should only append onto the end of the file; do not delete anything. These files are re-created every time the program runs.<br><br>All messages that are sent to the mock chat room are displayed in stdout (this includes your messages and the bot's responses).
--quiet | If specified, the bot will not output a greeting message when it starts up.
--version | Prints the version of this program.
--help | Prints descriptions of each argument.

# Questions/Feedback

One way to reach me is in Stack Overflow's [Java chat room](https://chat.stackoverflow.com/rooms/139). Please mention my name (@Michael) so I will see your message.

You can also submit bug reports and feature requests to the [issue tracker](https://github.com/JavaChat/OakBot/issues).
