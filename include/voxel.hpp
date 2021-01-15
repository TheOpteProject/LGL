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
#ifndef _VOXEL_HPP_
#define _VOXEL_HPP_

#include <iostream>
#include <unordered_set>
#include "cube.hpp"
#include "aPthread.hpp"

//------------------------------------------------------------
// A voxel IS a cube with occupants inside.
// What you are looking for is probably in 
// cube.h.
//------------------------------------------------------------

template < typename Occupant >
class Voxel : public Cube< typename Occupant::precision , 
			   Occupant::dimension >
{

 private:
  typedef typename std::unordered_set< Occupant * > OL_;
  typedef Voxel< Occupant > Voxel_;
  typedef Cube< typename Occupant::precision , 
		Occupant::dimension > Cube_;

 public:
  typedef typename OL_::size_type size_type;
  typedef typename OL_::iterator occupant_iterator;
  typedef typename OL_::const_iterator const_occupant_iterator;
  typedef Occupant occupant_type;

 protected:
  unsigned int index_; 
  long interactionCtr_;
  Amutex mutex;
  OL_ occupants;

 public:
  // CONSTRUCTORS
  Voxel() : Cube_() , index_(0) , interactionCtr_(0), mutex() { }
  Voxel( const Voxel_& v ) { Voxel_::operator=(v); }

  // ACCESSORS
  unsigned int index() { return index_; }

  int lock() { return mutex.lock(); }
  int unlock() { return mutex.unlock(); }
  int trylock() { return mutex.trylock(); }

  occupant_iterator begin() { return occupants.begin(); }
  occupant_iterator end() { return occupants.end(); }
  const_occupant_iterator begin() const { return occupants.begin(); }
  const_occupant_iterator end() const { return occupants.end(); }

  size_type occupancy() const { return occupants.size(); }

  long interactionCtr() const { return interactionCtr_; }

  bool empty() const { return occupants.empty(); }

  // MUTATORS

  void index( unsigned int i ) { index_=i; }

  void insert( Occupant& o ) { occupants.insert(&o); }
  void remove( Occupant& o ) { occupants.erase(&o); }

  void incInteractionCtr( long i=1 ) { interactionCtr_+=i; }
  void interactionCtr( long i ) { interactionCtr_=i; }

  void copy( const Voxel_& v ) {
    Cube_::operator=(v);
    mutex=v.mutex;
    index_=v.index_; 
    interactionCtr_=v.interactionCtr_;
    occupants=v.occupants;
  }

  void print( std::ostream& o=std::cout ) const {
    o << "Vox: " << index_ << "\tOcc: " << occupants.size() 
      << "\tCtr: " << interactionCtr_ << '\t';
    Cube_::print(o);
  }

  void resize( const Cube_& c ) {
    Cube_::operator=(c);
  }

  bool operator== ( const Voxel_& v ) const {
    return index_==v.index_;
  }

  bool operator!= ( const Voxel_& v ) const {
    return !(*this==v);
  }

  const Voxel_& operator= ( const Voxel_& v ) {
    Voxel_::copy(v); return *this;
  }
};

//------------------------------------------------------------

#endif
