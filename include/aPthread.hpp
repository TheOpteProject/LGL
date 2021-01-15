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
//--------------------------------------------------
// These are just simple thread/mutex wrappers.
//--------------------------------------------------

#ifndef APTHHREAD_H_
#define APTHHREAD_H_

//--------------------------------------------------

#include <pthread.h>
#include <iostream>
#include <unistd.h>
#include <cassert>

using namespace std;

//--------------------------------------------------

typedef void *(*FunctionPtr) (void*);

class Apthread {

 protected:
  pthread_t handle_;
  pthread_attr_t attr_;
  FunctionPtr functionPtr_;

 public:
  Apthread() { 
    pthread_attr_init(&attr_); 
    pthread_attr_setdetachstate(&attr_,PTHREAD_CREATE_JOINABLE); 
  }
  Apthread( const pthread_attr_t& Attr ) : attr_(Attr) { }

  int create( FunctionPtr f , void * args = 0 ) {
    functionPtr_=f;
    return pthread_create(&handle_,&attr_,f,args);
  }

  int create(  void * args = 0 ) { 
    return pthread_create(&handle_,&attr_,functionPtr_,args);
  }

  void functionPtr ( FunctionPtr f ) { functionPtr_=f; }
  FunctionPtr functionPtr ( ) const { return functionPtr_; }

  int scope( int scope ) {
    return pthread_attr_setscope(&attr_, scope);
  }

  void handle( const pthread_t& h ) { handle_=h; }
  pthread_t handle() const { return handle_; }

  void attribute( const pthread_attr_t& a ) { attr_=a; }
  const pthread_attr_t& attribute() const { return attr_; }

  int scope() const {
    int scope;
    pthread_attr_getscope(const_cast<pthread_attr_t*>(&attr_),&scope);
    return scope;
  }

  int wait( void** args=0 ) {
    return pthread_join(handle_,args);
  }

  void copy( const Apthread& t ) {
    handle_ = t.handle_;
    attr_ = t.attr_;
  }

  Apthread& operator= ( const Apthread& t ) {
    Apthread::copy(t); return *this;
  } 

  bool operator== ( const Apthread& t ) const {
    // This sucks
    return &handle_==&(t.handle_) && &attr_ == &(t.attr_);
  }

  bool operator!= ( const Apthread& t ) const {
    return !(*this==t);
  }

  const pthread_t& threadHandle() { return handle_; }
  pthread_t id() { return( pthread_self() ); }
  
  ~Apthread() { }

};

//--------------------------------------------------

class ApthreadContainer {

 public:
  typedef unsigned int size_type;
  
 protected:
  Apthread * threads_;
  size_type size_;
  
 public:
  Apthread defaultThread;

 public:
  ApthreadContainer( ) : threads_(0) , size_(0) { }
  ApthreadContainer( size_type count ) : size_(count) {
    threads_ = new Apthread[count];
  }

  void copy ( const ApthreadContainer& c ) {
    if (*this==c) return;
    delete [] threads_;
    size_=c.size_;
    threads_ = new Apthread[c.size_];
    for ( size_type ii=0; ii<size_; ++ii ) {
      threads_=c.threads_;
    }
  }

  void create( FunctionPtr f , void * args = 0 ) {
    int rc;
    defaultThread.functionPtr(f);
    for ( size_type ii=0; ii<size_; ++ii ) {
      rc=threads_[ii].create(f,args);
      if ( rc != 0 ) {
	std::cerr << "ApthreadContainer: Error on thread create."
		  << std::endl;
	//exit(1);
      }
    }
  }

  void create( void * args = 0 ) {
    int rc;
    for ( size_type ii=0; ii<size_; ++ii ) {
      rc=threads_[ii].create(defaultThread.functionPtr(),args);
      if ( rc != 0 ) {
	std::cerr << "ApthreadContainer: Error on thread create."
		  << std::endl;
	//exit(1);
      }
    }    
  }

  void wait() {
    int rc;
    for ( size_type ii=0; ii<size_; ++ii ) {
      rc=threads_[ii].wait();
      if ( rc != 0 ) {
	std::cerr << "ApthreadContainer: Error on thread wait.\t";
	std::cerr << rc << endl;
	//exit(1);
      }
    }
  }

  void applyAttributes() {
    for ( size_type ii=0; ii<size_; ++ii ) {
      threads_[ii].attribute( defaultThread.attribute() );
    }
  }

  size_type size() const { return size_; }

  ApthreadContainer& operator= ( const ApthreadContainer& c ) {
    ApthreadContainer::copy(c); return *this;
  }

  bool operator== ( const ApthreadContainer& c ) {
    for (size_type i=0; i<size_; ++i) {
      if ( threads_[i] != c.threads_[i] ) return 0;
    } return 1;
  }

  Apthread& operator[] ( size_type i ) {
    return threads_[i];
  }

  ~ApthreadContainer() { delete [] threads_; }

};

//--------------------------------------------------
//const pthread_mutex_t mutex_Init__ = PTHREAD_ERRORCHECK_MUTEX_INITIALIZER_NP;

class Amutex {

 protected:
   pthread_mutex_t mutex_;
   int error;

   void errorCheck() {
     if ( error != 0 )
       Amutex::parseError();
   }

   void parseError() {
#if 0
     cerr << "Mutex error: " << error << '\t';
     if ( error == EINVAL ) 
       cerr << "mutex has not been properly initialized\n";
     else if ( error == EDEADLK ) 
       cerr << "the mutex is already locked by the calling thread\n";
     else if ( error == EBUSY )
       cerr << "mutex could not be acquired because it was currently locked\n";
     else
       cerr << "unknown error\n";
#endif
   }

 public:

   Amutex( const pthread_mutex_t& m ) : mutex_(m) { }
   Amutex()
     : mutex_()
   {
     const int err = pthread_mutex_init( &mutex_, nullptr );
     if ( err != 0 )
       throw std::runtime_error( "Mutex initialization failed with error code " + std::to_string( err ) );
   }

   void copy( const Amutex& m ) { mutex_=m.mutex_; }

   int lock() { 
     const int ret = pthread_mutex_lock(&mutex_); 
     if ( ret )
       std::cerr << "pthread_mutex_lock returned nonzero: " << ret << '\n';
     return ret;
   }

   int trylock() { 
     return pthread_mutex_trylock(&mutex_); 
   }

   int unlock() { 
     return pthread_mutex_unlock(&mutex_); 
   }

   Amutex& operator= ( const Amutex& m ) {
     Amutex::copy(m); return *this;
   }
};

//--------------------------------------------------
   
#endif
