//
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

#include <iostream>
#include <exception>

#include <unistd.h>
#include <pthread.h>
#include <assert.h> 
#include <iomanip>
#include <cstdio>
#include <cstdlib>

#include "configs.h"
#include "calcFuncs.h"
#include "particleInteractionHandler.hpp"

//#include <boost/timer.hpp>

using namespace std;
using namespace boost;

void displayUsage(char ** argv);

int main( int argc, char ** argv )
try
{

  //  boost::timer start_time;

  // There is just one input arg, 
  // a file name with all the connections.
  if (argc == 1){
    displayUsage(argv);
  } 

  TimeKeeper_t timer;

  // Now to deal with the options. First set to
  // defaults and if options are passed the vars
  // will be updated.
  int optch;
  char * initPosFile = 0;
  char * initMassFile = 0;
  char * rootNode = 0;
  const char * outfile = "lgl.out";
  std::string anchorsFile;
  bool doesWritemstfile = false;
  long processorCount = 1;
  prec_t cutOffPrecision = .00001;
  prec_t placementDistance = -1.0;
  prec_t placementRadius = .1;
  prec_t nbhdRadius = INTERACTION_RADIUS;
  prec_t eqDistance = INTERACTION_RADIUS/2;
  prec_t nodeSizeRadius = NODE_SIZE;
  prec_t mass = DEFAULT_NODE_MASS;
  prec_t outerRadius = -1.0;
  int writeInterval = WRITE_INTERVAL;
  prec_t casualSpringConstant = DEFAULT_SPRING_CONSTANT;
  prec_t specialSpringConstant = DEFAULT_SPRING_CONSTANT;
  EllipseFactors ellipseFactors;
  bool doesWriteEdgeLevels = false;
  bool useOriginalWeights = false;
  bool layoutTreeOnly = false;
  bool doesWriteLogFile = true;
  bool placeLeafsClose = false;
  bool isSilent = false; // Show progress
  bool disregardDisconnectedNodes = false;

  timer.max(MAXITER);
  timer.timeStep(PART_TIME_STEP);

  while ( (optch = getopt(argc,argv,"x:a:t:m:M:i:s:r:k:T:R:S:W:z:o:leOyu:v:Iq:E:L:D")) != -1 )
    {
      switch (optch) 
        {
        case 'x': initPosFile = strdup(optarg); break; 
        case 'a': anchorsFile = optarg; break; 
        case 'm': initMassFile = strdup(optarg); break;
        case 'M': mass = atof(optarg); break;
	case 't': processorCount = atol(optarg); break;
	case 'i': timer.max( atoi(optarg) ); break;
	case 's': specialSpringConstant = atof(optarg); break;
	case 'r': nbhdRadius = atof(optarg); break;
	case 'R': outerRadius = atof(optarg); break;
	case 'T': timer.timeStep( atof(optarg) ); break;
	case 'S': nodeSizeRadius = atof(optarg); break;
	case 'W': writeInterval = atoi(optarg); break;
	case 'k': casualSpringConstant = atof(optarg); break;
	case 'z': rootNode = strdup(optarg); break;
	case 'o': outfile = strdup(optarg); break;
	case 'l': doesWriteEdgeLevels = !doesWriteEdgeLevels; break;
	case 'e': doesWritemstfile = true; break;
	case 'O': useOriginalWeights = true; break;
	case 'y': layoutTreeOnly = true; break;
	case 'u': placementDistance = atof(optarg); break;
	case 'v': placementRadius = atof(optarg); break;
	case 'I': isSilent = true; break;
	case 'q': eqDistance = atof(optarg); break;
	case 'E': ellipseFactors = parseEllipseFactors(optarg); break;
	case 'L': placeLeafsClose = true; break;
	case 'D': disregardDisconnectedNodes = true; break;
	default : cerr << "Bad option -\t" << (char) optch 
		       << '\n'; exit(EXIT_FAILURE);
        }
    }

  cout << "Reading in Graph from " << argv[optind] << "..." << flush;
  Graph_t G;
  readLGL( G , argv[optind] );
  cout << "\nVertex Count: " << G.vertexCount() << '\n'
       << "Edge Count: " << G.edgeCount() << endl;

  // Simple check to see if we are going to use
  // non-existent weights
  if ( useOriginalWeights && ! G.hasWeights() ) {
    cerr << "\nYou want to use weights but none\n"
	 << "are provided. Exiting...\n";
    exit(EXIT_FAILURE);
  }

  // Create the particles now based on the graph.
  NodeContainer nodes( G.vertexCount() );
  // Initialize the ids of the nodes
  for ( NodeContainer::size_type ii=0; ii<nodes.size(); ++ii ) {
    nodes.ids[ii] = G.idFromIndex( ii );
    nodes[ii].id( G.idFromIndex( ii ) );
  }

  // This determines the space necessary to place all the nodes.
  if ( outerRadius < 0 ) {
    if ( DIMENSION == 2 ) { 
      outerRadius = (prec_t) sqrt( (double) nodes.size() );
    } else if ( DIMENSION == 3 ) {
      outerRadius = (prec_t) pow( (double) nodes.size() , .33333 );
    } else { cerr << "Only 2 or 3 dimensions\n"; exit(EXIT_FAILURE); }
    cerr << "Outer radius is set to " << outerRadius << endl;
  } 

  cout << "Initializing " << nodes.size() << " particles..."; cout.flush(); 
  PCChaperone chaperone( nodes ); 
  if ( initPosFile != 0 ) { chaperone.initPos(initPosFile); }
  else {  chaperone.randomizePosRange(outerRadius); }
  if ( initMassFile != 0 ) { chaperone.initMass(initMassFile); }
  else { chaperone.initMass(mass); }
  if ( !anchorsFile.empty() )
	  chaperone.initAnchors( anchorsFile );
  chaperone.initRadius(nodeSizeRadius);
  chaperone.posOutFile( outfile );
  chaperone.initAllParticles(); 
  if ( initPosFile )
	  interpolateUninitializedPositions( chaperone, G.boostGraph(), disregardDisconnectedNodes );	// without this call the_internet's results become unacceptably stretched and ugly
  cout << "Done." << endl;

  cout << "Initializing grid and placing particles..." << flush;
  Grid_t grid;
  prec_t voxelLength = nbhdRadius;
  gridPrepAndInit( nodes , grid , voxelLength );
  cout << "Done." << endl;

  cout << "Initializing handlers..."; cout.flush();
  VoxelHandler vh;
  NodeInteractionHandler nh;
  nh.timeStep( timer.timeStep() );
  nh.noiseAmplitude( 1.0 );
  GridSchedual_t schedual(grid);
  bool acceptable = schedual.threads( processorCount );
  long threadCount = schedual.threads();
  while ( !acceptable ) {
    acceptable = schedual.threads( --threadCount );
  }
  schedual.generateVoxelList_MT();
  cout << "Done." << endl;

  // First generate the tree to guide the layout
  unsigned int totalLevels = 1;
  LevelMap levels( nodes.size() , 1 );  
  ParentMap parents( nodes.size() , 0 );  
  Graph_t lG; // The graph that is currently being treated
  lG.vertexIdMap( G.vertexIdMap() );
  Graph_t mst; // The tree generated to guide the layout
  mst.vertexIdMap( G.vertexIdMap() ); 
  mst.weights( G.weights() );
  Graph_t::vertex_descriptor root = 0;
  if ( ! initPosFile ) {
    cout << "Generating Tree and checking for root.\nChecking for root node ... " << flush;
    if ( rootNode ) { 
      // Use the provided root node
      string r(rootNode);
      root = G.indexFromId(r);
      tie( root , totalLevels ) =
	generateLevelsFromGraph( G , levels , parents , &root , mst ,
				 useOriginalWeights );
    } else {
      // Try to find the root node
      tie( root , totalLevels ) =
	generateLevelsFromGraph( G , levels , parents , 
				 (Graph_t::vertex_descriptor *)0 , mst ,
				 useOriginalWeights );
    }
    cout << "Root Node: " << G.idFromIndex( root ) << "\n"
	 << "There are " << totalLevels << " levels." << endl;
    // Place root in graph
    shift_particle( nodes[root] , grid );
    for ( NodeContainer::size_type ii=0; ii<nodes.size(); ++ii ) { 
      if ( root != ii ) {
         if ( nodes[ii].isAnchor() )
            shift_particle( nodes[ii], grid );	// Place anchor in graph
         else
            nodes[ii].X(1e6); // Put everything else in some crazy place (pretty helpful for finding bugs)
      }
    }
  } else {
    cout << "Coords provided, skipping Tree Generation.\n";
    lG = G; // Give the full graph, counting all interactions
  }

  // Do we want to overwrite the given graph with the layout tree
  if ( layoutTreeOnly ) {
    G.clear();
    G = mst;
    //placeLeafsClose = true;
    tie( root , totalLevels ) =
      generateLevelsFromGraph( G , levels , parents , &root , mst ,
			       useOriginalWeights );
  }

  // This levelmap outputs which 'level' each edge is on. The
  // level is determined as the hop number from the root node. 
  if ( doesWriteEdgeLevels ) {
    string levelfile( outfile );
    levelfile += ".edge_levels";
    writeLevelMap2File( G , levels , levelfile.c_str() );
  }

  // This will output the tree used to guide the layout
  if ( doesWritemstfile ) {
    string mstfile( outfile );
    mstfile += ".mst.lgl";
    writeLGL( mst , mstfile.c_str() );
  }
  
  // Don't need anymore
  mst.clear();

  // Print the root node used to a file
  string rootfile( outfile );
  rootfile += ".root";
  ofstream rroot( rootfile.c_str() );
  if ( ! rroot ) {
    cerr << "Open of " << rootfile << " failed\n";
    exit(EXIT_FAILURE);
  }
  rroot << G.idFromIndex( root ) << '\n';
  rroot.close();

  cout << "Initializing " << threadCount << " thread(s)..."; cout.flush();
  ThreadContainer threads( threadCount );
  threads.defaultThread.scope(PTHREAD_SCOPE_SYSTEM);
  threads.applyAttributes();
  ThreadArgs * threadArgs = new ThreadArgs[threadCount];
  for( long threadCtr=0; threadCtr<threadCount; ++threadCtr )
    {
      ThreadArgs& current = threadArgs[threadCtr];
      current.nodes = &nodes;
      current.eqDistance = eqDistance;
      current.ellipseFactors = ellipseFactors;
      current.grid = &grid;
      current.nbhdRadius = nbhdRadius;
      current.threadCount = threadCount;
      current.voxelList = new FixedVec_l[grid.size()/threadCount+1];
      current.voxelListSize = 
	schedual.getVoxelList( threadCtr , current.voxelList );
      current.whichThread = threadCtr;
      current.gridIterator = new GridIterator(grid);
      current.gridIterator->id(threadCtr);
      current.stats = new ParticleStats_t();
      current.casualSpringConstant = casualSpringConstant;
      current.specialSpringConstant = specialSpringConstant;
      current.stats->collectStatsAtIteration( writeInterval ); 
      current.nodeHandler = new NodeInteractionHandler(nh);
      current.nodeHandler->id( threadCtr );
      current.nodeHandler->forceLimit( .1 * voxelLength /
				       static_cast<prec_t>(timer.timeStep()) );
      current.nodeHandler->eqDistance( eqDistance );
      current.nodeHandler->ellipseFactors( ellipseFactors );
      current.voxelHandler = new VoxelHandler(vh);
      current.voxelHandler->id(threadCtr);
      current.voxelHandler->interactionHandler(*(current.nodeHandler));
      current.full_graph = &G;
      current.layout_graph = &lG;
      current.levels = &levels;
      current.parents = &parents;
    }
  cout << "Done." << endl;

  bool givenCoords = false;
  if ( initPosFile != 0 ) { 
    givenCoords = true; 
    for ( NodeContainer::size_type ii=0; ii<nodes.size(); ++ii ) {
      shift_particle( nodes[ii] , grid );
    }
  }

  //boost::timer simulation_time_begin;

  beginSimulation( threads , cutOffPrecision , timer , 
		   threadArgs , chaperone , totalLevels , 
		   givenCoords , placementDistance , placementRadius ,
		   placeLeafsClose , isSilent );
  // Final settle
  cutOffPrecision *= .1;
  cerr << "\nFinal Settle\n";
  beginSimulation( threads , cutOffPrecision , timer , 
		   threadArgs , chaperone , totalLevels , 
		   true , placementDistance , placementRadius ,
		   placeLeafsClose , isSilent ); 

  //boost::timer simulation_time_end;

  chaperone.posOutFile( outfile );
  chaperone.writeOutFiles();

  //boost::timer stop_time;

  // This will output the most important parameters in order to
  // make this layout more reproducible
  if ( doesWriteLogFile ) {
    string logfile( outfile );
    logfile += ".log";
    ofstream log( logfile.c_str() );
    if ( !log ) {
      cerr << "Open of logfile " << logfile << " failed.\n";
      exit(EXIT_FAILURE);
    }
    // The command line argument that called this program:
    log << "Command Line:";
    for ( int ii=0; ii<argc; ++ii ) { log << " " << argv[ii]; }
    log << '\n';
    if ( initPosFile != 0 )
      log << "Init Position File: " << initPosFile << '\n';
    if ( initMassFile != 0 )
      log << "Init Mass File: " << initMassFile << '\n';
    if ( !anchorsFile.empty() )
       log << "Anchors File: " << anchorsFile << '\n';
    log << "Root Node: " << G.idFromIndex( root ) << '\n'
	<< "Outfile: " <<  outfile << '\n'
	<< "Does Write MST File: " <<  doesWritemstfile  << '\n'
	<< "Thread Count: " <<  threadCount << '\n'
	<< "Precision: " <<  cutOffPrecision << '\n'
	<< "Placement Distance: " <<  placementDistance << '\n'
	<< "Placement Radius: " << placementRadius << '\n'
	<< "Nbhd Radius: " << nbhdRadius << '\n'
	<< "E.Q. Distance: " << eqDistance << '\n'
	<< "Node Radius: " << nodeSizeRadius << '\n'
	<< "Default Mass: " <<  DEFAULT_NODE_MASS << '\n'
	<< "Outer Radius: " << outerRadius << '\n'
	<< "Write Interval: " << writeInterval << '\n'
	<< "Casual Spring Constant: " << casualSpringConstant << '\n'
	<< "Special Spring Constant: " << specialSpringConstant << '\n'
	<< "Does Write Edge Levels: " << doesWriteEdgeLevels << '\n'
	<< "Use Original Weights: " << useOriginalWeights << '\n'
	<< "Ellipse Factors: ";
	std::copy( ellipseFactors.begin(), ellipseFactors.end(), std::ostream_iterator< prec_t >( log, " " ) );
	log << '\n'
	<< "Layout Tree Only: " << layoutTreeOnly << '\n';
    //double elapsed = start_time.elapsed() - stop_time.elapsed();
    //log << "Total Run Seconds: " << elapsed << '\n';
    //elapsed = simulation_time_begin.elapsed() - simulation_time_end.elapsed();
    //log << "Simulation Seconds: " << elapsed << '\n'
    log << "Place Leafs Close: " << placeLeafsClose << '\n'
	<< '\n';   
  }

  cout << "\n - Done - " << endl;
  
  return EXIT_SUCCESS;
} 
catch ( std::exception const &e ) {
	std::cerr << "Error: " << e.what() << '\n';
	return EXIT_FAILURE;
}

//----------------------------------------------------------

void displayUsage(char ** argv)
{
  cerr << "\nUsage: " << argv[0] << " [-x InitPositionFile] [-a AnchorsFile]" 
       << "\n\t[-t ThreadCount] [-m InitMassFile] [-i IterationMax] "
       << "\n\t[-s] [-r nbhdRadius] [-T timeStep] [-S nodeSizeRadius]\n"
       << "\t[-k casualSpringConstant] [-s specialSpringConstant]\n"
       << "\t[-e] [-l] [-y] [-q EQ Distance] [-u placementDistance]\n"
       << "\t[-E ellipseFactors] [-v placementRadius] [-L] nodeFile.lgl\n\n";
  cerr << "\n\t-[mx]\t A file that has the node id followed by\n"
       << "\t\tthe initial values.\n";
  cerr << "\n\t-t\tThe number of threads to spawn.\n"
       << "\t\tThis is capped by the processor count.\n";
  cerr << "\n\t-i\tThe maximum number of iterations.\n";
  cerr << "\n\t-r\tThe neighborhood radius for each particle. It\n"
       << "\t\tdefines the interaction range for casual (generally\n"
       << "\t\trepulsive) interactions\n";
  cerr << "\n\t-T\tThe time step for each iteration\n";
  cerr << "\n\t-S\tThe 'radius' of each node.\n";
  cerr << "\n\t-M\tThe 'mass' of each node.\n";
  cerr << "\n\t-R\tThe radius of the outer perim.\n";
  cerr << "\n\t-W\tThe write interval.\n";
  cerr << "\n\t-z\tRoot node you want to use.\n";
  cerr << "\n\t-l\tWrite out the edge level map.\n";
  cerr << "\n\t-e\tOutput the mst used.\n";
  cerr << "\n\t-O\tUse original weights.\n";
  cerr << "\n\t-y\tLayout the tree only.\n";
  cerr << "\n\t-I\tDon't show layout progress, be quiet (kinda)\n";
  cerr << "\n\t-q\tEquilibrium distance.\n";
  cerr << "\n\t-E\tEllipse factors.\n";
  cerr << "\n\t-u\tPlacement distance is the distance you want\n"
       << "\t\tthe next level to be placed with respect to\n"
       << "\t\tthe previous level. If this float value is not\n"
       << "\t\tgiven a formula calculates the placement distance.\n";
  cerr << "\n\t-v\tPlacement radius is a measure of the placement density\n";
  cerr << "\n\t-L\tPlace the leafs close by. This applies to trees more than\n"
       << "\t\tgraphs. Setting this option will place the child vertices very\n"
       << "\t\tnear the parent vertex if all of its children have none themselves.\n";
  cerr << "\n";
  exit(EXIT_FAILURE);
}

//----------------------------------------------------------



