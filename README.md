Crime City Bot
==============

This is a money-collecting bot for the Crime City iOS and Android game.

Requirements
------------
* Java 7
* Maven 3 for compiling

Compiling
---------
	export JAVA_HOME=`/usr/libexec/java_home -v 1.7`
	mvn clean assembly:assembly -DdescriptorId=jar-with-dependencies

Configuration
-------------
Sniff your network parameters while launching the game, and report them to a `.properties` file. There are 2 sample files in the distribution (one for iOS, one for Android). You can also adjust some parameters there.

Running
-------
	/opt/java/jre7/bin/java -ea -jar target/ccbot-*-jar-with-dependencies.jar my.properties

Hacking
-------
Main class is CCBot. Most classes are unused (they are leftover from the days where AMF ruled the world).