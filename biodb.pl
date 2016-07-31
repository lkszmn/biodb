#! /usr/bin/perl -w

#########################################################################################
#	Download all Genome Distribution files from NCBI
#	Updated Version July 2016
#
#   Author: Lukas Zimmermann
#########################################################################################
use strict;
use warnings;
use Cwd;
use Net::FTP;	
use Data::Dumper;
use Getopt::Long;
use File::Path;
use List::Util;
use File::Copy;
use File::Fetch;
use File::Find qw(find);

# TODOS 
# Abstract ENSEMBL sequence_type
# Make CHECKSUM retrieval faster
# Add --no--checksum flag to list and fetch
# Implement the --max-items flag to limit the number of files downloaded
# per block
#

#########################################################################################
# Global variables
#########################################################################################
# Modes supported by the program
my @modes = ('info', 'list', 'fetch', 'format'); # Which modes this script supports
my $ncbi  = "ftp.ncbi.nih.gov";    # Hostname of NCBI FTP server
  
my $baseDir; #TODO Remove this variable


# Hashes a tuple describing two selector names to a respective hash
my @mappings; 


#########################################################################################
# Ensembl  TODO Replace by Hash
#########################################################################################

# The hostname of the Ensembl FTP server
my $ensembl = "ftp.ensembl.org";

# Name of the checksum file for ensembl data
my $ensemblChecksumFile = "CHECKSUMS";
my $ensemblChecksumType = "sum";

# In which subdirectories the individual sequence_types can be found
my %ensembl_sequence_type_loc = (
    "cdna"  => "cdna",
    "cds" => "cds",
    "dna"  => "dna",
    "dna_rm" => "dna",
    "dna_sm" => "dna",
    "ncrna" => "ncrna",
    "pep" => "pep"
);
my @ensembl_selectors = ("species", "assembly", "sequence_type", "status", "id_type", "id");

#########################################################################################
# NCBI
#########################################################################################
my $ncbiChecksumFile = "md5checksums.txt";
my $ncbiChecksumType = "md5";

#########################################################
# Some constants
my $a_s_delim = '\t';
     


# The name of the file where the filtered organism names are stored
my $organismFileName = "sources.dat";
my $selectorsFileName = "selectors.dat";
my $checksumsFileName = "checksums.dat";
my $wget_log;
my ($index_of, $allowed_values);

# Change these variables in case of changes to the server URL
# Global Variable declaration
# NCBI Sources
my $srcNCBIBacDir       = "/genomes/refseq/bacteria/";
my $srcNCBIEukDir       = "/genomes/";
my $srcNCBIFunDir       = "/genomes/refseq/fungi/";
my $srcNCBIFluDir  	= "/genomes/INFLUENZA/";
my $sleeptime           = 900;


my $v = 1;

# Will hold the connection to the FTP 
my $ftp;
my $ftp2;


# User has not provided any subroutine
if(@ARGV < 1) {

    die "ERROR    Please specify one of the subroutine {'info', 'list', 'fetch', 'format'}";
}

# What the program was asked to do
my $mode = $ARGV[0];
my $rootDir;

if(not grep /^$mode$/, @modes) {

    die "ERROR    Unknown Subroutine '$mode'";
}
#########################################################################################
# MODE is Info, alllows do display some general infos without any specifcations
#########################################################################################
if ($mode eq "info") {

    if(@ARGV < 2) {

        die "ERROR   Please specify one of {'attribute'}\n"
    }
    if($ARGV[1] eq "attributes") {
	
        $ftp  = Net::FTP->new($ncbi, Debug => 0) or die "Cannot connect to $ncbi: $@";
        $ftp->login("anonymous",'-anonymous@');
        ($index_of, $allowed_values) = getAssemblyHeaders(); 
        while (my ($key, $value) = each %$allowed_values) {
          
            my @value_deref = @$value;
            if(scalar @value_deref == 0) {

                print "$key=*\n"; 
            } else {

                print "$key=",  join(",",@value_deref), "\n"; 
            }
         }
          $ftp->close;
    } else {

	    print "Currently not supported\n";
    }
   exit;
} 
#########################################################################################
# EXIT INFO
#########################################################################################

       
#########################################################################################
# Download the needed Taxonomy Data into the previously specified 'taxonomy' directory
# For this, the user must have specified the '--taxonomy' switch
#########################################################################################
#if($taxonomy) { 
#    mkdir("$baseDir/taxonomy", 0755);
#    my @taxFiles = ("ftp://".$ncbi."/pub/taxonomy/taxdump.tar.gz",
#                    "ftp://".$ncbi."/pub/taxonomy/gi_taxid_prot.dmp.gz");
#    printv("Collecting NCBI Taxonomy Data\n");
#    getFiles(\@taxFiles, "wget --passive-ftp --timeout=60 --tries=20 --recursive --no-host-directories --timestamping --cut-dirs=1 --directory-prefix=".$baseDir." --append-output=".$wget_log);
#    foreach (@taxFiles){
#        if(/ftp:\/\/.*?\/.+?\/(.+)$/) {
#            my $file = $baseDir.$1;
#	    printv("Extracting $file\n");
#	    extract($file);
#        } 
#    } 
#    printv("Finished collecting NCBI Taxonomy Data\n");
#
#} else { 
#
#  printv("Skipping taxonomy, as  '--taxonomy' was not specified.\n");
#}
#if(scalar @blocks == 0) {
#
#    print "No blocks encountered in targets file, terminating.\n";
#    exit;
#}


#########################################################################################
#   LIST MODE, write .dat files with URLS, further information and checksum
########################################################################################
if($mode eq "list") {
	
my $d = '';
my $f = '';
my $m = '';
GetOptions('d=s' => \$d,
           'f=s' => \$f,
           'm=s' => \$m);

# Parse Mapping file
if($m) {
    die "ERROR    Mapping file '$m' does not exist.\n" if(! -f $m);
    
    printv("Mapping file provided: $m\n");
    readMapping($m);
}



if(not $d) {

    die "ERROR    Select a directory to assemble your files with '-d'\n";
}

# If the directory does not exist, fail if no target file was specified
if(not -d "$d" and not $f) {

    die "ERROR    Directory '$d' does not exist and no target file was specified with '-f'\n";
}

# Create the rootdirectory or die if not possible
if(not -d "$d") {
    mkdir("$d", 0755) or die "Could not create root directory '-d'.\n";
}
$rootDir = $d . '/';

# Check whether there already exists a blocks file in the root directory, if yes, prefer it, otherwise
# use the pne provided by '-f' and copy this over
	
	
my $blocks;
if(-f "${rootDir}BLOCKS" and $f) {
   
    print "WARNING    BLOCKS file found in root directory. Your provided file will be preferred.\n";
    copy("$f", "${rootDir}BLOCKS");
} else {

    if(not $f) {

        die "ERROR    The directory you specified does not contain a BLOCKS file and you did not provide one with '-f'. Terminating.\n"
    }
    if(not -f $f) {

        die "ERROR    The path you specified with '-f' is not a file. Terminating.\n"
    }
    # Copy the BLOCKS File over to the new root directory
    copy("$f", "${rootDir}BLOCKS");
}
$blocks = "${rootDir}BLOCKS"; 
printv("Using BLOCKS file $blocks\n");

# Parse targets file and assemble blocks array.\
my @blocks;
my $databaseName = '';
my $domainName;
my $inBlock = 0;
my $filterString;
my $extractTo = '';
my $groupBy = '';
my $makeBLASTDB = 0;
my $type = '';
open(FILE1, $blocks);

while(<FILE1>) {
	
    # Chop away comments
    $_ = (split '#', $_)[0];

    # Block has ended
    if($_ =~ /^\s*$/) {
        if(not $inBlock) {
           next;
    }
    $inBlock = 0;
	if(not $databaseName) {
            die "ERROR  Database name is missing in block entry of target file. Terminating.\n";

    } 
    if($filterString) {
        substr($filterString, 0, 1) = "";
    }
    if(not $extractTo) {

        $extractTo = "/$databaseName/$domainName";
    }
    push @blocks, {databaseName => $databaseName, 
                   domainName => $domainName, 
                   extractTo => $extractTo,
                   groupBy => $groupBy,
                   makeBLASTDB => $makeBLASTDB,
                   type => $type,
                   selector => parseSelectorString($filterString)};

	$databaseName = '';
    $domainName = '';
    $filterString = '';
    $extractTo = '';
    $groupBy = '';
    $type = '';
    $makeBLASTDB = 0;

      # Database specifier encountered
    } elsif($_ =~ /\s*DATABASE\s*=\s*([^\s]+)\s+/) {
        $inBlock = 1;
	    $databaseName = $1;

      # Domain specifier encountered
    } elsif($_ =~ /\s*DOMAIN\s*=\s*([^\s]+)\s+/) {
        $inBlock = 1;
        $domainName = $1;

      # Extract_To Line encountered
    } elsif($_ =~ /\s*EXTRACT_TO\s*=\s*([^\s]+)\s+/) {      

       $inBlock = 1;
       $extractTo = $1;

      # GROUP_BY encountered
    } elsif($_ =~ /\s*GROUP_BY\s*=\s*([^\s]+)\s+/) {
        $inBlock = 1;
        $groupBy = $1;

      # MAKEBLASTDB Encountered
    } elsif($_ =~ /^\s*MAKEBLASTDB\s*$/) {      
       $inBlock = 1;
       $makeBLASTDB = 1;
      
      # TYPE Encountered
    } elsif($_ =~ /\s*TYPE\s*=\s*([^\s]+)\s+/) {      
       $inBlock = 1;
       $type = $1;
      
       # Filter string line
    } elsif($_ =~ /\s*([A-Za-z_]+)\s*=\s*([A-Za-z_][A-Za-z_ ,]*[A-Za-z_])\s*/) {

         $filterString .= ":$1=$2";
     } else {

         die "ERROR  Strange line detected in target file: $_";
     }
}
if($inBlock) {

    if(not $databaseName) {
            die "ERROR  Database name is missing in block entry of target file. Terminating.\n";

    } 
    substr($filterString, 0, 1) = "";

    if(not $extractTo) {

        $extractTo = "/$databaseName/$domainName";
    }
    push @blocks, {databaseName => $databaseName, 
                   domainName   => $domainName,
                   extractTo => $extractTo,
                   groupBy => $groupBy,
                   makeBLASTDB => $makeBLASTDB,
                   type => $type,
                   selector => parseSelectorString($filterString)};
}
close(FILE1);







# Sort blocks by databases, so we do not have to switch ftp Server so often
@blocks = sort { $a->{databaseName} cmp $b->{databaseName} } @blocks;
$databaseName = '';
while(@blocks) {        
	
	# Get Block and bring it into hash context        
    my %block = %{shift @blocks};
            
    # Database name has changed, connect to new Server 
    if($databaseName ne $block{databaseName}) {
            
       $databaseName = $block{databaseName};
       connectToServer($databaseName);
    }
       
    if($databaseName eq "refseq" || $databaseName eq "genbank") {

          listNCBI($databaseName, $block{domainName}, $block{selector}, $block{extractTo},  $block{groupBy});

    } elsif($databaseName eq "ensembl") {
            
        listEnsembl($block{selector}, $block{extractTo},  $block{groupBy}, $block{type});

    } else {

        die "ERROR   Unsupported Database\n";
    }  
}

#########################################################################################
# Download all files within the organisms files and make checksum test
########################################################################################
} elsif($mode eq "fetch") {
	
my $d = '';
my $no_extract = '';
my $max_items = '';
GetOptions('d=s' => \$d,
           'no-extract' => \$no_extract,
           'max-items=i' => \$max_items); # Limits the number of items that are downloaded per block
         
die "ERROR    Select a directory to assemble your files with '-d'\n" if(not $d);
die "ERROR    $d is not a directory\n" if(not -d $d);
               
$rootDir = $d;
printv("NOTE    Files will not be extracted\n") if ($no_extract);

# Go through all sources files with unlimited depth
foreach(list_dirs(makePathAbsolute($rootDir))) {
	
	$_ =~ /(\/.*)*\/$organismFileName/;
	chdir($1) or die "Cannot change Dir to $1";
	open(my $fh, "<", $organismFileName);
    my $id_counter = 0;

	while(<$fh>) {
		
		# Make Directory structure and download file
		my @spt =  split /\s+/, $_;
		mkdir($spt[0], 0755);
        chdir($spt[0]);

        # Skip if the file has already been downloaded
        if(-f "${id_counter}_DONE") {
            print "EXIST    $spt[1]\n";
        # Restart this file, delete possible fragments
        } else {
            unlink glob "$spt[1]*";
            printv("DOWNLOAD    $spt[1]\n");
  		    if(0 == system("wget --no-directories --passive-ftp $spt[1] -o /dev/null")) {

                # Extract file if necessary
				if(not $no_extract) {
                	extract("$spt[1]", 1);
			    }     
                system("touch ${id_counter}_DONE");
            } else {
                print("WARNING    Download of file $spt[1] failed!\n");
            }
        }
        
        chdir("..");
        $id_counter++;
        
        # Prevent further iteration if max items given
        if($max_items and $max_items == $id_counter) {	
			printv("NOTE    Option '--max-items $max_items' prevented the download of further items for this $organismFileName file\n");
			last;
		}
        
	}
	close($fh);
}
printv("All listed files have been downloaded successfully.\n");

exit	

#########################################################################################
# FORMAT TODO Implement me
#########################################################################################
} elsif($mode eq "format") {

   # print "Need to execute /usr/bin/makeblastdb -dbtype nucl -out /tmp/target -in test.fna -parse_seqids\n";
    
    my $d = '';
    GetOptions('d=s' => \$d);

	die "ERROR    Select a directory to assemble your files with '-d'\n" if(not $d);
	die "ERROR    $d is not a directory\n" if(not -d $d);
    $rootDir = $d;
   
    foreach(map {getcwd . "/$_"}list_fasta($rootDir)) {

	 # The call to makeBLASTDB is currently hard-coded
	 print "$_"; 
	

    }     
}
#########################################################################################
# Core Routines
#########################################################################################

#########################################################################################
# Parses the README_assembly_summary file and returns a hash with the headers as keys
# and the allowed values as a list 
# (Lukas Zimmermann)
#########################################################################################
sub getAssemblyHeaders {

    $ftp->cwd("/genomes");
    my %index_of;
    my $handle = $ftp->retr("README_assembly_summary.txt");
    my $current_heading;
    my %allowed_values;
    my $values_follow = 0;
    my $indent;
    while(my $line = <$handle>) {
     
         if($line =~ /Column/) { 
		
     	      $line =~ /Column\s+(\d+):\s+"(.*)"/;
              $index_of{$2} = $1 - 1;
              $current_heading = $2;
              $allowed_values{$current_heading} = [];

          } elsif($line =~ /\s+Values:.*/) {
              $values_follow = 1;

          } elsif($values_follow == 1) {
          
              $line =~ /(\s+)([A-Za-z ]+)(\s+-\s+)[a-zA-z ]+/;
              $indent = length($1) + length($2) + length($3);
              push @{$allowed_values{$current_heading}}, trim($2);
              $values_follow = 2;

          } elsif($values_follow == 2 && $line =~ /(\s+)[A-Aa-z- ]+/    ) {

              if(length($1) == $indent)   {
                  next;
        
          } elsif($values_follow == 2 && $line =~ /\s+([A-Za-z ]+)\s+-\s+[a-zA-z ]+/) {
      
              push @{$allowed_values{$current_heading}}, trim($1);
          }
       } 
    }
   $ftp->abort;
   close($handle);
   return (\%index_of, \%allowed_values);
}


#########################################################################################
# This routine lists files for download for the NCBI databases 'refseq' and 'genbank'
#########################################################################################
sub listNCBI {

    my $dbName = shift; 
    my $domainName = shift;
    my $selector = shift;
    my $extractTo = shift;
    my $groupBy = shift;
    mkpath("$rootDir$extractTo",0, 0755);
    
    print "Fetching URL from Database $dbName with domain $domainName\n";
     
    # Change to the correct directory and check whether species exists
    $ftp->cwd("/genomes/$dbName/$domainName") or die "ERROR  Could not change in directory 'genomes/$dbName/$domainName'\n";
   
    # Read the assembly summary file
    my $handle = $ftp->retr("assembly_summary.txt");
    open(my $fh1, ">>", "$rootDir$extractTo/$organismFileName")
    	or die "Can't open > $rootDir$extractTo/$organismFileName: $!\n";
    open(my $fh2, ">>", "$rootDir$extractTo/$selectorsFileName")
    	or die "Can't open > $rootDir$extractTo/$selectorsFileName: $!\n";
    open(my $fh6, ">>", "$rootDir$extractTo/$checksumsFileName")
    	or die "Can't open > $rootDir$extractTo/$checksumsFileName: $!\n";	

    while(my $line = <$handle>) {

        #ignore comment line
        if($line =~ /^\s*#/) {
            next;
        }
        my @spt = split $a_s_delim, $line ;
        my $passed = 1;
        # Check whether lines matches selector String
        while (my ($header, $allowed_here) = each %{$selector}) {
              
              if(not grep  /^$spt[$index_of->{$header}]$/, @$allowed_here) {
                  $passed = 0;       
              }
         }        
         if($passed) {   
                my $ftpPath = $spt[19];
                
               	#my $checksumFile = $ftpPath . "/$ncbiChecksumFile";
               	#$checksumFile =~ s/ftp:\/\/ftp.ncbi.nlm.nih.gov// ;
                #$checksumFile = ncbiReadChecksum($checksumFile);
        
                # Write the complete file name to the output file
                $ftpPath =~ /(\/.*)*\/(.*)/;
                my $fileName = "$2_protein.faa.gz";
                $ftpPath = $ftpPath . "/$fileName";    
                my $write = $spt[$index_of->{$groupBy}];
                print $fh1 "$write\t$ftpPath\n";

                # Assemble selector string part
                foreach(@spt) {
                    $write = $_;

                    print $fh2 "$write\t";
                }       
                
                # Only download genomic fna for bacteria
                if($domainName eq "bacteria" || $domainName eq "archaea") {
					# Write the complete file name to the output file
					$ftpPath = $spt[19];
					$ftpPath =~ /(\/.*)*\/(.*)/;
					$fileName = "$2_genomic.fna.gz";
					$ftpPath = $ftpPath . "/$fileName";  
					$write = $spt[$index_of->{$groupBy}];  
					print $fh1 "$write\t$ftpPath\n";

					# Assemble selector string part
					foreach(@spt) {
						$write = $_;

						print $fh2 "$write\t";
					} 
				}
				  
					#print $fh6 "$ncbiChecksumType $checksumFile->{$fileName}\n";
			}
  }
  $ftp->abort;
  close($handle);
  close($fh1);
  close($fh2);
  close($fh6);
}
#########################################################################################
# This routine lists the files to download for the database 'ensembl'
# (Lukas Zimmermann)
#########################################################################################
sub listEnsembl {

    my $selector = shift;
    my $extract_to = shift;
    my $group_by = shift;


	# TODO Will be removed on implemented blocks validation
	if(not grep /$group_by/, @ensembl_selectors){

		die "GroupBy Selector not allowed\n"
	}	
    my $type  = shift;
    
    print "Fetching URL from Database ensembl type $type\n";
    
    mkpath("$rootDir$extract_to",0, 0755);

    # Look which sequence_typess have been specified
    my @sequence_types;
    if(exists $selector->{sequence_type}) {
    
        @sequence_types = @{$selector->{sequence_type}};
    } else {

        @sequence_types = ('cdna', 'cds', 'dna', 'dna_rm', 'dna_sm', 'ncrna', 'pep');
    }
    # Note that the organism file is opened for appendin
    open(my $fh3, ">>", "$rootDir$extract_to/$organismFileName")
    	or die "Can't open > $rootDir$extract_to/$organismFileName: $!\n";
    open(my $fh4, ">>", "$rootDir$extract_to/$selectorsFileName")
    	or die "Can't open > $rootDir$extract_to/$selectorsFileName: $!\n";
    open(my $fh5, ">>", "$rootDir$extract_to/$checksumsFileName")
    	or die "Can't open > $rootDir$extract_to/$checksumsFileName: $!\n";
   
    if($type eq "FASTA") {
        
        $ftp->cwd("/pub/current_fasta/") or die "Could not change directory\n"; 
        
        # Recursively go through all the species
        foreach($ftp->ls) {
            my $species = $_;
            $ftp->cwd($_);
            foreach(map {$ensembl_sequence_type_loc{$_}} @sequence_types) {

                my $type = $_;
                $ftp->cwd($_);
                
                # Read the checksum file
                #my $checksum  = ensemblReadChecksum($ensemblChecksumFile);
                
                # For each file
                foreach($ftp->ls) {
                    
                    # Assume that we have passed    
                    my $passed = 1;
                    if($_ =~ /.*fa\.gz$/) {
                        
                        my $file_decomp =ensemblBreakFile($_);

                        # Check whether file matches selector string
                       while (my ($header, $allowed_here) = each %{$selector}) {
                           if(exists $file_decomp->{$header} and defined $file_decomp->{$header} and not grep /^$file_decomp->{$header}$/, @$allowed_here) {

                                 $passed = 0;                              
                            }
                       }        
                        if($passed) {   
                             
                             #TODO Write Selector fILE
                             my $ftpPath = "$ensembl/pub/current_fasta/$species/$type/$_";
                             my $write = $file_decomp->{$group_by};
                             print $fh3 "$write\t$ftpPath\n";
                             #print $fh5 "$ensemblChecksumType $checksum->{$_}\n";
                        }
                    }            
                } 
                $ftp->cwd("..");
            }
            $ftp->cwd("..");
        } 
    }
  $ftp->abort;
  close($fh3);
  close($fh4);
  close($fh5);
}

#########################################################################################
# Parses the selector String and returns a hash with the headers as keys and
# the valued within arrays
#########################################################################################
sub parseSelectorString {
   
  my $res = shift;
  if(not $res) {
    return {};
  }  

  my %headers;
  foreach(split '\s*:', $res) {
        my @spt = split '=', $_;
        my $header = $spt[0];
        @headers{ $spt[0] } = [split '\s*,\s*', $spt[1]];
  }
  return \%headers;
}


#########################################################################################
# Helper functions
#########################################################################################


#########################################################################################
# Only print if verbosity level is larger than 0;
#########################################################################################
sub printv {
    if($v > 0) {
        print shift;
    }
}
#########################################################################################
# Simple Download Method with additional Logging (Tries 10 times before quiting Job)
# (Christian Mayer)
#########################################################################################
sub getFiles {
    
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
	}
    }
}

sub getFile {

    my $src = shift;
    my $command = shift;
    my $count = 1;
    my $state = -1;

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
	}
}




#########################################################################################
# Extract with tar as system command
# (Christian Mayer)
#########################################################################################
sub extract {

    my $file = shift;
    $file =~ /\/([^\/]+)$/;
    $file = $1;
    my $toDelete = shift;
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
    if($res == 0 and $toDelete) {
        unlink($file);
    }
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

sub  trim {
    my $s = shift; $s =~ s/^\s+|\s+$//g;  
    return $s 
};


#########################################################################################
# Use Connect $ftp to the appropriate file server
#########################################################################################
sub connectToServer {

    if($ftp) {
    
        $ftp->close;
    }
    if($ftp2) {
    	
    	$ftp2->close;
    }
    
    my $dbname = shift;
    
    if($dbname eq "refseq" || $dbname eq "genbank") {

        $ftp  = Net::FTP->new($ncbi, Debug => 0) or die "Cannot connect to $ncbi: $@";
        $ftp->login("anonymous",'-anonymous@');
        
      	# New ftp connection to read the Checksum File
 		$ftp2 = Net::FTP->new($ncbi, Debug => 0) or die "Cannot connect to $ncbi: $@";
    	$ftp2->login("anonymous",'-anonymous@');
        

        #If we switch the database to NCBI and the allowed NCBI headers are not yet defined
        if(not $index_of) {

            ($index_of, $allowed_values) = getAssemblyHeaders();
        }   

    
    } elsif($dbname eq "ensembl") {

       $ftp  = Net::FTP->new($ensembl, Debug => 0) or die "Cannot connect to $ensembl: $@";
       $ftp->login("anonymous",'-anonymous@');
    }

}



# Returns a directory with particular file components, depending on the sequence type
sub ensemblBreakFile { 

    my $file_name = shift;
    # The regular expression used depends on the sequec 
    # cdna:  <species>.<assembly>.<sequence type>.<status>.fa.gz        status = all | abinitio
    # cds:   <species>.<assembly>.<sequence type>.<status>.fa.gz        status = all | abinitio
    # pep:   <species>.<assembly>.<sequence type>.<status>.fa.gz        status = all | abinitio
    # ncrna: <species>.<assembly>.<sequence type>.fa.gz 
                  
    # dna:   <species>.<assembly>.<sequence type>.<id type>.<id>.fa.gz  id optional
    # dna_rm:  same as DNA
    # dna_sm:  same as DNA

 
    my %res;
    my @new_values;

    # Pattern for cdna|cds|pep|ncrna
    if(grep /\.(cdna|cds|pep|ncrna)\./, $file_name) {

                            #species      #assembly          #sequence_type        # status
        $file_name =~ /^\s*([A-Za-z_]+)\.([A-Za-z0-9_\-\.]+)\.(cdna|cds|pep|ncrna)(\.(all|abinitio))?\.fa\.gz/;

       %res = (species => $1, 
               assembly   => $2,
               sequence_type => $3,
               status => $5);
        @new_values = (getMapping("species", $1),
                       getMapping("assembly", $2),
                       getMapping("sequence_type", $3),
                       getMapping("status", $5));
      
        
    } else {                #species      #assembly          # sequence_type     # id_type        # id
        $file_name =~ /^\s*([A-Za-z_]+)\.([A-Za-z0-9_\-\.]+)\.(dna|dna_rm|dna_sm)\.([a-zA-Z0-9_]+)(\.([a-zA-Z0-9_]+))?\.fa\.gz/;

        %res = (species => $1, 
                assembly   => $2,
                sequence_type => $3,
                id_type => $4,
                id => $6);

       @new_values = (getMapping("species", $1),
                      getMapping("assembly", $2),
                      getMapping("sequence_type", $3),
                      getMapping("id_type", $4),
                      getMapping("id", $6));
    }

    foreach(@new_values) {
             my $hash = $_;

             foreach(keys %{$_}) {
                   
                if(not defined $res{$_}) {
                    $res{$_} = $hash->{$_}; 
                }
            }
    }
    return \%res;    
}


sub ensemblReadChecksum {
	
	my $fh = $ftp->retr(shift);
	my %mapping;
	while(<$fh>)  {
		my @spt = split /\s+/, $_;
		$mapping{$spt[2]} = $spt[0] . " " . $spt[1];
	}
	$ftp->abort;
	close($fh);
	return \%mapping;
}


sub ncbiReadChecksum {
	
	my $fh = $ftp2->retr(shift) or die "Could not open file";
	my %mapping;
	while(<$fh>)  {
		my @spt = split /\s+/, $_;
		$mapping{substr($spt[1], 2)} = $spt[0];
	}
	$ftp2->abort;
	close($fh);
	return \%mapping;
}

# Join these funtions
sub list_dirs {
        my @dirs = @_;
        my @files;
        find({ wanted => sub { push @files, $_ if grep /.*$organismFileName/,$_ } , no_chdir => 1 }, @dirs);
        return @files;
}
sub list_fasta {

        my @dirs = @_;
        my @files;
        find({ wanted => sub { push @files, $_ if grep /.*(fa|fna|faa|fasta|fas)$/,$_ } , no_chdir => 1 }, @dirs);
        return @files;
}
#########################################################################################
# Stuff for Mapping
#########################################################################################


# This routine adds a mapping provided by a mapping file to the array of mappings
sub readMapping {

    open(my $fh, "<", shift);
    my @spt = split /\s+/, readline($fh);
    die "Error    Mapping file invalid\n" if(scalar @spt != 2);
   
    my @element = ($spt[0], $spt[1]);

	if(grep $spt[0],@ensembl_selectors) {

		push @ensembl_selectors, $spt[1];
	}


    my %hash; 
    while(<$fh>) {
   
        @spt = split /\t/, $_;
        $hash{$spt[0]} = trim($spt[1]);
    }
    push @element, \%hash;
    push @mappings, \@element;

    close($fh);
}


sub getMapping {
    
    my $find = shift;
    my $value = shift; 
    my %res;

    foreach(@mappings) {
            
        my @element = @$_;        
        if($find eq $element[0]) {

            if(defined  $element[2]->{$value}) {
               $res{$element[1]} = $element[2]->{$value};
            }
        }
    }
    return \%res;
}


# Returns 'prot' if FASTA file describes protein
# sequences, otherwise 'nucl'
sub detectFASTADBType {

	open(my $fh, "<", shift);
	while(<$fh>) {
		
		if(not $_ =~ /^\s*>.*$/	){

			if($_ =~ /^\s*[ACGT]+\s*$/) {
				close($fh);
				return "nucl";
			} else {
				close($fh);
				return "prot"
			}
		}
	}
}



# If the Path is relative, if will be made absolute by prepending
# the cwd. Otherwise, nothing happens
sub makePathAbsolute {
	
	my $path = shift;
	if($path =~ /^\/.*$/) {
		return $path;
	} else {
		return getcwd . "/$path";
	}
}


