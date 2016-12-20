#! /usr/bin/perl

my @file_array=@ARGV;

if(@file_array <1){
	print "Please provide a valid file \n";
	exit;
}
my $file = @file_array[0];
print "Validating File : ".$file."\n";
# Unzip data file 
system(" gunzip $file" ) ;
substr($file, -3 )="";

#print $file ;

open (OUT, ">$file.tmp")|| die || "Cannot write Temp File\n";
open (FILE, $file)|| die "Cannot read File $file \n";
my $prev_line ="";
while (<FILE>){
	my $current_line =$_;
	if ($prev_line =~ />/ && $current_line =~ />/){
		print "$prev_line\n";
	}else{
		print OUT $prev_line;
	}
	$prev_line= $current_line;
	
}
print OUT $prev_line;

close OUT;


close FILE || die "cannot close File \n";

system("gzip $file.tmp");

system("mv $file.tmp.gz $file.gz");
system("rm $file");

