# normalizeOneExperiment()
# 	- Reads in a batch of Affymetrix CEL files and produces a normalized data
# 	matrix via robust multichip averaging (RMA).
# ARGUMENTS:
# 	- files <- a vector of CEL file names
# 	- scans <- a vector of corresponding scan names (should have the same length as files)
# 	- outFile <- filename to store normalized data matrix
# 	- mode <- which Bioconductor package to use (affy|oligo)
normalizeOneExperiment <- function(files, outFile, scans, mode) {

	# Use try() to catch errors
	error <- try({

		# Use oligo package for pre-processing
		if(mode == "oligo") {

			# Load oligo
			library(oligo)

			# Read in data from CEL files using read.celfiles().
			# read.celfiles() creates a Biobase object whose class varies
			# depending on the chip type: 
			# 	3'IVT array --> ExpressionFeatureSet
			# 	Gene ST array --> GeneFeatureSet
			# 	Exon ST array --> ExonFeatureSet
			# 	(SNP 5.0 or 6.0 array (use clrmm pkg) --> SnpCnvFeatureSet)
			print("Reading in CEL files")
			featureSet <- read.celfiles(files)
			print("Running rma")

			# Run RMA using rma().
			#
			# Robust Multipchip Averaging (RMA):
			# 	1) Model-based background adjustment:
			#       - remove noise signature from data, just leave signal.
			#       - any 'mismatch' probes are ignored.
			# 	2) Quantile normalization:
			# 		- Identifies a target distribution and then coerces the
			# 		intensity data from each array towards that distribution.
			# 		This means that data from all arrays in the dataset are now
			# 		comparable.
			# 	3) Summarization:
			# 		- We have intensities for individual probes; this step
			# 		takes these intensities and summarizes them per probe set
			# 		so that we get a single value for each one.
			# 		- For 3'IVT arrays, a probe set =~ a gene/transcript.
			# 		- For ST arrays, a probe set =~ an exon.
			#
			# For ST arrays (ExonFeatureSet or GeneFeatureSet), the
			# summarization target argument is needed:
			# 	- target="core" obtains gene-level summaries of expression for
			# 	"well-annotated" loci (annotated by RefSeq). 
			# 	- Exon (not Gene) arrays also have probes for "predicted genes"
			# 	and other less well-described loci. These can be accessed with
			# 	target="extended" (mRNA evidence) or target="full"
			# 	(bioinformatic prediction only).
			# 	- To summarize at the exon level, e.g. to look at expressions
			# 	for alternative splice forms, use target="probeset".
			# 	- For now we want to display information for genes, we are not
			# 	going as far as individual exons, so we'll use target="core".
			#
			# For non-ST arrays (ExpressionFeatureSet or SnpCnvFeatureSet), do
			# not specify target (dies if you do).
			#
			# rma() creates an ExpressionSet object which we'll call eSet.
			
			# If the first element of the vector returned by class() matches
			# "Gene" or "Exon", we've got an ST array...
			if( grepl("Gene", class(featureSet)[1]) || grepl("Exon", class(featureSet[1]) )) {
				eSet <- rma(featureSet, target="core")
			} 
			# ...otherwise, we haven't.
			else {
				eSet <- rma(featureSet) # no target
			}
			print("rma finished")
		} 
		

		# Use affy package for pre-processing
		else if(mode == "affy") {

			# Load affy
			library(affy)

			# Read in CEL files and normalize using justRMA().
			print("Running justRMA")
			eSet <- justRMA(filenames = files, celfile.path = NULL)
			print("justRMA finished")
		}
	

		# Now the pre-processing is done, either with oligo or affy, and the
		# data is stored in the eSet object. 
	
		# Next, make sure the scan names in the vector 'scans' are in the same
		# order as their corresponding samples in eSet.
				
		# relSort()
		# 	- Function to sort the scan names (in scans) so they are in the correct
		# 	order for the data in eSet.
		# ARGUMENTS:
		# 	- toSort <- a vector of things to reorder (scan names).
		# 	- keys <- a vector of CEL filenames.
		# 	- sortedKeys <- a vector of the same CEL filenames in the order they
		# 	are in the eSet object.
		relSort <- function(toSort, keys, sortedKeys) {

			# Copy contents of toSort to a new vector called sorted
			sorted <- toSort

			# For i in 1:length(sortedKeys):
			# 	- Find the position index of the CEL filename in the vector
			# 	'keys' that matches the CEL filename at position 'i' in the
			# 	vector 'sortedKeys';
			#	- Get the scan name at that position in 'toSort' and put it at
			#	position 'i' in the 'sorted' vector.
			for (i in 1:length(sortedKeys)) sorted[i] <- toSort[which(keys==sortedKeys[i])]
			
			# Return the reordered scan names
			return(sorted)
		}

		# shortFileNames is vector of CEL filenames with /path/to/files/ stripped off (i.e. just base names)
		shortFileNames <- gsub(".+/", "", files)
		
		# Create a vector of scan names in the correct order with relSort()
		scansSorted <- relSort(scans, shortFileNames, sampleNames(eSet))
		

		# Write the normalized data matrix in eSet to a tab-delimited textfile
		# (filename in 'outFile').
		
		# First make some headers for the columns.
		
		# How many columns do we need
		len <- length(sampleNames(eSet))
		
		# The scan names after sorting
		write(c("Scan REF", scansSorted), outFile, ncolumns = len+1, sep = "\t")
		
		# Not sure why we need this row. Repeats "GEO:AFFYMETRIX_VALUE" for
		# each column. Don't know what happens to this textfile between its
		# creation here and analytics.R -- maybe it's needed then or something
		# (or does analytics.R expect it? can't remember).
		write(c("Composite Element REF", rep("GEO:AFFYMETRIX_VALUE", len)), outFile, ncolumns = len+1, sep="\t", append=TRUE)


		# This line was commented out.
		# Seems that it adds "Affymetrix:CompositeSequence:HG-U133_Plus_2:" to
		# the beginning of each probe set ID in the eSet.
		# Leaving in for now, but not sure that we need to keep it?
		#featureNames(eSet) = paste("Affymetrix:CompositeSequence:HG-U133_Plus_2:", featureNames(eSet), sep = "")


		# Now write the normalized and summarized expression values (accessed
		# via 'exprs(eSet)') to the outFile. The left-most column is the
		# probe set IDs. Leave out the column names (col.names=FALSE) as we
		# already put some in above.
		write.table(exprs(eSet), file = outFile, sep = "\t", quote = FALSE, row.names = TRUE, col.names = FALSE, append = TRUE)
	
	})

	# error catching -- return the error message if there was one (class(error)
	# returns "try-error"), otherwise return NULL.
	if ("try-error" == class(error)) {
		return(error)
	} else {
		return(NULL)
	}
}
