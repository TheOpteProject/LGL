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
  typedef unsigned int size_type;
  typedef Particle* iterator;

 protected:
  size_type size_;
  Particle * particles;
  precision avg_temp;
  precision avg_dx;

 public:
  vector<string> ids; // This will hold the names

  void initParticles() {
    for ( size_type ii=0; ii<size_; ++ii) {
      particles[ii].index(ii);
      particles[ii].container(-1);
    }
  }

  void initVals() { 
    size_=0; particles=0;
  }

 public:
  ParticleContainer(){ PC_::initVals(); }
  ParticleContainer( size_type s ) {
     PC_::initVals(); PC_::resize(s); PC_::initParticles();
  }

  void resize (size_type s) {
    ids.resize(s);
    if ( particles==0 ) { particles = new Particle[s]; assert(particles); }
    else {
      Particle * temp = new Particle[s]; assert(particles);
      for ( size_type ii=0; ii<size_; ++ii ){ temp[ii]=particles[ii]; }
      particles=temp; temp=0;
    }
    size_=s;
  }
  
  iterator begin() { return &particles[0]; }
  iterator end() { return &particles[size_]; }

  precision temp() const { return avg_temp; }
  precision dx() const { return avg_dx; }
  size_type size() const { return size_; }

  void copy( const PC_& pc ) {
    if ( *this == pc ) return;
    delete [] particles; particles=0; PC_::resize(size_);
    for ( size_type ii=0; ii<size_; ++ii) {
      particles[ii]=pc.particles[ii];
    }
  }

  PC_& operator= ( const PC_& pc ) { PC_::copy(pc); return *this; }

  bool operator== ( const PC_& pc ) const {
    if ( pc.size_ != size_ ) return 0;
    for ( size_type ii=0; ii<size_; ++ii) {
      if ( particles[ii] != pc.particles[ii] ) return 0;
    } 
    return 1;
  }

  Particle& particle ( size_type entry ) {
    return particles[entry];
  }

  Particle& operator[] ( size_type entry ) {
    return PC_::particle(entry);
  }
  
  void print( std::ostream& o = std::cout ) const {
    for (size_type ii=0; ii<size_; ++ii) {
      o << ids[ii] << '\n'; 
      particles[ii].print(o);
    }
  }
  
  virtual ~ParticleContainer(){ delete [] particles; }

/*    friend std::ostream& operator<< ( std::ostream& o, const PC_& p ); */

};

//----------------------------------------------------

#endif
