#!/bin/bash

# Script for Download and processing the genomes databases, based upon Christian Meyers Jar Script
# Joern Marialke, Genecenter 2012
# Updated to be better adaptable to other clusters by Klaus Faidt, 2014

: ${TK_ROOT:=/local/dummy_toolkit}
: ${DB_ROOT:=${TK_ROOT}/databases}
: ${SCRIPTDIR:=${DB_ROOT}/update_scripts}
GENOMES_SCRIPTS=${SCRIPTDIR}/genomes
GENOMES_LOG=~/log/db_update/genomes

function abort {
    local EXIT_CODE=$1
    local MESSAGE=$2
    echo "$MESSAGE"
    exit $EXIT_CODE
}

# 1 Download Genomes
if ! perl ${GENOMES_SCRIPTS}/genomes_download.pl fetch -d ${DB_ROOT}/genomes/ -v 1
then
    abort 1 "genomes_download.pl failed"
fi






cd ${DB_ROOT}/genomes/current/distfiles/ensembl
# 1a. Check the Ensembl Genomes for Header Files with no sequences 
for i in `find -type f -name "*.gz"`; do perl ${GENOMES_SCRIPTS}/check_genome_ensembles.pl $i;  done
cd ${DB_ROOT}/genomes

# 2 Process DB Build and initialize DB 
echo "java -server -Xmx20G  -classpath ${GENOMES_SCRIPTS}/ genomes/Main   -u  -c ${GENOMES_SCRIPTS}/config/genomes_conf.txt >> ${GENOMES_LOG}/java_update_genomes_u.log"
java -server -Xmx20G  -classpath ${GENOMES_SCRIPTS}/ genomes/Main   -u  -c ${GENOMES_SCRIPTS}/config/genomes_conf.txt >> ${GENOMES_LOG}/java_update_genomes_u.log
# 3 Format DB Create data folder, unpack and process fasta files 
echo "java -server -Xmx10G  -classpath ${GENOMES_SCRIPTS}/ genomes/Main   -f  -c ${GENOMES_SCRIPTS}/config/genomes_conf.txt  >> ${GENOMES_LOG}/java_update_genomes_f.log"
java -server -Xmx10G  -classpath ${GENOMES_SCRIPTS}/ genomes/Main   -f  -c ${GENOMES_SCRIPTS}/config/genomes_conf.txt  >> ${GENOMES_LOG}/java_update_genomes_f.log
# 4 Create Genomes Tree
echo "java -server -Xmx10G -classpath ${GENOMES_SCRIPTS}/ genomes/Main    -w  -c ${GENOMES_SCRIPTS}/config/genomes_conf.txt   >> ${GENOMES_LOG}/java_update_genomes_w.log"
java -server -Xmx10G -classpath ${GENOMES_SCRIPTS}/ genomes/Main    -w  -c ${GENOMES_SCRIPTS}/config/genomes_conf.txt   >> ${GENOMES_LOG}/java_update_genomes_w.log
echo "genomes_runner.sh finished."
