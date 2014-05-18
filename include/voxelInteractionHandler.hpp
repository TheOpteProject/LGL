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
#ifndef VOXELINTERACTIONHANDLER_HPP_
#define VOXELINTERACTIONHANDLER_HPP_

//--------------------------------------------- 

#include <iostream>
#include "voxel.hpp"

//--------------------------------------------- 

template < typename Voxel , typename InteractionHandler >
class VoxelInteractionHandler {

 private:
  typedef VoxelInteractionHandler< Voxel , InteractionHandler > VIH_;

 public:
  typedef Voxel voxel_type;
  typedef typename InteractionHandler::precision precision;
  typedef typename Voxel::occupant_type occupant_type;
  typedef typename voxel_type::size_type size_type;
  typedef typename voxel_type::occupant_iterator occupant_iterator;
  typedef typename voxel_type::const_occupant_iterator const_occupant_iterator;
  typedef InteractionHandler interaction_handler_type;

 protected:  
  voxel_type * current;
  voxel_type * nbhr;
  interaction_handler_type * ih;
  int id_;
  Amutex mutex;

  void initVars() {
    current=0; nbhr=0; ih=0; id_=0;
  }

 public:

  VoxelInteractionHandler() { VIH_::initVars(); }
  VoxelInteractionHandler( const VIH_& v ) {
    VIH_::copy(v);
  }

  void copy( const VIH_& v ) {
    current=v.current;
    nbhr=v.nbhr;
    ih=v.ih;
    id_=v.id_;
  }

  int lock() { return mutex.lock(); }
  int unlock() { return mutex.unlock(); }
  void interactionHandler( interaction_handler_type& i ) { ih=&i; }
  interaction_handler_type& interactionHandler() const { return *ih; }
  void currentVoxel( voxel_type& v ) { current=&v; }
  voxel_type& currentVoxel() { return *current; }
  void currentNbhrVoxel( voxel_type& v ) { nbhr=&v; }
  voxel_type& currentNbhrVoxel() { return *nbhr; }
  void id( int i ) { id_=i; }
  int id() const { return id_; }

  void twoVoxelInteractions() const {
    VIH_::twoVoxelInteractions( *current , *nbhr );
  }
  
  void twoVoxelInteractions( voxel_type& v1 , voxel_type& v2 ) const {
    // This doesn't do an occupancy check since that should
    // have been done already.
    if ( v1.index() == v2.index() ) {
      return VIH_::sameVoxelInteractions( v1 );
    } else {
      //cout << "DIFFERENT" << endl;
      for ( occupant_iterator ii = v1.begin(); ii!=v1.end(); ++ii ) {
	for ( occupant_iterator jj = v2.begin(); jj!=v2.end(); ++jj ) {
	  //(*ii)->print();
	  //(*jj)->print(); 
	  ih->interaction( *(*ii) , *(*jj) );
	}
      }
      //cout << "DONE" << endl;
    }
  }

  void sameVoxelInteractions() const {
    VIH_::sameVoxelInteractions(*current);
  }

  void sameVoxelInteractions( voxel_type& v1 ) const {
    if ( v1.occupancy() < 2 ) { return; }
    for ( occupant_iterator ii = v1.begin(); ii!=v1.end(); ++ii ) {
      occupant_iterator current = ii;
      for ( occupant_iterator jj = ++current; jj!=v1.end(); ++jj ) {
	//(*ii)->print();
	//(*jj)->print();
	ih->interaction( *(*ii) , *(*jj) );
      }
    }
  }

  void print( std::ostream& o=std::cout ) const {
    o << "VIH:" << id_ << "\tInteractionHandler:\n";
    ih->print(o);
  }

  VIH_& operator=( const VIH_& v ) {
    VIH_::copy(v);
    return *this;
  }

  bool operator==( const VIH_& v ) const {
    return id_ == v.id_;
  }

  ~VoxelInteractionHandler() { VIH_::initVars(); /* Don't delete anything */ }

};

//--------------------------------------------- 

#endif
