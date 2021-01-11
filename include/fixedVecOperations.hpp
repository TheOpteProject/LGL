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
#ifndef _FIXEDVECTOROPERAIONS_H_
#define _FIXEDVECTOROPERAIONS_H_

//-------------------------------------------

#include <cmath>

//-------------------------------------------
// Does all pairs comparison, redundant. This
// does i && j and j && i and with i == j.

template < typename Iterator , typename BinaryFunction >
void allPairsCompleteForeach( Iterator begin , Iterator end ,
			      BinaryFunction& f )
{
  Iterator begin2 = begin;
  for ( ; begin!=end; ++begin ) {
    for ( ; begin2!=end; ++begin2 ) {
      f( *begin , *begin2 );
    }
  }
}

//-------------------------------------------
// Does a genuine all pairs, not redundant.
// For all i>j

template < typename Iterator , typename BinaryFunction >
void allPairsForeach( Iterator begin , Iterator end ,
		      BinaryFunction& f )
{
  for ( ; begin!=end; ++begin ) {
    Iterator begin2 = begin;
    for ( ++begin2; begin2!=end; ++begin2 ) {
      f( *begin , *begin2 );
    }
  }
}

//-------------------------------------------

template < typename Iterator1 , typename precision >
precision sum( Iterator1 begin1, Iterator1 end1 ) {
  precision sum = 0;
  for ( ; begin1!=end1; ++begin1 ) {
    sum += *begin1;
  }
  return sum;
}

//-------------------------------------------

template < typename Iterator1 , typename Iterator2 >
void normalize( Iterator1 begin1, Iterator1 end1, Iterator2 begin2 )
{  
  for ( ; begin1!=end1; ++begin1, ++begin2 ) {
    (*begin1) /= (*begin2);
  }
}

//-------------------------------------------

template < typename Iterator1 >
void normalize( Iterator1 begin1, Iterator1 end1 )
{
  double m = magnitude( begin1 , end1 );
  if ( m == 0 ) { return; }
  scale( begin1 , end1 , 1.0/m );
}

//-------------------------------------------

template < typename Iterator1 >
void normalize( Iterator1 begin1, Iterator1 end1, Iterator1 begin2 )
{
  normalize<Iterator1,Iterator1>(begin1,end1,begin2);
}

//----------------------------------------------------

template < typename Iterator , typename prec_ >
prec_ standardDeviation ( Iterator begin1 , Iterator end1 )
{
  // Calculate the average
  Iterator beginT = begin1;
  prec_ average = 0;
  unsigned int n = 0;
  for ( ; begin1!=end1; ++n , ++begin1 ) {
    average += *begin1;
  }
  average /= static_cast<prec_>(n);
  if ( n == 1 ) { return average; }
  prec_ sigma = 0;
  // Now to do standard deviation
  for ( ; beginT!=end1; ++beginT ) {
    prec_ d = *beginT-average;
    sigma += d * d;
  }
  return sqrt(sigma/static_cast<prec_>(n-1));
}

//-------------------------------------------

template < typename Iterator1 >
double magnitude( Iterator1 begin , Iterator1 end )
{
  double sum(0);
  for ( ; begin!=end; ++begin ) {
    sum += *begin * (*begin);
  }
  return sqrt(sum);
}

//-------------------------------------------

template < typename Iterator  >
double euclideanDistanceSquared( Iterator begin1, Iterator end1, Iterator begin2 )
{ 
  double d = 0;
  for ( ; begin1!=end1; ++begin1, ++begin2 ) {
    double dx = *begin2 - *begin1;
    d += dx * dx;
  }
  return d;
}

template < typename Iterator  >
double euclideanDistance( Iterator begin1, Iterator end1, Iterator begin2 )
{ 
  return sqrt( euclideanDistanceSquared( begin1, end1, begin2 ) );
}

//-------------------------------------------

template< typename ForwardIterator >
void translate( ForwardIterator begin , ForwardIterator end , double t ) {
  for ( ; begin!=end; ++begin )
    *begin += t;
}

//-------------------------------------------

template < typename Iterator1 , typename Iterator2 >
void translate( Iterator1 begin1, Iterator1 end1, Iterator2 begin2 )
{  
  for ( ; begin1!=end1; ++begin1, ++begin2 ) {
    (*begin1) += (*begin2);
  }
}

//-------------------------------------------

template < typename Iterator1 , typename precision >
void scale( Iterator1 begin , Iterator1 end , precision f )
{
  for ( ; begin!=end; ++begin ) { *begin *= f; }
}

//-------------------------------------------

template < typename Array >
void rotate2DCartesianVector( Array& to , const Array& from , double alpha )
{
  double sina = sin( alpha );
  double cosa = cos( alpha );
  to[0] = cosa * from[0] - sina * from[1];
  to[1] = sina * from[0] + cosa * from[1];
}

//-------------------------------------------

#endif
