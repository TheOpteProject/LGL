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
#ifndef _CUBE_H_
#define _CUBE_H_

#include <iostream>
#include "fixedVec.hpp"

//------------------------------------------------------------

template < typename prec_, unsigned int dimension_ >
class Cube {

 private:
  typedef Cube<prec_,dimension_> Cube_;
  typedef FixedVec<prec_,dimension_> Vec_;
  typedef typename Vec_::size_type size_type;

 protected:
  Vec_ orig_; 
  prec_ radius_; // edgeLength=2*radius

 public:
  Cube() : orig_(0) { radius_=0; }

  Cube( const Vec_& p, prec_ r ) : orig_(p), radius_(r) { }

  Cube( const Cube_& c ) {
    Cube_::copy(c);
  }

  prec_ radius() const { return radius_; }
  const Vec_& orig() const { return orig_; }

  void radius( prec_ r ) { radius_=r; }
  void orig( const Vec_& p ) { orig_=p; }

  void copy( const Cube_& c ) {
    radius_=c.radius_; orig_=c.orig_;
  }

  bool checkInclusion ( const Vec_& p ) const {
    for ( size_type ii=0; ii<dimension_; ++ii ) {
      if ( p[ii] < (orig_[ii]-radius_) || 
	   p[ii] > (orig_[ii]+radius_) ) {
	return 0;
      }
    } return 1;
  }

  bool checkInclusionFuzzy  ( const Vec_& p ) const {
   for ( size_type ii=0; ii<dimension_; ++ii ) {
      if ( p[ii] < (orig_[ii]-radius_-.001) || 
	   p[ii] > (orig_[ii]+radius_+.001) ) {
	return 0;
      }
    } return 1;    
  }

  void print( std::ostream& o = std::cout ) const {
    o << "Rad: " << radius_ << "\tAt: ";
    orig_.print(o);
  }

  Cube_& operator= ( const Cube_& c ) {
    Cube_::copy(c); return *this;
  }

  bool operator== ( const Cube_& c ) const {
    return ( radius_==c.radius_ && orig_==c.orig_ );
  }

  bool operator!= ( const Cube_& c ) const {
    return !(*this==c);
  }
};

//------------------------------------------------------------

#endif
