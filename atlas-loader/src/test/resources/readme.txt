+-----------------------------+
| ATLAS-LOADER TEST RESOURCES |
+-----------------------------+

This directory contains files that are used in unit tests for the Atlas Loader.
You should not change them, as this will throw out the accuracy of the unit
tests - they make assumptions about the data contained within these files.

Note that E-GEOD-3790 is parsed by all unit tests and the data integrity
checked.  E-GEOD-3790B is an edited version of E-GEOD-3790 which replaces
"hybridizations" with "assays" to test the assay loading handler, but is
identical in other regards.