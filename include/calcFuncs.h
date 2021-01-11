// This file has functions which the threads
// could call.

#ifndef CALCFUNCS_H_
#define CALCFUNCS_H_

#include "configs.h"
#include "fixedVecOperations.hpp"
#include "sphere.hpp"
#include <iostream>
#include <iomanip>
#include <boost/graph/adjacency_list.hpp>
#include <boost/graph/kruskal_min_spanning_tree.hpp>
#include <boost/graph/visitors.hpp>
#include <boost/graph/breadth_first_search.hpp>
#include <boost/property_map/property_map.hpp>
#include <cstdlib>
#include <cstdio>
#include <vector>
#include <algorithm>

//------------------------------------------------

using namespace boost;
typedef adjacency_list< vecS, vecS, directedS > out_graph;

//------------------------------------------------

struct ThreadArgs
{
  NodeContainer * nodes;
  VoxelHandler * voxelHandler;
  NodeInteractionHandler * nodeHandler;
  FixedVec_l * voxelList;
  ParticleStats_t * stats;
  long voxelListSize;
  Grid_t * grid;
  prec_t eqDistance;
  EllipseFactors ellipseFactors;
  GridIterator * gridIterator;
  long threadCount;
  long whichThread;
  prec_t nbhdRadius;
  prec_t casualSpringConstant;
  prec_t specialSpringConstant;
  Graph_t * full_graph;
  Graph_t * layout_graph;
  LevelMap * levels;
  ParentMap * parents;
  unsigned int currentLevel;
};

//------------------------------------------------

void * calcInteractions ( void * arg );
void * integrateParticles ( void * arg );
void * onlyEdgeInteractions( void * arg );
void * collectEdgeStats( void * arg ); 

//------------------------------------------------

int generateMSTFromNodes( NodeContainer& nodes ,  ThreadArgs * args , long p );

prec_t collectOutput ( ThreadArgs * args , PCChaperone& chaperone  );
void beginSimulation( ThreadContainer& threads , 
		      prec_t cutOffPrecision , TimeKeeper_t& timer, 
		      ThreadArgs * threadArgs , PCChaperone& chaperone ,
		      unsigned int totalLevels , bool b , prec_t placementDistance,
		      prec_t placementRadius , bool placeLeafsClose , bool silentOutput);

void adjustWeightsBasedOnChildrenCount( NodeContainer& nodes );
void solidifyLargeEdges( NodeContainer& nodes );

FixedVec_p calcCenterOfMass( Graph_t& g , NodeContainer& nodes ,  LevelMap& levels ,  unsigned int currentLevel);

void initializeCurrentLayer( Graph_t& g , NodeContainer& nodes , LevelMap& levels ,
			     ParentMap& p , Grid_t& grid , unsigned int currentLevel, Graph_t& full ,
			     prec_t placementDistance, prec_t placementRadius , bool placeLeafsClose );


long activeEdgeCount( Node& n );

prec_t  activateNextLayerOfEdges( NodeContainer& n , unsigned int currentLevel , ThreadArgs * threadArgs );
prec_t getBufferDistance( Node& n );
void printOutput(long i,prec_t d, long ll, ostream& o );

void layerNPlacement( NodeContainer& nodes , Grid_t& grid , out_graph& g , FixedVec_p& cm , unsigned int currentLevel , ParentMap& parents , Graph_t& full, LevelMap& lm , prec_t placementDistance,
		      prec_t placementRadius , bool placeLeafsClose );
void generatePlacementVector( FixedVec_p& d , const FixedVec_p& parentNode,
			      const FixedVec_p& parentParentNode , const FixedVec_p& cm );
prec_t placementFormula( prec_t placementDistance , int vertices2place, int dimension );

void gridPrepAndInit( NodeContainer& nc, Grid_t& g , prec_t voxelLength );
bool doesVertexHaveAnyChildren( Graph_t& G, Graph_t::vertex_descriptor v , out_graph& g , ParentMap& parents );

EllipseFactors parseEllipseFactors( const std::string& optionStr );

// Attempts to initialize any particle positions that are not initialized yet, by way of interpolation from its neighbors which have initialized positions already, if any.
// This is done by picking the "center point" of those neighbors' positions to be used as the initial position for a particle that wasn't initialized before.
// The process continues until either all particles have initial positions properly set, or no further progress can be made (due to isolated and totally uninitialized islands).
// Also, the progress of the stages and the final accomplishment is printed to stdout.
void interpolateUninitializedPositions( PCChaperone& chaperone, const Graph_t::boost_graph& g, bool remove_disconnected_nodes );

//------------------------------------------------

#endif

