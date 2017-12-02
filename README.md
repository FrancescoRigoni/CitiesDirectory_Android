**Cities directory (200000 cities)**

This is the product of a coding challenge I did some time ago. A json file is provided which contains
200000 cities. The goal is to create an algorithm to efficiently show the cities in a list while
allowing fast filtering.
Constraints are: do not use databases, do not use external libraries, implement test automation 
for the algorithm.

**Approach**
The idea is to split and store the list of cities in a file system structure, optimized for filtering,
and then iterate over this structure when the user navigates through the cities.

There are lots of duplicate cities in the file, this is on purpose to increase the number of entries.
The FS structure is based on a directory tree, inside these directories, smaller cities.json
are stored, as well as a json file containing the count of cities stored there.
The directories are three levels deep, each level is one of the first characters in the name
of the city which resides there.
So for instance:

Amsterdam       -> a/m/s/cities.json
Amstelveen      -> a/m/s/cities.json
Berlin          -> b/e/r/cities.json

And so on.
The whole structure is around 140 MB big, mostly inodes.
When the list is filtered, the path where to start looking for cities is derived directly from
the filter string. When the user inputs a, the a/ subdirectory will be scanned, and enough
elements to display will be loaded. On scroll more elements will be loaded.
When the user inputs one more character the path restricts to, let's say a/b/.
With this structure the filtering gets faster the more characters there are.
File system unfriendly characters are replaced with an underscore, this causes an issue
when filtering with any such character. It's a known issue that can be solved by using
special sequences for such characters.

After three characters a more fine algorithm is used to locate the cities we want inside the
cities.json files.
This files however are way smaller than the original one and can be processed without issues.
The biggest cities.json holds about 3700 cities.

The tree structure is created at the first run of the app, it takes about 3-5 min on an average
device to generate the whole tree, after that, the loading time of the app is negligible.

**Known issues**
Filtering with some special characters, as mentioned above, can sometimes cause incorrect results.

**Test automation**
The test automation focuses on the data structure, the IndexTree. The UI is quite basic anyway.

