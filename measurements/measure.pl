#!/usr/bin/perl -w

# Set global information
my $IndexPath = "/home/fotis/Workspace/CSD/cs463/CollectionIndex";
my $SourcePath = "/home/fotis/Workspace/CSD/cs463/fileSources";

# Go to the directory with binaries
# --Notice : in case of noclassfound exception go one dir up
chdir('/home/fotis/Workspace/CSD/cs463/bin') or die "$!";
print "Binary path : " . getcwd() . "\n" ;
my $indexer = "java cs463/Scavenger";

# Hash of files and corresponding size in bytes
my %sourceList = ();
my $totalDirSize = 0;


opendir(SOURCES, $SourcePath) || die("Could not open source directory\n");
while( $filename = readdir(SOURCES) ) {
    $filename = "$SourcePath/$filename";
    $sourceList{ $filename } =  (stat($filename))[7];
    $totalDirSize += $sourceList{ $filename };
};
closedir(SOURCES);

use POSIX;
print "Collection documents : " . keys (%sourceList) . "\n";
print "Collection bytes : " . $totalDirSize . " bytes\n";
print "Avg collection filesize : " . ceil( $totalDirSize / (keys %sourceList) ) . " bytes\n";;

# Size per file

# Total collection size

# Find execution Time
my $start_run = time();
`$indexer`;
my $end_run = time();
print "Running time : " . ($end_run - $start_run) . " seconds\n";



