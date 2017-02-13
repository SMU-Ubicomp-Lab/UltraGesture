# UltraGesture

Gesture detection application.

## UltraGesturePhone Folder

This is the android application built in the Eclipse IDE that collects gesture data.  It's a relatively straigtforwad app that asks a user to preform each gesture in a random order.  While the user is executing a gesture, the phone emits a high-frequency soundwave.  The movement from the user's hand is picked up in the change in the high-frequency tone due to doppler effect.  Samsung Galaxy S5's also have an array of sensors on the top right of the screen that detects hand swipes at any angle.  This data is also used for classifying gestures.

## Data Folder

This is where all of the data files are.  Below is a brief description of the gesture files.

### Sessions

Data here is separated by session and gesture.  For each file, the first number is the session number and the following word is the shorthand notation for the prompted gesture.

Sessions 0-6 were test sessions and are not included.

Sessions 7-35 were done by students.  They were not asked to do multiple sessions.

The remaining sessions were done by adults who were asked to repeat the sessions at most ten times.  Each of the following bullets represents a user with their corresponding session numbers.

* 36, 37, 43, 47, 52, 56, 60, 64, 91, 92
* 38, 39, 42, 46, 51, 55, 59, 63, 67, 70
* 40, 45, 49, 54, 58, 62, 66, 69, 73, 75
* 41, 44, 48, 53, 57, 61, 65, 68, 71, 74
* 72, 76, 77, 78, 79, 80, 81, 82, 83, 84
* 85, 86, 87, 88, 89, 90, 94, 95, 96, 97
* 93, 100, 102, 110, 111, 112, 113, 114, 115, 116
* 98, 99, 101, 103, 104, 105, 106 107, 108, 109

### .gest files

The gesture files were recorded using a Samsung Galaxy S5.  Use the Gesture class in gesture.py to read in this data. Here is the file makeup:

1. Header
  1. revision (byte)
  2. sampleRate (int)
  3. freq1 (int)
  4. freq2 (int)
  5. numSamples (int)
  6. numDirSamples (byte)
  7. duration (long)
2. Audio samples (short * numSamples)
3. Direction samples (3 ints * numDirSamples)
  1. timestamp (int)
  2. velocity (int)
  3. angle (int)
