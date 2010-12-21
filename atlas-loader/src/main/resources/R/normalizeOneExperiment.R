# files <- a list on CEL files names
# scans <- a list of corresponding scan names (should have the same length)
# outFile <- a name of file to store normalized data
# parallel <- run a parlallelized version of normalization script (requires affyParaEBI R package)

normalizeOneExperiment <- function(files, outFile, scans, parallel = FALSE) {
  library(affy)

  es <- NULL
  if (parallel) {
    ae <- ReadAffy(filenames = files)

    library(affyParaEBI)
    numClusters <- 10 #Change this if you have few assays than clusters or if you want to speed up the process
    cluster <- makeCluster(numClusters, type = "RCLOUD")
    numClusters <- length(cluster)
    cluster <- clusterOptimization(cluster, substitute = TRUE) #this will replace possible low performance clusters 
    es <- preproParaEBI(path = filesPath, cluster = cluster)       #this preforms the preprocessing
    dim(exprs(es))
    mean(exprs(es))
  } else {
    print("Running justRMA")
    es <- justRMA(filenames = files, celfile.path = NULL)
    print("justRMA finished")
  }

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
}
