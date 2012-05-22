# files <- a list on CEL files names
# scans <- a list of corresponding scan names (should have the same length)
# outFile <- a name of file to store normalized data
# parallel <- run a parlallelized version of normalization script (requires affyParaEBI R package)
# mode <- affy or oligo
normalizeOneExperiment <- function(files, outFile, scans, mode) {
	  error <- try({
	        if(mode == "oligo") {

	                # load oligo
	                library(oligo)

	                # Call all oligo functions explicitly with "oligo::function" -- some
	                # seem to get masked by other packages (affy and limma). Probably
	                # doesn't matter if just running one expt at a time.

	                # Read in data from cel files. oligo doesn't seem to have an equivalent of
	                # affy's justRMA which will read in cel files and do the normalization.
	                print("Reading in CEL files")
	                featureSet <- oligo::read.celfiles(files)

	                # run RMA using oligo::rma function.
	                print("Running rma")

	                # For ST arrays (ExonFeatureSet or GeneFeatureSet), the target argument is needed.
	                # target="core" obtains gene-level summaries of expression -- and for
	                # now we want to display information for genes, we are not going as far as
	                # individual exons.
	                # For non-ST arrays (ExpressionFeatureSet or SnpCnvFeatureSet), do not
	                # specify target (dies if you do).
	                if( grepl("Gene", class(featureSet)[1]) || grepl("Exon", class(featureSet[1]) )) {
	                        es <- oligo::rma(featureSet, target="core")
	                } else {
	                        es <- oligo::rma(featureSet)    # non-ST arrays, no target
	                }
	                print("rma finished")

	        } else if(mode == "affy") {

	                # load affy
	                library(affy)

	                # read in cel files and normalize using justRMA
	                print("Running justRMA")
	                es <- justRMA(filenames = files, celfile.path = NULL)
	                print("justRMA finished")
	        }

	        # function to sort the source names (in scans) so they are in the correct
	        # order for the data in es
	       relSort <- function(toSort, keys, sortedKeys) {
	          sorted <- toSort
	          for (i in 1:length(sortedKeys)) sorted[i] <- toSort[which(keys==sortedKeys[i])]
	          return(sorted)
	       }

	    len <- length(sampleNames(es))
	    shortFileNames <- gsub(".+/", "", files)
	    scansSorted <- relSort(scans, shortFileNames, sampleNames(es))
	    write(c("Scan REF", scansSorted), outFile, ncolumns = len+1, sep = "\t")
	    write(c("Composite Element REF", rep("GEO:AFFYMETRIX_VALUE", len)), outFile, ncolumns = len+1, sep="\t", append=TRUE)
	    #featureNames(es) = paste("Affymetrix:CompositeSequence:HG-U133_Plus_2:", featureNames(es), sep = "")
	    write.table(exprs(es), file = outFile, sep = "\t", quote = FALSE, row.names = TRUE, col.names = FALSE, append = TRUE)
	  })
  if ("try-error" == class(error)) {
    return(error)
  } else {
    return(NULL)
  }
}
