from names import shortNames
from gesture import Gesture

class UserSet:

	def __init__(self, id, load=False):
		self.id = id

		if load:
			self.load()

	def load(self):
		self.gestures = { }
		for name in shortNames:
			self.gestures[name] = Gesture("data/" + str(self.id) + "_" + name + ".gest", True)