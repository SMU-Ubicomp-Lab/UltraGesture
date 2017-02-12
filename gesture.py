import numpy as np
import os
from matplotlib import pyplot as plt
from math import sqrt

class Gesture:

	def __init__(self, filename, load=True):
		self.file = open(filename, "r")
		if load:
			self.load()

	def load(self):
		#create the datatypes
		longdtype  = np.dtype('>i8')
		intdtype   = np.dtype('>i4')
		shortdtype = np.dtype('>i2')
		bytedtype  = np.dtype('>i1')

		#load header
		self.revision = np.fromfile(self.file, dtype=bytedtype, count=1)[0]
		self.sampleRate = np.fromfile(self.file, dtype=intdtype, count=1)[0]
		self.freq1 = np.fromfile(self.file, dtype=intdtype, count=1)[0]
		self.freq2 = np.fromfile(self.file, dtype=intdtype, count=1)[0]
		self.numSamples = np.fromfile(self.file, dtype=intdtype, count=1)[0]
		self.numDirSamples = np.fromfile(self.file, dtype=bytedtype, count=1)[0]
		self.duration = np.fromfile(self.file, dtype=longdtype, count=1)[0]

		#get data
		self.rawData = np.fromfile(self.file, dtype=shortdtype, count=self.numSamples) / 32767.0

		#get footer data (gestures)
		gesturedtype = np.dtype(">i4, >i4, >i4")
		self.sgestures = np.fromfile(self.file, dtype=gesturedtype)


		self.dirs = np.zeros(4)
		for x in range(self.numDirSamples):
			angle = self.sgestures[x][2]
			if angle >= 45 and angle < 135:
				self.dirs[3] += 1.0
			elif angle >= 135 and angle < 225:
				self.dirs[0] += 1.0
			elif angle >= 225 and angle < 315:
				self.dirs[2] += 1.0
			else:
				self.dirs[1] += 1.0

		self.loaded = True

	def printData(self):
		if self.loaded:
			print("Header Data")
			print("Revision:    v%d" % (self.revision))
			print("Sample rate: %d hz" % (self.sampleRate))
			print("Frequency 1: %d hz" % (self.freq1))
			print("Frequency 2: %d hz" % (self.freq2))
			print("Num Samples: %d" % (self.numSamples))
			print("Dir Samples: %d" % (self.numDirSamples))
			print("Duration:    %d ns" % (self.duration))
			print("Gestures (%d)" % (self.numDirSamples))
			for i in range(self.numDirSamples):
				print("G%d" % (i))
				print("\tIndex: %d" % (self.sgestures[i][0]))
				print("\tSpeed: %d" % (self.sgestures[i][1]))
				print("\tAngle: %d" % (self.sgestures[i][2]))
		else:
			print("Not loaded yet")

	def plotSpecgram(self, NFFT=4096, noverlap=2048):
		plt.figure(num=self.file.name)
		plt.specgram(self.rawData, NFFT=NFFT, noverlap=noverlap)
		#plt.figure()
		#plt.imshow(spec[tbin-20:tbin+20, :],extent=[0,1,0,1])

		

	def generateSpecgram(self, NFFT=4096, noverlap=2048):
		Pxx, freqs, bins, im = plt.specgram(self.rawData, NFFT=NFFT, noverlap=noverlap)
		return 20 * np.log(Pxx)

	def getFeatureSet(self):
		#get spec data
		spec = self.generateSpecgram(noverlap=3072)
		freqs = np.fft.fftfreq(4096)
		tbin = int(round(4096.0 * self.freq1 / self.sampleRate))
		brange = 4

		#get mags
		lowMags = np.max(spec[tbin-brange*2:tbin-brange, :], axis=0) / np.max(spec[tbin-brange:tbin+brange, :], axis=0)
		hiMags = np.max(spec[tbin+brange:tbin+2*brange, :], axis=0) / np.max(spec[tbin-brange:tbin+brange, :], axis=0)

		#zero mean mags
		lowMags = lowMags - np.average(lowMags);
		hiMags = hiMags - np.average(hiMags);

		#plt.figure()
		#plt.plot(range(spec.shape[1]-2), lowMags[2:], range(spec.shape[1]-2), hiMags[2:])

		bot = len(lowMags) - 40
		
		return np.concatenate((lowMags[bot:], hiMags[bot:], self.dirs))





