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

# Generates an edge color file for lglview
# based on a *.edge_levels file from lglayout.
# The format is simple though, just 3 cols:
# vertex1 vertex2 level
# where level is a number
# Results are printed to STDOUT.

use strict;
#use Math::Utils qw(:utility); 

my $infile = shift;

die "Usage: $0 file.edge_levels\n" unless $infile;

my %edges;
loadEdges( $infile , \%edges );

my %levels;
getColorsLevel( \%levels , \%edges );

print STDERR "Printing output\n";
foreach my $e ( sort keys %edges )
{
    my ($v1,$v2) = split(/_/,$e);
    my ($r,$g,$b) = @{$levels{ $edges{$e} }};
    print "$v1 $v2 $r $g $b\n";
} 

#---------------------------------------------------

sub loadEdges
{
    my ($file,$c) = @_;
    open I, "<$file" or die "Open Failed: $file: $!\n";
    while(<I>)
    {
	chomp;
	my @cc = split;
	$c->{"$cc[0]_$cc[1]"} = $cc[2];
	# print "LOADED : $cc[0]_$cc[1] $cc[2]\n";
    }
}

#----------------------------------------------------

sub getColorsLevel
{
    my ( $c , $e ) = @_;
    my ( $min , $max ) = findMinMaxLevel( $e );
    if ( $min == $max ) {
	print STDERR "All edges are same level. Exiting\n";
	exit;
    }
    # setup the starting color combo
    my ($r,$g,$b) = (0.0,0.0,1.0);
    my $color = 'g'; # <- start color to switch (i.e. gradient)
    my $dcolor = abs(3.0/($max-$min+1));
    my ($rd,$gd,$bd) = ($dcolor,$dcolor,-$dcolor); # fix with formulas
    
    for ( my $i=$min; $i<=$max; $i++ )
    {
	if ( $color eq 'r' ) {
	    $r += $rd;
	    if (($r < 0.0) || ($r > 1.0)) {
		$r = 0.0;
		print STDERR "At Limit\n";
	    }
	}
	if ( $color eq 'g' ) {
	    $g += $gd;
	    if (($g < 0.0) || ($g > 1.0)) {
		$b += -($b - 1);
		$g = 1.0;
		$color = 'b';
	    }
	}
	if ( $color eq 'b' ) {
	    $b += $bd;
	    if (($b < 0.0) || ($b > 1.0)) {
		$g += $b;
		$b = 0.0;
		$color = 'r';
	    }
	}
	push @{$c->{$i}} , ($r,$g,$b);
	# print "$i $r $g $b\n";
    }
}

#---------------------------------------

sub findMinMaxLevel
{
    my $e = shift;
    my $max = 0;
    my $min = 100000000;
    foreach my $e1 ( keys %{$e} ) {
	my $newlevel = $e->{$e1};
	$max = $newlevel if $newlevel > $max;
	$min = $newlevel if $newlevel < $min;
    }
    return ($min,$max);
}

#---------------------------------------
