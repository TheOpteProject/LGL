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
#ifndef TIMEKEEPER_H_
#define TIMEKEEPER_H_

//----------------------------------------------------

#include <iostream>

//----------------------------------------------------

template < typename prec_ = float >
class TimeKeeper {

public:
  typedef prec_ precision;
  
private:
  typedef unsigned int ctr_t;
  typedef TimeKeeper<prec_> TK_;
  
private:
  void initVars() {
    timeStep_=0;
    iteration_=0;
    totalTime_=0;
    maxIteration_=0;
    minIteration_=0;
  }
  
protected:
  prec_ timeStep_;
  prec_ totalTime_;
  ctr_t iteration_;
  ctr_t maxIteration_;
  ctr_t minIteration_;
  
public:
  TimeKeeper() { TK_::initVars(); }

  void increment( prec_ dt ) {
    ++iteration_;
    totalTime_ += dt;
  }

  void increment() {
    ++iteration_;
    totalTime_ += timeStep_;
  }

  ctr_t iteration() const { return iteration_; }
  prec_ timeStep() const { return timeStep_; }
  void timeStep( prec_ dt ) { timeStep_=dt; }  
  prec_ time() const { return totalTime_; }
  void max( ctr_t t ) { maxIteration_=t; }
  prec_ max() const { return maxIteration_; }
  void min( prec_ t ) { minIteration_=t; }
  prec_ min() const { return minIteration_; }

  bool rangeCheck() const { 
    return ( (iteration_ >= minIteration_) && 
	     (iteration_ <= maxIteration_) ); 
  }
  
  void copy( const TimeKeeper& tk ) {
    totalTime_=tk.totalTime_;
    iteration_=tk.iteration_;
    timeStep_=tk.timeStep_;
    minIteration_=tk.minIteration_;
    maxIteration_=tk.maxIteration_;
  }

  void operator= ( const TimeKeeper& tk ) {
    TK_::copy(tk);
  }

  void operator++ () {
    TK_::increment();
  }

  void operator+= ( prec_ dt ) {
    TK_::increment(dt);
  }
};

//----------------------------------------------------

#endif
