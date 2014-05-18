#-------------------------------------------------------------
# This perl module is used to parse variables out
# of config files. The format used must always be of
# the form:
# VARIABLE=VALUE(\n|\s+#)
# An important part is delimiting VALUE by '=' and 
# a carriage return or # or space. Value will be read in as every-
# thing between those 2. 
# Comments can be included in the config file by starting
# the line with a '#' (no surprise here).
#-------------------------------------------------------------
  
package ParseConfigFile;
use strict;

our ( $VERSION );

#-------------------------------------------------------------

BEGIN
{ 
    $VERSION = 1.0;
}

#-------------------------------------------------------------

sub new
{
    my $pkg = shift;
    my $file = shift;
    my $vars = {};

    # Open the configfile and read it in
    die "ParseConfigFile: Could not open $file" unless open CONFIGFILE, "<$file";
    
    while(<CONFIGFILE>)
    {
	# If line starts with a # do nothing...
	next if (/^\#/);
	# The vars must have an equal sign. (pretty reasonable).
	if (/=/) { cleanUpLine( $vars, $_); }
    }

    close(CONFIGFILE);
    
    bless $vars, $pkg;
}

#-------------------------------------------------------------

sub cleanUpLine
{
    my ( $obj , $line ) = @_;

    # First we have trim the string
    chomp($line);

    # Don't count anything past the comment markers.
    my $commentCtr = index( $line , '#' );
    if ($commentCtr != -1)
    {
	$line = substr($line, 0, $commentCtr);
    }

    # First we have to find the '='
    my @varsVals = split('=',$line);
    $varsVals[0] =~ s/\s//g;             # Take out any spaces
    $varsVals[1] =~ m/('|")(.*)('|")/; # Take whats btw the quotes

    $obj->{$varsVals[0]} = $2;
}

#-------------------------------------------------------------

sub value
{
    my ( $obj , $key ) = @_;
    return $obj->{$key};
}

#-------------------------------------------------------------

END{ }
1;
