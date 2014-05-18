#ifndef _CONFIGS_H_
#define _CONFIGS_H_

//------------------------------------------------------

#include "particle.hpp"
#include "voxel.hpp"
#include "grid.hpp"
#include "particleContainer.hpp"
#include "particleContainerChaperone.hpp"
#include "particleStats.hpp"
#include "particleInteractionHandler.hpp"
#include "aPthread.hpp"
#include "timeKeeper.hpp"
#include "voxelInteractionHandler.hpp"
#include "gridSchedual.hpp"
#include "graph.hpp"
#include <vector>
#include <set>
#include <algorithm>
#include <iterator>

//------------------------------------------------------

typedef float prec_t;
const unsigned int DIMENSION = 3;

//------------------------------------------------------

typedef Particle< prec_t , DIMENSION > Node;
typedef ParticleContainer< Node > NodeContainer;
typedef Grid< Node > Grid_t;
typedef FixedVec< prec_t , DIMENSION > FixedVec_p;
typedef FixedVec< long , DIMENSION > FixedVec_l;
typedef GridIter< Grid_t > GridIterator;
typedef ParticleContainerChaperone< Node > PCChaperone;
typedef ApthreadContainer ThreadContainer;
typedef Voxel< Node > Voxel_t;
typedef TimeKeeper<prec_t> TimeKeeper_t;
typedef ParticleInteractionHandler< Node > NodeInteractionHandler;
typedef VoxelInteractionHandler< Voxel_t , NodeInteractionHandler > VoxelHandler;
typedef GridSchedual_MTS< Grid_t > GridSchedual_t;
typedef ParticleStats< Node > ParticleStats_t;
typedef Graph< prec_t > Graph_t;
typedef vector< unsigned > LevelMap;
typedef vector< unsigned > ParentMap;
typedef vector< bool > PlacementStatus;

//------------------------------------------------------

// Time step for the force calcs
const prec_t PART_TIME_STEP = .001;

// The program stops and writes to file
// at this count. (Default value).
const int MAXITER = 250000;

// This automatically dums xcoords to file
// at these multiples.
const int WRITE_INTERVAL = 0; // Off by default

// The range that make casual interactions 
// applicable
const prec_t INTERACTION_RADIUS = 1.0;

// The default value of k in F=-kx for casual interactions
const prec_t DEFAULT_SPRING_CONSTANT = 10.0;

// This is an initial force term for the 
// drag. The drag probably increases with 
// each time step though.
const prec_t INIT_RESISTANCE = 1.0;

// This is the scale factor for all the spring constants
// (Scales col1 of the relational data)
const prec_t SPRING_CONSTANT_FACTOR = 1.0;

// This is the size of each node. It is important to 
// have a size since if a particle gets really really
// really close the unit vector -> infinite
//const prec_t NODE_SIZE = BEST_EQ_DISTANCE*.25;
const prec_t NODE_SIZE = .01;

// If this = 0 the input data has no connection strengths.
const int DEFAULT_WEIGHT_FORMAT = 1;

// This is the default mass size of each node.
const prec_t DEFAULT_NODE_MASS = 1.0;

//------------------------------------------------

#endif
