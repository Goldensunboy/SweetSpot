# Makefile for building the SweetSpot server

SRC = app/src/main/java/com/sweetspot/server/SweetSpotServer.java app/src/main/java/com/sweetspot/shared/Metadata.java app/src/main/java/com/sweetspot/shared/Definitions.java
JAR = SweetSpotServer.jar
LIB = jaudiotagger-2.2.3/jaudiotagger-2.2.3.jar

PORT=30012
DIR=/home/andrew/Music

all :
	@echo "[JAR] Creating $(JAR)"
	@cp $(SRC) .
	@javac -cp .:$(LIB) *.java
	@mkdir -p com/sweetspot/server
	@mkdir -p com/sweetspot/shared
	@mv SweetSpotServer*.class com/sweetspot/server
	@mv *.class com/sweetspot/shared
	@jar xf $(LIB)
	@jar cfe build/$(JAR) com.sweetspot.server.SweetSpotServer com org
	@rm -rf *.java *.class com org META-INF

run : all
	@echo "[RUN] $(JAR)"
	java -jar build/$(JAR) -p $(PORT) -d $(DIR)

clean :
	@echo "[CLEAN] Removing $(JAR)"
	@rm -rf build/$(JAR)

