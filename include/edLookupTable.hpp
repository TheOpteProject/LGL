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
#ifndef EDLOOKUPTABLE_HPP_
#define EDLOOKUPTABLE_HPP_

//----------------------------------------------------

#include <iostream>
#include <set>
#include <vector>
#include "fixedVecOperations.hpp"
#include <string>

//----------------------------------------------------

template < typename Entry , typename prec = float >
struct EDPlace {
  Entry entry;
  prec value;
  EDPlace() { }
  EDPlace( Entry& er , prec vv ) : entry(er) , value(vv) { }
  void print( std::ostream& o = std::cout ) const {
    o << "ENTRY: "; entry.print();
    o << "VALUE: " << value << '\n';
  }
  bool operator<( const EDPlace<Entry,prec>& e2 ) const {
    return value < e2.value;
  }
  bool operator==( const EDPlace<Entry,prec>& e2 ) const {
    return value == e2.value;
  }
};

//----------------------------------------------------

template < typename Entry , typename prec = float >
class EDLookupTable
{

public:
  typedef prec precision;
  typedef Entry value_type;
  typedef typename std::vector<precision> vec_type;
  typedef typename std::set<std::string> EntryList;
  typedef typename std::vector<value_type> ResultList;

private:
  typedef EDLookupTable<value_type,precision> LookupTable;
  typedef EDPlace<value_type,precision> EDE;
  typedef typename std::multiset<EDE> DimensionEntries;
  typedef typename std::vector<DimensionEntries> EDTable;
  typedef typename DimensionEntries::const_iterator const_dentry_iterator;

  EDLookupTable() { }

protected:
  EDTable table;  
  EntryList entries;
  
public:
  // CONSTRUCTORS
  explicit EDLookupTable( typename EDTable::size_type dimension ) : table(dimension)
  { }

  // ACCESSORS
  bool closeToEntries( const vec_type& v , precision cutoff )
  {
    // Go through each dimension and look for the one that
    // satisfies the cutoff
    for ( typename vec_type::size_type ii=0; ii<v.size(); ++ii ) {
      DimensionEntries& dentries = table[ii];
      typename DimensionEntries::iterator theend = dentries.end();
      EDE lower;
      lower.value = v[ii] - cutoff;
      typename DimensionEntries::iterator begin = dentries.lower_bound( lower );
      EDE upper;
      upper.value = v[ii] + cutoff;
      typename DimensionEntries::iterator end = dentries.upper_bound( upper );
      if ( begin != theend && end != theend ) {
	for(; begin!=end; ++begin) {
	  precision d = euclideanDistance( begin->entry.location().begin() , 
					   begin->entry.location().end() , 
					   v.begin() );
	  if ( d < cutoff + begin->entry.radius() ) { return true; }
	}
      }
    }
    return false;
  }

  void print( std::ostream& o = std::cout ) const
  {
    o << "ED LOOKUP TABLE\n";
    for ( typename EDTable::size_type ii=0; ii<table.size(); ++ii ) {
      o << "\tDIMENSION: " << ii << '\n';
      for ( typename DimensionEntries::const_iterator jj=table[ii].begin() ;
	    jj!=table[ii].end(); ++jj ) {
	(jj)->print(o);
      }
    }
    o << "\tCURRENT ENTRIES:\n";
    for ( typename EntryList::const_iterator ii=entries.begin(); 
	  ii!=entries.end(); ++ii ) { o << *ii << '\n'; }
  }


  // MUTATORS
  void clear() { table.clear(); entries.clear(); }

  bool insert( Entry& e , vec_type& v )
  {
    if ( entries.find( e.ID() ) != entries.end() ) { return false; }
    for ( typename vec_type::size_type ii=0; ii<v.size(); ++ii ) {
      table[ii].insert( EDE( e , v[ii]) );
    }
    entries.insert( e.ID() );
    return true;
  }

  // OPERATORS

  LookupTable& operator= ( const LookupTable& t1 ) {
    table = t1.table; entries=t1.entries;
  }

  ~EDLookupTable() { }

};

//----------------------------------------------------

#endif
