#!/bin/sh

# make Blast databases
echo "RUNNING Parmaeter -u "
java -server -Xmx8G -classpath /cluster/databases/update_scripts/genomes/ genomes/Main  -c /cluster/databases/update_scripts/genomes/genomes/config.txt   -u  >> /cluster/databases/genomes/current/log/java_update_std.log
# Copy Files
echo "RUNNING Parmaeter -f "
java -server -Xmx8G -classpath /cluster/databases/update_scripts/genomes/ genomes/Main  -c /cluster/databases/update_scripts/genomes/genomes/config.txt  -f >> /cluster/databases/genomes/current/log/java_format_std.log
# Create Genome Trees
echo "RUNNING Parmaeter -w "
java -server -Xmx8G -classpath /cluster/databases/update_scripts/genomes/ genomes/Main -c /cluster/databases/update_scripts/genomes/genomes/config.txt  -w >> /cluster/databases/genomes/current/log/java_web_std.log

