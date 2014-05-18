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
#ifndef PARTICLEINTERACTIONHANDLER_HPP_
#define PARTICLEINTERACTIONHANDLER_HPP_

//--------------------------------------------- 

#include "particle.hpp"
#include "particleStats.hpp"
#include <iostream>
#include <cmath>

//--------------------------------------------- 

template < typename Particle >
class ParticleInteractionHandler {

private:
  typedef ParticleInteractionHandler< Particle > PIH_;

public:
  enum { dimension = Particle::dimension };
  typedef Particle particle_type;
  typedef typename particle_type::precision precision;
  typedef typename particle_type::vec_type vec_type;

 protected:
  precision timeStep_;
  precision springConstant_;
  precision eqDistance_;
  precision forceConstraint_;
  precision noiseAmplitude_;
  int id_;

  // This encourages pass throughs by giving a boost to whatever
  // direction particles were going in the first place
  void handleCollision( Particle& p1, Particle& p2 ) const {
    const vec_type& f1 = p1.F();
    const vec_type& f2 = p2.F();
    const precision mag1 = p1.F().magnitude();
    precision mag1m1 = 0;
    if ( mag1 > .01 ) { mag1m1 = 1.0/mag1; }
    else { mag1m1=.1; }
    const precision mag2 = p2.F().magnitude();
    precision mag2m1 = 0;
    if ( mag2 > .01 ) { mag2m1 = 1.0/mag2; }
    else { mag2m1=.1; }

    vec_type f1_;
    vec_type f2_;    

    for ( unsigned int ii=0; ii<dimension; ++ii ){
      f1_[ii] = -.1*(f1[ii] * mag1m1);
      f2_[ii] = -.1*(f2[ii] * mag2m1);
    }

    p1.lock();
    p1.f += f1_;
    p1.unlock();

    p2.lock();
    p2.f += f2_;
    p2.unlock();
  }

  void initVars() {
    timeStep_=0; springConstant_=0;
    eqDistance_=0; id_=0;
    forceConstraint_=0;
  }

 public:
  ParticleInteractionHandler() {  PIH_::initVars(); }  
  ParticleInteractionHandler( const PIH_& p ) {  
    PIH_::copy(p);
  }  

  void copy ( const PIH_& pi ) {
    timeStep_=pi.timeStep_;
    springConstant_=pi.springConstant_;
    eqDistance_=pi.eqDistance_;
    noiseAmplitude_=pi.noiseAmplitude_;
    id_=pi.id_;
  }

  int id() const { return id_; }
  void id ( int i ) { id_=i; }

  precision timeStep() const { return timeStep_; }
  void timeStep( precision t ) { timeStep_=t; }

  precision springConstant() const { return springConstant_; }
  void springConstant( precision k ) { springConstant_=k; }

  precision eqDistance() const { return eqDistance_; }
  void  eqDistance( precision e )  { eqDistance_=e; }

  void forceLimit( precision v ) { forceConstraint_=v; }
  precision forceLimit() const { return forceConstraint_; }

  void enforceFLimit( Particle& p1 ) const {
    for ( unsigned int d=0; d<dimension; ++d ) {
      if ( p1.f[d] > forceConstraint_ ) {
	p1.f[d] = forceConstraint_;
      } else if ( p1.f[d] < -1.0 * forceConstraint_ ) {
	p1.f[d] = -1.0*forceConstraint_;
      }
    }
    // p1.print(cout);
   }

  void noiseAmplitude( precision n ) { noiseAmplitude_=n; }
  precision noiseAmplitude() const { return noiseAmplitude_; }
  
  void addNoise( Particle& p1 ) const {
    precision factor = 1.0;
    for ( unsigned d=0; d<dimension; ++d ) {
      if ( (((precision)rand()) / ((precision)RAND_MAX+1.0)) < .5 ) {
	factor *= -1;
      }
      p1.f[d] += factor * noiseAmplitude_ * (((precision)rand()) /
					     ((precision)RAND_MAX+1.0));
    }    
  }

  void interaction( Particle& p1 , Particle& p2 ) const {
    if ( euclideanDistance( p1.X().begin() , p1.X().end() , p2.X().begin() ) <
	 eqDistance_ ) {
      springRepulsiveInteraction( p1 , p2 );
    }
  }

  void springRepulsiveInteraction( Particle& p1 , Particle& p2 ) const {
    
    if ( p1.collisionCheck(p2) ) { addNoise(p1); addNoise(p2); return; }

    const vec_type& x1 = p1.X();
    const vec_type& x2 = p2.X();
    const precision magx1x2 = x1.distance( x2 );
    const precision sepFromIdeal = ( magx1x2 - eqDistance_ );
    const precision scale = -1.0 * springConstant_ * sepFromIdeal / magx1x2;

    vec_type f_;
    vec_type fm1_;

    for ( unsigned int ii=0; ii<dimension; ++ii ){
      precision f = ( x1[ii] - x2[ii] ) * scale;
      f_[ii] = f;
      fm1_[ii] = -1.0*f;
    }

//     cout << "d: " << magx1x2 << "\tsep: " << sepFromIdeal << "\tSc: " 
// 	 << scale << "\tEQ: " << eqDistance_ << endl;
//     cout << "B4" << endl; 
//     p1.print(); 
//     p2.print(); 

    p1.lock();
    p1.add2F(f_);
    p1.unlock();

    p2.lock();
    p2.add2F(fm1_);
    p2.unlock();

//    cout << "AFTER" << endl; 
//    p1.print(); 
//    p2.print(); 

  }

  void integrate( Particle& p1 ) const {    
    PIH_::integrateFirstOrder(p1);
  }

  void integrate(  Particle& p1 , precision t ) const {
    PIH_::integrateFirstOrder(p1,t);
  }

  void integrateFirstOrder(  Particle& p1 , precision t ) const {
    for (unsigned int ii=0; ii<dimension; ++ii)
      {	
	precision finc = p1.f[ii] * t;
  	if ( finc < 0 ) { finc = -min<precision>((precision).05,abs(finc)); } 
  	else { finc = min<precision>((precision).05,finc); } 
	p1.x[ii] += finc;
      }
  }

  void integrateFirstOrder(  Particle& p1 ) const {
    PIH_::integrateFirstOrder(p1,timeStep_);
  }

  void print( std::ostream& o=std::cout ) const {
    o << "PIH:" << id_ << "\tT: " << timeStep_ 
      << "\tk:" << springConstant_ << "\ta: " << eqDistance_
      << '\n';
  }

  PIH_& operator= ( const PIH_& p ) {
    PIH_::copy(p); return *this;
  }

  bool operator== ( const PIH_& p ) const {
    return id_==p.id_;
  }

  virtual ~ParticleInteractionHandler() { }

};

//--------------------------------------------- 

#endif
