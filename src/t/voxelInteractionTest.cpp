
/////////////////////////////////////////////////////////

#include <iostream>
#include <vector>
#include "particle.hpp"
#include "voxel.hpp"
#include "grid.hpp"
#include "voxelInteractionHandler.hpp"
#include "gridSchedual.hpp"
#include "particleContainerChaperone.hpp"
#include "fixedVec.h"
#include <algorithm>

/////////////////////////////////////////////////////////

typedef float prec_t;
const int COUNT = 1500;
const unsigned int DIMENSION = 2;
const prec_t R = 100.;
const prec_t voxelLength = 1.0;

/////////////////////////////////////////////////////////

typedef Particle< prec_t , DIMENSION > Node_t;
typedef Voxel< Node_t > Voxel_t;
typedef Grid< Node_t > Grid_t;
typedef GridIter< Grid_t > GridIterator_t;
typedef ParticleContainerChaperone< Node_t > PCChaperone_t;
typedef ParticleContainer< Node_t > NodeContainer_t;
typedef FixedVec< prec_t , DIMENSION > FixedVec_p;

/////////////////////////////////////////////////////////

struct Pair
{
  int index1;
  int index2;
  prec_t d;
  Pair( int i1 , int i2 , prec_t dd ) : index1(i1) , index2(i2) , d(dd) { }
  bool operator< ( const Pair& other ) const {
    return index1 < other.index1;
  }
  void print( std::ostream& o=std::cout ) const {
    o << "I1: " << index1 << "\tI2: " << index2
      << "\tD: " << d << '\n';
  }
};

struct less_than_1
{
  bool operator()( const Pair& p1 , const Pair& p2 ) const {
    return p1.index1 < p2.index1; 
  }
};


struct less_than_2
{
  bool operator()( const Pair& p1 , const Pair& p2 ) const {
    return p1.index2 < p2.index2; 
  }
};


struct less_than_d
{
  bool operator()( const Pair& p1 , const Pair& p2 ) const {
    return p1.d < p2.d; 
  }
};

typedef std::vector< Pair > DistanceMap;

/////////////////////////////////////////////////////////

struct DistanceTest
{

  DistanceMap dmap;
  bool operator()( Node_t& n1 , Node_t& n2 )
  {
    prec_t d = euclideanDistance( n1 , n2 );
    if ( d < voxelLength ) {
      dmap.push_back( Pair( n1.index() , n2.index() , d )  );
    }
  }
  void print( std::ostream& o=std::cout ) {
    DistanceMap::iterator begin=dmap.begin(), end=dmap.end();
    sort( begin , end , less_than_d() );
    begin=dmap.begin(), end=dmap.end();
    for( ; begin!=end ; ++begin ) {
      begin->print(o);
    }
  }
};

/////////////////////////////////////////////////////////

template < typename prec_ >
struct Handler
{
  typedef prec_ precision;
  DistanceTest dtest;
  void interaction( Node_t& n1 , Node_t& n2 ) {
    dtest(n1,n2);
  }
};

typedef VoxelInteractionHandler< Voxel_t , Handler<prec_t> > Vih_t;

/////////////////////////////////////////////////////////

void gridPrepAndInit( NodeContainer_t& nc , Grid_t& nw , 
		      prec_t voxelLength );

using namespace std;

/////////////////////////////////////////////////////////

int main( int argc, char ** argv )
{

  NodeContainer_t nodes( COUNT );
  PCChaperone_t chaperone( nodes );
  chaperone.randomizePosRange( R );
  chaperone.initRadius( .1 );
  chaperone.initAllParticles();
  Grid_t grid;
  gridPrepAndInit( nodes , grid , voxelLength );
  place_particles( nodes.begin() , nodes.end() , grid );
  //nodes.print();
  
  // All possible comparisons for distances shorter than a voxelLength.
  DistanceTest dtest1;
  for ( NodeContainer_t::size_type ii=0; ii<nodes.size(); ++ii ) {
    for ( NodeContainer_t::size_type jj=ii+1; jj<nodes.size(); ++jj ) {
      dtest1( nodes[ii] , nodes[jj] );
    }
  }

  // The same but using the grid with nbhr iterators etc  
  Vih_t vih;
  Handler<prec_t> h;
  vih.interactionHandler( h );
  GridIterator_t gi( grid );
  while( gi.inc() ) {
    Voxel_t& vox1 = gi.currentVox();
    while( gi.incNbhr() ) {
      Voxel_t& vox2 = gi.nhbrVox();
      vih.twoVoxelInteractions( vox1 , vox2 );
    }
  }

  ofstream out1("dtest1results");
  if ( !out1 ) { cerr << "Open failed" << endl; exit(1); }
  dtest1.print(out1);
  out1.close();
  ofstream out2("dtest2results");
  if ( !out2 ) { cerr << "Open failed" << endl; exit(1); }
  h.dtest.print(out2);
  out2.close();

}

//-------------------------------------------------
 
void gridPrepAndInit( NodeContainer_t& nc , Grid_t& nw , 
		      prec_t voxelLength )
{
  typedef NodeContainer_t::size_type size_type;
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
    // The voxel Length is determined by the average weight
    // between the vParticles (edges)
  }

  // Just adding some pad onto the grid.
  for ( size_type jj=0; jj<mins.size(); ++jj ) {
    prec_t span = .25*(maxs[jj]-mins[jj]);
    mins[jj] -= span; maxs[jj] += span;
  }
  
  nw.min(mins);
  nw.max(maxs);
  nw.voxelWidth(voxelLength);
  nw.initGrid();

}

//------------------------------------------------ 
