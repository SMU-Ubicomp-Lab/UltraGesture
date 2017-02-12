import numpy as np
import os
from names import *
from gesture import Gesture

print("gesture|avg |had |u |d |l |r ")
print("-------+----+----+--+--+--+--")
for name in shortNames:
	total = 0
	had = 0
	angles = [0, 0, 0, 0] #up, down, left, right
	for i in range(7,37):
		#Get gestures
	 	gest = Gesture("data/" + str(i) + "_" + name + ".gest", True)

	 	#Get the total num of dir samples
	 	total += gest.numDirSamples

	 	#if there were inc had counter
	 	if gest.numDirSamples > 0:
	 		had += 1

	 	#Get general directions
	 	for x in range(gest.numDirSamples):
	 		angle = gest.sgestures[x][2]
	 		if angle >= 45 and angle < 135:
	 			angles[3] += 1
	 		elif angle >= 135 and angle < 225:
	 			angles[0] += 1
	 		elif angle >= 225 and angle < 315:
	 			angles[2] += 1
	 		else:
	 			angles[1] += 1

	print("%7s|%.2f|%.2f|%2d|%2d|%2d|%2d" %
		(name, total / 30.0, had / 30.0, angles[0], angles[1], angles[2], angles[3]))