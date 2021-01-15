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
#ifndef _GRAPH_HPP_
#define _GRAPH_HPP_

//--------------------------------------------------

#include <boost/tokenizer.hpp>
#include <boost/graph/adjacency_list.hpp>
#include <boost/graph/kruskal_min_spanning_tree.hpp>
#include <boost/graph/visitors.hpp>
#include <boost/graph/breadth_first_search.hpp>
#include <boost/property_map/property_map.hpp>
#include <boost/limits.hpp>
#include <boost/lexical_cast.hpp>
#include <iostream>
#include <fstream>
#include <vector>
#include <iomanip>
#include <map>
#include <string>
#include "mutualMap.hpp"
#include "fixedVecOperations.hpp"

//--------------------------------------------------

namespace GraphDetail
{
  template < typename T >
  inline T mmax( T x , T y )
  {
    return (x > y) ? x : y;
  }
}

//--------------------------------------------------

template < typename Weight >
class Graph
{

public:
  typedef typename std::pair<unsigned int,unsigned int> Edge;
  typedef MutualMap< std::string , int > vertex_index_map;
  typedef typename boost::adjacency_list < boost::setS, boost::vecS, boost::undirectedS, 
					   boost::property< boost::vertex_name_t, int > , 
					   boost::property< boost::edge_weight_t , Weight > > boost_graph;
  typedef typename boost_graph::edge_descriptor edge_descriptor;
  typedef typename boost_graph::vertex_descriptor vertex_descriptor;
  typedef typename boost_graph::edge_iterator edge_iterator;
  typedef typename boost::property_map< boost_graph , boost::edge_weight_t >::type weight_map;
  typedef typename boost_graph::vertices_size_type vertices_size_type;
  typedef typename boost_graph::edges_size_type edges_size_type;
  typedef typename boost_graph::vertex_iterator vertex_iterator;
  typedef typename boost_graph::out_edge_iterator out_edge_iterator;
  typedef Weight weight_type;

private:
  boost_graph G;
  vertex_index_map mutmap;
  bool weight;

public:

  Graph() : weight(false) { }
  Graph( const Graph<Weight>& g ) { Graph<Weight>::operator=(g); }

  // MUTATORS
  void vertexIdMap( const vertex_index_map& m ){ mutmap = m; }
  void boostGraph( const boost_graph& g ) { G=g; }
  void clear() { G.clear(); mutmap.clear(); }
  void weights( const weight_map& w )
  {
    edge_iterator e , eend;
    for ( tie(e,eend) = edges(G); e!=eend; ++e ) {
      get(  boost::edge_weight , G )[*e] = w[*e];
    }
    weight = true;
  }

  void removeEdge( const edge_descriptor& e ) { remove_edge( e , G ); }

  void eraseEdge(  const edge_descriptor& e )
  { 
    removeEdge( e );
    if ( mutmap.doesMapExist( source( e , G ) ) )
      mutmap.removeMap( source( e , G ) );
    if ( mutmap.doesMapExist( target( e , G ) ) )
      mutmap.removeMap( target( e , G ) );
  }
  
  // ACCESSORS
  typename boost_graph::vertices_size_type vertexCount() const { return num_vertices(G); }
  typename boost_graph::edges_size_type edgeCount() const { return num_edges(G); }

  const vertex_index_map& vertexIdMap() const { return mutmap; }
 
  boost_graph& boostGraph() { return G; }
  const boost_graph& boostGraph() const { return G; }

  weight_map weights() { return get( boost::edge_weight, G); }
  weight_type getWeight( edge_descriptor e ) const {
    return get(  boost::edge_weight , G )[e];
  }
  bool hasWeights() const { return weight; }
  void hasWeights( bool b ) { weight=b; }
  
  std::string idFromIndex( int i ) const { return mutmap.findFormer(i); }
  int indexFromId( std::string& s ) const { return mutmap.findLatter(s); }
  
  bool doesEdgeExist( vertex_descriptor u , vertex_descriptor v ) const
  {
    if ( num_edges( G ) == 0 ) { return false; }
    if ( num_vertices( G ) <= u ) { return false; }
    if ( num_vertices( G ) <= v ) { return false; }
    edge_descriptor e;
    bool found;
    tie(e,found) = edge(u,v,G);
    return found;
  }

  void addEdge( edge_descriptor e , weight_type w )
  {
    addEdge( source( e , G ) , target( e , G ) , w );
  }
  
  void addEdge( edge_descriptor e )
  {
    addEdge( source( e , G ) , target( e , G ) );
  }
  
  void addEdge( vertex_descriptor v1 , vertex_descriptor v2 , weight_type w )
  {
    weight = true;
    add_edge( v1 , v2 , w, G );
  }
  
  void addEdge( vertex_descriptor v1 , vertex_descriptor v2 )
  {
    add_edge( v1 , v2 , G );
  }
  
  void print( std::ostream& o = std::cout ) const
  {
    edge_iterator e , eend;
    for ( tie(e,eend) = edges(G); e!=eend; ++e ) {
      //o << "(" << source(*e,G) << "," << target(*e,G) << ")";
      o << "(" << idFromIndex(source(*e,G)) << "," << idFromIndex(target(*e,G))
      	<< ")" << std::flush;
      if ( weight ) {
	o << " = " << getWeight(*e);
      }
      o << '\n';
    }
  }
  
  // OPERATORS
  Graph<Weight>& operator= ( const Graph<Weight>& g ) {
    G=g.boostGraph();
    weight=g.hasWeights();
    mutmap=g.vertexIdMap();
    return *this;
  }

  ~Graph() { }
};

//--------------------------------------------------

template< typename Graph >
void remap( Graph& g )
{
  using namespace boost;
  const typename Graph::boost_graph& bg = g.boostGraph();
  typename Graph::boost_graph ng;
  typename Graph::vertex_index_map nids;
  typename Graph::vertex_descriptor v1 , v2, v1new, v2new;
  typename Graph::edge_iterator ee1, ee2;

  int index = 0;
  std::vector<int> marked( num_vertices(bg) , -1 );
  for ( tie(ee1,ee2) = edges(bg); ee1!=ee2; ++ee1 ) {
    v1 = source( *ee1 , bg );
    if ( marked[v1] == -1 ) {
      nids.createMap( g.idFromIndex(v1) , index );
      v1new = index;
      marked[v1] = index;
      ++index;
    } else {
      v1new = nids.findLatter( g.idFromIndex(v1) );
    }
    v2 = target( *ee1 , bg );
    if ( marked[v2] == -1 ) {
      nids.createMap( g.idFromIndex(v2) , index );
      v2new = index;
      marked[v2] = index;
      ++index;
    } else {
      v2new = nids.findLatter( g.idFromIndex(v2) );
    }
    if ( ! g.hasWeights() )
      add_edge( v1new , v2new , ng );
    else
      add_edge( v1new , v2new , g.getWeight(*ee1), ng );
  }
  g.boostGraph( ng );
  g.vertexIdMap( nids );
}

//--------------------------------------------------

template < typename Graph >
typename Graph::weight_type
weightBasedOnNegativeOfEdgeCount( const Graph& g , const typename Graph::Edge& e )
{
  typename Graph::out_edge_iterator ee1, ee2;
  typename Graph::vertex_descriptor v1 , v2;

  v1 = boost::source( g.boostGraph() , e );
  v2 = boost::target( g.boostGraph() , e );

  // Get edge count of each weight
  tie( ee1 , ee2 ) = out_edges( *v1 , g.boostGraph() );
  typename Graph::weight_type w = -1 * distance(ee1,ee2);

  tie( ee1 , ee2 ) = out_edges( *v2 , g.boostGraph() );
  w += -1 * distance(ee1,ee2); 

  return w;
}

template < typename Graph >
typename Graph::weight_type
actualWeight( const Graph& g , const typename Graph::Edge& e )
{
  return g.getWeight( e );
}

//--------------------------------------------------

template< typename Graph >
void generateWeightMapFromNegativeAdjacentVertexCount( Graph& g )
{
  typename Graph::edge_iterator e , eend;
  typename Graph::vertex_iterator v , vend;
  typename Graph::out_edge_iterator ee1, ee2;
  std::map< typename Graph::vertex_descriptor ,
    typename Graph::weight_type > weights;
  for ( tie(v,vend) = vertices(g.boostGraph()); v!=vend; ++v ) {
    tie(ee1,ee2) = out_edges( *v , g.boostGraph() );
    weights[*v] = -1 * distance(ee1,ee2);
  }
  for ( tie(e,eend) = edges(g.boostGraph()); e!=eend; ++e ) {
    get( boost::edge_weight , g.boostGraph() )[*e] =
      // This makes the weight positive
      //boost::numeric_limits< typename Graph::weight_type >::max() +
      weights[source(*e,g.boostGraph())] +
      weights[target(*e,g.boostGraph())];
  }
  g.hasWeights( true );
}

//--------------------------------------------------

template< typename Graph >
std::pair< typename Graph::vertex_descriptor , typename Graph::vertices_size_type >
getVertexWMostEdges( Graph& g )
{
  typename Graph::vertex_iterator v , vend;
  typename Graph::out_edge_iterator ee1, ee2;
  std::pair< typename Graph::vertex_descriptor, typename Graph::vertices_size_type> p;
  typename Graph::vertices_size_type count, most = 0;
  for ( tie(v,vend) = vertices(g.boostGraph()); v!=vend; ++v )
    {
      tie(ee1,ee2) = out_edges( *v , g.boostGraph() );
      count = distance( ee1 , ee2 );
      if ( count > most ) { p.first = *v; most = p.second = count; }
    }
  return p;
}

//--------------------------------------------------

template< typename Graph >
void setMSTFromGraph( Graph& g , Graph& mst )
{
  std::vector< typename Graph::edge_descriptor > new_edges;
  typename Graph::edge_descriptor e;
  typename Graph::boost_graph& mst_graph = mst.boostGraph();
  const typename Graph::boost_graph& old_graph = g.boostGraph();
  typename Graph::weight_map old_weights = g.weights();
  kruskal_minimum_spanning_tree( old_graph , std::back_inserter( new_edges ) );
  bool inserted;
  for ( unsigned int ii=0; ii<new_edges.size(); ++ii ) {
    tie(e,inserted) = add_edge( source( new_edges[ii] , old_graph ) , 
				target( new_edges[ii] , old_graph ) , 
				mst_graph );
    get( boost::edge_weight , mst_graph )[e] =
      old_weights[ new_edges[ii] ]; 
  }
  mst.hasWeights( true );
  mst.vertexIdMap( g.vertexIdMap() );
}

//--------------------------------------------------
// Determines the number of levels two vertices

template < typename LevelMap , typename vertex >
class dos_recorder : public boost::default_bfs_visitor
{
private:
  LevelMap& m;
  vertex v;
  bool found;
  int level;
public:
  dos_recorder( LevelMap& mm , vertex vv ) :
    m(mm) , v(vv) , found(false) , level(1) { }
  template <typename Edge , typename BoostGraph>
  void tree_edge( Edge e , const BoostGraph& bg )
  {
    if ( found ) { return; }
    level = m[ boost::source(e,bg) ] + 1;
    if ( boost::target(e,bg) == v ) {
      found = true;
    }
    m[ boost::target(e,bg) ] = level;
  }
};

template < typename LevelMap , typename Vertex >
dos_recorder<LevelMap,Vertex>
record_dos( LevelMap& m , Vertex v ) { return dos_recorder<LevelMap,Vertex>(m,v); }


template < typename Graph >
int degreesOfSeparation( const Graph& g , typename Graph::vertex_descriptor u ,
			 typename Graph::vertex_descriptor v )
{
  const typename Graph::boost_graph& mst_bg = g.boostGraph();
  std::vector<int> lm( boost::num_vertices(mst_bg) , 0 );
  breadth_first_search( mst_bg , u , boost::visitor( record_dos(lm,v) ) );
  return lm[v];
}

//--------------------------------------------------
// Records the number of levels from one vertex to 
// any other vertex (Minimum spanning distance)

template < typename LevelMap , typename ParentMap >
class level_recorder : public boost::default_bfs_visitor
{
public:
  level_recorder( LevelMap& dist, ParentMap& parents ) : d(dist), p(parents) { }
  template <typename Edge , typename BoostGraph>
  void tree_edge( Edge e , const BoostGraph& bg )
  {
    d[ boost::target(e,bg) ] = d[ boost::source(e,bg) ] + 1;
    p[ boost::target(e,bg) ] = boost::source(e,bg);
  }
private:
  LevelMap& d;
  ParentMap& p;
};

template < typename LevelMap, typename ParentMap >
level_recorder<LevelMap,ParentMap>
record_levels( LevelMap& m , ParentMap& p )
{ return level_recorder<LevelMap,ParentMap>(m,p); };


template< typename Graph , typename LevelMap , typename ParentMap >
void setLevelMapFromMST( const Graph& mst , LevelMap& lm , ParentMap& parents ,
			 typename Graph::vertex_descriptor root )
{
  const typename Graph::boost_graph& mst_bg = mst.boostGraph();
  lm.clear();
  lm.resize( num_vertices(mst_bg) );
  parents.clear();
  parents.resize( num_vertices(mst_bg) ); 
  lm[root] = 0; // Source or root Node
  parents[root] = -1; // Root has no parent
  breadth_first_search( mst_bg , root , boost::visitor(record_levels(lm,parents)));
}

//----------------------------------------------------

template < typename LevelMap >
class level_recorder2 : public boost::default_bfs_visitor
{
public:
  level_recorder2( LevelMap& dist ) : d(dist) { }
  template <typename Edge , typename BoostGraph>
  void tree_edge( Edge e , const BoostGraph& bg )
  {
    d[ boost::target(e,bg) ] = d[ boost::source(e,bg) ] + 1;
  }
private:
  LevelMap& d;
};

template < typename LevelMap >
level_recorder2<LevelMap>
record_levels2( LevelMap& m ) { return level_recorder2<LevelMap>(m); };

template< typename Graph >
int sumDOS( const Graph& g , typename Graph::vertex_descriptor root )
{
  std::vector<int> lm;
  lm.resize( g.vertexCount() );
  lm[root] = 0; // Source or root Node
  breadth_first_search( g.boostGraph() , root , boost::visitor(record_levels2(lm)));
  return sum< typename std::vector<int>::iterator , int >( lm.begin() , lm.end() );
}

//--------------------------------------------------
// Start at the center of the mst (root), and do a depth
// first search in g for nodes missing from the mst.
//--------------------------------------------------

template< typename Graph , typename LevelMap >
void addNextLevelFromMap( Graph& g , const Graph& model , const LevelMap& lm ,
			  unsigned int level )
{
  using namespace boost;
  typename Graph::boost_graph& g_bg = g.boostGraph();
  const typename Graph::boost_graph& model_bg = model.boostGraph();
  typename Graph::vertex_iterator v , vend;
  typename Graph::out_edge_iterator ee1, ee2;
  // Go through each vertex of model and look for vertices
  // with the previous level.
  for ( tie(v,vend) = vertices(model_bg); v!=vend; ++v ) {
    if ( lm[*v] == level ) {
      // cout << "CURRENT: " << model.idFromIndex(*v) << endl;
      for ( tie( ee1 , ee2 ) = out_edges( *v , model_bg ); ee1!=ee2; ++ee1 ) {
	// Add any vertices (making an edge) that are at the current level
	// or below
	if ( lm[target( *ee1 , model_bg )] <= level ) {
	  // 	  cout <<  model.idFromIndex(source( *ee1 , model_bg )) << " " 
	  // 	       <<  model.idFromIndex(target( *ee1 , model_bg )) << endl;
	  add_edge( source( *ee1 , model_bg  ) , target( *ee1 , model_bg ) , g_bg );
	}
      }
    }
  }
}

//--------------------------------------------------
// Take any edges that are in the model and apply them
// to g if g has those vertices. It repopulates the 
// missing edges only and does not add any new vertices.
//--------------------------------------------------

template< typename Graph , typename LevelMap >
void fillMissingEdgesFromModel( Graph& g , const Graph& model , const LevelMap& lm ,
				int level )
{
  using namespace boost;
  typename Graph::boost_graph& bg_g = g.boostGraph() , 
    bg_model = model.boostGraph();
  typename Graph::vertex_iterator v , vend;
  typename Graph::out_edge_iterator ee1, ee2;
  // Go through each vertex of the given graph and look for ones
  // at the current level
  for ( tie(v,vend) = vertices(bg_model); v!=vend; ++v ) {
    if ( lm[*v] != level ) { continue; }
    // The only edges that should need repopulating are ones originating
    // from the current level
    for ( tie( ee1 , ee2 ) = out_edges( *v , bg_model ); ee1!=ee2; ++ee1 ) {
      // Add any vertices (making an edge) that are lower than the current
      // level
      if ( lm[ target( *ee1 , bg_model ) ] <= level )
	add_edge( source( *ee1 , bg_model  ) , target( *ee1 , bg_model ) , bg_g );
    }
  }
}

//--------------------------------------------------

template< typename Graph , typename LevelMap , typename ParentMap >
std::pair<typename Graph::vertex_descriptor,int>
generateLevelsFromGraph( const Graph& g , LevelMap& levels , ParentMap& parents , 
			 typename Graph::vertex_descriptor * rootPtr , Graph& mst ,
			 bool useOriginalWeights )
{
  using namespace boost;
  Graph newGraph( g );
  // First issue is to weight the nodes if desired
  if ( ! useOriginalWeights )
    generateWeightMapFromNegativeAdjacentVertexCount( newGraph );
  // Now to make a tree based on these weights
  setMSTFromGraph( newGraph , mst );
  // Now to determine the root node
  typename Graph::vertex_iterator v , vend;
  tie(v,vend) = vertices( mst.boostGraph() );
  typename Graph::vertex_descriptor root, v1, v2;


  if ( !rootPtr ) {

    // linear time algorithm to find the root by Ying Wang (yw1984@stanford.edu)

    int i, k, n = mst.vertexCount();
    vector <typename Graph::vertex_descriptor> queue(n+1);
    int *ccount, *d;
    long long tot = 0, min, *a;
    typename Graph::out_edge_iterator ee1,ee2;

    int p, q;
    queue[p = q = 0] = *v;
    a = new long long[n+1];
    ccount = new int[n+1];
    d = new int[n+1];
    for(i = 0; i <= n; i++) ccount[i] = 1, d[i] = -1, a[i] = 0;
    d[ *v ] = 0;
    const typename Graph::boost_graph& bg = mst.boostGraph();

    for(; p<=q; p++) {
      v1 = queue[p];
      tie(ee1, ee2) = out_edges(v1, bg);
      for(; ee1 != ee2; ee1++) {
	v2 = target(*ee1, bg);
	if (d[v2] == -1) queue[++q] = v2, tot += (d[v2] = d[v1] + 1);
      }
    }

    a[ *v ] = min = tot;
    root = *v;

    for(k = q; k >= 0; k--) {
      v1 = queue[k];
      tie(ee1, ee2) = out_edges(v1, bg);
      for(; ee1 != ee2; ee1++) {
	v2 = target(*ee1, bg);
	if (d[v2] > d[v1]) {
	  ccount[v1] += ccount[v2];
	}
      }
    }

    for(k = 0; k <= q; k++) {
      v1 = queue[k];
      tie(ee1, ee2) = out_edges(v1, bg);
      for(; ee1 != ee2; ee1++) {
	v2 = target(*ee1, bg);
	if (d[v2] > d[v1]) {
	  a[v2] = a[v1] - ccount[v2] + (n - ccount[v2]);
	  if (a[v2] < min) root = v2, min = a[v2];
	}
      }
    }

    delete []a;
    delete []d;
    delete []ccount;
    cerr << "root finding done!" << endl;
  } else {
    root = *rootPtr;
  }
  // The node with the most edges is at root
  //tie( root , count ) = getVertexWMostEdges( mst );  
  // Generate a breadth first level map
  setLevelMapFromMST( mst , levels , parents , root );
  int maxL = 0;
  for ( typename LevelMap::const_iterator i=levels.begin(); i!=levels.end(); ++i ) {
    maxL = (int) GraphDetail::mmax<long>( *i ,maxL );
  }
  // Return the descriptor to the one that started it all
  // with the total level count
  return std::make_pair(root,maxL);
}

//--------------------------------------------------

template< typename Graph , typename LevelMap , typename ParentMap >
std::pair<typename Graph::vertex_descriptor,int>
generateLevelsFromGraphProper( const Graph& g , LevelMap& levels ,
			       ParentMap& parents , 
			       typename Graph::vertex_descriptor * rootPtr )
{
  // Now to determine the root node
  typename Graph::vertex_iterator v , vend;
  tie(v,vend) = vertices( g.boostGraph() );
  typename Graph::vertex_descriptor root;
  if ( !rootPtr ) {
    // Init mins and find root if none was given
    int min = sumDOS( g , *v );
    root = *v;
    int ctr = 1;
    std::cerr << std::setw(8) << ctr << std::flush;
    for (; v!=vend; ++v ) {
      int sum = sumDOS( g , *v );
      if ( sum < min ) { min = sum; root=*v; }
      std::cerr << "\b\b\b\b\b\b\b\b" << std::setw(8) << ++ctr << std::flush;
    }
  } else {
    root = *rootPtr;
  }
  // The node with the most edges is at root
  //tie( root , count ) = getVertexWMostEdges( mst );  
  // Generate a breadth first level map
  setLevelMapFromMST( g , levels , parents , root );
  int maxL = 0;
  for ( typename LevelMap::const_iterator i=levels.begin(); i!=levels.end(); ++i ) {
    maxL = (int) GraphDetail::mmax<long>( *i ,maxL );
  }
  // Return the descriptor to the one that started it all
  // with the total level count
  return std::make_pair(root,maxL);
}

//--------------------------------------------------

template< typename Graph >
struct vertex_id_compare
{
  const Graph& g;
  vertex_id_compare( const Graph& g1 ) : g(g1) { }
  bool operator()( const typename Graph::vertex_descriptor v1,
		   const typename Graph::vertex_descriptor v2 ) const
  {
    return g.idFromIndex( v1 ) < g.idFromIndex( v2 );
  }
};

template< typename Graph >
void writeLGL( const Graph& g , const char * file )
{
  using namespace boost;
  const typename Graph::boost_graph& bg = g.boostGraph();
  const typename Graph::vertex_index_map& ids = g.vertexIdMap();
  typedef typename Graph::weight_type W;
  typename Graph::vertex_iterator vi, viend;
  typename Graph::vertex_descriptor v1 , v2;
  typename Graph::out_edge_iterator ee1, ee2;

  if ( g.edgeCount() == 0 ) { return; }

  std::ofstream out( file );
  if ( !out ) { 
    std::cerr << "writeLGL: Open of " << file << " failed.\n";
    exit(EXIT_FAILURE);
  }
  
  // Sort based on ids
  vertex_id_compare<Graph> vid(g);
  std::vector< typename Graph::vertex_descriptor > vertices( g.vertexCount() );
  int ii=0;
  for ( boost::tie(vi,viend) = boost::vertices( bg ); vi!=viend; ++vi, ++ii )
    vertices[ii]= *vi;
  sort( vertices.begin() , vertices.end() , vid );
  
  for ( unsigned ii=0; ii<vertices.size(); ++ii ) {
    v1 = vertices[ii];
    tie( ee1 , ee2 ) = out_edges( v1 , bg );
    if ( ee1 == ee2 ) { continue; }
    std::string lines("# " + ids.findFormer( v1 ) + '\n');
    bool entry = false;
    while( ee1 != ee2 ) {
      v2 = target( *ee1 , bg ); 
      if ( g.idFromIndex( v1 ) < g.idFromIndex( v2 ) ) {
	entry = true;
	lines += ids.findFormer( v2 );
	if ( g.hasWeights() ) {
	  lines += " " + boost::lexical_cast<std::string,W>(g.getWeight( *ee1 ));
	}
	lines += '\n';
      }
      ++ee1;
    }
    if ( entry ) { out << lines; }
  }

}

//--------------------------------------------------

template< typename Graph >
void writeNCOL( const Graph& g , const char * file )
{
  using namespace boost;
  const typename Graph::boost_graph& bg = g.boostGraph();
  const typename Graph::vertex_index_map& ids = g.vertexIdMap();
  typedef typename Graph::weight_type W;
  typename Graph::vertex_iterator vi, viend;
  typename Graph::vertex_descriptor v1 , v2;
  typename Graph::out_edge_iterator ee1, ee2;

  if ( g.edgeCount() == 0 ) { return; }

  std::ofstream out( file );
  if ( !out ) { 
    std::cerr << "writeLGL: Open of " << file << " failed.\n";
    exit(EXIT_FAILURE);
  }
  
  // Sort based on ids
  vertex_id_compare<Graph> vid(g);
  std::vector< typename Graph::vertex_descriptor > vertices( g.vertexCount() );
  int ii=0;
  for ( boost::tie(vi,viend) = boost::vertices( bg ); vi!=viend; ++vi, ++ii )
    vertices[ii]= *vi;
  sort( vertices.begin() , vertices.end() , vid );
  
  for ( unsigned ii=0; ii<vertices.size(); ++ii ) {
    v1 = vertices[ii];
    tie( ee1 , ee2 ) = out_edges( v1 , bg );
    if ( ee1 == ee2 ) { continue; }
    std::string lines;
    bool entry = false;
    while( ee1 != ee2 ) {
      v2 = target( *ee1 , bg ); 
      if ( g.idFromIndex( v1 ) < g.idFromIndex( v2 ) ) {
	entry = true;
	lines += ids.findFormer( v1 ) + " ";
	lines += ids.findFormer( v2 );
	if ( g.hasWeights() ) {
	  lines += " " + boost::lexical_cast<std::string,W>(g.getWeight( *ee1 ));
	}
	lines += '\n';
      }
      ++ee1;
    }
    if ( entry ) { out << lines; }
  }

}

//--------------------------------------------------

template< typename Graph >
void readLGL( Graph& g , const char * file )
{
  readLGL( g ,file, std::numeric_limits<typename Graph::weight_type>::max() );
}

//--------------------------------------------------

template< typename Graph >
void readLGL( Graph& g , const char * file , typename Graph::weight_type cutoff )
{
  using namespace boost;
  typedef typename Graph::boost_graph BG;
  typedef typename Graph::vertex_index_map VIM;
  typedef boost::tokenizer< boost::char_separator<char> > tokenizer;
  typename Graph::weight_type w{};	// value-initializing to avoid non-legit warning on line 804: ‘w’ may be used uninitialized in this function [-Wmaybe-uninitialized]

  std::ifstream in( file );
  if ( !in ) { 
    std::cerr << "readLGL: Open of " << file << " failed.\n";
    exit(EXIT_FAILURE);
  }
  
  boost::char_separator<char> sep(" ");
  VIM idmap;
  BG bg;
  int index = 0;
  int u=0 , v=0;

  bool hasweight = false;
  std::string head(256,' ');

  while (true)
    {
      std::string id1(256,' ');
      std::string id2(256,' ');
      char l[256];
      if ( in.eof() ) { break; }
      in.getline( l , 256 );
      std::string line(l);
      //cout << "LINE: " << line << endl;
      if ( line == "" ) { break; }
      tokenizer tokens( line , sep );
      tokenizer::iterator tok_iter = tokens.begin();
      id1 = *tok_iter++;
      if ( tok_iter != tokens.end() ) {	id2 = *tok_iter; }
      //cout << "IDS: " << id1 << " " << id2 << endl;
      if ( id1 == "#" ) {
	head = id2;
	continue;
      } else {
	if ( id2[0] != ' ' ) {
	  w = atof(id2.c_str());
	  hasweight = true;
	}
	if ( hasweight && w > cutoff ) { continue; }
	if ( idmap.doesMapExist( head ) ) {
	  u = idmap.findLatter( head );
	} else {
	  idmap.createMap( head , index );
	  u = index++;	  
	}
	if ( idmap.doesMapExist( id1 ) ) {
	  v = idmap.findLatter( id1 );
	} else {
	  idmap.createMap( id1 , index );
	  v = index++;	  
	}
      }      
      if ( ! hasweight ) { add_edge( u , v , bg ); }
      else {
	// Take the lowest edge if it is redundant
	int nv = num_vertices(bg);
	typename Graph::edge_descriptor eee;
	bool found = false;
	if ( nv > u && nv > v  ) {
	  tie( eee , found ) = edge( u , v , bg );
	}
	if ( found ) {
	  w = min( w , get(  boost::edge_weight , bg )[eee] );
	  get( boost::edge_weight , bg )[eee] = w;
	} else {
	  add_edge( u , v , w , bg );
	}
      }
      //cout << "READ: ADDED " << u << " " << v << " " << w << endl;
    }
  
  g.boostGraph(bg);
  if ( hasweight ) { g.hasWeights( true ); }
  g.vertexIdMap( idmap );
}

//--------------------------------------------------

template< typename Graph >
void readNCOL( Graph& g , const char * file )
{
  typedef typename Graph::boost_graph BG;
  typedef typename Graph::vertex_index_map VIM;

  std::ifstream in( file );
  if ( !in ) { 
    std::cerr << "readNCOL: Open of " << file << " failed.\n";
    exit(EXIT_FAILURE);
  }
  
  VIM idmap;
  BG bg;
  int index = 0;

  std::string id1(256,' ');
  std::string id2(256,' ');

  in >> id1;
  while ( id1 != "" && !in.eof() )
    {
      int u , v;
      if ( idmap.doesMapExist( id1 ) ) {
	u = idmap.findLatter( id1 );
      } else {
	idmap.createMap( id1 , index );
	u = index++;
      }
      in >> id2;
      if ( idmap.doesMapExist( id2 ) ) {
	v = idmap.findLatter( id2 );
      } else {
	idmap.createMap( id2 , index );
	v = index++;
      }      
      add_edge( u , v , bg );
      in >> id1;
    }
  
  g.boostGraph(bg);
  g.vertexIdMap( idmap );

}

//--------------------------------------------------

template < typename Graph , typename LevelMap >
void writeLevelMap2File( const Graph& g , const LevelMap& levels , 
			 const char * file )
{
  const typename Graph::boost_graph& bg = g.boostGraph();
  const typename Graph::vertex_index_map& ids = g.vertexIdMap();
  typedef typename LevelMap::value_type level_type;
  typedef typename Graph::vertex_descriptor vertex_descriptor;
  typename Graph::edge_iterator ee1, ee2;

  std::ofstream out( file );
  if ( !out ) {
    std::cerr << "writeLevelMap2File: Open of " << file << " failed.\n";
    exit(EXIT_FAILURE);
  }

  for ( tie(ee1,ee2) = edges( bg ); ee1 != ee2 ; ++ee1 ) {
    vertex_descriptor v1 = source( *ee1 , bg );
    vertex_descriptor v2 = target( *ee1 , bg );
    level_type l1 = levels[v1];
    level_type l2 = levels[v2];
    level_type l = GraphDetail::mmax< level_type >( l1 , l2 );
    out << ids.findFormer( v1 ) << " " << ids.findFormer( v2 ) << " "
	<< l << '\n';
  }

}

//--------------------------------------------------

template< typename Graph >
void readLGL_weightMin( Graph& g , const char * file ,
			typename Graph::weight_type cutoff )
{
  using namespace boost;
  typedef typename Graph::boost_graph BG;
  typedef typename Graph::vertex_index_map VIM;
  typedef boost::tokenizer< boost::char_separator<char> > tokenizer;
  typename Graph::weight_type w;

  std::ifstream in( file );
  if ( !in ) { 
    std::cerr << "readLGL: Open of " << file << " failed.\n";
    exit(EXIT_FAILURE);
  }
  
  boost::char_separator<char> sep(" ");
  VIM idmap;
  BG bg;
  int index = 0;
  int u=0 , v=0;

  bool hasweight = false;
  std::string head(256,' ');

  while (true)
    {
      std::string id1(256,' ');
      std::string id2(256,' ');
      char l[256];
      if ( in.eof() ) { break; }
      in.getline( l , 256 );
      std::string line(l);
      //cout << "LINE: " << line << endl;
      if ( line == "" ) { break; }
      tokenizer tokens( line , sep );
      tokenizer::iterator tok_iter = tokens.begin();
      id1 = *tok_iter++;
      if ( tok_iter != tokens.end() ) {	id2 = *tok_iter; }
      //cout << "IDS: " << id1 << " " << id2 << endl;
      if ( id1 == "#" ) {
	head = id2;
	continue;
      } else {
	if ( id2[0] != ' ' ) {
	  w = atof(id2.c_str());
	  hasweight = true;
	}
	if ( hasweight && w <= cutoff ) { continue; }
	if ( idmap.doesMapExist( head ) ) {
	  u = idmap.findLatter( head );
	} else {
	  idmap.createMap( head , index );
	  u = index++;	  
	}
	if ( idmap.doesMapExist( id1 ) ) {
	  v = idmap.findLatter( id1 );
	} else {
	  idmap.createMap( id1 , index );
	  v = index++;	  
	}
      }      
      if ( ! hasweight ) { add_edge( u , v , bg ); }
      else {
	// Take the lowest edge if it is redundant
	int nv = num_vertices(bg);
	typename Graph::edge_descriptor eee;
	bool found = false;
	if ( nv > u && nv > v  ) {
	  tie( eee , found ) = edge( u , v , bg );
	}
	if ( found ) {
	  w = min( w , get(  boost::edge_weight , bg )[eee] );
	  get( boost::edge_weight , bg )[eee] = w;
	} else {
	  add_edge( u , v , w , bg );
	}
      }
      //cout << "READ: ADDED " << u << " " << v << " " << w << endl;
    }
  
  g.boostGraph(bg);
  if ( hasweight ) { g.hasWeights( true ); }
  g.vertexIdMap( idmap );
}

//--------------------------------------------------

#endif
