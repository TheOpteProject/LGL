#-------------------------------------------------------------
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
#-------------------------------------------------------------

package LGLFormatHandler;

use strict;
use IO::Handle;

our ( $VERSION , $DEFAULT_WEIGHT );

BEGIN { $VERSION=1.0; $DEFAULT_WEIGHT = '_no_weight_'; }

#-------------------------------------------------------------

sub new
{
    my $pkg = shift;
    my $vars = {
	_edges => { } ,
	_vertexCount => 0 ,
	_edgeCount => 0
	};
    bless $vars , $pkg;  
    return $vars;
}

#-------------------------------------------------------------

sub loadFromFile
{
    my ( $obj , $file ) = @_;
    if ( $file =~ /\.ncol$/ ) { $obj->loadFromFile_ncol($file); }
    elsif ( $file =~ /\.lgl$/ ) { $obj->loadFromFile_lgl($file); }
    else { die "LGL::loadFromFile: Unknown file format : $file\n"; }
}

#-------------------------------------------------------------

sub print2File
{
    my ( $obj , $file ) = @_;
    if ( $file =~ /\.ncol$/ ) { $obj->print2File_ncol($file); }
    elsif ( $file =~ /\.lgl$/ ) { $obj->print2File_lgl($file); }
    else { die "LGL::print2File: Unknown file format : $file\n"; }
}

#-------------------------------------------------------------

sub loadFromFile_lgl
{
    my ( $obj , $file ) = @_;
    open I , "<$file" or die "LGL::loadFromFile_lgl: Open of $file Failed: $!\n";
    return $obj->loadFromFH_lgl( \*I );
}

#-------------------------------------------------------------

sub loadFromFH_lgl
{
    my ($obj,$fh) = @_;
    my $v1 = '';
    my $line = <$fh>;
    my @l;
    my $edgeCount = 0;
    my $errorMsg = "This is not a .lgl file or there is a " .
	"file format problem. Problem with edge ";
    while( ! eof( $fh ) )
    {
	@l = split(/\s+/,$line);
	my $count = @l;
	if ( $l[0] ne '#' || $count != 2 )
	{
	    $errorMsg .= "$edgeCount.\n";
	    return ( $edgeCount , $errorMsg );
	}
	my $v1 = $l[1];
	while( <$fh> )
	{
	    $line = $_;
	    @l = split(/\s/,$line);
	    $count = @l;
	    if ( $count > 2 )
	    {
		$errorMsg .= "$edgeCount.\n";
		return ( $edgeCount , $errorMsg );
	    }
	    last if $l[0] eq '#';
	    #next if ( $v1 eq $l[0] );
	    if ( $v1 eq $l[0] )
	    {
		$errorMsg .= "$edgeCount. Can't have edge to self.\n";
		return ( $edgeCount , $errorMsg );
	    }
	    #next if ( $obj->{_edges}->{$l[0]}->{$v1} ||
	    #	      $obj->{_edges}->{$v1}->{$l[0]} );
	    if ( defined $l[1] )
	    {
		$obj->{_edges}->{$v1}->{$l[0]} = $l[1];
	    }
	    else
	    {
		$obj->{_edges}->{$v1}->{$l[0]} = $DEFAULT_WEIGHT;
	    }
	    $edgeCount++;
	}
    }

    $obj->{_edgeCount} = $edgeCount;

    return ($edgeCount,""); 
}

#-------------------------------------------------------------

sub loadFromFile_ncol
{
    my ( $obj , $file ) = @_;
    open IN, "$file" or die "LGL::loadFromFile_ncol: Open of $file failed: $!\n";
    return $obj->loadFromFH_ncol( \*IN );
}

#-------------------------------------------------------------

sub loadFromFH_ncol
{
    my ( $obj , $fh ) = @_;
    my $edgeCount = 0;
    my $errorMsg = "This is not a .ncol file or there is a " .
	"file format problem. Problem with edge ";
    while( <$fh> )
    {
	chop;
	my @l = split;
	my $count = @l;
	#print STDERR "@l\n";
	#print STDERR "$count\n";
	if ( $count < 2 || $count > 3 )
	{
	    $errorMsg .= "$edgeCount.\n";
	    return ($edgeCount,$errorMsg);
	}
	my ( $vertex1 , $vertex2 , $weight ) = @l;
	# next if $vertex1 eq $vertex2;
	if ( $vertex1 eq $vertex2 )
	{
	    $errorMsg .= "$edgeCount. Can't have edge to self.\n";
	    return ($edgeCount,$errorMsg);
	}
	#next if ( $obj->{_edges}->{$vertex2}->{$vertex1} ||
	#$obj->{_edges}->{$vertex1}->{$vertex2} );
	if ( $obj->{_edges}->{$vertex2}->{$vertex1} ||
	     $obj->{_edges}->{$vertex1}->{$vertex2} )
	{
	    $errorMsg .= "$edgeCount. Multiply defined edge.\n";
	    return ($edgeCount,$errorMsg);
	}
	if ( defined $weight )
	{
	    $obj->{_edges}->{$vertex1}->{$vertex2} = $weight;
	}
	else
	{
	    $obj->{_edges}->{$vertex1}->{$vertex2} = $DEFAULT_WEIGHT;
	}
	# print STDERR "$vertex1 $vertex2\n";
	$edgeCount++;
    }

    $obj->{_edgeCount} = $edgeCount;
    return ($edgeCount,"");
}

#-------------------------------------------------------------

sub edges
{
    my ( $obj , $ref ) = @_;
    if ( $ref ) { $obj->{_edges} = $ref; }
    return $obj->{_edges};
}

#-------------------------------------------------------------

sub print2File_lgl
{
    my ( $obj , $file ) = @_;
    open FH, ">$file" or die "LGL::print2File_lgl: Open of $file failed: $!\n";
    $obj->print2FH_lgl( \*FH );
    close( FH );
    return $obj;
}

#-------------------------------------------------------------

sub print2FH_lgl
{
    my ( $obj , $fh ) = @_;
    foreach my $node1 ( sort keys %{$obj->{_edges}} )
    {
	my $line = '';
	foreach my $node2 ( sort keys %{$obj->{_edges}->{$node1}} )
	{
	    my $w = $obj->{_edges}->{$node1}{$node2};
	    if ( $w ne $DEFAULT_WEIGHT )
	    {
		$line .= "$node2 $w\n";
	    }
	    else
	    {
		$line .= "$node2\n";
	    }
	}
	next if $line eq '';
	print $fh "# $node1\n$line";
    }
    return $obj;
}

#-------------------------------------------------------------

sub print2File_ncol
{
    my ( $obj , $file ) = @_;
    open FH, ">$file" or die "LGL::print2File_ncol: Open of $file failed: $!\n";
    $obj->print2FH_ncol( \*FH );
    close( FH );
    return $obj;
}

#-------------------------------------------------------------

sub print2FH_ncol
{
    my ( $obj , $fh ) = @_;
    foreach my $node1 ( sort keys %{$obj->{_edges}} )
    {
	foreach my $node2 ( sort keys %{$obj->{_edges}->{$node1}} )
	{
	    print $fh "$node1 $node2";
	    my $weight = $obj->{_edges}->{$node1}{$node2};
	    if ( $weight ne $DEFAULT_WEIGHT )
	    {
		print $fh " $weight\n";
	    }
	    else
	    {
		print $fh "\n";
	    }
	}
    }
    return $obj;
}

#-------------------------------------------------------------

sub getVertexCount
{
    my $obj = shift;    
    return $obj->{_vertexCount} if $obj->{_vertexCount};
    my %count;
    foreach my $v1 ( keys %{$obj->{_edges}} )
    {
	$count{$v1}++;
	foreach my $v2 ( keys %{$obj->{_edges}->{$v1}} )
	{
	    $count{$v2}++;
	}
    }
    return $obj->{_edgeCount} = scalar keys %count;
}

#-------------------------------------------------------------

sub getEdgeCount
{
    my $obj=shift;    
    return $obj->{_edgeCount} if defined $obj->{_edgeCount};
    my $count = 0;
    foreach my $v1 ( keys %{$obj->{_edges}} )
    {
	foreach my $v2 ( keys %{$obj->{_edges}->{$v1}} )
	{
	    $count++;
	}
    }
    return $obj->{_edgeCount} = $count;
}

#-------------------------------------------------------------

1;
__END__
