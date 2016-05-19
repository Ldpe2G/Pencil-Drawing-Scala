from scipy.sparse import lil_matrix
from scipy.sparse.linalg import spsolve
import os


A = lil_matrix((10, 10))
b = lil_matrix((10, 10))

for line in open( os.getcwd() + '/python/' + 'AA.txt','r').readlines():
	lists = line.split()
	if len(lists) == 2:
		A = lil_matrix( ( int(lists[0]), int(lists[1]) ) )
	else:
		A[int(lists[0]), int(lists[1])] = float(lists[2])

for line in open( os.getcwd()  + '/python/' + 'bb.txt','r').readlines():
	lists = line.split()
	if len(lists) == 2:
		b = lil_matrix( ( int(lists[0]), int(lists[1]) ) )
	else:
		b[int(lists[0]), int(lists[1])] = float(lists[2])

A = A.tocsr()
x = spsolve(A, b)

fo = open( os.getcwd() + '/python/' + 'xx.txt', 'wb')
for xx in x:
	fo.write( str(xx) + '\n' );
 
fo.close()