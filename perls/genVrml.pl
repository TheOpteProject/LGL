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

# This file will generate .wrl given the 3D
# coord files, and the .lgl file that has the 
# links. Info for the modules are best explained
# in the perldocs, ie 'perldoc VRML'.
# On a side note, the VRML code produced by this
# script seems to be inefficient and bloated
# (probably because of the way I am calling the
# methods) so I very open to code suggestions.

use strict;
use VRML;
use VRML::Color;
# Get LGLFormatHandler.pm where you got this script
use LGLFormatHandler;
use IO::Handle;
use Getopt::Std;

# These are the default options that can
# be edited for your purposes

my $radius = .1;           # Radius of VRML spheres (vertices)
my $edgeColor = 'blue';    # Default color of VRML line (edges)
my $showNodeID = 1;        # if=1 show node id, elsif=0 don't
my $showNode = 1;          # if=1 show node (sphere), elsif=0 don't
my $backgroundColor = 'black';
my $multipleEdgeColor = 'white'; # This color is for mulitply defined edges
my $nodeColor = 'red';     # Default Color of VRML spheres (vertices)

printUsage() && exit(1) unless @ARGV;

####################################################
#           SHOULD NOT EDIT BELOW HERE             #
####################################################

############################# DEALING WITH OPTIONS

my %options = ();
getopts( 'sNIC:S:l:a:' , \%options );
if ( $options{I} ) { $showNodeID = 0; }
if ( $options{N} ) { $showNode = 0; }
my %coloredNodes;  # This option permits unique colors of vertices.
if ( $options{C} )
{ 
    readNodeColor( $options{C} , \%coloredNodes );
}
my %shapedNodes;   # This options permits one of the predefined shapes.
if ( $options{S} )
{
    readNodeShape( $options{S} , \%shapedNodes );
}
my %lineColor;     # This options allows the edges to be colored
if ( $options{l} )
{
    my $file = $options{l};
    readEdgeColors( $file , \%lineColor );
}

############################# END OPTIONS

my $lsFile = shift;
my $coordFile = shift;
my $outfile = $coordFile . '.wrl';

my %coords;
print STDERR "Loading coords..."; STDERR->autoflush;
getCoordsFromFile( $coordFile , \%coords );
print STDERR "Done.\n"; STDERR->autoflush;

my $vrml = new VRML(97);
$vrml->background( skyColor => "$backgroundColor" , 
		   groundColor => "$backgroundColor" );

setViewpoint( $vrml , \%coords );

print STDERR "Generating node/text coordinates in VRML..."; STDERR->autoflush;
foreach my $node ( sort keys %coords )
{
    my $string = makeStringFromArray( \@{$coords{$node}} );
    $vrml->begin();
    if ( $showNode != 0 )
    {
	my $url = generateAnchor( $options{a} , $node ) if $options{a};
	$vrml->anchor_begin("$url") if $options{a};
	$vrml->at($string);
	determineShapeAndColorOfNode( $node , \%shapedNodes , 
				      \%coloredNodes , $vrml ,
				      $radius );
	$vrml->anchor_end() if $options{a};
	if ( $showNodeID != 0 )
	{
	    my $fontsize = $radius*.5;
	    $vrml->billtext("$node","","$fontsize SERIF BOLD", 'MIDDLE');
	}
	$vrml->back();
    }
    $vrml->end();
}
print STDERR "Done.\n"; STDERR->autoflush;
    
print STDERR "Loading edges from file..."; STDERR->autoflush;
my $lsinfo = LGLFormatHandler->new();
$lsinfo->loadFromFile( $lsFile );
print STDERR "Done.\n"; STDERR->autoflush;

print STDERR "Generating lines in VRML..."; STDERR->autoflush;
my $edges = $lsinfo->edges();
if (defined $lsFile)
{
    foreach my $edge1 ( sort keys %{$edges} )
    {
	foreach my $edge2 ( sort keys %{$edges->{$edge1}} )
	{
	    $vrml->begin();
	    my $from = makeStringFromArray( \@{$coords{$edge1}}  );
	    my $to = makeStringFromArray( \@{$coords{$edge2}} );
	    # Here a check is to see if the line is given a non default
	    # color.
	    my $color = 'blue';
	    if ( $options{l} && defined $lineColor{$edge1}{$edge2} )
	    {
		$color = $lineColor{$edge1}{$edge2};
	    }
	    # if ( $color eq 'white' ) { print "WHITE: $edge2 $edge1\n"; }
	    $vrml->line( $from , $to , 0 , "$color" );	    
	    $vrml->end();
	}
    }
} else {
    print STDERR "No edges for this wrl file...";
}
print STDERR "Done.\n";

if ( $options{s} )
{
    print STDERR "Writing..."; STDERR->flush();
    $vrml->print(1);
}
else
{
    print STDERR "Writing to $outfile..."; STDERR->flush();
    $vrml->save( $outfile );
}
print STDERR "Done.\n";

#-----------------------------------------------------------------

sub readEdgeColors
{
    my $file = shift;
    my $linecolors  = shift;
    open IN, "<$file" or die "Can't open line colorfile $file : $!\n";
    while(<IN>)
    {
	chop;
	my @a = split;
	my $color = '';
	my $v1 = shift @a;
	my $v2 = shift @a;
	my $length = @a;
	if ( $length == 1 )
	{
	    $color = rgb_color( $a[0] );
	}
	elsif ( $length == 3 )
	{
	    # The color is in the RGB format
	    $color = rgb_color( makeStringFromArray( \@a ) );
	}
	else { die "Format problem with edge color file\n"; }
	if ( defined $linecolors->{$v1}->{$v2} )
	{
	    # This edge was already defined...make sure it is
	    # the same color. If not then give it the MEC.
	    if ( $linecolors->{$v1}->{$v2} ne $color )
	    {
		$linecolors->{$v1}->{$v2} = $multipleEdgeColor;
		$linecolors->{$v2}->{$v1} = $multipleEdgeColor;
	    }
	}
	else
	{
	    # Give both possible combinations
	    $linecolors->{$v1}->{$v2} = $color;
	    $linecolors->{$v2}->{$v1} = $color;
	}
    }
    close(IN);
}

#-----------------------------------------------------------------

sub makeStringFromArray
{
    my $a = shift;
    my $s = '';
    foreach ( @{$a} )
    {
	$s .= "$_ ";
	#print "$s\n";
    }
    $s =~ s/\s$//; # Take out last space
    # print "\|$s\|\n";
    return $s;
}

#-----------------------------------------------------------------

sub getCoordsFromFile
{
    my $coordFile = shift;
    my $ref = shift;
    open CF , "<$coordFile" or die "getCoordsFromFile: Can't open $coordFile: $!\n";
    while(<CF>)
    {
	chomp;
	my @a = split;
	push ( @{$ref->{$a[0]}} , @a[1..$#a] );
	#print "@{$ref->{$a[0]}}\n";
    }
    close(CF);
}

#-----------------------------------------------------------------

sub readNodeShape
{
    my ( $file , $ref ) = @_;
    open I, "<$file" or die "readNodeShape: Can't open $file: $!\n";
    while(<I>) {
	chomp;
	my @a = split;
	# Node name     Shape
	$ref->{$a[0]} = $a[1];
    }
    close(I);
}

#-----------------------------------------------------------------

sub readNodeColor
{
    my ( $file , $ref ) = @_;
    open I, "<$file" or die "readNodeColor: Can't open $file: $!\n";
    while(<I>) {
	chomp;
	my @a = split;
	my $length = @a;
	if ( $length == 2 )
	{
	    # The color in this case is just a single word
	    $ref->{$a[0]} = $a[1];
	}
	elsif ( $length == 4 )
	{
	    # The color is in the RGB format
	    my $id = shift @a;
	    my $color = rgb_color( makeStringFromArray( \@a ) );
	    $ref->{$id} = $color;
	}
	else { die "Format problem with node color file\n"; }
    }
    close(I);
}

#-----------------------------------------------------------------

sub setViewpoint
{
    my ($vrml, $coords) = @_;
    my @max = (-100000,-100000,-100000);
    my @min = (100000,100000,100000);
    # Finding maxes and mins of coords
    foreach my $xx ( values %{$coords} )
    {
	my $i=0;
	foreach my $x ( @{$xx} )
	{
	    $max[$i] = $x if ( $x > $max[$i] );
	    $min[$i] = $x if ( $x < $min[$i] );
	    $i++;
	}
    }
    my @avg = (0,0,0);
    for ( my $i=0; $i<3; $i++)
    {
	$avg[$i] = .5 * ( $max[$i] + $min[$i] );
    }
    # Add offset to avg for position
    $avg[1] += $max[1];
    my $position = makeStringFromArray( \@avg );
    $vrml->viewpoint('',"$position",'TOP');
}

#-----------------------------------------------------------------
# This generates the url given to each node (vertex). The 2 input
# args are anchorbase, provided by option -a, and the second is the
# id of the vertex. This can be changed to suit whatever.

sub generateAnchor
{
    my ($anchorBase,$id) = $_;
    return "$options{a}$id";
}

#-----------------------------------------------------------------

sub determineShapeAndColorOfNode
{
    my ( $node , $nodeShapeConversion , $nodeColorConversion ,
	 $vrml , $radius  ) = @_;
    my $appearance = '';
    if ( defined $nodeColorConversion->{$node} ) { 
	$appearance = "d=$nodeColorConversion->{$node};tr=.5";
    } else { 
	$appearance = "d=$nodeColor;tr=.5"; 
    }
    my $shape = $nodeShapeConversion->{$node};
    # print "$node $nodeColor $appearance\n";
    if ( !defined $shape ) {
	$vrml->sphere("$radius","$appearance");
    } elsif ( $shape eq 'cylinder' ) {
	$vrml->cylinder("$radius $radius","$appearance");
    } elsif ( $shape eq 'cone' ) {
	$vrml->cone("$radius $radius","$appearance");
    } elsif ( $shape eq 'box' ) {
	$vrml->box("$radius $radius $radius","$appearance");
    } elsif ( $shape eq 'sphere' ) {
	$vrml->sphere("$radius","$appearance");
    } elsif ( $shape eq 'pyramid' ) {
	$vrml->pyramid("$radius","$appearance");
    } else {
	die "Bad node shape type: $shape\n";
    }
}

#-----------------------------------------------------------------

sub printUsage
{
print <<END 

Usage: genVrml.pl [-s] [-N] [-I] [-C vertexColorList]
         [-S vertexShapeList] [-l edgeColorList]
         [-a anchorBase] edgefile coordfile
     
     -s Write to STDOUT

     -N Don't show node (only lines or ids are drawn)

     -I Don't show node id (only the shapes or lines are drawn)

     The edgefile has to be an edge file supported by
LGLFormatHandler.pm. The coordFile3D is the coordinate file in
3D (surprise). Its format is simply:

vertex1 x1 y1 z1
vertex2 x2 y2 z2
.
.
.

The vertex shape file has a simple 2 column format.
For the vertexShapeList:

vertex1 shape1
vertex2 shape2
.
.
.

The allowed shapes are cylinder, cone ,box , sphere,
and pyramid. The vertexColorList format is simply:

vertex1 vertex1color
vertex2 vertex2color
.
.
.

and the edgeColorList format is:

vertex1 vertex2 edge1color
vertex3 vertex4 edge2color
.
.
.

The format and allowed colors for any of these are the 
same as the ones described in VRML::Color docs ( see
perldoc VRML::Color). These are colors such as '1 0 0',
'red', 'FF0000', etc. A default color
called multipleEdgeColor is used when an edge was given
with two different colors in the edgeColorList. The
columns in all cases are whitespace delimited. For any
vertices where this information is not provided the defaults
are used instead. These are at the top of the script and can
be easily changed. Support is also built in for anchor
insertion into the vertex. This allows a clickable interface
to the viewer that can take the user to a separate URL,
such as a html description of that vertex. By default,
if a url is given with the -a option the vertex id is
tacked onto the end of the url. However, this can be changed by
modifying only the subroutine 'generateAnchor' to return
whatever you wish.

END
;
}

#-----------------------------------------------------------------
