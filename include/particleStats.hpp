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
#ifndef PARTICLESTATS_HPP_
#define PARTICLESTATS_HPP_

//--------------------------------------------- 

#include "particle.hpp"
#include <iostream>

//--------------------------------------------- 

template < typename Particle >
class ParticleStats {

  typedef ParticleStats< Particle > ParticleStats_;

public:
  typedef Particle particle_type;
  typedef typename particle_type::precision precision;
  typedef unsigned int size_type;
      
 protected:

  struct Stats {
    precision sumDist_;
    precision sumVel_;
    precision sumTemp_;
    Stats() {
      sumDist_ = sumVel_ = sumTemp_ = 0;
    }
    const Stats& operator= ( const Stats& s ) {
      sumDist_=s.sumDist_;
      sumVel_=s.sumVel_;
      sumTemp_=s.sumTemp_;
    }
    const Stats& operator= ( precision ii ) {
      sumDist_ = ii;
      sumVel_ = ii;
      sumTemp_ = ii;
      return *this;
    }
    const Stats& operator/= ( size_type ii ) {
      sumDist_ /= ii;
      sumVel_ /= ii;
      sumTemp_ /= ii;
      return *this;
    }
    const Stats& operator+= ( const Stats& s ) {
      sumDist_ += s.sumDist_;
      sumVel_ += s.sumVel_;
      sumTemp_ += s.sumTemp_;
      return *this;
    }
    void print( std::ostream& o = std::cout ) const {
      o << "\tDist: " << sumDist_ << "\tVel: " 
      << sumVel_ << "\tTemp: " << sumTemp_ << '\n';
    }
  };

  Stats stats;
  size_type particleCtr_;
  size_type iteration2CollectStats;

 public:

  ParticleStats() : stats() , particleCtr_(0) , iteration2CollectStats(0) { }
  ParticleStats( const ParticleStats_& ps ) {
    ParticleStats_::copy(ps);
  }

  void add2Stats_dx ( precision d ) { stats.sumDist_ += d; }

  void count( size_type c ) { particleCtr_=c; }

  void collectStatsAtIteration( size_type ii ) {
    iteration2CollectStats=ii;
  }

  size_type collectStatsAtIteration() const {
    return iteration2CollectStats;
  }
  
  precision dist() const { return stats.sumDist_; }
  precision vel() const { return stats.sumVel_; }
  precision temp() const { return stats.sumTemp_; }

  bool collectStatsCheck( size_type iteration ) {
    return (iteration2CollectStats > 0) && 
      (iteration % iteration2CollectStats == 0);
  }

  void avg() { stats /= particleCtr_; }

  void integrateWithOther( const ParticleStats_& ps ) {
    stats += ps.stats;
    particleCtr_ += ps.particleCtr_;
  }

  void reset() { stats = 0.0; particleCtr_ = 0; }

  void copy ( const ParticleStats_& p ) {
    stats=p.stats; 
    particleCtr_ = p.particleCtr_;
    iteration2CollectStats = p.iteration2CollectStats;
  }

  const ParticleStats_& operator= ( const ParticleStats_& p ) {
    ParticleStats_::copy(p);
    return *this;
  }

  void printStats( std::ostream& o=std::cout ) const {
    stats.print(o);
  }

  void print( std::ostream& o=std::cout ) const {
    o << "ParticleCount: " << particleCtr_ << '\t'
      << "CollectStatsAt: " << iteration2CollectStats << '\t';
    ParticleStats_::printStats(o);
  }
};


//--------------------------------------------- 

#endif
