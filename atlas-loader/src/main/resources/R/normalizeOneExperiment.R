# files <- a list on CEL files names
# scans <- a list of corresponding scan names (should have the same length)
# outFile <- a name of file to store normalized data
# parallel <- run a parlallelized version of normalization script (requires affyParaEBI R package)

normalizeOneExperiment <- function(files, outFile, scans, parallel = FALSE) {
  library(affy)

  ae <- ReadAffy(filenames = files)

  es <- NULL
  if (parallel) {
    library(affyParaEBI)
    numClusters <- 10 #Change this if you have few assays than clusters or if you want to speed up the process
    cluster <- makeCluster(numClusters, type = "RCLOUD")
    numClusters <- length(cluster)
    cluster <- clusterOptimization(cluster, substitute = TRUE) #this will replace possible low performance clusters 
    es <- preproParaEBI(path = filesPath, cluster = cluster)       #this preforms the preprocessing
    dim(exprs(es))
    mean(exprs(es))
  } else {
    es <- expresso(ae,
          bgcorrect.method="rma",#TODO: Check CDF files, affymetrix ones are possibly outdated
          normalize.method = "quantiles.robust",
          pmcorrect.method = "pmonly", summary.method = "medianpolish")
  }

  len <- length(sampleNames(es))
  write(c("Scan REF", scans), outFile, ncolumns = len+1, sep = "\t")
  write(c("Composite Element REF", rep("GEO:AFFYMETRIX_VALUE", len)), outFile, ncolumns = len+1, sep="\t", append=TRUE)
  featureNames(es) = paste("Affymetrix:CompositeSequence:HG-U133_Plus_2:", featureNames(es), sep = "")
  write.table(exprs(es), file = outFile, sep = "\t", quote = FALSE, row.names = TRUE, col.names = FALSE, append = TRUE)
}
