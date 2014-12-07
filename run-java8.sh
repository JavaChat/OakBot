/home/michael/jdk1.8.0_25/bin/javadoc \
-doclet oakbot.doclet.OakbotDoclet \
-docletpath target/classes:/home/michael/.m2/repository/org/jsoup/jsoup/1.8.1/jsoup-1.8.1.jar \
-sourcepath ../java8-src \
-subpackages java \
-subpackages javax \
-subpackages org \
-quiet