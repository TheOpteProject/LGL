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
#ifndef PARTICLE_CONTAINER_CHAPERONE_HPP_
#define PARTICLE_CONTAINER_CHAPERONE_HPP_

//----------------------------------------------------
// This class is a helper for particleContainer. It takes
// care of initializations and such of X, V, etc
//----------------------------------------------------

#include <iostream>
#include <fstream>
#include <string>
#include <cstdlib>
#include <map>
#include <stdexcept>
#include <unordered_map>
#include <string.h>	// for strdup

#include "particle.hpp"
#include "particleContainer.hpp"

//----------------------------------------------------

const unsigned int _X_FILE__ = 0;  // Position file
const unsigned int _V_FILE__ = 1;  // Velocity file
const unsigned int _M_FILE__ = 2;  // Mass file
const unsigned int _T_FILE__ = 2;  // Temprature file

//----------------------------------------------------

template < typename Particle >
class ParticleContainerChaperone {

 protected:
  typedef ParticleContainer< Particle > PC_;
  typedef ParticleContainerChaperone< Particle > PCC_;

public:
  enum { dimension = Particle::dimension };
  typedef Particle particle_type;
  typedef typename particle_type::vec_type vec_type;
  typedef typename particle_type::precision precision;
  typedef typename PC_::size_type size_type;

  PC_& pc_;
  vec_type initVel_;
  vec_type vel_;
  vec_type initPos_;
  std::unordered_map< std::string, vec_type > positions_from_file_;
  precision radius_;
  precision initMass_;
  char * file_in[3];     //  X V M
  precision posRange_;       // Range for randomizing positions  
  precision velRange_;       //   "    "      "       velocities
  bool randomPos_;
  bool randomVel_;
  ifstream streams_in[3];   // X V M
  ofstream streams_out[3];  // X V Temprature
  char * file_out[3];       // X V Temprature ( These are output files )
  bool file_out_flag[3];
  int level_;

  void openInFiles() {
    for ( int i=0; i<3; ++i ) {
      if ( file_in[i] != 0 ) {
	streams_in[i].open(file_in[i]); 
	if (!streams_in[i]) {
	  cerr << " Could not open Input File: " << file_in[i] << endl;
	  std::exit(EXIT_FAILURE);
	}
      }
    }
  }

  void openOutFiles() {
    for ( int i=0; i<3; ++i ) {
      if ( file_out[i] != 0 ) {
	streams_out[i].open(file_out[i]); 
	if (!streams_out[i]) {
	  cerr << " Could not open Out File: " << file_out[i] << endl;
	  std::exit(EXIT_FAILURE);
	}
      }
    }
  }

  void closeInFiles() {
    for ( int i=0; i<3; ++i ) {
      if ( file_in[i] != 0 ) {
	streams_in[i].close();
      }
    }
  }

  void closeOutFiles() {
    for ( int i=0; i<3; ++i ) {
      if ( file_out[i] != 0 ) {
	streams_out[i].close();
      }
    }
  }

  void orderingError( const char * file ) {
    cerr << "The ordering in the initialization file " 
	 << file << " is off.\n";
	 std::exit(EXIT_FAILURE);
  }
  
  void  writeXout ( const Particle& p , const string& id ) {
    streams_out[_X_FILE__] << id << " ";
    p.printXCoords(streams_out[_X_FILE__]);
    streams_out[_X_FILE__] << '\n';
  }
  
#if 0
  void  writeVout ( const Particle& p , const string& id ) {
    streams_out[_V_FILE__] << id << " "; 
    p.printVCoords(streams_out[_V_FILE__]);
    streams_out[_V_FILE__] << '\n';
  }
  
  void  writeTout ( const Particle& p , const string& id ) {
    streams_out[_T_FILE__] << id << " " << p.temp() << '\n';
  }
#endif

  bool setXFromFile( Particle &p , const string &id2check ) {
#if 1
	  // assuming id2check isn't empty for simplicity of this optimized implementation
	  assert( !id2check.empty() );
	  const auto it = positions_from_file_.find( id2check );
	  if ( it != positions_from_file_.end() ) {
		  p.X( it->second );
		  return true;
	  }
#else
	  string id = "";
	  if ( !streams_in[_X_FILE__].eof() && streams_in[_X_FILE__] >> id ) {
		  if ( id2check != "" && id2check != id ) {
			  for ( size_type ii=0; ii<pc_.size(); ++ii ) {
				  if ( pc_.ids[ii] == id ) {
					  for ( size_type jj=0; jj<dimension; ++jj) {
						  streams_in[_X_FILE__] >> pos_[jj];
					  }
					  pc_[ii].X(pos_);
					  return true;
				  }
			  }
			  // Warn that `id` found in initial coordinates file is not found in the main input file? That could be too noisy in some use cases...
			  float ignored;
			  for ( size_type ii=0; ii<dimension; ++ii)
				  streams_in[_X_FILE__] >> ignored;
			  return false;
		  }
		  for ( size_type ii=0; ii<dimension; ++ii) {
			  streams_in[_X_FILE__] >> pos_[ii];
		  }
		  p.X(pos_);
		  return true;
	  }
#endif
	  return false;
  }

  bool setXFromAnchors( Particle& p, const std::string& id2check ) const
  {
	  typename AnchorPositions_t::const_iterator const it = anchorPositions_.find( id2check );
	  if ( it == anchorPositions_.end() ) 
		  return false;
	  p.X( it->second );
	  p.markAnchor();
	  return true;
  }
  
  bool setVFromFile( Particle& p , string id2check= "") {
    string id = "";
    if ( !streams_in[_V_FILE__].eof() && streams_in[_V_FILE__] >> id ) {
      if ( id2check != "" && id2check != id ) {
	PCC_::orderingError(file_in[_V_FILE__]);
      }
      for ( size_type ii=0; ii<dimension; ++ii) {
	streams_in[_V_FILE__] >> vel_[ii];
      }
      p.V(vel_);
      return 1;
    } else {
      return 0;
    }
  }
  
  bool setMFromFile( Particle& p , string id2check= "") {
    string id = "";
    if ( !streams_in[_M_FILE__].eof() && streams_in[_M_FILE__] >> id ) {
      if ( id2check != "" && id2check != id ) {
	PCC_::orderingError(file_in[_M_FILE__]);
      }
      streams_in[_M_FILE__] >> initMass_;
      p.mass(initMass_);
      return 1;
    } else {
      return 0;
    }
  }

  vec_type& randomVec( vec_type& v , precision range ) {
    for ( unsigned int jj=0; jj<dimension; ++jj ) {
      v[jj] = static_cast<precision>(std::rand()/(RAND_MAX+1.0))*range;
    } return v;
  }
  
  void initVals() { 
    for ( unsigned int jj=0; jj<3; ++jj ) { 
      file_in[jj]=0; file_out[jj]=0; file_out_flag[jj]=0; 
    }
    vel_=0; initVel_=0; initPos_=0; radius_=0;
    initMass_=0; posRange_=0; velRange_=0; randomPos_=0; randomVel_=0;
  }

  ParticleContainerChaperone() { }

 public:

  ParticleContainerChaperone( PC_& pc__ ) : pc_(pc__) { PCC_::initVals(); }

  void initAllParticles();
  void initParticle( size_type p );
  void writeOutFiles();

  void initPos( const char * x ) { file_in[_X_FILE__]=strdup(x); }
  void initMass( const char * m ) { file_in[_M_FILE__]=strdup(m); }

  void initAnchors( const std::string &filepath )
  {
	  std::ifstream f( filepath.c_str() );
	  if ( !f )
		  throw std::runtime_error( "Failed to open " + filepath + " for reading" );
	  readAnchors( f );
  }

  void initVel( const vec_type& v ) { initVel_=v; randomVel_=0; }
  void initPos( const vec_type& x ) { initPos_=x; randomPos_=0; }
  void initMass( precision m ) { initMass_=m; }
  void initRadius( precision r ) { radius_=r; }
  void randomizePosRange( precision p ) { posRange_=p; randomPos_=1; }
  void randomizeVelRange( precision v ) { velRange_=v; randomVel_=1; }
  int level() const { return level_; }
  void level( int l ) { level_=l; }
  void velOutFile( const char * v ) { 
    file_out[_V_FILE__]=strdup(v); file_out_flag[_V_FILE__]=1;
  }
  void posOutFile( const char * x ) { 
    file_out[_X_FILE__]=strdup(x); file_out_flag[_X_FILE__]=1;
  }
  void tempOutFile( const char * t ) { 
    file_out[_T_FILE__]=strdup(t); file_out_flag[_T_FILE__]=1;
  }

  const vec_type& initVel() { return initVel_; }
  const vec_type& initPos() { return initPos_; }
  precision initMass() { return initMass_; }
  precision initRadius() { return radius_; }

  ~ParticleContainerChaperone() {
    for ( int i=0; i<3; ++i ) {
		 delete file_in[i];
		 delete file_out[i];
    }
  }

private:
	typedef std::map< std::string, vec_type > AnchorPositions_t;
	AnchorPositions_t anchorPositions_;

	void readAnchors( std::istream &is )
	{
		std::string id;
		vec_type pos;
		while ( is >> id ) {
			if ( !readPos( is, pos ) )
				throw std::domain_error( "Anchor position input failed for node '" + id + '\'' );
			if ( !anchorPositions_.insert( std::make_pair( id, pos ) ).second )
				throw std::domain_error( "Anchor '" + id + "' has already been specified!" );
		}
		if ( !is.eof() )
			throw std::domain_error( "Anchor file input failed around node '" + id + '\'' );
	}

	void readXin()
	{
		auto &is = streams_in[_X_FILE__];
		std::string id;
		vec_type pos;
		while ( is >> id ) {
			if ( !readPos( is, pos ) )
				throw std::domain_error( "Initial position input failed for node '" + id + '\'' );
			if ( !positions_from_file_.insert( { std::move( id ), pos } ).second )
				throw std::domain_error( "Node '" + id + "' has already been specified earlier in the positions input file!" );
		}
		if ( !is.eof() )
			throw std::domain_error( "Initial positions file input failed around node '" + id + '\'' );
	}

	static std::istream &readPos( std::istream &is, vec_type &pos )
	{
		for ( size_type ii = 0; ii < dimension; ++ii )
			is >> pos[ ii ];
		return is;
	}
};

//----------------------------------------------------

template < typename Particle >
void ParticleContainerChaperone<Particle>::initAllParticles() 
{
  PCC_::openInFiles();
  readXin();
  size_type nodeCount = pc_.size();
  for ( size_type ii=0; ii<nodeCount; ++ii ) {
    const string &id = pc_.ids[ii];
    if ( file_in[_X_FILE__] ) {
      PCC_::setXFromFile(pc_[ii],id);
    } else if ( !PCC_::setXFromAnchors( pc_[ii], id ) ) {
      if ( randomPos_ ) { pc_[ii].X( PCC_::randomVec(initPos_,posRange_) ); }
      else { pc_[ii].X(initPos_); }
    }
#if 0
    if ( file_in[_V_FILE__] ) {
      PCC_::setVFromFile(pc_[ii],id);  
    } else {
      if ( randomVel_ ) { pc_[ii].V( PCC_::randomVec(initVel_,velRange_) ); }
      else { pc_[ii].V(initVel_); }
    }
#endif
    if ( file_in[_M_FILE__] ) {
      PCC_::setMFromFile(pc_[ii],id);
    } else {
      pc_[ii].mass(initMass_);
    }
    pc_[ii].radius(radius_);
    //pc_[ii].print();
  }
  PCC_::closeInFiles();
}

//----------------------------------------------------

template < typename Particle >
void ParticleContainerChaperone<Particle>::writeOutFiles()
{
  PCC_::openOutFiles();
  size_type nodeCount = pc_.size();
  for ( size_type ii=0; ii<nodeCount; ++ii ) {
    string id = pc_.ids[ii];
    if (file_out_flag[_X_FILE__] != 0 ) {
      PCC_::writeXout(pc_[ii],id);
    }
#if 0
    if (file_out_flag[_V_FILE__] != 0 ) {
      PCC_::writeVout(pc_[ii],id);
    }
    if (file_out_flag[_T_FILE__] != 0 ) {
      PCC_::writeTout(pc_[ii],id);
    }
#endif
  }
  PCC_::closeOutFiles();
}

//----------------------------------------------------

#endif
