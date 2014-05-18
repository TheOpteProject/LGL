//  (C) Copyright Boost.org 2001. Permission to copy, use, modify, sell and
//  distribute this software is granted provided this copyright notice appears
//  in all copies. This software is provided "as is" without express or implied
//  warranty, and with no claim as to its suitability for any purpose.

//  See http://www.boost.org for most recent version.

//  Intel compiler setup:

#include "boost/config/compiler/common_edg.hpp"

#if defined(__INTEL_COMPILER)
#  define BOOST_INTEL_CXX_VERSION __INTEL_COMPILER
#elif defined(__ICL)
#  define BOOST_INTEL_CXX_VERSION __ICL
#elif defined(__ICC)
#  define BOOST_INTEL_CXX_VERSION __ICC
#elif defined(__ECC)
#  define BOOST_INTEL_CXX_VERSION __ECC
#endif

#define BOOST_COMPILER "Intel C++ version " BOOST_STRINGIZE(BOOST_INTEL_CXX_VERSION)
#define BOOST_INTEL BOOST_INTEL_CXX_VERSION

#if (BOOST_INTEL_CXX_VERSION <= 500) && defined(_MSC_VER)
#  define BOOST_NO_EXPLICIT_FUNCTION_TEMPLATE_ARGUMENTS
#  define BOOST_NO_TEMPLATE_TEMPLATES
#endif

#if (BOOST_INTEL_CXX_VERSION <= 600) || !defined(BOOST_STRICT_CONFIG)

#  if defined(_MSC_VER) && (_MSC_VER <= 1300) // added check for <= VC 7 (Peter Dimov)

// Boost libraries assume strong standard conformance unless otherwise
// indicated by a config macro. As configured by Intel, the EDG front-end
// requires certain compiler options be set to achieve that strong conformance.
// Particularly /Qoption,c,--arg_dep_lookup (reported by Kirk Klobe & Thomas Witt)
// and /Zc:wchar_t,forScope. See boost-root/tools/build/intel-win32-tools.jam for
// details as they apply to particular versions of the compiler. When the
// compiler does not predefine a macro indicating if an option has been set,
// this config file simply assumes the option has been set.
// Thus BOOST_NO_ARGUMENT_DEPENDENT_LOOKUP will not be defined, even if
// the compiler option is not enabled.

#     define BOOST_NO_SWPRINTF
#  endif

// Void returns, 64 bit integrals don't work when emulating VC 6 (Peter Dimov)

#  if defined(_MSC_VER) && (_MSC_VER <= 1200)
#     define BOOST_NO_VOID_RETURNS
#     define BOOST_NO_INTEGRAL_INT64_T
#  endif

#endif

// See http://aspn.activestate.com/ASPN/Mail/Message/boost/1614864
#if BOOST_INTEL_CXX_VERSION < 700
#  define BOOST_NO_INTRINSIC_WCHAR_T
#else
// _WCHAR_T_DEFINED is the Win32 spelling
// _WCHAR_T is the Linux spelling
#  if !defined(_WCHAR_T_DEFINED) && !defined(_WCHAR_T)
#    define BOOST_NO_INTRINSIC_WCHAR_T
#  endif
#endif

#if _MSC_VER+0 >= 1000
#  if _MSC_VER >= 1200
#     define BOOST_HAS_MS_INT64
#  endif
#  define BOOST_NO_SWPRINTF
#elif defined(_WIN32)
#  define BOOST_DISABLE_WIN32
#endif

// I checked version 6.0 build 020312Z, it implements the NRVO.
// Correct this as you find out which version of the compiler
// implemented the NRVO first.  (Daniel Frey)
#if (BOOST_INTEL_CXX_VERSION >= 600)
#  define BOOST_HAS_NRVO
#endif

//
// versions check:
// we don't support Intel prior to version 5.0:
#if BOOST_INTEL_CXX_VERSION < 500
#  error "Compiler not supported or configured - please reconfigure"
#endif
//
// last known and checked version:
#if (BOOST_INTEL_CXX_VERSION > 800)
#  if defined(BOOST_ASSERT_CONFIG)
#     error "Unknown compiler version - please run the configure tests and report the results"
#  elif defined(_MSC_VER)
#     pragma message("Unknown compiler version - please run the configure tests and report the results")
#  endif
#endif




