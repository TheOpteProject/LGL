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
#ifndef SCHEDUALITERATORS_H_
#define SCHEDUALITERATORS_H_

//---------------------------------------------------------------------------

#include <iostream>
#include <cassert>
#include "grid.hpp"
#include "voxel.hpp"

//---------------------------------------------------------------------------

template < typename Grid >
class GridSchedual_MTS {

 public:
  enum { dimension = Grid::dimension };
  typedef long size_type;
  
 private:
  typedef FixedVec<long,dimension> Vec_l;
  typedef typename Grid::iterator iterator;
  typedef typename Grid::voxel_type voxel_type;
  typedef GridSchedual_MTS< Grid > GS_;

  bool threadCheck() {
    // Make sure the threadCount is reasonable.
    // This is for linux boxes
    long _PROCESSOR_COUNT_ = 100; // sysconf(_SC_NPROCESSORS_ONLN);
    // This is necessary on SGIs
    //  long _PROCESSOR_COUNT_ = sysconf(_SC_NPROC_ONLN);
    long threadCount2 = ( threadCount < _PROCESSOR_COUNT_ ) ? 
      threadCount : _PROCESSOR_COUNT_;
    threadCount2 = min<long>(threadCount,grid->voxelsPerEdge(0)/2);
    // LeapCount_=3 guarantees no two interacting voxels are accessed
    // simultaneously
    return threadCount==threadCount2;
  }

  void initVars() {
    threadCount=1;
    offsetCtr=0;
    endOfGrid=0;
    voxelList=0;
    inGridCheck=1;
    currentVoxel=0;
  }

 protected:
  size_type threadCount;
  iterator iter;
  size_type offsetCtr;
  Grid * grid;
  Vec_l * voxelList;
  bool inGridCheck;
  bool endOfGrid;
  size_type gridSize;
  Amutex mutex;
  size_type currentVoxel;

  // Not sure what to do with these
  GridSchedual_MTS() { GS_::initVars(); }

  void initFromGrid( Grid& g ) {
    iter.initFromGrid(g);
    grid = &g;
    gridSize=g.size();
  }

 public:

  GridSchedual_MTS( Grid& g ) : iter(g) , grid(&g) , gridSize(g.size()) { 
    GS_::initVars(); 
  }

  int lock() { return mutex.lock(); }
  int unlock() { return mutex.unlock(); }

  void generateVoxelList();
  void generateVoxelList_MT();
  void generateVoxelList_ST();

  size_type getVoxelList( long thread , Vec_l * l ) {
    size_type jj=0;
    for ( size_type ii=thread; ii<gridSize; ii+=threadCount, ++jj ) {
      //cout << ii << " " << jj << " "; voxelList[ii].print();
      l[jj] = voxelList[ii];
    }
    return jj;
  }

  bool getNextVoxel( iterator& i );
  void renew();

  bool inGrid() const { return !endOfGrid; }

  bool threads( long p ) { threadCount=p; return GS_::threadCheck(); }
  long threads() const { return threadCount; }

  void printVoxelList ( std::ostream& o= std::cout ) const {
    if ( voxelList != 0 ) {
      o << "VoxelList:\tDimensions: ";
      for (size_type ii=0; ii<dimension; ++ii)
	o << grid->voxelsPerEdge(ii) << '\t';
      o << '\n';
      for (size_type ii=0; ii<gridSize; ++ii) {
	o << "Step " << ii << '\t'; voxelList[ii].print(o);
      }
    }
  }

  ~GridSchedual_MTS() { delete [] voxelList; }
};

//---------------------------------------------------------------------------

template < typename Occupant >
void GridSchedual_MTS<Occupant>::generateVoxelList_MT()
{
  if ( threadCount == 1 ) { return GS_::generateVoxelList_ST(); }
  GS_::renew();
  delete [] voxelList;
  voxelList = new Vec_l[gridSize];
  long iteration = 1;
  long voxelCtr = 0;
  bool finishedGrid = 0;

  while (!finishedGrid) {

    bool activatedNbhr = 0;
    voxel_type& v = iter.currentVox();

    // Check to see if any neighbors
    // are used this iteration
    if ( v.interactionCtr() == 0 ) {
      while ( iter.incNbhr() ) { 
	voxel_type& v2 = iter.nhbrVox();
	if ( v2.interactionCtr() == iteration ) {
	  activatedNbhr = 1; 
	  break;
	} 
      }
    }

    // If the voxel is "alone" then mark it
    // and its nbhrs.
    if ( activatedNbhr != 1 && (v.interactionCtr() == 0) ) {      
      voxelList[voxelCtr] = iter.current();
      ++voxelCtr;
      v.interactionCtr( -1 );
      iter.rewindNbhrInc();
      iter.incNbhr(); // Get past self.
      while ( iter.incNbhr() ) { 
	voxel_type& v2 = iter.nhbrVox();
	if ( v2.interactionCtr() != -1 )
	  v2.interactionCtr( 1 );
      }
    }

    inGridCheck = ++iter;

    // We have hit the end of this pass
    if ( !inGridCheck ) { 
      // Clear all counters.
      iter.reset();
      long remainingVoxels = 0;
      for ( long ii=0; ii<gridSize; ++ii ) {
	voxel_type& vc = grid->voxel(ii);
	if ( vc.interactionCtr() > 0 ) {
	  vc.interactionCtr( 0 );
	  ++remainingVoxels;
	}
      }
      // We have to keep iterating until all
      // voxels are included in the list
      if ( remainingVoxels == 0 ) {
	finishedGrid = 1;
      } else {
	++iteration;
      }
    } 

  }

}

//---------------------------------------------------------------------------
// This method presumes a single thread will be iterating through the grid.
template < typename Occupant >
void GridSchedual_MTS< Occupant >::generateVoxelList_ST()
{
  GS_::renew();
  delete [] voxelList;
  voxelList = new Vec_l[gridSize];
  long voxelCtr = 0;
  bool inGridCheck = true;
  while ( inGridCheck ) {
    voxelList[voxelCtr] = iter.current();
    inGridCheck = ++iter;
    ++voxelCtr;
  }
}

//---------------------------------------------------------------------------

template < typename Occupant >
bool GridSchedual_MTS<Occupant>::getNextVoxel( iterator& i )
{
  size_type occupancy = 0;
  while ( currentVoxel < gridSize && occupancy == 0 ) {
    voxel_type& v = grid->voxel(currentVoxel);
    occupancy = v.occupancy();
    if ( occupancy != 0 ) { i.current( voxelList[currentVoxel] ); }
    ++currentVoxel;
  }
  if ( currentVoxel > gridSize ) { endOfGrid=1; return 0; }
  else { return 1; }
}

//---------------------------------------------------------------------------

template < typename Occupant >
void GridSchedual_MTS<Occupant>::renew()
{
  offsetCtr=0;
  endOfGrid=0;
  iter.reset();
  inGridCheck=true;
  currentVoxel=0;
}

//---------------------------------------------------------------------------

#endif
