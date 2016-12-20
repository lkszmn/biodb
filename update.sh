#!/bin/bash
# Used to make a new snapshot of the NCBI genomes and Ensembl database
# for the old Toolkit

# Author: Lukas Zimmermann
# Created on: 2016-07-28


###################################################################
## Set Paramters here ! 
###################################################################

# Which target file to use 
TARGET="targets/TARGET_OLD"

# The root directory of the genomes database
ROOT="/cluster/toolkit/production/databases/genomes_new"


###################################################################
## Validate Parameters
###################################################################
if [ ! -f ${TARGET} ] ; then

    echo "ERROR    The file ${TARGET} does not exist"
    exit 1
fi

if [ ! -d ${ROOT} ] ; then

    echo "ERROR    Root directory ${ROOT} does not exist"
    exit 1
fi    

#################################################
# Make a new Update directory
DEST="${ROOT}/`date \"+%Y_%m_%d__%H_%M_%S\"`"
 
#mkdir "${DEST}"
rm -f "${ROOT}/current"
ln -s "${DEST}" "${ROOT}/current"




# Start the biodb.pl program to assemble a list of file to be downloaded
# Mapping file allows to translate from species (scientific name in Ensembl)
# to the taxid from the NCBI taxonomy
perl biodb.pl list -d "${DEST}" -f "${TARGET}" -m "species_taxid.dat" 

# Download the listed files
perl biodb.pl fetch -d "${DEST}"


# Download the taxonomy needed by the Java code
mkdir "${DEST}/taxonomy"
cd "${DEST}/taxonomy"
wget --no-directories --passive-ftp "ftp://ftp.ncbi.nlm.nih.gov/pub/taxonomy/taxdump.tar.gz"
tar -xzf "taxdump.tar.gz"
rm "taxdump.tar.gz"
wget --no-directories --passive-ftp "ftp://ftp.ncbi.nlm.nih.gov/pub/taxonomy/gi_taxid_prot.dmp.gz"
gunzip "gi_taxid_prot.dmp.gz"
cd -





# Run the java stuff to format the databases and build the rhtml trees
#java -jar Genomes_update.jar -u -c config/genomes_conf.txt
#java -jar Genomes_update.jar -f -c config/genomes_conf.txt
#java -jar Genomes_update.jar -w -c config/genomes_conf.txt

