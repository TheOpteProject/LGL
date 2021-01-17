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
#ifndef _FIXEDVEC_H_
#define _FIXEDVEC_H_

//----------------------------------------------------

#include <iostream>
#include <cmath>
#include <utility>

//----------------------------------------------------

template < typename T >
inline auto sqr( T t ) -> decltype( t * t )
{
	return t * t;
}

template < typename T, unsigned int dimension_ >
class FixedVec {

 public:
  using prec_ = decltype( std::declval< T & >() + std::declval< T & >() );	// this handles atomics as well as raw arithmetic types
  typedef prec_ precision;
  typedef unsigned int size_type;
  typedef T * iterator;
  typedef const T * const_iterator;

 private:
  typedef FixedVec< T, dimension_ > Vec_;

  T orig[dimension_];   // The origin
  
 public:
  // CONSTRUCTORS
  FixedVec(){}
  FixedVec( prec_ v ) { Vec_::constant(v); }
  FixedVec( const Vec_& p ){ Vec_::copy(p); }

  // ACCESSORS
  size_type size() const { return dimension_; }
  iterator begin() { return orig; }
  const_iterator begin() const { return orig; }
  iterator end() { return &orig[0]+dimension_; }
  const_iterator end() const { return &orig[0]+dimension_; }

  // MUTATORS
  template < typename U >
  void copy( const FixedVec< U, dimension_ > &p ) {
    for ( size_type ii=0; ii<dimension_; ++ii)
      orig[ii] = p[ii];
  }

  // OPERATIONS
  void translate( const Vec_& p ) {
    for ( size_type ii=0; ii<dimension_; ++ii)
      orig[ii] += p[ii];
  }

  void translate( prec_ x ) {
    for ( size_type ii=0; ii<dimension_; ++ii)
      orig[ii] += x;
  }

  void subtract( prec_ x ) {
    for ( size_type ii=0; ii<dimension_; ++ii)
      orig[ii] -= x;
  }

  void scale( const Vec_& p ) {
    for ( size_type ii=0; ii<dimension_; ++ii)
      orig[ii] *= p[ii];
  }

  void scale( prec_ x ) {
    for ( size_type ii=0; ii<dimension_; ++ii)
      orig[ii] *= x;
  }

  void constant ( prec_ x ) {
    for ( size_type ii=0; ii<dimension_; ++ii)
      orig[ii]=x;
  }

  prec_ magnitudeSquared() const {
    prec_ mag=0;
    for ( size_type ii=0; ii<dimension_; ++ii)
      mag += sqr( (*this)[ii] );
    return mag;
  }

  prec_ magnitude() const {
    return sqrt( Vec_::magnitudeSquared() );
  }

  prec_ distanceSquared( const Vec_& p ) const {
    prec_ distance=0;
    for ( size_type ii=0; ii<dimension_; ++ii) {
      prec_ dx = orig[ii]-p[ii]; 
      distance += sqr( dx );
    } 
    return distance;
  }

  prec_ distance( const Vec_& p ) const {
    return sqrt( Vec_::distanceSquared( p ) );
  }

  prec_ sum() const {
    prec_ sum=0;
    for ( size_type ii=0; ii<dimension_; ++ii)
	sum += orig[ii];
    return sum;
  }

  prec_ average() const {
    return this->sum()/static_cast<prec_>(dimension_);
  }

  prec_ product() const {
    prec_ product=1;
    for ( size_type ii=0; ii<dimension_; ++ii)
	product *= orig[ii];
    return product;
  }

  prec_ dotProduct( const Vec_& v ) const {
    prec_ product=0;
    for ( size_type ii=0; ii<dimension_; ++ii)
		 product += orig[ii] * v[ii];
    return product;
  }

  // STREAMS
  std::istream& loadFromStream( std::istream& i = std::cin ) {
    for ( size_type ii=0; ii<dimension_; ++ii) {
      i >> orig[ii];
    }
    return i;
  }

  std::ostream& print( std::ostream& o = std::cout ) const {
    o << orig[0];
    for ( size_type ii=1; ii<dimension_; ++ii)
      o << " " << orig[ii];
    o << '\n';
    return o;
  }

  // OPERATORS
  Vec_& operator=( const Vec_& p2 ) {
    Vec_::copy(p2); return *this;
  }

  template < typename U >
  Vec_ &operator=( const FixedVec< U, dimension_ > &p2 )
  {
    Vec_::copy(p2);
	 return *this;
  }

  void operator-= ( const Vec_& p2 ) {
    for ( size_type ii=0; ii<dimension_; ++ii)
      orig[ii] -= p2[ii];
  }
  
  template < typename U >
  void operator+=( const FixedVec< U, dimension_ > &p2 ) {
    for ( size_type ii=0; ii<dimension_; ++ii)
      orig[ii] += p2[ii];
  }
  
  Vec_& operator=( prec_ p ) {
    Vec_::constant(p); return *this;
  }

  bool operator== ( const Vec_& p2 ) const {
    for ( size_type ii=0; ii<dimension_; ++ii) {
      if ( orig[ii] != p2[ii] ) return 0;
    } return 1;
  }

  bool operator!= ( const Vec_& p2 ) const {
    return !( *this == p2 );
  }

  T& operator[] ( size_type i ) {
    return orig[i];
  }

  prec_ operator[] ( size_type i ) const {
    return orig[i];
  }
};

//----------------------------------------------------

#endif
