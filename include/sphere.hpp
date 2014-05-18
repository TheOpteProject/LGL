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
#include <boost/random.hpp>
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
  Sphere( const std::string& i , const vec_type& v , precision r = 1 ) : id(i) , x(v) ,
									 radius_(r) { }
  Sphere( const vec_type& v , precision r = 1 ) : x(v) , radius_(r) { }
  
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
    x.clear();
    copy( b , e , x.begin() );
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

template< typename Sphere >
std::vector< typename Sphere::vec_type >
seriesOfPointsOnSphere( Sphere& s , int count )
{
  typedef std::vector< typename Sphere::vec_type > VV; 
  typedef typename Sphere::vec_type vec_type;
  VV v( count , s.dimension() );
  for ( int ii=0; ii<count; ++ii ) {
      v[ii] = randomPointOnSurface( s );
  }
  return v;
}

///////////////////////////////////////////////////////

template < typename Sphere >
bool doIntersect( const Sphere& s1 , const Sphere& s2 )
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

static unsigned int randomPointOnSurfaceCall___ = rand() + getpid();

template < typename Sphere >
typename Sphere::vec_type 
randomPointOnSurface( const Sphere& s )
{
  typedef typename boost::hellekalek1995 precision;
  typedef typename Sphere::vec_type vec_type;  
  precision rr(++randomPointOnSurfaceCall___);
  boost::uniform_on_sphere<precision> usph( rr, s.dimension() );
  std::vector<double> v = usph();
  //copy( v.begin() , v.end() , ostream_iterator<double>(cerr," ")); cerr << '\n';
  typename Sphere::vec_type r( v.begin() , v.end() );
  // Scale the vec to match the radius of the sphere
  scale( r.begin() , r.end() , s.radius() );
  // Recenter for the sphere
  translate( r.begin() , r.end() , s.location().begin() );
  return r;
}

///////////////////////////////////////////////////////

#endif
