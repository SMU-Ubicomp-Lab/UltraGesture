import json
import os
from names import shortNames
from gesture import Gesture
from matplotlib import pyplot as plt

class DataList:

	def __init__(self):
		if os.path.isfile("datalist.json"):
			self.jsonData = json.load(open("datalist.json", "rb"))
		else:
			self.jsonData = { }
			for i in range(len(shortNames)):
				self.jsonData[i] = []

	def commit(self):
		json.dump(self.jsonData, open("datalist.json", "wb"), sort_keys=True, indent=4, separators=(',', ': '))

	def add(self, gestType, subject):
		if self.isGood(gestType, subject):
			return
		self.jsonData[str(gestType)].append(subject)

	def remove(self, gestType, subject):
		if self.isGood(gestType, subject):
			self.jsonData[str(gestType)].remove(subject)

	def isGood(self, gestType, subject):
		return subject in self.jsonData[str(gestType)]

	def numGood(self, gestType):
		return len(self.jsonData[str(gestType)])
