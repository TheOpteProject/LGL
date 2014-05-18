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
#ifndef TRANSPROBS_HPP_
#define TRANSPROBS_HPP_

//----------------------------------------------------

#include <vector>
#include <map>
#include <set>
#include <iostream>
#include "matrix.hpp"

//----------------------------------------------------

template < typename State >
struct Transition
{
  State to;
  State from;
  Transition( State& t , State& f ) : to(t) , from(f) { }
  bool operator== ( const Transition<State>& other ) const {
    return other.to == to && other.from == from;
  }
};

//----------------------------------------------------

template < typename value >
struct value_incrementer : public binary_function< value , value , value >
{
  value operator()( const value& old_value , const value& new_value )
  {
    return old_value + new_value;
  }
};

//----------------------------------------------------

template < typename State >
class TransitionStateHandler
{
public:
  typedef Transition< State > transition_type;
  typedef Matrix< State > transition_matrix;

private:
  typedef typename std::multimap< State , State > TransitionContainer;
  typedef typename std::set< State > FromStateContainer;

  TransitionContainer transitions;
  FromStateContainer fromstates;

public:

  // CONSTRUCTORS
  TransitionStateHandler() { }

  // ACCESSORS
  typename TransitionContainer::size_type size() const
  {
    return transitions.size();
  }

  // MUTATORS
  void add_transition( transition_type& t )
  {
    add_transition( t.from , t.to );
  }

  void add_transition( State f , State t )
  {
    fromstates.insert( f );
    transitions.insert( std::pair<State,State>(f,t) );
  }

  void calcTransitionProbs( transition_matrix& m )
  {
    typedef typename FromStateContainer::iterator from_state_iterator;
    typedef typename TransitionContainer::iterator trans_iterator;
    typedef typename transition_matrix::value_type value_type;
    value_incrementer<value_type> vi;
    m.clear();
    for ( from_state_iterator fromstate = fromstates.begin();
	  fromstate != fromstates.end(); ++fromstate )
      {
	std::pair< trans_iterator , trans_iterator > range =
	  transitions.equal_range( *fromstate );
	trans_iterator begin = range.first, end = range.second;
	for ( ; begin!=end; ++begin )
	  {
	    if ( ! m.exists( begin->first , begin->second ) )
	      m.insert( begin->first , begin->second , 1.0 );
	    else
	      m.binary_operate_on_value( begin->first , begin->second , 1.0, vi );
	  }
	row_normalize( m );
      }
  }

};

//------------------------------------------------------

#endif
