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
#ifndef SPHERE_HPP_
#define SPHERE_HPP_

///////////////////////////////////////////////////////

#include <vector>
#include "fixedVec.hpp"
#include <string>
#include <cstdlib>
#include <sys/types.h>
#include <unistd.h>

///////////////////////////////////////////////////////

template< typename prec >
class Sphere
{
public:
  typedef prec precision;
  typedef std::vector<precision> vec_type;
  
private:
  std::string id;
  vec_type x; // Location in Space
  precision radius_;
  
public:
  Sphere() : x(2,0) { }
  Sphere( const std::string& i , const vec_type& v , precision r = 1 ) :
    id(i) , x(v) , radius_(r) { }
  Sphere( const vec_type& v , precision r = 1 ) :
    x(v) , radius_(r) { }
  
  int dimension() const { return x.size(); }
  const vec_type& location() const { return x; }
  vec_type& location() { return x; }
  precision radius() const { return radius_; }
  void radius( precision r ) { radius_=r; }
  void ID( const std::string& s ) { id=s; }
  const std::string& ID() const { return id; }

  template< typename iterator >
  void location( iterator b , iterator e  )
  {
    x.assign( b, e );
  }

  void print( std::ostream& o = std::cout ) const {
    o << "ID: " << ID() << "\tR: " << radius_ << "\tX: ";
    std::copy( x.begin() , x.end() , std::ostream_iterator<precision>(o," "));
    o << '\n';
  }
  
  void printBasic( std::ostream& o = std::cout ) const {
    o << ID() << " ";
    std::copy( x.begin() , x.end() , std::ostream_iterator<precision>(o," "));
    o << '\n';
  }
  
  Sphere<precision>& operator=( const Sphere<precision>& s )
  {
    id = s.ID();
    x = s.location();
    radius_ = s.radius();
    return *this;
  }
  
};

///////////////////////////////////////////////////////

template< typename Sphere , typename Vectype >
inline void seriesOfPointsOnSphere( Sphere& s , int count , Vectype& v )
{
  v.resize( count );
  for ( int ii=0; ii<count; ++ii )
    randomPointOnSurface( s , v.at(ii) );
}

///////////////////////////////////////////////////////

template < typename Sphere >
inline bool doIntersect( const Sphere& s1 , const Sphere& s2 )
{
  typedef typename Sphere::vec_type vec_type;
  typedef typename Sphere::precision precision;
  const vec_type& x1 = s1.location();
  const vec_type& x2 = s2.location();
  precision min = s1.radius() + s2.radius();
  precision d = euclideanDistance( x1.begin() , x1.end() , x2.begin() );
  return d < min;
}

///////////////////////////////////////////////////////

template < typename Vectype >
inline void uniform_on_sphere_vec( Vectype& v , int dimension )
{
  // Assume radius = 1
  typedef typename Vectype::value_type value_type;
  const value_type PI = 3.141592654;
  v.resize( dimension );
  if ( dimension == 2 )
    {
      value_type theta = rand()/(RAND_MAX+1.0) * 2.0 * PI;
      v.at(0) = cos( theta ); // X
      v.at(1) = sin( theta ); // Y
    }
  else if ( dimension == 3 )
    {
      value_type theta = 2.0 * PI * rand()/(RAND_MAX+1.0);
      value_type phi   = acos( 1.0 - 2.0 * rand()/(RAND_MAX+1.0) );
      v.at(0) = cos( theta ) * sin( phi ); // X
      v.at(1) = sin( theta ) * sin( phi ); // Y
      v.at(2) = cos( phi );                // Z
    }
  else
    {
      std::cerr << "Unsupported dimension: must be 2 or 3\n";
      exit(EXIT_FAILURE);
    }
}

///////////////////////////////////////////////////////

template < typename Sphere >
inline void randomPointOnSurface( const Sphere& s ,
				  typename Sphere::vec_type& r )
{
#if 1
  uniform_on_sphere_vec( r , s.dimension() );
#else
  // TODO use this instead of the hand-made code?
  typedef typename boost::hellekalek1995 precision;
  precision rr(++randomPointOnSurfaceCall___);
  boost::uniform_on_sphere<> usph( s.dimension() );
  std::vector<double> v = usph( rr );
  std::copy( v.begin() , v.end() , std::ostream_iterator<double>(std::cerr," ")); std::cerr << '\n';
  typename Sphere::vec_type r( v.begin() , v.end() );
#endif
  // Scale the vec to match the radius of the sphere
  scale( r.begin() , r.end() , s.radius() );
  // Recenter for the sphere
  translate( r.begin() , r.end() , s.location().begin() );
}

///////////////////////////////////////////////////////

#endif
