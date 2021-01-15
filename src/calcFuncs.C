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

#include <boost/algorithm/string/split.hpp>
#include <boost/algorithm/string/classification.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/foreach.hpp>

#include "thread_pool.hpp"
#include "particleInteractionHandler.hpp"
#include "calcFuncs.h"

using namespace boost;

//---------------------------------------------------------------

void * calcInteractions ( void * arg_ )
{
  //cout << "VoxelInteractions" << endl;  
  ThreadArgs * args = static_cast<ThreadArgs*>(arg_);
  GridIterator& grid_i = *(args->gridIterator);
  VoxelHandler& vh = *(args->voxelHandler);
  args->nodeHandler->springConstant( args->casualSpringConstant );
  //const Graph_t& layout_graph = *(args->layout_graph);
  //layout_graph.print();
  for ( long ii=0; ii<args->voxelListSize; ++ii ) {
    grid_i.current( args->voxelList[ii] );
    Voxel_t& vox1 = grid_i.currentVox();
    if ( ! vox1.empty() ) {
      // Iterate through the neighbors of each voxel
      // The first nbhr is actually the same voxel
      while ( grid_i.incNbhr() ) {
	Voxel_t& vox2 = grid_i.nhbrVox();
	if ( ! vox2.empty() ) {	  
	  vh.twoVoxelInteractions( vox1 , vox2 );
	} 
      }
    }
  }
  return args;
}

//---------------------------------------------------------------

void * integrateParticles ( void * arg_ )
{
  typedef Graph_t::vertex_iterator Vi;
  //cout << "integrateParticles" << endl;
  ThreadArgs& args = *(static_cast<ThreadArgs*>(arg_));  
  long whichThread = args.whichThread;
  long threadCount = args.threadCount;
  NodeContainer& nodes = *(args.nodes);
  Grid_t& grid = *(args.grid);
  NodeInteractionHandler& nih = *(args.nodeHandler);
  const Graph_t& layout_graph = *(args.layout_graph);
  const LevelMap& levels = *(args.levels);
  unsigned int currentLevel = args.currentLevel;
  Vi v, vend;
  int vertexCount = num_vertices( layout_graph.boostGraph() );
  tie( v , vend ) = vertices( layout_graph.boostGraph() );
  if ( whichThread != 0 ) { std::advance( v , whichThread ); }
  for ( int vctr = whichThread; vctr<vertexCount; vctr+=threadCount ) {
    if ( vctr != whichThread ) { std::advance( v , threadCount ); }
    //cout << *v << " " << levels.size() << endl;
    if ( levels[*v] > currentLevel ) { continue; }
    //cout << *v << " -" << endl;
    Node& n = nodes[ *v ];
    nih.enforceFLimit( n ); 
    nih.integrate( n );
    // This is an edge of the grid check.
    if ( grid.checkInclusion( n.X() ) ) { shift_particle( n , grid ); }
    else {
      // Particle is outside the grid.
    }
    // Reset the forces to zero for next 
    // iteration
    n.F(0);
  }
  return arg_;
}

//---------------------------------------------------------------

void * collectEdgeStats( void * arg_ )
{
  //cout << "collectingStats" << endl;
  typedef Graph_t::edge_iterator Ei;
  typedef Graph_t::vertex_descriptor vertex_descriptor;
  ThreadArgs& args = *(static_cast<ThreadArgs*>(arg_));
  const unsigned int currentLevel = args.currentLevel;
  const LevelMap& levels = *(args.levels);
  NodeContainer& nodes = *(args.nodes);
  long whichThread = args.whichThread;
  long threadCount = args.threadCount;
  const Graph_t& layout_graph = *(args.layout_graph);
  int edgeCount = num_edges( layout_graph.boostGraph() );
  int ctr = 0;
  prec_t dx = 0;
  Ei ei , eend;
  tie( ei , eend ) = edges( layout_graph.boostGraph() );
  if ( whichThread != 0 ) { std::advance( ei , whichThread ); }
  for ( int ectr = whichThread; ectr<edgeCount; ectr+=threadCount )
    {
      if ( ectr != whichThread ) { std::advance( ei , threadCount ); }
      vertex_descriptor v1 = source( *ei , layout_graph.boostGraph() );
      vertex_descriptor v2 = target( *ei , layout_graph.boostGraph() );
      if ( levels[ v1 ] == currentLevel ||
	   levels[ v2 ] == currentLevel )
	{
	  const Node& n1 = nodes[ v1 ];
	  const Node& n2 = nodes[ v2 ];
	  dx += n1.X().distance( n2.X() );
	  ++ctr;
	}
    }
  args.stats->add2Stats_dx( dx );
  args.stats->count( ctr );  
  return arg_;
}

//---------------------------------------------------------------

void * onlyEdgeInteractions( void * arg_ )
{
  //cout << "onlyEdgeInteractions" << endl;
  typedef Graph_t::edge_iterator Ei;
  ThreadArgs& args = *(static_cast<ThreadArgs*>(arg_));
  NodeContainer& nodes = *(args.nodes);
  long whichThread = args.whichThread;
  long threadCount = args.threadCount;
  NodeInteractionHandler& nih = *(args.nodeHandler);
  nih.springConstant( args.specialSpringConstant );
  const Graph_t& layout_graph = *(args.layout_graph);
  int edgeCount = num_edges( layout_graph.boostGraph() );
  Ei ei , eend;
  tie( ei , eend ) = edges( layout_graph.boostGraph() );
  if ( whichThread != 0 ) { std::advance( ei , whichThread ); }
  for ( int ectr = whichThread; ectr<edgeCount; ectr+=threadCount )
    {
      if ( ectr != whichThread ) { std::advance( ei , threadCount ); }
      Node& n1 = nodes[ source( *ei , layout_graph.boostGraph() ) ];
      Node& n2 = nodes[ target( *ei , layout_graph.boostGraph() ) ];
      prec_t d = euclideanDistance( n1.X().begin() , n1.X().end() ,
				    n2.X().begin() );
      if ( d > nih.eqDistance() ) {
	nih.springRepulsiveInteraction( n1 , n2 );
      }
    }
  return arg_;
}
 
//--------------------------------------------------------------

void beginSimulation( ThreadContainer& threads , 
		      prec_t cutOffPrecision , TimeKeeper_t& timer , 
		      ThreadArgs * threadArgs , PCChaperone& chaperone ,
		      unsigned int totalLevels , bool givenCoords, prec_t placementDistance,
		      prec_t placementRadius , bool placeLeafsClose , bool silentOutput)
{
  Graph_t& current_layout = *(threadArgs->layout_graph);
  Graph_t& full_graph = *(threadArgs->full_graph);
  LevelMap& levels = *(threadArgs->levels);
  ParentMap& parents = *(threadArgs->parents);
  NodeContainer& nodes = *(threadArgs->nodes);
  Grid_t& grid = *(threadArgs->grid);
  long threadCount = threads.size();
  unsigned int currentLevel = 1;

  thread_pool threadpool( threadCount );
  std::vector< std::future< void > > futures;
  futures.reserve( threadCount );

  const auto wait_for_futures = [&futures] {
		for ( auto &f : futures )
			f.get();
		futures.clear();
  };

  while( currentLevel <= totalLevels ) {

    // Place and initialize the next layer of the graph
    if ( ! givenCoords ) {
      addNextLevelFromMap( current_layout , full_graph ,
			   levels , currentLevel );    
      initializeCurrentLayer( current_layout , nodes , levels , 
			      parents , grid , currentLevel , full_graph ,
			      placementDistance , placementRadius , placeLeafsClose  );
    } else {
      currentLevel = totalLevels;
    }

    prec_t avgPrevious = 0.0;
    prec_t dx = 10000000.; 
    int iterationCtr = 0; 

    do { 

      // Repulsive terms
      for ( long ii=0; ii<threadCount; ++ii ) {
	threadArgs[ii].currentLevel = currentLevel;
	threadArgs[ii].nodeHandler->springConstant( threadArgs[ii].casualSpringConstant );
	threadArgs[ii].nodeHandler->eqDistance( threadArgs[ii].nbhdRadius );
	futures.push_back( threadpool.run( calcInteractions , 
			    static_cast<void *>(&threadArgs[ii]) ) );
      }
		wait_for_futures();

      // Attractive terms
      for ( long ii=0; ii<threadCount; ++ii ) {
	threadArgs[ii].nodeHandler->springConstant( threadArgs[ii].specialSpringConstant );
	threadArgs[ii].nodeHandler->eqDistance( threadArgs[ii].eqDistance );
	futures.push_back( threadpool.run( onlyEdgeInteractions , 
			    static_cast<void *>(&threadArgs[ii]) ) );
      }
		wait_for_futures();

      // Integrate for next time step
      for ( long ii=0; ii<threadCount; ++ii ) {
			futures.push_back( threadpool.run( integrateParticles ,
			    static_cast<void *>(&threadArgs[ii]) ) ); 
      }
		wait_for_futures();
      
      // Collect stats for progress
      for ( long ii=0; ii<threadCount; ++ii ) {
	futures.push_back( threadpool.run( collectEdgeStats , 
			    static_cast<void *>(&threadArgs[ii]) ) );
      }
		wait_for_futures();

      prec_t dxNew = collectOutput( &threadArgs[0] , chaperone ); 
      if ( ! silentOutput ) {
	printOutput( timer.iteration() , dxNew , currentLevel , std::cerr );
      }
      chaperone.level( currentLevel );

      prec_t avg = ( dxNew + dx ) * .5;
      if ( abs(dxNew - dx)/dxNew < cutOffPrecision || iterationCtr > 150 ||
	   abs(avgPrevious - avg)/avg < .1 * cutOffPrecision ) {
	++timer;
	break; 
      } 

      avgPrevious = avg;
      dx = dxNew;

      if ( threadArgs[0].stats->collectStatsCheck( timer.iteration() ) ) {
	char layerChar[64];
	sprintf(layerChar,"coords/%dlayout%d",timer.iteration(),currentLevel);
	chaperone.posOutFile( layerChar );
	chaperone.writeOutFiles();
      }

      ++iterationCtr;
      ++timer;
      
    } while ( timer.rangeCheck() );

    ++currentLevel;

  } // End of iterative layout.

}

//----------------------------------------------------------

void initializeCurrentLayer( Graph_t& layout_graph , NodeContainer& nodes ,
			     LevelMap& levels , ParentMap& parents , Grid_t& grid ,
			     unsigned int currentLevel , Graph_t& actualG,
			     prec_t placementDistance, prec_t placementRadius ,
			     bool placeLeafsClose )
{
  Graph_t::vertex_iterator v, vend; 
  out_graph edges2add;
  FixedVec_p cm;
  // Get the center of mass for the current level
  if ( currentLevel == 1 ) {
    tie( v , vend ) = vertices( layout_graph.boostGraph() );
    for ( ; v!=vend ; ++v ) {
      // Find the root node and that is the cent mass
      if ( levels[*v] == 0 ) { cm = nodes[*v].X(); break; }
    }
  } else {
    cm = calcCenterOfMass( layout_graph , nodes , levels , currentLevel );
  }
  tie( v , vend ) = vertices( layout_graph.boostGraph() );
  // Determine which edges are going to be added to the current layout
  for ( ; v!=vend ; ++v ) {
    if ( levels[*v] == currentLevel ) {
      add_edge( parents[*v] , *v , edges2add );
    }
  }
  layerNPlacement( nodes , grid , edges2add , cm , currentLevel , parents ,
		   actualG , levels , placementDistance, placementRadius , placeLeafsClose );
}

//----------------------------------------------------------

void layerNPlacement( NodeContainer& nodes , Grid_t& grid , out_graph& g , 
		      FixedVec_p& cm , unsigned int currentLevel , ParentMap& parents ,
		      Graph_t& actualG, LevelMap& lm , prec_t placementDistance,
		      prec_t placementRadius , bool placeLeafsClose )
{
  typedef Sphere<prec_t> S;
  typedef S::vec_type Vec;
  typedef out_graph::out_edge_iterator oei;
  typedef out_graph::vertex_descriptor vertex_descriptor;
  typedef out_graph::vertex_iterator vertex_iterator;  

  vertex_iterator v , vend;
  tie( v , vend ) = vertices( g );

  for ( ; v!=vend; ++v ) { 

    oei e, eend;
    tie( e , eend ) = out_edges( *v , g );
    int vertices2place=0;
    oei ee = e;
    while( ee++ != eend ) { ++vertices2place; }
    //int vertices2place = distance( e , eend );
    if ( vertices2place == 0 ) { continue; }
    Node& parentNode = nodes[ *v ];
    Node& parentParentNode = nodes[ parents[*v] ];
    Vec spot( parentNode.X().begin() , parentNode.X().end() );
    vector< Vec > x;

    if ( currentLevel == 1 ) {

      S s( spot );
      s.radius( 1.0 );
      seriesOfPointsOnSphere( s , vertices2place , x ); 

    } else {

      FixedVec_p d;
      generatePlacementVector( d , parentNode.X() , parentParentNode.X() , cm );
      prec_t m = magnitude( d.begin() , d.end() );

      // Determine if any of these children are parents
      tie( e , eend ) = out_edges( *v , g );
      bool hasChildren = true;
      if ( placeLeafsClose ) {
	for ( ; e!=eend ; ++e ) {
	  vertex_descriptor other = target( *e , g );
	  if ( ! doesVertexHaveAnyChildren( actualG, other , g , parents ) ) {
	    hasChildren = false;
	    break;
	  }
	}
      }
      
      // The real placement distance is a function of the number of vertices
      // to place, if the graph is not a tree
      prec_t scalef = placementFormula( placementDistance , vertices2place ,
					NodeContainer::dimension );

      // Put placement distance at 0
      if ( ! hasChildren && placeLeafsClose ) { scalef = 0.0; }

      scale( d.begin() , d.end() , scalef/m );
      translate( spot.begin() , spot.end() , d.begin() );
      S s( spot );
      s.radius( placementRadius );
      Vec direction( d.begin() , d.end() );
      seriesOfPointsOnSphere( s , vertices2place , x );

    }

    // Initialize the positions of the next level
    int ctr = 0;    
    tie( e , eend ) = out_edges( *v , g );
    for ( ; e!=eend ; ++e ) {
      vertex_descriptor other = target( *e , g );
      if ( !nodes[ other ].isAnchor() ) {
         nodes[ other ].X( x[ctr].begin() , x[ctr].end() );
         ++ctr;
      }
    }

  }

}  

//----------------------------------------------------------

static std::set<long> areParents;

bool doesVertexHaveAnyChildren( Graph_t& G, Graph_t::vertex_descriptor v ,
				out_graph& g , ParentMap& parents )
{ 
  if ( areParents.empty() ) {
    // Generate list for the first time
    for ( ParentMap::size_type ii=0; ii<parents.size(); ++ii ) {
      if ( parents[ii] >= 0 )
	areParents.insert( parents[ii] );
    }
  }

  typedef out_graph::out_edge_iterator oei;
  
  oei e, eend;
  tie( e , eend ) = out_edges( v , g );

  // Go through all child vertices to see if they 
  // have any of their own.
  for ( ; e!=eend ; ++e ) {
      Graph_t::vertex_descriptor other = target( *e , g );
      if ( areParents.find( other ) != areParents.end() ) {
	return true;
      }
  }

  //cout << G.idFromIndex(v) <<  " has no children.\n";
  return false;
}

//----------------------------------------------------------

void generatePlacementVector( FixedVec_p& d , const FixedVec_p& parentNode,
			      const FixedVec_p& parentParentNode ,
			      const FixedVec_p& cm )
{
  // Find the parent node with respect to c.m.
  d = parentNode;
  d -= cm;
  // Make it a unit vector
  prec_t m =  1.0/magnitude( d.begin() , d.end() );
  scale( d.begin() , d.end() , 1.0/m );
  // Find the position of the parent with respect to its parent
  FixedVec_p d2( parentNode );
  d2 -= parentParentNode;
  // Make it a unit vector
  prec_t mag = magnitude( d2.begin() , d2.end() );
  if ( mag > 0 ) {
    m =  1.0/mag;
    scale( d2.begin() , d2.end() , 1.0/m );
    // Average the two
    d += d2;  
    d.scale( .5 );
  }
}

//----------------------------------------------------------

prec_t placementFormula( prec_t placementDistance , int vertices2place , int dimension )
{
  if ( placementDistance >= 0 ) // Independent of dimension
    return placementDistance;
  else if ( dimension == 2 )
    return min<prec_t>( .25 * sqrt((double)vertices2place) , 10 );
  else // ( dimension == 3 )
    return min<prec_t>( .25 * pow((double)vertices2place,.34) , 10 );
}

//----------------------------------------------------------

prec_t collectOutput ( ThreadArgs * args , PCChaperone& chaperone )
{
  for ( long ii=1; ii<args[0].threadCount; ++ii ) {
    args[0].stats->integrateWithOther( *(args[ii].stats) );
    args[ii].stats->reset();
  }
  // average the stats and get the average
  args[0].stats->avg();
  prec_t dx = args[0].stats->dist();
  // This clears the stats for the next runs
  args[0].stats->reset();
  // Return the avg dx
  return dx;
}

//----------------------------------------------------------

FixedVec_p calcCenterOfMass( Graph_t& g , NodeContainer& nodes , LevelMap& levels ,
			     unsigned int currentLevel )
{
  FixedVec_p center(0);
  prec_t totalMass = 0;
  Graph_t::vertex_iterator vi, vend;
  for ( tie(vi,vend)=vertices(g.boostGraph()); vi!=vend; ++vi )
    {
      if ( levels[*vi] >= currentLevel ) { continue; }
      Node& n = nodes[ *vi ];
      FixedVec_p location( n.X() );
      location.scale( n.mass() );
      center += location;
      totalMass += n.mass();
    }
  center.scale( 1.0/totalMass );
  return center;
}

//---------------------------------------------------------------

static int iteration__ = 0;

void printOutput( long i , prec_t d , long la , ostream& o )
{
  const char * im = "Iteration: ";
  const char * dx = " Dx: ";
  const char * l  = " Level: ";
  if ( iteration__ > 0 ) { 
    int j=0;
    while ( ++j<=(24+20) ) { o << '\b'; }
  }
  o << im << setw(6) << i;  
  o << dx << setw(10) << d;
  o << l << setw(4) << la << flush;
  ++iteration__;
}

//----------------------------------------------------------
// This determines the size of the grid to generate
// to hold all the particles. It requires that the
// particle positions be set already.
//----------------------------------------------------------

void gridPrepAndInit( NodeContainer& nc , Grid_t& nw , 
		      prec_t voxelLength )
{
  typedef NodeContainer::size_type size_type;
  FixedVec_p mins;
  FixedVec_p maxs;

  // First job is to determine the size of the grid.
  mins=nc[0].X(); maxs=nc[0].X();
  for ( size_type ii=1; ii<nc.size(); ++ii ) {    
    const FixedVec_p& x = nc[ii].X();
    // Get info on mins and maxes
    for ( size_type jj=0; jj<x.size(); ++jj ) {
      // Min check
      if ( x[jj]<mins[jj] ) 
	mins[jj] = x[jj];
      // Max check
      if ( x[jj]>maxs[jj] ) 
	maxs[jj] = x[jj];
    }
  }

  // Just adding some pad onto the grid.
  for ( size_type jj=0; jj<mins.size(); ++jj ) {
    prec_t span = 1.00*(maxs[jj]-mins[jj]);
    mins[jj] -= span; maxs[jj] += span;
  }
  
  nw.min(mins);
  nw.max(maxs);
  nw.voxelWidth(voxelLength);
  nw.initGrid();

}

//----------------------------------------------------------

EllipseFactors parseEllipseFactors( const std::string& optionStr )
{
	EllipseFactors ret;
	std::vector< std::string > factors;
	boost::algorithm::split( factors, optionStr, boost::algorithm::is_any_of( "x" ) );
	BOOST_FOREACH( const std::string& f, factors )
		ret.push_back( boost::lexical_cast< prec_t >( f ) );
	return ret;
}

//----------------------------------------------------------

void interpolateUninitializedPositions( PCChaperone& chaperone, const Graph_t::boost_graph& g, bool remove_disconnected_nodes )
{
	std::size_t	num_uninitialized_positions_still, num_uninitialized_positions_before;
#if 0
	std::ofstream interpolation_log( "interpolation.log" );
#endif
	do {
		num_uninitialized_positions_before = num_uninitialized_positions_still = 0;
		for ( std::size_t ii = 0; ii < chaperone.pc_.size(); ++ii ) {
			if ( !chaperone.pc_[ii].isPositionInitialized() ) {
				++num_uninitialized_positions_before;
				std::size_t count_initialized_neighbors = 0;
				Graph_t::out_edge_iterator eb, ee;
				for ( tie( eb, ee ) = out_edges( ii, g ); eb != ee; ++eb ) {
					const auto src = source( *eb, g ), tgt = target( *eb, g );
					assert( src == ii || tgt == ii );
					const auto other = src == ii ? tgt : src;
					if ( chaperone.pc_[other].isPositionInitialized() ) {
						++count_initialized_neighbors;
						for ( std::size_t dim = 0; dim < chaperone.pc_[ii].X().size(); ++dim )
							chaperone.pc_[ii].x[dim] += chaperone.pc_[other].X()[dim];
					}
				}
				if ( count_initialized_neighbors )
					for ( float &coord : chaperone.pc_[ii].x )
						coord /= count_initialized_neighbors;
				else
					++num_uninitialized_positions_still;
#if 0
				interpolation_log << chaperone.pc_[ii].id() << " was uninitialized, now coords are";
				for ( float coord : chaperone.pc_[ii].X() )
					interpolation_log << ' ' << coord;
				interpolation_log << '\n';
#endif
			}
#if 0
			else {
				interpolation_log << chaperone.pc_[ii].id() << " was already initialized with coords";
				for ( float coord : chaperone.pc_[ii].X() )
					interpolation_log << ' ' << coord;
				interpolation_log << '\n';
			}
#endif
		}
		cout << "\nOut of " << num_uninitialized_positions_before << " uninitialized positions that had remained, "
			  << num_uninitialized_positions_before - num_uninitialized_positions_still << " have just been interpolated";
		// until either finished or there is no more progress being made
	} while ( num_uninitialized_positions_still > 0 && num_uninitialized_positions_still < num_uninitialized_positions_before );

	if ( num_uninitialized_positions_still ) {
		cout << "\nThere are " << num_uninitialized_positions_still << " nodes that are DISCONNECTED from any nodes which had their positions initialized!\nTHOSE NODES ARE:\n";
		for ( std::size_t ii = 0; ii < chaperone.pc_.size(); ++ii )
			if ( !chaperone.pc_[ii].isPositionInitialized() )
				cout << '\t' << chaperone.pc_[ii].id() << '\n';
		if ( remove_disconnected_nodes ) {
			cout << "Removing them from the graph before further processing...\n";
			for ( std::size_t ii = chaperone.pc_.size(); ii > 0; --ii )
				if ( !chaperone.pc_[ii - 1].isPositionInitialized() )
					chaperone.pc_.erase( ii - 1 );
		}
		cout << std::endl;
	}
	else
		cout << "\nInterpolation of uninitialized positions completed successfully." << std::endl;
}
