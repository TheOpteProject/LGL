/////////////////////////////////////////////////////////////////////////

#include "configs.h"
#include "graph.hpp"
#include <iostream>
#include <iomanip>

using namespace std;

/////////////////////////////////////////////////////////////////////////

void usage( char ** a );

/////////////////////////////////////////////////////////////////////////

int main( int argc, char ** argv ) 
{
  // There is just one input arg, 
  // a file name with all the connections.
  if (argc != 3)
    usage( argv );

  string infile  = argv[1];
  string outfile = argv[2];

  Graph_t g; 
  
  if ( (infile.find(".ncol") != std::string::npos &&
	infile.find(".lgl") == std::string::npos) || 
       (outfile.find(".lgl") != std::string::npos &&
	outfile.find(".ncol" ) == std::string::npos) )
    {
      cerr << "Converting .ncol file ---> .lgl file\n";
      cerr << "Loading " << infile << "..." << flush;
      readNCOL( g , infile.c_str() );
      cerr << " Done.\nWriting " << outfile << "..." << flush;
      writeLGL( g , outfile.c_str() );
      cerr << " Done.\n";
    }
  else if ( (infile.find(".lgl") != std::string::npos &&
	     infile.find(".ncol") == std::string::npos ) || 
	    (outfile.find(".ncol") != std::string::npos &&
	     outfile.find(".lgl" ) == std::string::npos ) )
    {
      cerr << "Converting .lgl file ---> .ncol file\n";
      cerr << "Loading " << infile << "..." << flush;
      readLGL( g , infile.c_str() );
      cerr << " Done.\nWriting " << outfile << "..." << flush;
      writeNCOL( g , outfile.c_str() );
      cerr << " Done.\n";
    }
  else
    {
      cerr << "Could not determine file types.\n";
    }

  return EXIT_SUCCESS;
}

void usage( char ** argv )
{
  cerr << "\n Usage:\n\t" << argv[0] << " infile.ncol outfile.lgl\n\n\tOR\n\n\t";
  cerr << argv[0] << " infile.lgl outfile.ncol\n\n";
  exit(EXIT_FAILURE);
} 
