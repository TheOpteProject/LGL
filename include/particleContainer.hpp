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
#ifndef PARTICLE_CONTAINER_HPP_
#define PARTICLE_CONTAINER_HPP_

//----------------------------------------------------

#include <iostream>
#include "particle.hpp"
#include <string>
#include <vector>

//----------------------------------------------------

template < typename Particle >
class ParticleContainer {

private:
  typedef ParticleContainer< Particle > PC_;

public:
  enum { dimension = Particle::dimension };
  typedef Particle particle_type;
  typedef typename particle_type::precision precision;
  using container_type = std::vector< Particle >;
  using size_type = typename container_type::size_type;
  using iterator = typename container_type::iterator;

 protected:
  container_type particles_;
  precision avg_temp = 0;
  precision avg_dx = 0;

 public:
  vector<string> ids; // This will hold the names

  void initParticles() {
    for ( size_type ii=0; ii<size(); ++ii) {
      particles_[ ii ].index( ii );
    }
  }

 public:
  ParticleContainer() = default;

  explicit ParticleContainer( size_type s ) {
     PC_::resize(s); PC_::initParticles();
  }

  void resize (size_type s) {
	  const size_type old_size = size();
    ids.resize(s);
	 particles_.resize( s );
	 for ( size_type ii = old_size; ii < s; ++ii ) {
		 particles_[ii].index( ii );
		 particles_[ii].container( -1 );
	 }
  }

  void erase( size_type index )
  {
	  particles_.erase( particles_.begin() + index );
	  ids.erase( ids.begin() + index );
	  for ( size_type ii = index; ii < size(); ++ii ) {
		  particles_[ii].index( ii );
		  particles_[ii].container( -1 );
	  }
  }
  
  iterator begin() noexcept { return particles_.begin(); }
  iterator end() noexcept { return particles_.end(); }

  precision temp() const { return avg_temp; }
  precision dx() const { return avg_dx; }
  size_type size() const { return particles_.size(); }

  bool operator== ( const PC_& pc ) const {
	  return particles_ == pc.particles_;
  }

  Particle& particle ( size_type entry ) {
    return particles_[ entry ];
  }

  Particle& operator[] ( size_type entry ) {
    return PC_::particle(entry);
  }
  
  void print( std::ostream& o = std::cout ) const {
    for ( size_type ii = 0; ii < size(); ++ii ) {
      o << ids[ii] << '\n'; 
      particles_[ ii ].print( o );
    }
  }
  
/*    friend std::ostream& operator<< ( std::ostream& o, const PC_& p ); */

};

//----------------------------------------------------

#endif
