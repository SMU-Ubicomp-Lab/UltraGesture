import numpy as np
import os
from names import *
from datalist import DataList
from gesture import Gesture
from userset import UserSet
from matplotlib import pyplot as plt
from sklearn.svm import SVC
import pickle

#get the data list
dl = DataList()

print "gathering data"
train = np.empty(shape=(110 * len(shortNames), 84))
correct = np.empty(110 * len(shortNames))
nextInd = 0
for n in range(len(shortNames)):
	#if n == 11 or n == 12:
	#	continue

	for i in range(37, 117):
		#discard the no good data
		#if not dl.isGood(n, i):
		#	continue

		#get the feature set from the data
		gest = Gesture("data/" + str(i) + "_" + shortNames[n] + ".gest", True)
		train[nextInd] = gest.getFeatureSet()
		correct[nextInd] = n

		#increment index
		nextInd += 1

print "training"
clf = SVC()
clf.fit(train[:nextInd], correct[:nextInd])

#save the data
pickle.dump(train[:nextInd], open("train.p", "wb"))
pickle.dump(correct[:nextInd], open("correct.p", "wb"))
#pickle.dump(clf, open("clf.p", "wb"))

print "predicting"

#overall count
overallGood = 0
overallTotal = 0

#confusion matrix
conf = np.zeros(shape=(len(shortNames), len(shortNames)))

for n in range(len(shortNames)):
	#if n == 11 or n == 12:
	#	continue

	good = 0
	for i in range(37, 117):
		#skip bad data
		#if not dl.isGood(n, i):
		#	continue

		#Get gesture data and predict
		gest = Gesture("data/" + str(i) + "_" + shortNames[n] + ".gest", True)
		guess = int(clf.predict(gest.getFeatureSet())[0])

		#increment conf matrix
		conf[n][guess] += 1

		#increment good guess if correct
		if guess == n:
			good += 1

	#get the total good samples
	total = 110

	#increment overall
	overallGood += good
	overallTotal += total

	#print this gesture's accuracy
	print("%15s: %.2f (%2d/%2d)" % (longNames[n], float(good)/total, good, total))

#print overall
print("Overall: %.2f (%2d/%2d)" % (float(overallGood)/overallTotal, overallGood, overallTotal))

#print confusion matrix
for row in range(len(shortNames)):
	print("%7s" % (shortNames[row])),
	for col in range(len(shortNames)):
		print("%2d" % (conf[row][col])),
	print ""