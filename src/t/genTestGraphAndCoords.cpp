/////////////////////////////////////////////////////////

#include <iostream>
#include <fstream>
#include <vector>
#include <cmath>

using namespace std;

/////////////////////////////////////////////////////////

const int SIZE = 9;
const float LENGTH = 1.0;

/////////////////////////////////////////////////////////

struct X
{
  float x;
  float y;
  void print( std::ostream& o = std::cout ) {
    o << x << " " << y << "\n";
  }
};

/////////////////////////////////////////////////////////

int main( int argc , char ** argv )
{
  X coords[SIZE][SIZE];
  float dx = 0, dy = 0;
  int ctr = 0;
  ofstream coordfile("coords");
  for ( int ii=0; ii<SIZE; ++ii ) {
    for ( int jj=0; jj<SIZE; ++jj ) {
      X x;
      x.x = dx;
      x.y = dy;
      coords[ii][jj] = x;
      coordfile << ctr << " "; x.print(coordfile);
      dy += LENGTH;
      ++ctr;      
    }
    dy = 0.0;
    dx += LENGTH;
  }
  coordfile.close();

  X * x1 = &coords[0][0];
  X * x2 = &coords[0][0];
  ofstream edges("edges.ncol");
  for ( int ii=0; ii<SIZE*SIZE; ++ii ) {
    for ( int jj=ii+1; jj<SIZE*SIZE; ++jj ) { 
      X& xx1 = *(x1+ii);
      X& xx2 = *(x1+jj);
      dx = xx1.x - xx2.x;
      dy = xx1.y - xx2.y;
      float d = sqrt( dx*dx + dy*dy );
      if ( d < (LENGTH + .1*LENGTH) )
	edges << ii << " " << jj << '\n';
    }
  }
  edges.close();
  return EXIT_SUCCESS;
}
