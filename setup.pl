#!/usr/bin/perl -w
#  
#  Copyright (c) 2003 Alex Adai, All Rights Reserved.
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

# This is the very crude install/setup script for LGL.
# It should compile 2D/3D versions of LGL
# and place the executables in the bin dir

use strict;
use Getopt::Std;

usage() unless @ARGV;

my %options;
getopts('c:i',\%options);

my $dir = shift || `pwd`;
chomp $dir;
$dir .= '/' if !($dir =~ /\/$/);
my $sourceDir = "$dir" . 'src';
print "Source Directory: $sourceDir\n";

# Print out a default config file for lgl.pl
# to read
if ( defined $options{c} ) {
    printConfFile( $options{c} );
}

if ( $options{i} ) {
    compileSources();
}

############################################

sub printConfFile
{
    my $file = shift;
    open O , ">$file" or die "Can't open conf file $file: $!\n";
    print O <<CONFFILE
###########################################################
# After generating this config file and modifying it, you 
# can usually just run LGL with the command:
# lgl.pl -c config_file_name
#
# You can also modify the defaults in lgl.pl instead of
# using this config file.
#
# If you want to use the defaults given in lgl.pl just
# set any entry in here to ''. (The empty string)
#
# All values must be quote delimited, and key/values must
# be separated by a "=" (without the quotes). Values also
# can not have quotes in them.
#
# All paths should be absolute.
###########################################################

# The output directory for all the LGL results. Note that
# several files and subdirectories will be generated.
# This has to be a valid directory name.
tmpdir = '/tmp/lgl'

# The edge file to use for the layout. Has to be a file readable
# by LGLFormatHandler.pm. It has to be an existing/valid file name,
# with the absolute path.
inputfile = ''

# The output file that will have the final coordinates of
# each vertex. This has to be a valid file name, and it
# will be place in 'tmpdir'. This really does not have to
# be changed.
finaloutcoords = 'final.coords'

# Layout only the MST that lglayout generated, and NOT the
# entire graph. This can be '0' or '1' or '' (default) 
treelayout = '0'

# Use the given edge weights in the edge file to generate
# the MST. This can be '0' or '1' or '' (default) 
useoriginalweights = '0'

# Output the level of each vertex in the layout tree, which
# is usually just the MST. This can be '0' or '1' or '' (default) 
edgelevelmap = '1'

# Output the MST that was generated as an .lgl edge file. 
# This can be '0' or '1' or '' (default) 
outputmst = '1'

# The number of threads for lglayout to spawn for the layout.
# This will only make a difference for graphs having several
# thousands of edges and with the threadcount == processor count
# lglayout will cap this value to the processor count.
threadcount = '1'

# The dimension of the layout you want. Can only be '2' or '3'
dimension = '2'

# Cutoff value for weights. Any edge with a weight value greater
# than cutoff will be dropped. The empty string means keep ALL
# edges. This can be a valid float or ''.
cutoff = ''

# Use only the MST to begin with. lglbreakup generates the MST
# which are fed into lglayout. The provided edges must have weights.
# This is beneficial for HUGE graphs with millions of edges. 
# Doing the MST at the lglbreakup will save time later on down
# the road, not to mention less I/O. This will only do a layout
# of the MST.
# This can be '0' or '1' or '' (default).
usemst = '0'

# This will cut back on some of the output/progress reports,
# and make it a little quieter. '0' is verbose while '1' is
# not.
issilent = '0'

# If an LGL run was interrupted, the pickupdir is the dir
# where the simulation left off.
pickupdir = ''

# integratetype is the method for integrating all the individual
# layouts. By default, a 'funnel' is used, but if you want a DLA 
# integration then set this to '-d'. Currently, only two types
# of integration exist.
integratetype = ''

# By specifying a placement distance, you determine the distance
# of separation of the next level placement. This can be
# a + float value, but set to '' if you want to use the default
# calculated distance. EXPERIMENTAL
placmentdistance = ''

# The placement of a subsequent level is done by placing the nodes
# on a circle perimeter. This value determines the radius of that
# circle. Therefore smaller values will make the initial placements
# very tight. This can be any + float or '' for default. EXPERIMENTAL
placementradius = ''

# The placement distance of subsequent levels is usually done by 
# solving a formula based on how many vertices there are. However
# if you have a tree you may want to place vertices that are all
# leafs "on top of" the parent vertex.  This might produce better
# images for trees than for graphs.  This can be '0' or '1' or '' 
# (default). EXPERIMENTAL
placeleafsclose = '0'

CONFFILE
;
    close(O);
}

############################################

sub compileSources
{
    chdir("$sourceDir") or die "Can't cd to $sourceDir: $!\n";
    
    print "#### Compiling 2D ####\n";
	changeDimension(2);
    runMakes(2);
    
    print "#### Compiling 3D ####\n";
	changeDimension(3);
    runMakes(3);
    
    # Just clean up the .o s
    #my $status = system("make clean");
    #die "Final Make clean failed" unless $status == 0;
}

############################################

sub runMakes
{
    my $d = shift;
    my $status = system("make clean");
    die "Make clean failed" unless $status == 0;
    $status = system("make");
    die "Make failed" unless $status == 0;
    $status = system("make install");
    die "Make install failed" unless $status == 0;
    # The layout is the only part that is dimension dependent
    my $lglayout_i = '../bin/lglayout';
    my $lglayout_f = '../bin/lglayout' . "$d" . 'D';    
    `mv -f $lglayout_i $lglayout_f`;
}

############################################

sub changeDimension
{
    my $dimension = shift;
    my $tmpfile = time;
    open I, '../include/configs.h' or
	die "changeDimension: Open of configs.h failed: $!\n";
    open O, ">$tmpfile" or
	die "changeDimension: Open of tmp failed: $!\n";   
    while(<I>)
    {
	((print O) && next) unless /DIMENSION/;
	s/DIMENSION\s*=\s*\d/DIMENSION = $dimension/;
	print O;
    }    
    close(I);    
    `mv -f $tmpfile ../include/configs.h`;
}

############################################

sub usage
{
    print <<USAGE

Usage: $0 [-i] [-c example_config_file_name]

  -i Install LGL. The destination is set to the
     source directory + ../bin/ where the source
     directory has all LGL source code. This presumes
     you are in the same directory as $0 (./)

  -c This will print out an example config file
     that can be changed and then given to lgl.pl
     to get you started quickly. You can then run
     lgl.pl like this:

     ./bin/lgl.pl -c config_file_name

USAGE
;
    exit(0);
}

############################################
