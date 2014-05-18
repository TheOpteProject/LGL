//  (C) Copyright Boost.org 2001. Permission to copy, use, modify, sell and
//  distribute this software is granted provided this copyright notice appears
//  in all copies. This software is provided "as is" without express or implied
//  warranty, and with no claim as to its suitability for any purpose.

//  See http://www.boost.org for most recent version.

//  SGI Irix specific config options:

#define BOOST_PLATFORM "SGI Irix"

#define BOOST_NO_SWPRINTF 
//
// these are not auto detected by POSIX feature tests:
//
#define BOOST_HAS_GETTIMEOFDAY
#define BOOST_HAS_PTHREAD_MUTEXATTR_SETTYPE

#ifdef __GNUC__
   // GNU C on IRIX does not support threads (checked up to gcc 3.3)
#  define BOOST_DISABLE_THREADS
#endif

// boilerplate code:
#define BOOST_HAS_UNISTD_H
#include <boost/config/posix_features.hpp>


