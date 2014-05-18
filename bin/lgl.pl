#!/usr/bin/perl -w
#  
#  Copyright (c) 2002 Alex Adai, All Rights Reserved.
#  
#  This program is free software; you can redistribute it and/or
#  modify it under the terms of the GNU General Public License as
#  published by the Free Software Foundation; either version 2 of
#  the License, or (at your option) any later version.
#  
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
#  GNU General Public License for more details.
#  
#  You should have received a copy of the GNU General Public License
#  along with this program; if not, write to the Free Software
#  Foundation, Inc., 59 Temple Place, Suite 330, Boston,
#  MA 02111-1307 USA
#  

# This is the driver script for the LGL suite

use strict;
use Getopt::Std;

die "Usage: $0 [-c configfile] [-p perl_mod_path] [graph.lgl]\n" unless @ARGV;

my %option;
getopts("c:p:", \%option );

unshift( @INC , $option{p} ) if $option{p};

# The input file can be given here or provided in the 
# config file
my $INPUTFILE = shift || '';

################################################
# DEFAULT ARGS IN CASE A CONF FILE IS NOT PROVIDED

# The location of all the lgl executables (The bin directory)
my $LGLDIR = '';

# TMP dir needed by program. All results
# are stored there (total output directory)
my $TMPDIR = '/tmp/lgl_temp';

# This is relative to TMDIR
my $FINALOUTCOORDS = 'final.coords';

# LGLBREAKUP OPTIONS
my $CUTOFF = '';
my $USEMST = 0; # Just use and output the MSTs

# LGLAYOUT OPTIONS
my $TREELAYOUT = 0;          # Layout the MST only ( 1 or 0 )
my $USEORIGINALWEIGHTS = 0;  # Use the edge weights for MST ( 1 or 0 )
my $EDGELEVELMAP = 0;        # Output the level of each edge ( 1 or 0 )
my $OUTPUTMST = 0;           # File Marker for the MST
my $THREADCOUNT = 1;         # Number of threads 2 spawn ( positive integer )
my $ISSILENT = 0;            # Don't show as much output
my $PLACEMENTDISTANCE = '';  # Set to a float to activate
my $PLACEMENTRADIUS = '';    # Set to a float to activate (positive)
my $PLACELEAFSCLOSE = 0;     # Place children that are all leafs close
my $USEEDGEASWEIGHTS = 0;    # Treat the edges as EQ distances

# LGLBUILD OPTIONS
my $INTEGRATETYPE = ''; #'-d'

# Dimension of the layout
my $DIMENSION = 3;

# Set this to the directory for lglayout to start
# at. It has to be on previously started by lgl.pl
my $PICKUPDIR = '';

######################################################
# DO NOT EDIT BELOW HERE
######################################################

require ParseConfigFile;
require LGLFormatHandler;

if ( $LGLDIR eq '' or ! -d $LGLDIR )
{
    print STDERR <<ENDMSG
	
	You must enter the bin directory of the LGL
executables (Set variable LGLDIR in bin/lgl.pl)

ENDMSG
;
    exit;
}
if ( $option{c} )
{
    # This will overwrite the above defaults if
    # the corresponding arg is given.
    loadConfig( $option{c} );
}

my $status = 0;
my $infile = $INPUTFILE;

######################################################
# PART I
# Division into connected sets.
######################################################

my $date;
my $outdir;
if ( $PICKUPDIR eq '' )
{
    # Pretty restrictive
    mkdir( $TMPDIR , 0700 );
    convertToLGL();
    $date = time;
    $outdir = "$TMPDIR\/$date";
    # print STDOUT "MKDIR: $outdir\n";
    mkdir( $outdir , 0700 );
    my @part1;
    # Path to executable
    push @part1 , "$LGLDIR\/lglbreakup";
    # A possible cutoff argument
    if ( $CUTOFF ne '' && defined $CUTOFF ) { push @part1 , ('-c', "$CUTOFF"); }
    push @part1 , '-m' if ( $USEMST != 0 );
    # The output directory
    push @part1 , ('-d', "$outdir");
    push @part1 , "$infile";
    print STDOUT "LGLBREAKUP: @part1\n";
    $status = system( @part1 );
    die "Problem with lglbreakup (Part I): $?" unless $status == 0;
}
else
{
    print STDOUT "Skipping PartI...\n";    
    $outdir = $PICKUPDIR;
    print STDOUT "Using old dir $PICKUPDIR\n";
    ($TMPDIR,$date) = $outdir =~ /(.*)\/(\d+)$/;
    print STDOUT "That is: $TMPDIR and $date\n";
    convertToLGL();
}

######################################################
# PART II
# Layout on each connected set.
######################################################

# Have to peel all the files out of the out dir of part I and stick
# those in as inputs
opendir INDIR, "$outdir" or die "Open of dir $outdir failed: $!";
my @lglinputfiles = readdir INDIR;
closedir INDIR;

# This will be the list of all the files that
# will be put back together
my @lglrebuildfiles;
# Now to run the layout
foreach my $file ( @lglinputfiles )
{
    # $file is the input file to lglayout
    next unless ($file =~ /^(\d+)\.lgl$/ || $file =~ /^(\d+)\.short$/);
    my $lgloutfile = $file;
    # The output coords file input comes in one of two ways
    $lgloutfile =~ s/\.lgl$/\.coords/;
    $lgloutfile =~ s/\.short$/\.coords/;
    # Now if it turns out a coords file already exists, skip
    # that one. It may be from an interrupted run
    my $out = "$outdir\/$lgloutfile";
    ( (print STDERR "___ Skipping run on $file ($lgloutfile exists) ___\n") && next ) if
	( -e $out );    
    # Record
    push @lglrebuildfiles , "$out";
    my @part2;
    my $lglayout = "$LGLDIR\/lglayout" . "$DIMENSION" . 'D';
    push @part2 , $lglayout;
    # Another possibility is that the root node was already found.
    my $root = "$out\.root";
    if ( -e $root )
    {
	my $r = `cat $root`;
	chomp $r;
	push @part2 , ('-z', "$r");
    }
    # Possible arg is the number of threads to spawn
    push @part2 , ('-o', "$outdir\/$lgloutfile");
    # Now to deal with all the options to lglayout
    push @part2 , '-y' if ( $TREELAYOUT != 0 );
    push @part2 , '-O' if ( $USEORIGINALWEIGHTS != 0 );
    push @part2 , ('-t', "$THREADCOUNT") if ( $THREADCOUNT > 1 );
    push @part2 , '-e' if ( $OUTPUTMST != 0 );
    push @part2 , '-l' if ( $EDGELEVELMAP != 0 );
    push @part2 , '-I' if ( $ISSILENT != 0 );
    push @part2 , '-L' if ( $PLACELEAFSCLOSE != 0 );
    push @part2 , '-w' if ( $USEEDGEASWEIGHTS != 0 );
    # Finally attatch the file for lglayout to process
    push @part2 , "$outdir\/$file";
    print STDOUT "LGLAYOUT: @part2\n";
    $status = system( @part2 );
    die "Problem with lglayout (Part II): $?" unless $status == 0;
}

######################################################
# PART III
# Putting all the pieces back together
######################################################

my @part3;
my $lglrebuild = "$LGLDIR\/lglrebuild";
push @part3, $lglrebuild;
push @part3, ('-o', "$TMPDIR\/$FINALOUTCOORDS");
push @part3, "$INTEGRATETYPE";
opendir INDIR, "$outdir" or
    die "lgl.pl::FileList:Open of dir $outdir failed: $!";
my @files2integrate = grep{ /\.coords$/ } readdir INDIR;
closedir INDIR;
my $coordFileList = "$TMPDIR\/coordFileList";
open O , ">$coordFileList" or
    die "Can't open coord file list $coordFileList\n";
foreach my $file ( @files2integrate ) { print O "$outdir\/$file\n"; }
close(O);
push @part3, ('-c',$coordFileList);
print STDOUT "LGLREBUILD: @part3\n";
$status = system( "@part3" );
die "Problem with lglrebuild (Part III): $?" unless $status == 0;

# If we laid out the MST only then it turns
# out we need to get all the .mst. files from
# all the runs and concat them
if ( $OUTPUTMST != 0 )
{
    opendir INDIR, "$outdir" or die "PartIII: Open of dir $outdir failed: $!";
    my @mstfiles = grep { /mst\.(lgl|short)$/ } readdir INDIR;
    closedir INDIR;
    my $finalmstfile = "$TMPDIR" . '/final.mst.lgl';
    open O, ">$finalmstfile" or
	die "PartIII: Open of MST file $finalmstfile failed: $!\n";
    foreach ( @mstfiles ) {
	open I, "<$outdir\/$_" or die "PartIII: Concat failed for $_: $!\n";
	while(<I>) { print O; }
	close(I);
    }
    close(O);
}

# End of Script
######################################################

sub convertToLGL
{
    if ( $infile !~ /\.lgl$/ )
    {
	$infile =~ s/.*\///;
	$infile = "$TMPDIR\/$infile";
	$infile =~ s/\.\w+$/\.lgl/;
	my $format = new LGLFormatHandler();
	my ($edgeCount,$msg) = $format->loadFromFile( $INPUTFILE );
	die "Error On file Convert: $msg\n" if $msg ne "";
	$format->print2File( $infile );
    }
}

######################################################

sub loadConfig
{
    my $file = shift;
    my $conf = ParseConfigFile->new( $file );
    $LGLDIR = $conf->value('lgldir') if $conf->value('lgldir');
    $FINALOUTCOORDS = $conf->value('finaloutcoords') if
	$conf->value('finaloutcoords');    
    $TMPDIR = $conf->value('tmpdir') if $conf->value('tmpdir');
    $CUTOFF = $conf->value('cutoff') if $conf->value('cutoff');
    $INTEGRATETYPE = $conf->value('integratetype') if
	$conf->value('integratetype');
    $TREELAYOUT = $conf->value('treelayout') if $conf->value('treelayout');
    $USEORIGINALWEIGHTS = $conf->value('useoriginalweights') if
	$conf->value('useoriginalweights');
    $EDGELEVELMAP = $conf->value('edgelevelmap') if $conf->value('edgelevelmap');
    $OUTPUTMST = $conf->value('outputmst') if $conf->value('outputmst');
    $THREADCOUNT = $conf->value('threadcount') if $conf->value('threadcount');
    $DIMENSION = $conf->value('dimension') if $conf->value('dimension');
    $INPUTFILE = $conf->value('inputfile') if $conf->value('inputfile');
    $USEMST = $conf->value('usemst') if $conf->value('usemst');
    $PICKUPDIR = $conf->value('pickupdir') if $conf->value('pickupdir');
    $ISSILENT = $conf->value('issilent') if $conf->value('issilent');
    $PLACEMENTDISTANCE = $conf->value('placementdistance') if $conf->value('placementdistance');
    $PLACEMENTRADIUS = $conf->value('placementradius') if $conf->value('placementradius');
    $PLACELEAFSCLOSE = $conf->value('placeleafsclose') if $conf->value('placeleafsclose');
    $USEEDGEASWEIGHTS = $conf->value('useedgeasweights') if $conf->value('useedgeasweights');
}

######################################################
