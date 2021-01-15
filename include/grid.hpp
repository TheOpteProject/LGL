/*
 *  
 *  Copyright (c) 2002 Alex Adai, All Rights Reserved.
 *  
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 2 of
 *  the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 *  MA 02111-1307 USA
 *  
*/
#ifndef _GRID_HPP_
#define _GRID_HPP_

#include "voxel.hpp"
#include "fixedVec.hpp"
#include <cassert>
#include <iostream>
#include <cstdlib>

//------------------------------------------------------------

// Forward declaration for befriending Grid
template < typename Grid >
class GridIter;

//------------------------------------------------------------
// This is a simple cubical grid. The number of rows=cols=levels.
// It is essential a handler for voxels that generate the grid.
//------------------------------------------------------------

template < typename Occupant >
class Grid : public Amutex {

  friend class GridIter< Grid<Occupant> >;

 public:
  enum { dimension = Occupant::dimension };
  typedef Occupant occupant_type;
  typedef unsigned int size_type;
  typedef typename Occupant::precision precision;
  typedef typename Occupant::vec_type vec_type;
  typedef Voxel< Occupant > voxel_type;
  typedef GridIter< Grid<Occupant> > iterator;

 private:
  typedef Grid< Occupant > Grid_;
  typedef FixedVec< long , dimension > Vec_l;
  
 protected:
  precision voxelLength;       // The length of each voxel
  Vec_l voxPerEdge;
  size_type voxelCount;    // Total voxels
  voxel_type * voxels_;        // Pointer the allocated voxels
  vec_type mins; // Xmin Ymin Zmin etc ...
  vec_type maxs; // Xmax Ymax Zmax etc ...
  Vec_l voxPerDim;

 private:
  
  void initVoxels() {
    vec_type xyz;
    vec_type xyzMin;
    precision vr = voxelLength * .5;
    // Init the mins in all dimensions and initialize
    // for the smallest value.
    for ( size_type ii=0; ii<dimension; ++ii ) { 
	xyzMin[ii] = mins[ii]+vr;  
        xyz[ii] = xyzMin[ii];
    }
    size_type ctr = 0;
    while( ctr<voxelCount ) {
      // Do a stretch of x's first (First dimension)
      long upper = ctr+voxPerEdge[0];
      for ( long ii=ctr; ii<upper; ++ii ) {	
	voxels_[ii].index(ii);
	voxels_[ii].orig(xyz);
	voxels_[ii].radius(vr);
	xyz[0] += voxelLength;
      }
      ctr += voxPerEdge[0];
      xyz[0] = xyzMin[0];
      for ( size_type d=1; d<dimension; ++d ) {
	// Check for a wall or edge in all higher dimensions
	if ( ctr%voxPerDim[d] == 0) {
	  xyz[d] += voxelLength;
	  // Here we have to reset all lower dimensions
	  // because of the increment in the higher one
	  for ( size_type dd=0; dd<d; ++dd ) {
	    xyz[dd] = xyzMin[dd];
	  }	      
	}
      }
    } // End Of While
  }
  
  void allocate() {
    voxels_ = new voxel_type[voxelCount]; assert(voxels_);
  }
  
 public:
  
  Grid( ) : voxels_(0) { }

  Grid( const vec_type& minsXYZ, const vec_type& maxsXYZ, precision l ) : 
    mins(minsXYZ) , maxs(maxsXYZ) , voxelLength(l) , voxels_(0) { }

  // This determines the number of voxels for each dimension,
  // and calls a couple of other init methods.
  void initGrid() {
    // This sets the number of voxels in each dimension
    for ( size_type ii=0; ii<dimension; ++ii ) {
      precision ctr = mins[ii]-voxelLength;  // Add some pad
      mins[ii] = ctr;                    
      voxPerEdge[ii] = (unsigned int) ceil( (maxs[ii]-ctr) / voxelLength );
      maxs[ii] = mins[ii] + voxPerEdge[ii] * voxelLength; 
    }
    voxelCount = voxPerEdge.product();
    // This determines the number of voxels enclosed
    // in each dimension.
    size_type count=1;
    voxPerDim[0]=count;
    for ( size_type ii=1; ii<dimension; ++ii) {
      count *= voxPerEdge[ii-1];
      voxPerDim[ii] = count;
    }
    Grid_::allocate();
    Grid_::initVoxels();
  }

  // This will determine which voxel the provided
  // point would be in. The current
  voxel_type * getVoxelFromPosition( const vec_type& x ) const {
  	Vec_l coord;
	for ( size_type d=0; d<dimension; ++d) {
	  coord[d]=static_cast<long>( (x[d]-mins[d]) / voxelLength );
	}
	// This converts dimension to 1D
	size_type entry = coord.dotProduct(voxPerDim);
	// Double check
	if ( (voxels_+entry)->checkInclusionFuzzy(x) ) {
	   return voxels_+entry;
	} else {
	  // The location will not fit in this grid 
	  return 0;
	}
  }
  
  void print( std::ostream& o=std::cout ) const {
    o << "voxelLength: " << voxelLength << '\t' << "voxelCount: " 
      << voxelCount << '\n';
    o << "VoxPerEdge: "; voxPerEdge.print(o);
    o << "Mins: "; mins.print(o);
    o << "Maxs: "; maxs.print(o);
    o << "voxPerDim: "; voxPerDim.print(o);
    o << " ***** VOXELS ***** \n";
    for ( size_type ii=0; ii<voxelCount; ++ii ) {
      voxels_[ii].print(o);
    }
  }

  void min( const vec_type& m ) { mins=m; }
  void max( const vec_type& m ) { maxs=m; }
  void voxelWidth( precision l ) { voxelLength=l; }
  const vec_type& min() const { return mins; }
  const vec_type& max() const { return maxs; }
  precision voxelWidth() const { return voxelLength; }
  size_type size() const { return voxelCount; }
  size_type voxelsPerDim( size_type ii ) const { return voxPerDim[ii]; }
  size_type voxelsPerEdge( size_type ii ) const { return voxPerEdge[ii]; }
  
  bool checkInclusion( const vec_type& p ) const {
    for ( size_type d=0; d<dimension; ++d) {
      if ( p[d] < mins[d] || p[d] > maxs[d] ) 
	return false;
    } return true;
  }

  voxel_type& voxel( size_type entry ) {
    return operator[](entry);
  }
  
  const voxel_type& voxel( size_type entry ) const {
    return operator[](entry);
  }

  voxel_type& operator[] ( size_type entry ) {
    return *(voxels_+entry);
  }

  const voxel_type& operator[] ( size_type entry ) const {
    return *(voxels_+entry);
  }

  ~Grid() { delete [] voxels_; }
};

namespace NbhrVoxelPositions {
//---------------------------------------------------------------------------
//            See "Art of Molecular Dynamics Simulations" by Rapaport
//          These have an offset of 0 though and includes a self reference
//                        0   1   2   3   4   5   6   7   8   9  10  11  12  13
const int off_[3][14] = {{0,  1,  1,  0, -1,  0,  1,  1,  0, -1, -1, -1,  0,  1},
			 {0,  0,  1,  1,  1,  0,  0,  1,  1,  1,  0, -1, -1, -1},
                         {0,  0,  0,  0,  0,  1,  1,  1,  1,  1,  1,  1,  1,  1}};

// The above index offset is good for 1D, 2D, and 3D if you stop BEFORE 
// the following locations.
const unsigned int iterMax1D = 2;
const unsigned int iterMax2D = 5;
const unsigned int iterMax3D = 14;
}

//---------------------------------------------------------------------------
// Todo:
//       *Should iterate in the direction of max(x,y,z)
//              and not x then y then z

template < typename Grid >
class GridIter {
  
public:
  enum { dimension = Grid::dimension };
  typedef typename Grid::occupant_type occupant_type;
  typedef typename Grid::voxel_type voxel_type;
  typedef GridIter< Grid > iterator;
  typedef typename Grid::size_type size_type;
  typedef typename Grid::vec_type vec_type;
  typedef FixedVec<long,dimension> Vec_l;

 protected:
  int iterID;
  size_type iterMax;
  size_type neighborCtr;
  voxel_type * begin_;        // THE beginning of the grid
  voxel_type * start_;        // Where this iterater is initialized
  voxel_type * end_;          // THE end of the grid
  voxel_type * current_;      
  voxel_type * neighbor_; 
  Vec_l dimensions;       // Dimensions of the grid
  Vec_l currentVoxel;     // Current location of the itr in the grid
  Vec_l currentNbhr;      // Current location of the nbhr in the grid
  Vec_l startingPoint;    // Starting location of the itr in the grid
  Vec_l voxPerDim;        // Number of voxels each dimension consumes

  void initVals() {
    iterID=0;
    start_=0; begin_=0; current_=0; end_=0; neighbor_=0;
    neighborCtr=0;
    currentVoxel=0;
    currentNbhr=0;
    startingPoint=0;
    voxPerDim=0;
    if ( dimension == 1 )
      iterMax=NbhrVoxelPositions::iterMax1D;
    else if ( dimension == 2 )
      iterMax=NbhrVoxelPositions::iterMax2D;
    else if ( dimension == 3 )
      iterMax=NbhrVoxelPositions::iterMax3D;
    else {
      cerr << "!! GridIter Can only handle dimensions 1-3 !!\n";
		std::exit(1);
    }
  }

 public:

  GridIter() { }

  GridIter( Grid& g ) {
    iterator::initFromGrid(g);
  }

  GridIter( const iterator& g ) {
    iterator::copy(g);
  }
  
  void initFromGrid( Grid& g ) {
    iterator::initVals();
    voxPerDim=g.voxPerDim;
    dimensions=g.voxPerEdge;
    start_=neighbor_=current_=begin_=&g[0];
    end_=&g[g.size()-1];
  }

  void print ( std::ostream& o = std::cout ) const {
    o << "Iter: " << iterID << '\n'
      << "\tPtrs: " << "Beg: " << begin_ << '\t'
      << "Start: " << start_ << '\t'
      << "End: " <<  end_ << '\t' << "Current: "
      << current_ << '\t' << "Nbhr: " << neighbor_
      << '\n' << "\tGridDimensions: "; 
    dimensions.print(o);
    o << "\tStartingPoints: "; 
    startingPoint.print(o);
    o << "\tCurrentVoxel: "; 
    currentVoxel.print(o);
    o << "CurrentNbhr: " << neighborCtr << " [" << iterMax << "] : "
      << currentNbhr.print(o) << '\n';
  }

  void reset () {
    current_=start_;
    neighbor_=start_;
    neighborCtr=0;
    currentVoxel=startingPoint;
    currentNbhr=startingPoint;
  }

  void copy( const iterator& g ) {
    iterID=g.iterID;
    iterMax=g.iterMax;
    start_=g.start_;
    begin_=g.begin_; current_=g.current_; 
    end_=g.end_; neighbor_=g.neighbor_;
    neighborCtr=g.neighborCtr;
    dimensions=g.dimensions;      
    currentVoxel=g.currentVoxel;   
    currentNbhr=g.currentNbhr;   
    startingPoint=g.startingPoint;
    voxPerDim=g.voxPerDim;
  } 

  // This will set/get the starting point of the
  // iterator in the grid
  void start( const Vec_l& s ) { 
    startingPoint=s; neighborCtr=0; 
    start_=neighbor_=current_=begin_+voxPerDim.dotProduct(startingPoint);
  }
  const Vec_l& start() const { return startingPoint; }

  // This will set/get the current point of the
  // iterator in the grid
  void current( const Vec_l& c ) { 
    currentVoxel=c; neighborCtr=0;
    neighbor_=current_=begin_+voxPerDim.dotProduct(currentVoxel);
  }
  const Vec_l& current() const { return currentVoxel; }

  // This sets the nbhr counter back to the first position
  void rewindNbhrInc() { neighbor_=current_; neighborCtr=0; }

  // This will set/get the id of this iterator
  void id( int i ){ iterID=i; }
  const int id() const { return iterID; }

  // This gives voxel info
  const voxel_type& startVox() const { return *start_; }
  const voxel_type& firstVox() const { return *begin_; }
  const voxel_type& endVox() const { return *end_; }
  const voxel_type& currentVox() const { return *current_; }
  const voxel_type& nhbrVox() const { return *neighbor_; }
  voxel_type& startVox()  { return *start_; }
  voxel_type& firstVox() { return *begin_; }
  voxel_type& endVox()  { return *end_; }
  voxel_type& currentVox()  { return *current_; }
  voxel_type& nhbrVox()  { return *neighbor_; }
  
  // This moves the iterator one voxel in the + direction
  // 0 is returned if the end is reached
  bool inc() {
    neighborCtr=0;
    ++currentVoxel[0]; ++current_; ++neighbor_;
    for ( size_type ii=0; ii<dimension; ++ii ) {
      if ( currentVoxel[ii] == dimensions[ii] ) {
	currentVoxel[ii] = 0; 
	if ( (ii+1) != dimension ) {
	  ++currentVoxel[ii+1];
	}
      } else {
	return 1;
      }
    } return 0;
  }

  // This is a pretty sad arbitrary incrementer.
  bool inc( size_type jj ) {
    for ( size_type ii=0; ii<jj; ++ii ) {
      if ( !iterator::inc() ) {
	return 0;
      }
    } 
    return 1;
  }

  // This moves the iterator one voxel in the - direction
  // 0 is returned if the end is reached
  bool dec() {
    neighborCtr=0;
    --currentVoxel[0]; --current_; --neighbor_;
    for ( size_type ii=0; ii<dimension; ++ii ) {
      if ( currentVoxel[ii] == 0 ) {
	currentVoxel[ii] = dimensions[ii]-1; 
	if ( (ii+1) != dimension ) {
	  --currentVoxel[ii+1];
	}
      } else {
	return 1;
      }
    } return 0;
  }
  
  // This is a pretty sad arbitrary decrementer.
  bool dec( long jj ) {
    for ( long ii=0; ii<jj; ++ii ) {
      if ( !iterator::dec() ) {
	return 0;
      }
    } 
    return 1;
  }
  
  // Through subsequent calls this will iterate
  // through the neighbors of the current voxel.
  // It determines the boundaries based on the grid
  // size (whether the current voxel is next to a 
  // wall or not). It will return 1 if a neighbor
  // is left to inspect and 0 if one has iterated
  // through them all.
  bool incNbhr() {
    bool gotOne=0;
    while( neighborCtr<iterMax && !gotOne ) {
      for ( unsigned int ii=0; ii<dimension; ++ii ) {
	currentNbhr[ii] = currentVoxel[ii] + 
	  NbhrVoxelPositions::off_[ii][neighborCtr];
	// This does a boundary check
	if ( currentNbhr[ii] == dimensions[ii] || currentNbhr[ii] == -1 ) {
	  break;
	} else if ( ii==dimension-1 ) {
	  gotOne=1;
	  ++neighborCtr;
	}
      }
      if ( !gotOne ) { ++neighborCtr; }
    } // End of for
    if ( gotOne ) { 
      neighbor_ = begin_ + voxPerDim.dotProduct(currentNbhr);
      return 1; 
    }
    else { return 0; }
  }

  friend bool operator++ ( iterator& i ) { 
    return i.inc();
  }

  bool operator+= ( size_type i ) { 
    return iterator::inc(i);
  }

  friend bool operator-- ( iterator& i ) { 
    return i.dec();
  }

  bool operator-= ( size_type i ) {
    return iterator::dec(i);
  }

  const iterator& operator= ( const iterator& g ) {
    iterator::copy(g); return *this;
  }

  ~GridIter() { /* Don't delete any voxels */ }
};

/////////////////////////////////////////////////////////////////////////////

template < typename Particle , typename Grid > 
void _placement_error( Particle& p , Grid& g , const char * message )
{
  std::cerr << "An error occured shifting a particle around in the voxels: "
	    << message << '\n';
  std::cerr << "If the entry is outside the grid, this could be fixed by initializing a larger grid\n";
  std::cerr << "Particle: "; p.print(std::cerr);
  std::cerr << "Grid: "; g.print(std::cerr);
  std::exit( EXIT_FAILURE );
}

// The particle does not have to be in the grid
// this to work.
template < typename Particle , typename Grid > 
void _place_particle( Particle& p , Grid& g )
{
  typename Grid::voxel_type * vox = g.getVoxelFromPosition( p.X() );
  if ( vox == 0 ) { return _placement_error( p , g , "Entry Is Outside Of Grid" ); }
  if ( vox->lock() != 0 )
    return _placement_error( p , g , "Failed to acquire lock on voxel" );
  vox->insert( p );
  vox->unlock();
  p.container( vox->index() );
}

// This assumes the particle is already in the grid.
// If the particle is not....!!
template < typename Particle , typename Grid > 
void _remove_particle( Particle& p , Grid& g )
{
  if ( p.container() < 0 ) { return _placement_error( p , g , "Remove is illegitimate" ); }
  typename Grid::voxel_type * vox = &(g[ p.container() ]);
  vox->lock();
  vox->remove( p );
  vox->unlock();
  p.container( -1 );
}

// This assumes the particle is already in the grid.
template < typename Particle , typename Grid >
void shift_particle( Particle& p , Grid& g )
{
  // Check to see if the particle has even
  // left the current voxel, which is usually
  // not the case.
  if ( p.container() >= 0 ) {
    const typename Grid::voxel_type& v = g[ p.container() ];
    if ( v.checkInclusionFuzzy( p.X() ) ) {
      // The particle has not left the voxel
      return;
    }
    _remove_particle(p,g);
  }
  _place_particle(p,g);
}

//---------------------------------------------------------------------------

#endif
