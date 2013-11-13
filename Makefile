all:
	mkdir -p build
	javac -classpath /home/m4mbr3/programs/joram_5_8_0/ship/bundle/joram-client-jms.jar:lib/*:. -d build *.java

clean:
	rm -r build


