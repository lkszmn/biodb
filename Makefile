install:	Genomes_update.jar

clean:
	rm Genomes_update.jar genomes/*.class

JAVAS = genomes/BashWrapper.java genomes/Builder.java genomes/Config.java \
	genomes/DirectoryFileFilter.java genomes/dmpReader.java \
	genomes/ExtensionFileFilter.java genomes/Genome.java \
	genomes/GenomesDB.java genomes/HTMLGenNode.java \
	genomes/HTMLGenTree.java genomes/Main.java genomes/Node.java \
	genomes/Search.java genomes/StreamReaderThread.java

JAVAC = javac
JAR = jar
JARFLAGS = -cfe
RMFJAR = zip -d

JFLAGS =

MAINCLASS = genomes/Main

CONFIG_PROTO = genomes/config.txt

genomes/Main.class: ${JAVAS}
		${JAVAC} genomes/Main.java

Genomes_update.jar:	genomes/Main.class
			${JAR} ${JARFLAGS} Genomes_update.jar ${MAINCLASS} genomes
			# ${RMFJAR} Genomes_update.jar ${CONFIG_PROTO}


