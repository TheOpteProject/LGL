//  
//  Copyright (c) 2002 Alex Adai, All Rights Reserved.
//  
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License as
//  published by the Free Software Foundation; either version 2 of
//  the License, or (at your option) any later version.
//  
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//  GNU General Public License for more details.
//  
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston,
//  MA 02111-1307 USA
//  
#ifndef MATRIX_HPP_
#define MATRIX_HPP_

//------------------------------------------------------
// So, honestly, is this probably the 100000th Matrix
// class implemented in the world?

#include <map>
#include <iostream>
#include <iomanip>
#include <vector>
#include <set>

//------------------------------------------------------

template < typename Entry , typename Value = float >
class Matrix
{

public:
  typedef Entry entry_type;
  typedef Value value_type;
  typedef typename std::map< entry_type , value_type > row_type;
  typedef typename std::map< entry_type , row_type  > matrix; 
  typedef typename matrix::iterator row_iterator;
  typedef typename row_type::iterator column_iterator;
  typedef typename std::pair< entry_type , row_type > row;  

private:
  typedef typename std::set< entry_type > Types;

private:
  matrix m;
  Types types;

  std::pair<row_iterator,column_iterator> fetch_entry( entry_type e1 , entry_type e2 , value_type v )
  {
    types.insert( e2 );
    row_iterator ri = m.lower_bound( e1 );
    if ( ri == m.end() || ri->first != e1 ) {
      ri = m.insert( ri , std::pair<entry_type,row_type>( e1 , row_type() ) );
    }
    column_iterator ci = ri->second.lower_bound( e2 );
    if ( ci == ri->second.end() || ci->first != e2 ) {  
      ci = ri->second.insert( ci , std::pair<entry_type,value_type>( e2 , v ) );
    }
    return std::pair<row_iterator,column_iterator>(ri,ci);
  }

public:
  // CONSTRUCTORS
  Matrix() { }

  // ACCEESSORS
  row_iterator row_begin() { return m.begin(); }
  row_iterator row_end() { return m.end(); }

  value_type& value( entry_type e1 , entry_type e2 )
  {
    m.find( e1 ).second.find( e2 );
  }

  bool exists( entry_type e1 , entry_type e2 )
  {
    row_iterator ri = m.find( e1 );
    if ( ri == row_end() ) { return false; }
    return ri->second.find( e2 ) != ri->second.end();
  }

  void print ( std::ostream& o = std::cout )
  {
    const int space = 16;
    string buffer(space,' ');
    o << buffer;
    std::vector< entry_type > entries;
    for ( typename Types::iterator ii = types.begin(); ii!=types.end(); ++ii )
      {
	o << setw(space) << *ii;
	entries.push_back( *ii );
      }
    o << '\n';
    for ( row_iterator ii=row_begin(); ii!=row_end(); ++ii )
      {
	o << setw(space) << ii->first;
	unsigned ctr = 0;
	for ( column_iterator jj=ii->second.begin(); jj!=ii->second.end(); ++jj )
	  {
	  AGAIN:
	    if ( entries[ctr++] == jj->first ) {
	      o << setw(space) << jj->second;
	    } else {
	      if ( ctr >= entries.size() )
		break;
	      else {
		o << setw(space) << "X";
		goto AGAIN;
	      }
	    }
	  }
	while( ctr++ < entries.size() ) { o << setw(space) << "X"; }
	o << '\n';
      }
  }

  bool empty() const { return m.empty(); }
  
  // MUTATORS
  void clear() { m.clear(); }
  
  void insert( entry_type e1 , entry_type e2 , value_type v )
  {
    fetch_entry( e1 , e2 , v );
  }

  template < typename BinaryOperation >
  void binary_operate_on_value( entry_type e1 , entry_type e2 , value_type v ,
				BinaryOperation& op )
  {
    std::pair<row_iterator,column_iterator> results = fetch_entry( e1 , e2 , v );
    row_iterator ri    = results.first;
    column_iterator ci = results.second;
    value_type new_value = op( ci->second , v );
    ci->second = new_value;
    ri->second.insert( ci , *ci );
  } 

};

//----------------------------------------------------

template < typename Matrix >
void row_normalize( Matrix& m )
{ 
  typedef typename Matrix::value_type value_type;
  for ( typename Matrix::row_iterator ii = m.row_begin();
	ii != m.row_end() ; ++ii )
    {
      value_type total(0);
      // Get the total count of the row
      for (  typename Matrix::column_iterator jj=ii->second.begin();
	     jj!=ii->second.end(); ++jj )
	{
	  total += jj->second;
	}
      // Divide each entry by the total count
      for (  typename Matrix::column_iterator jj=ii->second.begin();
	     jj!=ii->second.end(); ++jj )
	{
	  jj->second /= total;
	}
    }
}

//----------------------------------------------------

#endif
