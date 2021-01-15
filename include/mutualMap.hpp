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
#ifndef MUTUALMAP_HPP_
#define MUTUALMAP_HPP_

#include <map>
#include <exception>

// Create a mutual mapping from former to latter;
template < typename former , typename latter >
class MutualMap
{
  
public:
  typedef std::pair<former,latter> former_latter_pair;
  typedef std::pair<latter,former> latter_former_pair;
  typedef std::map<former,latter> Former2LatterMap;
  typedef std::map<latter,former> Latter2FormerMap;
  typedef typename Former2LatterMap::iterator former_latter_iterator;
  typedef typename Latter2FormerMap::iterator latter_former_iterator;

private:
  typedef MutualMap<former,latter> MutMap;
  Former2LatterMap former2latter;
  Latter2FormerMap latter2former;

  template < typename t1 , typename t2 >
  t2 findEntryInMap( const std::map<t1,t2> * m , const t1& t ) const
  {
    typename std::map<t1,t2>::const_iterator i = m->find( t );
    if ( i == m->end() ) { throw std::invalid_argument("mutualMap Find Failed"); }
    return i->second;
  }  

public:

  MutualMap( MutMap& m ) { MutMap::operator=(m); }
  MutualMap() { }
  
  void clear() { latter2former.clear(); former2latter.clear(); }

  void removeMap( const former& f , const latter& l )
  {
    former2latter.erase( f );
    latter2former.erase( l );
  }

  void removeMap( const former& f )
  {
    latter l = findLatter( f );
    removeMap( f ,  l );
  }

  void removeMap( const latter& l )
  {
    former f = findFormer( l );
    removeMap( f , l );
  }

  void createMap( const former& f , const latter& l )
  {
    former_latter_pair p( f , l );
    former2latter.insert( p );
    latter_former_pair pp( l , f );
    latter2former.insert( pp );
  }

  bool doesMapExist ( const former& f , const latter& l ) const
  {
    typename Former2LatterMap::const_iterator i = former2latter.find( f );
    return i->second() == l;
  }

  bool doesMapExist ( const former& f ) const
  {
    typename Former2LatterMap::const_iterator i = former2latter.find( f );
    return i != former2latter.end();
  }

  bool doesMapExist ( const latter& l ) const
  {
    typename Latter2FormerMap::const_iterator i = latter2former.find( l );
    return i != latter2former.end();
  }

  former findFormer( const latter &l ) const
  {
    return findEntryInMap( &latter2former , l );
  }

  latter findLatter( const former &f ) const
  {
    return findEntryInMap( &former2latter , f );
  }

  typename Former2LatterMap::const_iterator former2LatterMapBegin() { 
    return former2latter.begin(); 
  }

  typename Latter2FormerMap::const_iterator latter2FormerMapBegin() { 
    return latter2former.begin();
  }

  typename Former2LatterMap::const_iterator former2LatterMapEnd() { 
    return former2latter.end(); 
  }

  typename Latter2FormerMap::const_iterator latter2FormerMapEnd() { 
    return latter2former.end(); 
  }

  Former2LatterMap former2LatterMap() const { return former2latter; }
  Latter2FormerMap latter2FormerMap() const { return latter2former; }
  
  MutMap& operator= ( const MutMap& m ) {
    former2latter = m.former2LatterMap();
    latter2former = m.latter2FormerMap();
    return *this;
  }
  
};

#endif
