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
#ifndef MOLECULE_HPP_
#define MOLECULE_HPP_

//--------------------------------------

#include "fixedVecOperations.hpp"
#include <vector>
#include <iostream>
#include <fstream>
#include <iomanip>
#include <string>
#include <boost/tokenizer.hpp>
#include <boost/limits.hpp>
#include <boost/random/uniform_on_sphere.hpp>

//--------------------------------------

template< typename Particle >
class Molecule
{
public:
  typedef Particle particle_type;
  typedef typename particle_type::precision precision;
  typedef typename std::vector<particle_type> particle_holder;
  typedef typename particle_holder::size_type size_type;
  typedef typename particle_holder::iterator iterator;
  typedef typename particle_holder::const_iterator const_iterator;
  typedef typename std::vector<precision> vec_type;

private:
  particle_holder particles;
  std::string id;
  vec_type maxs;
  vec_type mins;

  void resetMinsMaxs()
  {
    for ( size_type ii=0; ii<dimension(); ++ii ) {
      maxs[ii] = -1e10;
      mins[ii] = 1e10;
    }
  }

  void init( size_type d )
  {
    mins.clear();
    maxs.clear();
    mins.resize( d );
    maxs.resize( d );
    resetMinsMaxs();
  }

  void min_max_test( const particle_type& s )
  {
    const vec_type& v = s.location();
    //s.print();
    for ( size_type ii=0; ii<dimension(); ++ii )
      {
	precision upper = v[ii] + s.radius();
	precision lower = v[ii] - s.radius();
	if ( lower < mins[ii] ) { mins[ii] = lower; }
	if ( upper > maxs[ii] ) { maxs[ii] = upper; }
      }
  }

  void translateParticle( size_type ii , const vec_type& m ) {
    vec_type& l = particles[ii].location();
    translate( l.begin() , l.end() , m.begin() );
  }


public:
  // CONSTRUCTORS
  Molecule( size_type d = 2 ) { init(d); }
  Molecule( size_type d , const std::string& i ) : id(i) { init(d); }
  Molecule( const Molecule<particle_type>& m ) { operator=(m); }

  // ACCESSORS
  const particle_holder& atoms() const { return particles; }
  const_iterator atoms_begin() const { return particles.begin(); }
  const_iterator atoms_end() const { return particles.end(); }
  iterator atoms_begin() { return particles.begin(); }
  iterator atoms_end() { return particles.end(); }

  const std::string& ID() const { return id; }

  size_type size() const { return particles.size(); }
  size_type dimension() const { return mins.size(); }

  const vec_type& max() const { return maxs; }
  const vec_type& min() const { return mins; }

  bool inRange( const Molecule<particle_type>& m ) const
  {
    // Look for overlap in maxs and mins
    const vec_type& mins2 = m.min();
    const vec_type& maxs2 = m.max();
    for ( size_type ii=0; ii<mins2.size(); ++ii )
      if ( (maxs2[ii] < mins[ii]) || (mins2[ii] > maxs[ii]) )
	return false;
    return true;
  }

  // MUTATORS
  
  void addMolecule( const Molecule& other ) {
    if ( other.dimension() != dimension() ) { }
    // Combine the ids
    id += "|" + other.ID();
    // Add all the particles from the other molecule
    for ( size_type ii=0; ii<other.size(); ++ii ) {
      push_back( other[ii] );
    }
  }
 
  void translateMolecule( const vec_type& m ) {
    for ( size_type ii=0; ii<particles.size(); ++ii ) {
      translateParticle( ii , m );
    }
    resetMinsMaxs();
    for ( size_type ii=0; ii<particles.size(); ++ii ) {
      min_max_test( particles[ii] );
    }
  }

  void clear()
  {
    size_type d = dimension();
    particles.clear();
    id.clear();
    init( d );
  }

  void ID( const std::string& i ) { id=i; }

  void push_back( const particle_type& s )
  { 
    min_max_test(s);
    particles.push_back(s); 
  }
  
  void resize( size_type ii ) { particles.resize(ii); }
  void reserve( size_type ii ) { particles.reserve(ii); }

  void print( std::ostream& o=std::cout ) const {
    o << "Molecule " << id << '\n';
    o << "Mins: "; 
    copy( mins.begin() , mins.end() , std::ostream_iterator<precision>(o," "));
    o << "\nMaxs: "; 
    copy( maxs.begin() , maxs.end() , std::ostream_iterator<precision>(o," "));
    o << '\n';
    for ( size_type ii=0; ii<particles.size(); ++ii ) {
      particles[ii].print(o);
    }
  }

  // OPERATORS
  Molecule<particle_type>& operator=( const Molecule<particle_type>& m )
  {
    particles = m.atoms();
    mins = m.min();
    maxs = m.max();
    id = m.ID();
    return *this;
  }

  const particle_type& operator[]( size_type ii ) const
  {
    return particles[ii];
  }

};

template < typename Molecule >
bool molecular_size_based_test( const Molecule& m1 , const Molecule& m2 )
{
    return m1.size() < m2.size();
}

//--------------------------------------

template < typename Molecule >
void moveMolecule( Molecule& m , const typename Molecule::vec_type& v )
{
  typedef typename Molecule::vec_type vec_type;
  vec_type x = simpleAverageMoleculePosition( m );
  scale( x.begin() , x.end() , -1.0 );  
  vec_type vv( v );
  translate( vv.begin() , vv.end() , x.begin() );
  m.translateMolecule( vv );
}

//--------------------------------------

template < typename Molecule >
typename Molecule::precision radiusOfMolecule( const Molecule& m )
{
  typedef const typename Molecule::vec_type& vec_ref;
  typename Molecule::vec_type center = simpleAverageMoleculePosition( m );
  vec_ref min = m.min();
  vec_ref max = m.max();
  return .5 * euclideanDistance( min.begin() , min.end() , max.begin() );
}

//--------------------------------------

template < typename Molecule >
Molecule readMoleculeFromCoordFile( const char * file , 
				    typename Molecule::precision radius )
{
  typedef typename boost::tokenizer< boost::char_separator<char> > tokenizer;
  typename tokenizer::iterator tok_iter , tok_beg , tok_end;
  typename Molecule::particle_holder particle_holder;
  typedef typename Molecule::size_type size_type;
  typedef typename Molecule::particle_type particle_type;
  typedef typename Molecule::vec_type vec_type;
  typedef typename Molecule::precision precision;

  std::ifstream in( file );
  if ( !in ) {
    std::cerr << "readMoleculeFromCoordFile: Open of " << file << " Failed\n";
    exit(EXIT_FAILURE);
  }

  boost::char_separator<char> sep(" ");
  size_type dimension = 0;
  Molecule m(3);

  while( ! in.eof() )
    {
      char l[256];
      in.getline( l , 256 );
      std::string line(l);
      if ( line == "" ) { break; }
      tokenizer tokens( line , sep );
      tok_beg = tokens.begin();
      tok_end = tokens.end();
      if ( dimension == 0 ) {
	dimension = distance( tok_beg , tok_end ) - 1;
	Molecule mm(dimension,std::string(file));
	m = mm;
      }
      std::string id( *tok_beg );
      vec_type coords;
      while(++tok_beg!=tok_end) {
	coords.push_back( (precision) atof( (*tok_beg).c_str() ) );
      }
      particle_type s(id,coords,radius);
      m.push_back(s);
    }

  return m;
}

//--------------------------------------

#if 0
template < typename Molecule >
bool doMoleculesIntersect( const Molecule& m1 , const Molecule& m2 )
{
  typedef typename Molecule::size_type size_type;
  if ( ! m1.inRange( m2 ) ) { return false; }
  for ( size_type ii=0; ii<m1.size(); ++ii )
    for ( size_type jj=0; jj<m2.size(); ++jj )
      if ( doIntersect( m1[ii] , m2[jj] ) )
	return true;
  return false;
}
#endif
//--------------------------------------

template < typename Molecule >
typename Molecule::vec_type simpleAverageMoleculePosition( const Molecule& m )
{
  typedef const typename Molecule::vec_type& vec_ref;
  vec_ref min = m.min();
  vec_ref max = m.max();
  typename Molecule::vec_type mean( min.size() );
  for ( typename Molecule::size_type ii=0; ii<min.size(); ++ii ) {
    mean[ii] = ( min[ii] + max[ii] ) * .5;
  }
  return mean;
}

//--------------------------------------

template < typename Iterator >
void printMolecules( std::ostream& o , Iterator begin, Iterator end )
{
  for (;begin!=end; ++begin) { begin->print(o); }
}

//--------------------------------------

template < typename Molecule , typename Sphere >
bool isMoleculeRoughlyInSphere( const Molecule& m , const Sphere& s )
{
  typename Molecule::vec_type x1 = 
    simpleAverageMoleculePosition( m );
  typename Molecule::precision r1 = 
    radiusOfMolecule( m );
  Sphere s1( x1 , r1 );
  return doIntersect( s , s1 );
}

//--------------------------------------

#endif
