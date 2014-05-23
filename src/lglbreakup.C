//  
//  Copyright (c) 2002 Alex Adai, All Rights Reserved.
//  
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License as
//  published by the Free Software Foundation; either version 2 of
//  the License, or (at your option) any later version.
//  
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//  GNU General Public License for more details.
//  
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston,
//  MA 02111-1307 USA
//  
//
/////////////////////////////////////////////////////////////////////////

#include "configs.h"
#include "graph.hpp"
#include <vector>
#include <unistd.h> 
#include <boost/graph/connected_components.hpp>
#include <cstdlib>
#include <cstring>
#include <cstdio>
#include <iostream>
#include <iomanip>

using namespace std;

/////////////////////////////////////////////////////////////////////////

const char * doutputdir = "/tmp/lgl_temp";
const bool defaultWrite = true;
const prec_t cutoff = 1e30;
const bool defaultDoesWriteLgl = true;

/////////////////////////////////////////////////////////////////////////

typedef std::set< Graph_t::vertex_descriptor > ProcessList;
typedef std::vector< vector<int> > WriteList;

/////////////////////////////////////////////////////////////////////////

void displayUsage(char ** argv);
int writeCurrentLGL( Graph_t& g , const char * outfile , int set ,
		       WriteList& wl , bool doesWrite ,
		       prec_t cut , std::ofstream & log );
int connected_sets( const Graph_t& g , WriteList& writelist );
void addAllEdgesFromVertices( const Graph_t& g , vector<int>& components, 
			      ProcessList& process );

/////////////////////////////////////////////////////////////////////////

bool set_size_sort( const vector<int>& v1 , const vector<int>& v2 ) {
  return v1.size() > v2.size();
}

/////////////////////////////////////////////////////////////////////////

int main( int argc, char ** argv ) 
{
  // There is just one input arg, 
  // a file name with all the connections.
  if (argc == 1){
    displayUsage( argv );
  } 

  // Defaults
  const char * outputdir = doutputdir;
  bool doesWrite = defaultWrite;
  bool writeNewLGL = defaultDoesWriteLgl;
  bool useMST = false;
  prec_t cut = cutoff;
  int b;

  int optch;
  while ( (optch = getopt(argc,argv,"d:w:c:sm")) != -1 )
    {
      switch (optch) 
        {
	case 'd': outputdir = strdup(optarg); break;
	case 'w': 
	  b = atoi(optarg);
	  if (b) { doesWrite=true; }
	  else { doesWrite=false; }
	  break;
	case 'c': cut = (prec_t) atof(optarg); break;
	case 's': writeNewLGL = !writeNewLGL; break;
	case 'm': useMST = true; break;
	default: cerr << "Bad Option. Exiting."; exit(EXIT_FAILURE); 
	}
    }

  char * infile = strdup( argv[optind] );

  cerr << "Loading " << infile << "..." << flush;
  Graph_t G; 
  readLGL( G , infile , cut );
  cerr << "Done." << endl;

  int vertexCount = G.vertexCount();
  int edgeCount = G.edgeCount();
  int emin = max<int>( (int) .5 *edgeCount , 25000 );

  cerr << vertexCount <<  " : Total Vertex Count\n"
       << edgeCount <<  " : Total Edge Count\n"
       << "Determining connected sets..." << flush;

  if ( vertexCount == 0 ) { cerr << "None.\n"; return EXIT_SUCCESS; }  

  if ( useMST )
    {
      cerr << "Using MSTs..." << flush;
      if ( ! G.hasWeights() ){
	cerr << "Tree doesn't have weights. Exiting.\n";
	exit(EXIT_FAILURE);
      }
      Graph_t mst;      
      setMSTFromGraph( G , mst ); 
      G.clear();
      G = mst;
      vertexCount = G.vertexCount();
      edgeCount = G.edgeCount();
    }

  // Finds connected sets without making any recursive calls.
  // ( Protects stacks from very large graphs )
  WriteList writelist;
  int num = connected_sets( G , writelist ); 
  cerr << "\nFound " << num << " connected sets." << endl;

  if ( writeNewLGL ) { 
    string keyfile( outputdir );
    keyfile += "_new_lgl.lgl"; 
    writeLGL( G , keyfile.c_str() );
  }

  string keyfile( outputdir );
  keyfile += "_vertex_file_match";
  ofstream fileSetMatch( keyfile.c_str() );
  if ( ! fileSetMatch ) {
    cerr << "Open of " << keyfile << " failed" << endl;
    exit(EXIT_FAILURE);
  }

  int eout = 0 , sets = 0;
  // Now to make a call for each connected set.
  for ( int currentSet=0; currentSet<num; ++currentSet )
    {
      char outfile[256];
      sprintf(outfile,"%s/%d.lgl",outputdir,currentSet);
      cerr << "Writing " << outfile << endl;
      eout += writeCurrentLGL( G , outfile , sets ,
				 writelist , doesWrite ,
				 cut , fileSetMatch );
      ++sets;
      if ( eout > emin ) {
	cerr << "Remapping\n";
	remap( G );
	emin = max<int>( (int).5 * ( edgeCount - eout ), 25000 );
	edgeCount -= eout;
	eout = 0;
	connected_sets( G , writelist );
	sets = 0;
      }
    }

  return EXIT_SUCCESS;
}

/////////////////////////////////////////////////////////////////////////

int connected_sets( const Graph_t& g , WriteList& writelist )
{
  Graph_t::vertex_descriptor vother;
  Graph_t::vertex_iterator v1, v2;
  Graph_t::out_edge_iterator oe, oend;
  ProcessList process;
  vector<int> components( num_vertices(g.boostGraph()) , -1 );

  int currentComponent = 0;
  // int vertexCounter = 0;
  // cerr << "Vertex: " << std::setw(8) << vertexCounter;
  for ( tie(v1,v2)=vertices(g.boostGraph()); v1!=v2; ++v1 )
    {
      // cerr << "\b\b\b\b\b\b\b\b" << std::setw(8) <<  ++vertexCounter << flush;
      if ( components[*v1] >= 0 ) { continue; }
      components[ *v1 ] = currentComponent;
      for ( tie(oe,oend)=out_edges(*v1,g.boostGraph()); oe!=oend; ++oe )
	{
	  vother = target( *oe , g.boostGraph() );
	  components[ vother ] = currentComponent;
	  process.insert(vother);
	}
      while( ! process.empty() )
	addAllEdgesFromVertices( g , components , process );
      ++currentComponent;
    }

  // Makes the output alot quicker to figure out what gets
  // written ahead of time
  writelist.clear();
  writelist.resize( currentComponent );
  for ( vector<int>::size_type ii=0; ii<components.size(); ++ii ) {
    writelist[ components[ii] ].push_back(ii);
  }

  sort( writelist.begin() , writelist.end() , set_size_sort );

  return currentComponent;
}

/////////////////////////////////////////////////////////////////////////

void addAllEdgesFromVertices( const Graph_t& g , vector<int>& components, 
			      ProcessList& process )
{
  Graph_t::vertex_descriptor vother;
  Graph_t::out_edge_iterator oe, oend;
  if ( process.empty() ) { return; }
  ProcessList process2;
  for ( ProcessList::iterator ii=process.begin(); ii!=process.end(); ++ii )
    {
      for ( tie(oe,oend)=out_edges(*ii,g.boostGraph()); oe!=oend; ++oe )
	{
	  vother = target( *oe , g.boostGraph() );
	  if ( components[ vother ] >= 0 ) { continue; }
	  components[ vother ] = components[ *ii ];
	  process2.insert( vother );
	}
    }
  process = process2;
}

/////////////////////////////////////////////////////////////////////////

int writeCurrentLGL( Graph_t& g , const char * outfile , int cset ,
		       WriteList& writelist , bool doesWrite ,
		       prec_t cut , std::ofstream& log )
{
  // Create a new graph of just the selected edges
  Graph_t::out_edge_iterator ei , eend;
  Graph_t::vertex_descriptor v1, v2;
  Graph_t newG;
  Graph_t::weight_type w;
  Graph_t::boost_graph& newg = newG.boostGraph();
  bool madeit = false;
  vector< Graph_t::edge_descriptor > edges2remove;
  set< int > already;

  for ( vector<int>::iterator ii=writelist[ cset ].begin();
	  ii!=writelist[ cset ].end(); ++ii ) {
    for ( tie(ei,eend)=out_edges( *ii, g.boostGraph() ); ei!=eend; ++ei )
      {
	v1 = source( *ei , g.boostGraph() );
	v2 = target( *ei , g.boostGraph() ); 
	if ( g.idFromIndex( v1 ) > g.idFromIndex( v2 ) ) { continue; }
	if ( already.find( v1 ) == already.end() ) {
	  log << g.idFromIndex( v1 ) << " " << outfile << '\n';
	  already.insert( v1 );
	}
	if ( already.find( v2 ) == already.end() ) {
	  log << g.idFromIndex( v2 ) << " " << outfile << '\n';
	  already.insert( v2 );
	}
	if ( ! g.hasWeights() ) {  
	  add_edge( v1 ,v2 , newg );
	} else { 
	  w = g.getWeight( *ei );
	  if ( w <= cut ) {
	    add_edge(v1 ,v2 ,w ,newg);
	    madeit = true;     
	  }   
	}
	edges2remove.push_back( *ei );
      }
  }

  newG.vertexIdMap( g.vertexIdMap() );
  if ( g.hasWeights() && madeit ) { newG.hasWeights(true); }
  remap ( newG ); 
  int edgecount = newG.edgeCount();
  cerr << newG.vertexCount() <<  " : Vertex Count\n"
       << edgecount << " : Edge Count" << endl;
  if ( doesWrite ) { writeLGL( newG , outfile ); }

  // This is necessary for subsequent calls to writeCurrentLGL with
  // very large graphs.
  for ( vector< Graph_t::edge_descriptor >::iterator ii=edges2remove.begin();
	ii!=edges2remove.end(); ++ii ) {
    g.removeEdge( *ii );
  }

  writelist[ cset ].clear();

  return edgecount;
}

/////////////////////////////////////////////////////////////////////////

void displayUsage( char ** argv )
{
  cerr << "\nUsage: " << argv[0] 
       << " [-d outputDirectory] [-w doesWriteBool0or1]\n\t"
       << "[-c cutoff] [-s] graph.lgl\n\n"
       << "\t-s\tToggle new .lgl file write.\n"
       << "\n\tDefault output dir: " << doutputdir << '\n'
       << "\tDefault Write: " <<  defaultWrite << '\n'
       << "\tDefault Cutoff: " << cutoff << '\n'
       << "\tDoes Write New Lgl: " << defaultDoesWriteLgl << endl;
  exit( EXIT_FAILURE );
}

/////////////////////////////////////////////////////////////////////////
