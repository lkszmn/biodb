#! /usr/bin/perl -w

#########################################################################################
#	Download all Genome Distribution files from NCBI and ensemble
#	Updated Version November 2011
#	0.1
#
#	Author: Jörn Marialke, MPI for Evolutionary Biology, Tuebingen 2011
#########################################################################################


use strict;
use warnings;
use Net::FTP;


my $usage = " Download Genomes from NCBI and Ensembl
Usage: perl genomes_download -d <directory> [-v]

Options: 
		-d <directory> 	Base Directory for the genomes data
		[-v]	<int>	Verbose mode 1 = Verbose 2 = Debug
 ";
my $options ="";
my $rootDir; ##     = "/home/joern/hn01/joern/genomes/";
#my $rootDir      = "/home/joern/development_toolkit/genomes_download/data/";
my $v=0;

# Processing command line options
if (@ARGV<1) {die $usage;}

for (my $i=0; $i<@ARGV; $i++) {
	$options.=" $ARGV[$i] ";
}

# Set options
if ($options=~s/ -d\s+(\S+) / /g)		{
	$rootDir=$1;
	print "MATCHES -d\n";
	print "ROOT DIR  ".$rootDir."\n";
}
if ($options=~s/ -v\s+(\d+) / /g)		{$v=$1;}

print "ROOT DIR  ".$rootDir."\n";


my @errors;

# Global Variable declaration
# NCBI Sources
my $ncbi                = "ftp.ncbi.nih.gov";
my $srcNCBIBacDir       = "/genomes/Bacteria/";
my $srcNCBIEukDir       = "/genomes/";
my $srcNCBIFunDir       = "/genomes/Fungi/";
my $srcNCBIFluDir  	= "/genomes/INFLUENZA/";
my $sleeptime           = 900;
# ENSEMBL
my $ensembl             = "ftp.ensembl.org";
my $srcEnsemblEukDir    = "/pub/current_fasta";

# Folder Structure Params 

my $timestamp = &getTimestamp;
my $baseDirRel   = $timestamp."/";
my $baseDir      = $rootDir.$baseDirRel;
my $distDir;
my $dataDir;
my $webDir;
my $logDir ;
my $ncbiDir;
my $ncbiEukDir;
my $ncbiFunDir;
my $taxDir;
my $ensemblDir;

# Setting up Logfile Parameters

my $logfile      = $baseDir."log/".$timestamp.".log";
my $wget_log     = $baseDir."log/".$timestamp."_wget.log";
my $formatdb_log = $baseDir."log/".$timestamp."_formatdb.log";


# sources for the taxonomy files
my @taxFiles;
$taxFiles[scalar(@taxFiles)] = "ftp://".$ncbi."/pub/taxonomy/taxdump.tar.gz";
$taxFiles[scalar(@taxFiles)] = "ftp://".$ncbi."/pub/taxonomy/gi_taxid_prot.dmp.gz";

# sources for the bacterial files
my @bacterialFiles;
# The gbk files only are needed for tool GI2Promoter. If that tool
# should not be provided any more, this line can be removed.
$bacterialFiles[scalar(@bacterialFiles)]= "ftp://".$ncbi."".$srcNCBIBacDir."all.gbk.tar.gz";
$bacterialFiles[scalar(@bacterialFiles)]= "ftp://".$ncbi."".$srcNCBIBacDir."all.faa.tar.gz";
$bacterialFiles[scalar(@bacterialFiles)]= "ftp://".$ncbi."".$srcNCBIBacDir."all.fna.tar.gz";

# sources for the Influenza files
my @influenzaFiles;
$influenzaFiles[scalar(@influenzaFiles)]= "ftp://".$ncbi."".$srcNCBIFluDir."influenza.faa.gz";
$influenzaFiles[scalar(@influenzaFiles)]= "ftp://".$ncbi."".$srcNCBIFluDir."influenza.fna.gz";



# Execution of scripnt
&create_folder_structure(); 
&getNCBITaxonomy(); 
&getNCBIBacteria();
&queryFtpFungi($ncbi, $srcNCBIFunDir, 1);
&queryFtpEucaryotaEnsembl($ensembl, $srcEnsemblEukDir);
&queryFtpEucaryotaNcbi($ncbi, $srcNCBIEukDir);
&getNCBIInfluenza();


#########################################################################################
# Create the new Folder structure for the downloaded genomes 
# This will be the same as in the old download script, we want to keep the .Jar file 
# still working
#
#########################################################################################
sub create_folder_structure(){
if($v>0){print "Creating Folder Structure\n" };


# the downloaded data, may be compressed *.gz
   $distDir      = $baseDir."distfiles/";

# $dataDir contains all formatted genomes
 $dataDir      = $baseDir."data/";
 $webDir       = $baseDir."web/";
 $logDir       = $baseDir."log/";
 $ncbiDir      = $distDir."ncbi/";
 $ncbiEukDir   = $ncbiDir."Eukaryota/";
 $ncbiFunDir   = $ncbiDir."Fungi/";
 $taxDir       = $baseDir."taxonomy/";
 $ensemblDir   = $distDir."ensembl/";


my $logfile      = $baseDir."log/".$timestamp.".log";
my $wget_log     = $baseDir."log/".$timestamp."_wget.log";
my $formatdb_log = $baseDir."log/".$timestamp."_formatdb.log";

my $link_name = "current";

### Folder Structure ####################################################################
#
#							<basedir = Timestamp>
#    _______________________________|_______________________________
#	|				|				|				|				|
# <logdir>		<distDir>		<dataDir>		<taxDir> 		<webDir>
# store logs		|							store tax *		store www stuff
#				____|____						
#			   |		 |	   		
#			<ensembl>  <ncbi> 
#  						 |
#						 |- Fungi +
#						 |- Eucaryota
#						 |- Bacteria  * 
#
#  * simple,single File Download
#  + traverse through NCBI Fungi folder and create for each org Folder + download .faa
#
#########################################################################################

#create directories and log file
if(!(-e $baseDir && -d $baseDir)){
	mkdir($baseDir,0755);
}
if(!(-e $logDir && -d $logDir)){
	mkdir($logDir,0755);
}

#### mkdir Dist and Subfolder (ncbi, ensemble)
if(!(-e $distDir && -d $distDir)){
	mkdir($distDir,0755);
	print "LINE 171 Building Directory : $distDir\n";
}
if(!(-e $ncbiDir && -d $ncbiDir)){
	mkdir($ncbiDir,0755);
	print "LINE 175 Building Directory : $ncbiDir\n";
}
if(!(-e $ncbiEukDir && -d $ncbiEukDir)){
	mkdir($ncbiEukDir,0755);
}
if(!(-e $ncbiFunDir && -d $ncbiFunDir)){
	mkdir($ncbiFunDir,0755);
	print "LINE 182 Building Directory : $ncbiFunDir\n";
}
if(!(-e $ensemblDir && -d $ensemblDir)){
	mkdir($ensemblDir,0755);
	print "LINE 186 Building Directory : $ensemblDir\n";
}
##################################################

#### mkdir Data Dir TODO: Find out where this is filled !
if(!(-e $dataDir && -d $dataDir)){
	mkdir($dataDir,0755);
}
#### mkdir Taxonomy Folder
if(!(-e $taxDir && -d $taxDir)){
	mkdir($taxDir,0755);
}
#### mkdir Web Directory
if(!(-e $webDir && -d $webDir)){
	mkdir($webDir,0755);
}

if($v>0){print "Finished Creating Folder Structure\n" };

	if(!(-e $rootDir.$link_name)){
 	 	symlink($baseDirRel,$rootDir.$link_name);
	}
	else{
		unlink($rootDir.$link_name);
		symlink($baseDirRel,$rootDir.$link_name);
	}
	
}


#########################################################################################
# Download the needeed Bacterial Data into the previously specified folder
#
#########################################################################################
sub getNCBIBacteria{
	if($v>0){print "Collecting NCBI Bacterial Data\n" };
    my $command = "wget --passive-ftp --timeout=60 --tries=20 --recursive --no-host-directories --timestamping --cut-dirs=1 --directory-prefix=".$ncbiDir." --append-output=".$wget_log;
    &getFiles(\@bacterialFiles, $command);
    foreach (@bacterialFiles){
	if(/ftp:\/\/.*?\/.+?\/(.+)$/){
	    my $file = $ncbiDir.$1;
	   if($v>0){print "Extracting $file\n"};
	    &extract($file);
	} 
    } 
    	if($v>0){print "Finished collecting NCBI Bacterial Data\n" };
}

#########################################################################################
# Download the needeed Influenza Data into the Eucaryota Folder (has to come last, all
# the folders have to be specified)
# distfiles/ncbi/Eucaryota
# 
#########################################################################################
sub getNCBIInfluenza{
	if($v>0){print "Collecting NCBI Influenza Data\n" };
    my $command = "wget --passive-ftp --timeout=60 --tries=20 --recursive --no-host-directories --timestamping --cut-dirs=1 --directory-prefix=".$ncbiEukDir." --append-output=".$wget_log;
    &getFiles(\@influenzaFiles, $command);
    foreach (@influenzaFiles){
	if(/ftp:\/\/.*?\/.+?\/(.+)$/){
	    my $file = $ncbiEukDir.$1;
	   if($v>0){print "Extracting $file\n"};
	    &extract($file);
	} 
    } 
    	if($v>0){print "Finished collecting NCBI Influenza Data\n" };
}



#########################################################################################
# Download the needeed Taxonomy Data into the previously specified foldedr
#
#########################################################################################
sub getNCBITaxonomy{
	if($v>0){print "Collecting NCBI Taxonomy Data\n" };
    my $command = "wget --passive-ftp --timeout=60 --tries=20 --recursive --no-host-directories --timestamping --cut-dirs=1 --directory-prefix=".$baseDir." --append-output=".$wget_log;
    &getFiles(\@taxFiles, $command);
    foreach (@taxFiles){
	if(/ftp:\/\/.*?\/.+?\/(.+)$/){
	    my $file = $baseDir.$1;
	   if($v>0){print "Extracting $file\n"};
	    &extract($file);
	} 
    } 
    	if($v>0){print "Finished collecting NCBI Taxonomy Data\n" };
}

#########################################################################################
# Utility Method to generate the timestamp for the genomes sub folder
#
#########################################################################################
sub getTimestamp{
    my($Second, $Minute, $Hour, $Day, $Month, $Year, $WeekDay, $DayOfYear, $IsDST) = localtime(time);
    $Year = $Year +1900;
    $Month++;
    return sprintf("%04u_%02u_%02u__%02u_%02u_%02u",$Year,$Month,$Day,$Hour,$Minute,$Second);
}

#########################################################################################
# Simple Download Method with additional Logging (Tries 10 times before quiting Job)
# (Christian Mayer)
#########################################################################################
sub getFiles(){
    
    my @files = @{ (shift) };
    my $command = shift;
    my $count = 1;
    my $state = -1;
    foreach my $src (@files){
	while($state!=0 && $count<11){
	    if($v>0){print "Fetching $src attempt $count\n"};
	    $state=system("$command $src");
	    $count++;
	    if ($state!=0) {
		# KFT: wait some time in hope to get connected to another file server
		wait4files("-()()-");
	    }
	}
	if($state==0){
	   if($v>0){print "Success downloading $src\n"};
	    $state = -1;
	    $count = 1;
	}else{
	   if($v>0){print "Error downloading $src\n"};
	   if($v>0){print "Cmd: $command $src\n"};
	   $errors[scalar(@errors)]="Error downloading $src";	
	}
    }
}

#########################################################################################
# Extract with tar as system command
# (Christian Mayer)
#########################################################################################
sub extract{
    my $file = shift;
    my $C = ".";
    if( $file =~/^(\/.+\/).+$/ ){ $C=$1; }   
    my $res;
    if( $file =~ /^(.+).tgz$/  or  $file =~ /^(.+).tar.gz$/ ){
	$res = system("tar -C $C -xzf $file");
    }elsif( $file =~ /^(.+).gz$/ ){
	$res = system("gzip -d -c $file > $1");
    }
    if ($res != 0) {
	die "Could not extract $file. Return code is $res.\n";
    }
    # KFT 2014: That result is never checked.
    #return $res;
}


#########################################################################################
# Connect with FTP Server and list all files in Folder
# This is only Valid for FUNGI Data (simpler Folder structure than Eucaryota) 
#
# Params : 
#			$ftp_site 			String  <ncbi / ensemble>
#			$ftp_directory		String  <parent directory>
#			$ftp_query_folder	Integer <Depth of folder traversal> default = 1 (anticipated)
#
# Output : TO BE DEFINED
#
# Author : Jörn Marialke, 2011
#########################################################################################
sub queryFtpFungi{
	
	# Init local variables
	my $ftp_site      		= $_[0];
	my $ftp_directory   	= $_[1];
	my $ftp_query_folder	= $_[2];
	my $ftp = &startftp($ftp_site, $ftp_directory);

	# get the listing for current site
	my @listing = $ftp->dir   or die "Could not generate Listing (probably directory $ftp_directory is empty)\n";
 	#printDir(@listing);
	foreach(@listing){
				
		my @folder = split(/\s+/, $_);
		# The Organism Name is $folder[8]
		
		$ftp->cwd($ftp_directory.$folder[8]) or next;
		my @fungi_data_dir = $ftp->dir   or die "Could not generate Listing\n";
		
		print "===============================================================\n";
		my $localFunSpeciesDir = $ncbiFunDir.$folder[8]."/";
		print "Creating Folder  $localFunSpeciesDir \n";
		
		if(!(-e $localFunSpeciesDir && -d $localFunSpeciesDir)){
			mkdir($localFunSpeciesDir,0755);
		}
		foreach(@fungi_data_dir){
			my @data_folder = split(/\s+/, $_);
			# If we find a .faa file Download it to the dist/ncbi/Fungi/<Species> Dir
			if($data_folder[8]=~ /.faa/){
				print "$data_folder[8] ->  $localFunSpeciesDir$data_folder[8]        \n";
				
				# Get the speciefied File and download it
				$ftp->binary();
				$ftp->get($data_folder[8],"$localFunSpeciesDir$data_folder[8]")or die "cannot get",$ftp->message;
			}
		}
		
		print "===============================================================\n";
		$ftp->cwd($ftp_directory); 
		
		
	}
	

	#print "Dir Size scalar(@listing)\n";
    print 'Remote directory is: ' . $ftp->pwd() . "\n"; 
    $ftp->quit();
	
	
}

#########################################################################################
# Connect with FTP Server and list all files in Folder from NCBI 
# This is only Valid for Eucaryota Data 
#
# Params : 
#			$ftp_site 			String  <ncbi / ensemble>
#			$ftp_directory		String  <parent directory>
#
# Output : TO BE DEFINED
#
# Author : Jörn Marialke, 2011
#########################################################################################
sub queryFtpEucaryotaNcbi{
	# Init local variables
	my $ftp_site   		= $_[0];
	my $ftp_directory   	= $_[1];
	my $ftp = startftp($ftp_site, $ftp_directory);
	
	# get the listing for current site  (All Organisms are foldernames)
 	my @listing = $ftp->dir   or die "Could not generate Listing (probably directory $ftp_directory is empty)\n";
  	
  	# Traverse through each Genome Folder
  	foreach(@listing){
  		
  		my @genome_name  = split(/\s+/, $_);
  		my $genome_name = $genome_name[8];
  		
  		# Enter the Genome Directory
  		$ftp->cwd($ftp_directory."/".$genome_name) or next;
  		# get the listing of Folders that contain certain subfolders
 		my @genome_dirs = $ftp->dir or print "Empty directory: $ftp_directory/$genome_name.\n";
 		################# Create the Folder Structure and Download data For Protein Conatining Organisms #######################
  		if(&contains("protein",@genome_dirs)){
  			print "$genome_name contains Protein folder \n";
  			$ftp->cwd($ftp_directory."/".$genome_name."/protein") or print "Cannot enter Directory $genome_name/protein/ $ftp->message \n"; 
  			my $ncbiEucaryotaGenomeDir = $ncbiEukDir."/".$genome_name;
 			my $ncbiEucaryotaGenomeProtDir = $ncbiEukDir."/".$genome_name."/protein";
  			my @prot_files = $ftp->dir or print "Protein directory $ftp_directory/$genome_name/protein is empty.\n";
			# we have found out that we have downloadable file, so create the folder  
  			if(&contains("\.fa\.gz|\.faa", @prot_files)){

  				print "Contains downloadable .faa / .fa.gz File \n";
  				# Create the folder
  				if(!(-e $ncbiEucaryotaGenomeDir && -d $ncbiEucaryotaGenomeDir)){
					mkdir($ncbiEucaryotaGenomeDir,0755);
				}
				# Create the protein subfolder
				
  				if(!(-e $ncbiEucaryotaGenomeProtDir && -d $ncbiEucaryotaGenomeProtDir)){
					mkdir($ncbiEucaryotaGenomeProtDir,0755);
				}
				# Download the protein file into the newly created Directory 
				foreach(@prot_files){
  					my @fasta_file = split(/\s+/, $_);
  					if($fasta_file[8] =~ /protein.fa/){
  						print "L489 ---> $fasta_file[8] \n";
  						$ftp->binary();
  						$ftp->get($ftp_directory."/".$genome_name."/protein/".$fasta_file[8],"$ncbiEucaryotaGenomeProtDir/$fasta_file[8]")or die "cannot get",$ftp->message;
  					}
				}
  			}	
  		}
 ############# END get Protein Containing Data Genomes #################################################################### 		
  		elsif(&contains("CHR_",@genome_dirs)){
      		print "L498 Contains Chromosomes \n";
      		
      		####  Create the Organism Folder #####
      		my $ncbiEucaryotaGenomeDir = $ncbiEukDir."/".$genome_name;			
  			if(!(-e $ncbiEucaryotaGenomeDir && -d $ncbiEucaryotaGenomeDir)){
					mkdir($ncbiEucaryotaGenomeDir,0755);
				}
      		
      		#### Enter the folder and list all Chromosome Folders
      		$ftp->cwd($ftp_directory."/".$genome_name) or print "Cannot enter Directory $genome_name $ftp->message \n"; 
      		my @chromosome_listing =  $ftp->dir or  print "directory $ftp_directory/$genome_name is empty.\n";
      		foreach(@chromosome_listing){
      			my @dirName = split(/\s+/, $_);
      			### Get only the Chromosome Directories 
      			if($dirName[8]=~ /CHR_/){
      				####  Create the ChromosomeFolder #####
      				my $ncbiEucaryotaChromosomeDir = $ncbiEucaryotaGenomeDir."/".$dirName[8];			
  					if(!(-e $ncbiEucaryotaChromosomeDir && -d $ncbiEucaryotaChromosomeDir)){
						mkdir($ncbiEucaryotaChromosomeDir,0755);
					}
      				#### Now copy from each Chromosome Folder the .faa file to the new subfolder
      				$ftp->cwd($ftp_directory."/".$genome_name."/".$dirName[8]) or print "Cannot enter Directory $genome_name/$dirName[8] $ftp->message \n"; 
      				my @file_listing =  $ftp->dir or  print "Chromosome folder $ftp_directory/$genome_name/$dirName[8] is empty.\n";
      				foreach(@file_listing){
      					my @fasta_file = split(/\s+/, $_);
      					if($fasta_file[8]=~ /.faa/ ){
      						$ftp->get($ftp_directory."/".$genome_name."/".$dirName[8]."/".$fasta_file[8],"$ncbiEucaryotaChromosomeDir/$fasta_file[8]")or die "cannot get",$ftp->message;
      					}    					
      				}
 					### Go back to the Genome Folder     				
      				$ftp->cwd($ftp_directory."/".$genome_name) or print "Cannot enter Directory $genome_name $ftp->message \n"; 
      				print "LINE 529 --> $dirName[8]\n";
				
      			
      			}
      		}
      		  		
  		}
  		
  		# Leave Genome Directory
  		$ftp->cwd($ftp_directory)|| die "LINE 538 Cannot exit Directory $ftp->message \n"; 		
  	}
  	
	 $ftp->quit();   
}

#########################################################################################
# Connect with FTP Server and list all files in Folder from Ensembl
# This is only Valid for Eucaryota Data 
#
# Params : 
#			$ftp_site 			String  <ncbi / ensemble>
#			$ftp_directory		String  <parent directory>
#
# Output : TO BE DEFINED
#
# Author : Jörn Marialke, 2011
#########################################################################################
sub queryFtpEucaryotaEnsembl{
	
	
	# Init local variables
	my $ftp_site      	= $_[0];
	my $ftp_directory   	= $_[1];
	my $ftp = &startftp($ftp_site, $ftp_directory);
	
	# get the listing for current site  (All Organisms are foldernames)
 	my @listing = $ftp->dir   or die "Could not generate Listing (directory $ftp_directory is empty)\n";
 	# Go into each organism directory, we will go directly to the pep Tag !
 	foreach(@listing){
 		my @folder = split(/\s+/, $_);
 		print "-->L411: Organism  $folder[8]  \n";
 		if($folder[8]=~ /saccharomyces_cerevisiae/){
 			print "LINE 589 Excluding Saccharomyces_cerviziae !\n";
 			next;
 		}
 		$ftp->cwd($ftp_directory."/".$folder[8]) or next; 
 		 	
 		 	# Check if we have a valid peptide Folder 
 			my @pep_listing = $ftp->dir;
 			if(&containsPEP(@pep_listing)){
 				# create the dist/ensembl/<organism> Directory
 				my $organism_ensembl_dir = $ensemblDir."/".$folder[8];
				if(!(-e $organism_ensembl_dir && -d $organism_ensembl_dir)){
					mkdir($organism_ensembl_dir,0755);
				}
 				# create the dist/ensembl/<organism>/pep/ Directory
 				$organism_ensembl_dir = $ensemblDir."/".$folder[8]."/pep";
				if(!(-e $organism_ensembl_dir && -d $organism_ensembl_dir)){
					mkdir($organism_ensembl_dir,0755);
				}
 				
 				# Go to ftp <organism>/pep directory and download the .all.fa.gz File
 				$ftp->cwd($ftp_directory."/".$folder[8]."/pep/")|| die "Cannot change to directory $ftp_directory/$folder[8]/pep/ : $ftp->message\n"; 
 				my @locallisting = $ftp->dir;
 				foreach(@locallisting){
 					my @fasta_file = split(/\s+/, $_);
 					if($fasta_file[8]=~ /all.fa.gz/){
 						print "---> $fasta_file[8]\n";
 						# Get the speciefied File and download it
 						$ftp->binary();
						$ftp->get($fasta_file[8],"$organism_ensembl_dir/$fasta_file[8]")or die "cannot get $fasta_file[8]",$ftp->message;
 						
 					}
 				}
 				
 			}
 		
 		$ftp->cwd($ftp_directory)|| die "Cannot change directory : $ftp->message\n"; 
 	}
 	
    $ftp->quit();   
	
}

#########################################################################################
# Utility Method to test if Dir contains a pep directory
# Input  : ftp->dir
# Output : 0 = no dir, 1 = contains pep dir
#########################################################################################
sub containsPEP(){
	my @data = @_;
	my $out = 0;
	foreach (@data){
		my @folder = split(/\s+/, $_);
		if($folder[8]=~ /pep/){
			$out = 1;
		}
	}
	return $out;
}

sub link_to_current(){
	
}


#########################################################################################
# Utility Method to test if Dir contains a  directory
# Input  : dir name
# Input  : ftp->dir
# Output : 0 = no dir, 1 = contains pep dir
#########################################################################################
sub contains(){
	my $name = shift;
	my @data = @_;
	my $out = 0;
	foreach (@data){
		my @folder = split(/\s+/, $_);
		if($folder[8]=~ /$name/){
			$out = 1;
		}
	}
	return $out;
}

sub printDir(){
	my @data = @_;
	
	foreach (@data){
		print $_."\n";
	}
	
	
}

#########################################################################################
# Utility Method establish an ftp connection
# Input  : ftp site
#          ftp directory
# Output : ftp connection
#########################################################################################
sub startftp() {
    my $ftp_site = shift();
    my $ftp_directory = shift();
    my $ftp;
    my $state = 0;
    my $max_try_count = 10;
    my $try_counter = 0;
    if ($v > 0) {
	print "FTP SITE: $ftp_site\n";
    }

    while (!$state && $try_counter < $max_try_count) {
	# Create an anonymous access to the ftp server
	$ftp = Net::FTP->new("$ftp_site", Passive => 'true', Timeout => 60, Debug => 0);
	if (!$ftp) {
	    $try_counter++;
	    &wait4files("Could not connect to $ftp_site: $@\n");
	    next;
	}

	$ftp->login("anonymous", "none\@none.com") or die "Could not login to server: ", $ftp->message;

	# Access the parent directory of interest
	if (!($ftp->cwd($ftp_directory))) {
	    # wait for another file server
	    $try_counter++;
	    &wait4files("Cannot change working directory: ".$ftp->message."\n");
	    next;
	}

	$state = 1;
    }
	
    if ($try_counter >= $max_try_count) {
	die "Could not establish ftp connection\n";
    }

    return $ftp;
}

#########################################################################################
# Utility Method to wait some time to get a more useful file server
# Input  : message to print while waiting
#########################################################################################

sub wait4files() {
    my $message=shift();
    print $message;
    sleep($sleeptime);
}
