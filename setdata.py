import os
from datalist import DataList
from names import shortNames
from gesture import Gesture
from matplotlib import pyplot as plt

name = "dtap"
gest = Gesture("data/40_"+name+".gest")
gest.getFeatureSet()
plt.show()

# dl = DataList()

# name = "waveout"

# for i in range(7, 17):
# 	gest = Gesture("data/"+str(i)+"_"+name+".gest")
# 	gest.plotSpecgram()
# 	print str(i) + ":" + str(gest.getFeatureSet()[80:])

# plt.show()

# for i in range(17, 27):
# 	gest = Gesture("data/"+str(i)+"_"+name+".gest")
# 	gest.plotSpecgram()
# 	print str(i) + ":" + str(gest.getFeatureSet()[80:])

# plt.show()

# for i in range(27, 37):
# 	gest = Gesture("data/"+str(i)+"_"+name+".gest")
# 	gest.plotSpecgram()
# 	print str(i) + ":" + str(gest.getFeatureSet()[80:])

# plt.show()