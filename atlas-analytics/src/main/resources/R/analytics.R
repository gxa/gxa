### Atlas Analytics Original Driver Functions
### Author: ostolop


(function() {

# read.atlas.nc()
# 	- Reads data from Atlas NetCDF file and builds a Biobase ExpressionSet
# 	object from it.
# ARGUMENTS:
# 	- filename <- the name of the data NetCDF file
#
# <<- means global assignment
read.atlas.nc <<- function (filename) {	
    
	# Load some packages
	# 	- ncdf for reading NetCDFs
	#	- Biobase so we can create the ExpressionSet
	library(ncdf)
	library(Biobase)

	library(gtools)

	# Read from the data NetCDF file and store in netCDF object
	netCDF = open.ncdf(filename)


	# Get some things out of the NetCDF object.

	# In data NetCDF under "ASacc" we have accessions/names from the "Scan Name"
	# column in the SDRF.
	assayScanNames = as.vector(get.var.ncdf(netCDF, "ASacc"))
	
	# designElements -- vector of the design element (a.k.a probe set names),
	# under "DEacc" in data NetCDF.
	designElements = as.vector(get.var.ncdf(netCDF, "DEacc"))

	# normalizedExpressions -- the matrix of normalized summarized expressions
	# output by RMA, which is stored under "BDC" in the data NetCDF.
	normalizedExpressions = fixMatrix(get.var.ncdf(netCDF, "BDC"), nRows = length(assayScanNames), nCols = length(designElements))

	# See if we've got factors -- these are under "EF" in the data NetCDF.
	if ("EF" %in% names(netCDF$dim)) {
		
		# factorNames -- vector of experimental factor names. These are in the
		# IDF under 'Experimental Factor Type', under "EF" in the data NetCDF
		# and under "propertyName" in the statistics NetCDF.
		factorNames = get.var.ncdf(netCDF, "EF")

		# factorValues -- matrix of factor values, from "FactorValue[]" columns
		# in SDRF, stored under "EFV" in data NetCDF and "propertyVALUE" in
		# stats NetCDF.
		# Each column corresponds to an experimental factor; each row to an
		# assay.
		factorValues = get.var.ncdf(netCDF, "EFV")
		
		# Name columns and rows with factor names and assay names
		colnames(factorValues) = factorNames
		rownames(factorValues) = assayScanNames

		# Make it into a data frame. This is in prep for addition to the
		# ExpressionSet object, to which attributes are added as Biobase
		# AnnotatedDataFrame objects.
		factorValues = data.frame(factorValues)

		# Log numbers of factors and factor values.
		print(paste("Read in EFV:", nrow(factorValues), "x", ncol(factorValues)))
	}

	
	# geneIDs -- a vector of internal Atlas gene IDs, stored under "GN" in
	# NetCDF.
	geneIDs = get.var.ncdf(netCDF, "GN")

	# exptAccession is experiment accession, e.g. 'E-MEXP-3392'.
	exptAccession = att.get.ncdf(netCDF,varid = 0,"experiment_accession")$value

	# Close the NetCDF now we don't need it.
	close.ncdf(netCDF)
	
	
	# Replace placeholders for missing expression values in NetCDF with 'NA'.
	normalizedExpressions = replaceMissingValues(normalizedExpressions)

	# The normalized data matrix is stored in the NetCDF with each row
	# corresponding to an assay, and each column to a design element. For
	# addition to the eSet and further analysis with BioConductor packages, we
	# need it to be the other way round, so here we transpose it.
	# If there's only one assay (unlikely), I think the behaviour is that R
	# decides 'normalizedExpressions' should be a vector instead of matrices.
	# We need it to be a matrix so we fix it here. Specifying number of rows
	# equal to the number of elements in the vector (i.e.
	# length(designElements)) will produce a matrix with a single column.
	if (length(assayScanNames) == 1) {
		normalizedExpressions = matrix(normalizedExpressions, nrow = length(designElements))
	} else {
		normalizedExpressions = t(normalizedExpressions)
	}

	# Set row and column names for the normalized data matrix.
	rownames(normalizedExpressions) = designElements
	colnames(normalizedExpressions) = assayScanNames

	# Log which experiment we're looking at and how big the normalized data
	# matrix is.
	print(paste("Read in", exptAccession))
	print(paste("Read in normalized data matrix:", nrow(normalizedExpressions), "x", ncol(normalizedExpressions)))


	# Create factorsForEset data frame. If we've got a data frame of factors
	# already, use this. If not, make an empty one with row names from the
	# assay names.
	if (exists("factorValues")) {
		factorsForEset <- factorValues
	} else {
		factorsForEset <- data.frame(row.names=assayScanNames)
	}
	

	# Create some Biobase AnnotatedDataFrame objects for addition to the ExpressionSet.
	
	# fDataFrame is a mapping between the gene IDs and design element (probe set) names.
	fDataFrame = data.frame(geneIDs = geneIDs, designElements = designElements)
	fData = new("AnnotatedDataFrame", data = fDataFrame)
	# Set feature names to the design element names
	featureNames(fData) = designElements

	# pData contains the factor info.
	pData = new("AnnotatedDataFrame", data = factorsForEset)


	# Create a Biobase AssayData object containing the normalized summarized
	# expression values (from normalized data matrix).
	# lockedEnvironment storage mode ensures data integrity: in R environments
	# are 'pass-by-reference'; using lockedEnvironment prevents modification of
	# values in this object if a copy of it is modified.
	aData <- assayDataNew(storage.mode = "lockedEnvironment", exprs = normalizedExpressions)
	# Set the feature names and sample names
	featureNames(aData) <- designElements
	sampleNames(aData) <- assayScanNames
	
	# Log what factors we have and how many. Factor names are accessed via
	# varLabels() function from the pData object.
	print(paste("Computed phenoData for", paste(varLabels(pData),collapse = ", "), nrow(pData), "x", ncol(pData)))
	
	# Create and return a Biobase ExpressionSet object containing the
	# normalized summarized expressions, factors, design element accessions and
	# gene IDs.
	return(new("ExpressionSet", assayData = aData, phenoData = pData, featureData = fData))
}





# log2.safe()
#	- Log function that does not return NAs
# ARGUMENTS:
# 	- x <- matrix of normalized summarized expressions
log2.safe <- function(x) {
	
	# Assumption is, expression levels are > 0 unless someone has _already_ applied log.
	# If the levels are < 1000, we're fine without log: moreover, that may as well mean
	# all the expression levels were > 1 (why not?), and someone has already applied log.
	# If the values are more than 1000, then log definitely was not used before, as 2^1000
	# is just way too much for expression level value.
	if (min(x, na.rm = TRUE) < 0 || max(x, na.rm = TRUE) < 1000) { return(x) }

	# Otherwise, log2 the normalized values
	print("Taking log2 of the expression matrix")

	# For most values, 1e-10 is far smaller than the precision expected, so we can add it and be (almost) sure
	# we've got rid of zeros without disturbing the stats.
	
	# Make a new matrix with the log2'ed values in
	tmp = log2(x + 1e-10)

	# Replace any non-finite values produced during the log2() with 0.
	# Keep NAs that were already present in normalized data matrix.
	tmp[!(is.finite(tmp) | is.na(x))] <- 0

	# return tmp
	tmp
}




# isEmptyEFV()
#	- function to check if factor value is empty.
#	- returns TRUE if value is NULL.
isEmptyEFV <- function(value) return (is.null(value))




# fstat.eset() 
#	- Omnibus one-way ANOVA (with moderated t) F-statistic computation.
# ARGUMENTS:
# 	- eset <- an ExpressionSet object containing normalized summarized
# 	expression data and factor data.
# 	- factorName <- a factor name.
fstat.eset <- function(eset, factorName) {
    
	# Log which factor we're looking at.
	print(paste("Calculating design matrix for", factorName))

	# Create the experiment design matrix for this factor. 
	# Each row in the design matrix corresponds to an array in the experiment;
	# each column corresponds to a factor value.
	# 
	# Here is an example SDRF:
	# 		Array	FactorValue[compound]
	#		array1	a
	#		array2	a
	#		array3	b
	#		array4	b
	# 
	# Our coefficients are 'a' and 'b'.
	# Here is the corresponding design matrix:
	# 		Array	a	b
	# 		array1	1	0
	# 		array2	1	0
	# 		array3	0	1
	# 		array4	0	1
	#
	# The design matrix is created from the data in the ExpressionSet for the
	# factor in question by model.matrix().
	# as.formula() returns the string created by paste() as a formula object.
	# The "0+" part removes the intercept column so that we get a column for
	# each factor value instead of intercept plus all but one factor value. The
	# "0+" part can also be written as "-1+", there is no difference to the
	# resulting design matrix.
	# (model.matrix() doesn't work on a string).
    designMatrix = model.matrix(as.formula(paste("~0+", factorName)), data = eset)

    print("Fitting model...")

	# Run lmFit() on the data in eset using the design matrix.
	# lmFit() fits a linear model for each probe set on the array:
	#
	# 	Y_{i} = b_{0} + b_{1}X_{i}
	#
	# In an example comparing 'normal' samples with 'tumor' samples:
	# 	- Assuming X{i} = 0 for 'normal' samples: 
	# 		Y_{N} = b_{0}
	# 	- Assuming X{i} = 1 for 'tumor' samples:
	# 		Y_{T} = b_{0} + b_{1}
	# 	- The coefficient b_{0} is the average log-intensity for 'normal' samples;
	# 	- The coefficient b_{1} is the change in average log-intensity
	# 	associated with 'tumor' samples; i.e. the gradient of the line when you
	# 	go from 'normal' log-intensity to 'tumor' log-intensity. If we want to
	# 	see if there is a significant difference between 'normal' and 'tumor'
	# 	expression, we are essentially going to ask if b_{1} is significantly
	# 	different from 0. Is it just a flat line or is there a gradient?
	# 
	# This line is fit for every design element summarized on the array. For
	# some genes the line's gradient will be positive, i.e. an increase in
	# log-intensity i.e. expression. For other genes the line's gradient will
	# be negative i.e. a decrease in log-intensity i.e. expression.
	# The results are stored in 'fit', a limma MArrayLM object. 
    fit = lmFit(eset, designMatrix)

	# Compute moderated t-test using eBayes().
	# This is a robust version of the t-test which reduces the false discovery
	# rate. Standard errors are moderated across genes (shrunk towards a common
	# value). A gene that has a large variance is less likely to actually be
	# differentially expressed. 
    fit = eBayes(fit)

    return(fit)
}




# allupdn()
#	- Moderated t one-way ANOVA post-hoc testing with global FDR adjustment.
# ARGUMENTS:
# 	- eset <- an ExpressionSet object.
# 	- factorNames <- vector of factor names from eset (renamed from 'evars').
allupdn <- function (eset, factorNames = varLabels(eset) ) {
	
	# Load limma.
    library(limma)

	# Get the normalized summarized expressions and log2 them if they need it
	# with log2.safe() function.
    exprs = exprs(eset)
    exprs(eset) = log2.safe(exprs)

	# Create a new list called allFits. We'll put each set of linear model fits
	# for each factor in here.
	allFits = list()

	# Fit linear model for each factor
	for(fName in factorNames){

		try({

			print(paste("Calculating lmFit and F-stats for", fName))
			
			# Find out the samples in eset for which this factor is not
			# NULL. Using isEmptyEFV() function.
			nonEmptyFactorValues = which(!sapply(eset[[fName, exact = TRUE]], isEmptyEFV))
			
			# Take a subset of eset containing only the samples with non-NULL
			# factor values.
			esetForVariable = eset[, nonEmptyFactorValues]
			
			# Encode this factor as a 'factor' object. Now the factor values
			# are 'levels'.
			esetForVariable[[fName, exact = TRUE]] = factor(esetForVariable[[fName, exact = TRUE]])


			# In Kapushesky et al. 2010
			# (http://nar.oxfordjournals.org/content/38/suppl_1/D690.full), it
			# is stated that "each EFV should be tested in at least two
			# replicates". Benilton Carvalho (biostatistician and contributor
			# to BioConductor) recommends at least four replicates per factor.
			# We will go with a minimum of 3 replicates per factor value for
			# now.

			# Vector of factor values
			factorValues = levels(esetForVariable[[fName, exact = TRUE]])
			
			# Set this to true if any factor values have less than 3 replicates.
			notEnoughReps = FALSE
			
			# For each factor value, test if the number of samples with that
			# factor value is less than 3. If so, log that it has too few
			# replcates and set notEnoughReps to TRUE. We can only proceed with
			# this factor as long as ALL values have >=3 replicates, so even if
			# just one value has <3 we have to leave it.
			for(fValue in factorValues) {
				
				if(length(which(pData(esetForVariable)[[fName, exact = TRUE]] == fValue)) < 3) {
					
					print(paste(fName, fValue, "has too few replicates"))
					notEnoughReps = TRUE
				}
			}
			
			# Count the number of levels i.e. distinct factor values.
			numVariableFactorLevels <- nlevels(esetForVariable[[fName, exact=TRUE]])
			
			# If (a) there aren't >=3 replicates for every factor value, or (b)
			# there are <2 factor values, log that we can't calculate
			# statistics and go to the next factor.
			if (notEnoughReps || numVariableFactorLevels < 2) {
				
				print("Can't compute statistics for poorly conditioned data: too few or too many factor levels.")
				
				# Break out and go to the next factor in the eset without
				# calculating statistics.
				next
			}


			# Fit the linear model for this factor and calculate moderated
			# t-statistics using limma.
			thisFit = fstat.eset(esetForVariable, factorName = fName)
			
			# Create the contrasts matrix. This is a matrix describing the
			# comparisons (contrasts) we want to make between the samples.
			# E.g., is gene x significantly differentially expressed in
			# 'normal' samples when compared with 'cancer' samples?

			# How many columns does the design matrix have? This is the number
			# of levels/factor values.
			n = ncol(thisFit$design)
			
			# The contrasts matrix is a square identity matrix (returned by
			# diag()) of size 'n', with 1/n subtracted.
			# So for a factor with three values, we get:
			#
            #            [,1]       [,2]       [,3]
			# [1,]  0.6666667 -0.3333333 -0.3333333
			# [2,] -0.3333333  0.6666667 -0.3333333
			# [3,] -0.3333333 -0.3333333  0.6666667
			# 
			# This a complete pairwise-comparisons contrast matrix (equivalent
			# to one-way Analysis of Variance) for an omnibus test of
			# differential expression across all factor values in the selected
			# factor. With this matrix we can compute t-statistics and
			# p-values to enable us to accept or reject the null hypothesis of
			# ‘equal means for all groups’. 
			# This essentially means we will ask for each gene, is the mean
			# expression level in each factor value significantly different
			# from its overall mean expression across all factor values (cf.
			# Kapushesky et al. (2010) doi: 10.1093/nar/gkp936).
			contrastsMatrix = diag(n) - 1/n

			# Now we apply the contrasts matrix to the linear model fits using
			# contrasts.fit, to ask the questions it represents. I.e., compute
			# estimated coefficients and standard errors for the given set of
			# contrasts.
			contr.fit = contrasts.fit(thisFit, contrastsMatrix)
			
			# Calculate moderated t-statistics with eBayes(), using estimated
			# coefficients and standard errors in contr.fit.
			# This is a robust version of the t-test which reduces the false discovery
			# rate. Standard errors are moderated across genes (shrunk towards a common
			# value). A gene that has a large variance is less likely to actually be
			# differentially expressed. The moderated t-statistic is also known
			# as the B-statistic.
			contr.fit = eBayes(contr.fit)
			
			
			# Add the vector of factor values to thisFit
			thisFit$factorValues = factorValues
		

			# Add the contrasts fit object to thisFit as well.
			thisFit$contr.fit = contr.fit
			
			# Add thisFit to out allFits list, under the name of the factor
			# that we're looking at.
			allFits[[fName]] = thisFit
			print("Done.")
		})
    }
	
	# Return the allFits list. This contains, for each factor, all the data
	# from the linear model fit, the results of applying the contrasts matrix,
	# with the moderated t-statistics and associated p-values.
    allFits
}





# computeAnalytics()
# 	- Atlas analytics.
# 	- Calls allupdn() to fit linear models and contrasts for all factors.
# 	- Writes t-statistics and p-values to statistics NetCDF.
# 	- Calls updateStatOrder() to rank genes based on differential expression.
# 	- Returns a vector of the result for each factor ("OK" for those where
# 	statistics were calculated successfully, "NOK" for those where they
# 	weren't).
# ARGUMENTS:
# 	- data_ncdf <- filename of NetCDF with experiment data.
# 	- statistics_ncdf <- filename of NetCDF with statistics data (calculated here).
computeAnalytics <<- function (data_ncdf, statistics_ncdf) {
	
	e <- try({
		
		# eset is an ExpressionSet object containing normalized summarized
		# expressions and experimental data such as factors and their values.
		eset = read.atlas.nc(data_ncdf)
		
		# data_nc is an ncdf object with experiment data.
		data_nc = open.ncdf(data_ncdf)
		# statistics_nc is an ncdf object for statistics data to be written to.
		# The statistics NetCDF file is created just before analytics are
		# calcluated.
		statistics_nc = open.ncdf(statistics_ncdf, write = TRUE)
		
		# How many samples are there? If there is only one, we can't do
		# anything sensible with this experiment so return here without going
		# any further.
		if (dim(eset)[2] == 1) {
			
			# Return a vector of the factor names as 'names' and "NOK" as the
			# value for each.
			return(sapply(varLabels(eset), function(i) "NOK"))
		}
		
		# Process the experiment data using allupdn() function.
		# This returns is a list object where each element is a limma MArrayLM
		# object containing (for a single factor) the linear model fit,
		# estimated coefficients and standard errors for the contrasts,
		# moderated t-statistics and associated p-values.
		allFits = allupdn(eset)
		
		# Get propertyNAME (factor name) and propertyVALUE (factor value) information from the NetCDF.
		propertyNAME = get.var.ncdf(statistics_nc, "propertyNAME")
		propertyVALUE = get.var.ncdf(statistics_nc, "propertyVALUE")

		# Stick them together with paste :)
		# pairs is a vector with strings "factor name||factor value" for every
		# factor name/value pair in the experiment.
		pairs <- paste(propertyNAME, propertyVALUE, sep = "||")

		# Create two matrices called tstat and pval and fill with 'NA's. The
		# number of rows is equal to the number of design elements/probe sets;
		# the number of columns is equal to the number of factor name/value pairs.
		tstat = matrix(NA, ncol = length(propertyNAME), nrow = nrow(eset));
		pval = matrix(NA, ncol = length(propertyNAME), nrow = nrow(eset));
		
		# Name columns with the factor name/value pairs.
		colnames(tstat) <- make.names(pairs)
		colnames(pval) <- make.names(pairs)

		# The function applied by sapply() returns a vector of the factor names
		# as 'names' and "OK" or "NOK" as the value for each one depending on
		# whether we managed to get stats OK or not. That's what goes into 'result'.
		# 
		# For each factor name (accessed by varLabels(eset)):
		# 	- check if we have stats (factor is "NOK" if not);
		# 	- Pull out adjusted p-values and moderated t-statistics for this
		# 	factor and put them in the appropriate columns in pval and tstat
		# 	matrices.
		result <- sapply(varLabels(eset),
			
			# factorName is renamed from 'varLabel'
			function(factorName) {
				
				print(paste("Processing",factorName))
				
				# If we do not see results from applying the contrasts matrix
				# for this factor, we don't have any statistics for it, so this
				# factor is "Not OK".
				if (is.null(allFits[[factorName, exact = TRUE]]$contr.fit)) {
					return("NOK")
				}
				
			
				# Get the moderated t-statistics from the contrasts fit.
				tStatistics <- allFits[[factorName, exact = TRUE]]$contr.fit$t
				
				# Get the p-values from the contrasts fit.
				pValues <- as.matrix(allFits[[factorName, exact = TRUE]]$contr.fit$p.value)
			
				# Which p-values are not NA?
				nonNaPvals = !is.na(pValues)
			
				# Run Benjamini and Hochberg (1995) fdr correction on the
				# non-NA p-values.
				pValues[nonNaPvals] = p.adjust(pValues[nonNaPvals], method = "fdr")
				
				# Make the column headings we want using the factor name and
				# the vector of factor values.
				namesForCols <- make.names(paste(factorName,allFits[[factorName, exact=TRUE]]$factorValues,sep = "||"))
				
				# Set the column names for tStatistics and pValues.
				colnames(tStatistics) <- namesForCols
				colnames(pValues) <- namesForCols

				# Now fill each column in tstat whose name matches a column in
				# tStatistics with the contents of the matching column in tStatistics.
				tstat[,which(colnames(tstat) %in% colnames(tStatistics))] <<- tStatistics[,colnames(tstat)[which(colnames(tstat) %in% colnames(tStatistics))]]

				# Likewise, fill each column in pval whose name matches a column in
				# pValues with the contents of the matching column in pValues.
				pval[,which(colnames(pval) %in% colnames(pValues))] <<- pValues[,colnames(pval)[which(colnames(pval) %in% colnames(pValues))]]
			

				# Now we've stored the info we need, return "OK" for this factor.
				return("OK")
			}
		)

		
		# Put the contents (transposed) of tstat and pval into the statistics
		# ncdf file.
		# Transposition of matrices for the NetCDFs is required because they are
		# stored in the NetCDF with each row corresponding to each assay, but
		# BioConductor packages output them so that each row corresponds to a
		# design element.
		print(paste("Writing tstat and pval to NetCDF:", ncol(tstat), "x", nrow(tstat)))
		put.var.ncdf(statistics_nc, "TSTAT", t(tstat))
		put.var.ncdf(statistics_nc, "PVAL", t(pval))
		
		# Get the factor names from the NetCDF.
		factorNames = get.var.ncdf(data_nc, "EF")
	
		# Write the statistics NetCDF file.
		sync.ncdf(statistics_nc)

		# Update the ranking order of the genes based on the p-values and
		# t-statistics, with most significantly differentially expressed gene
		# first.
		updateStatOrder(data_nc, statistics_nc)

		# Close open files.
		close.ncdf(data_nc)
		close.ncdf(statistics_nc)

		# Name the elements in the results vector with the factor names in efsc.
		names(result) <- factorNames
		
		# Return the result vector. This looks like e.g.:
    	# 
		# compound disease_state 
        #     "OK"         "NOK" 
		#
		return(result)
	})
	
	# Return the result or a try-error.
	return(e)
}





# updataStatOrder()
# 	- Computes and saves the order of design elements for each statfilter value.
# ARGUMENTS:
# 	- data_nc <- filename of NetCDF file with experiment data.
# 	- statistics_nc <- filename of NetCDF file with statistics data.
updateStatOrder <<- function(data_nc, statistics_nc) {
	
	# Now we are reading the p-values and t-statistics from the NetCDF file and
	# we transposed them before putting them in there. We need to transpose
	# them back again first, so that each row corresponds to a design element.
	
	# How many columns should they have? This is equal to the number of
	# property name entries in the NetCDF, which is equal to the total number
	# of factor name/value pairs (e.g. 'compound:none', 'compound:NaCl',
	# 'genotype:wild type', 'genotype:mutant').
	nCols <- length(get.var.ncdf(statistics_nc, "propertyNAME"))
	# Read in the t-statistics and p-values and transpose with
	# transposeMatrix().
	pval <- transposeMatrix(get.var.ncdf(statistics_nc, "PVAL"), nCols)
	tstat <- transposeMatrix(get.var.ncdf(statistics_nc, "TSTAT"), nCols)
	
	# Get the gene names (renamed to geneIDs from 'gn').
	geneIDs <- get.var.ncdf(data_nc, "GN")

	# Log how much data we've just read in. Number of rows is equal to number
	# of design elements; number of columns is equal to number of factor
	# name/value pairs.
	print(paste("T(rows:", nrow(tstat), "cols:", ncol(tstat), ")"))
	print(paste("P(rows:", nrow(pval), "cols:", ncol(pval), ")"))

	# Replace placeholders for missing expression values in NetCDF with 'NA'.
	tstat <- replaceMissingValues(tstat)
	pval <- replaceMissingValues(pval)

	# Find row indices of gene IDs that are 0. These are design elements with
	# no mapped gene, which we don't display information for.
	zeroGnIdxs <- (geneIDs == 0)
	print(paste("length( Zero GN rows ):", length(which(zeroGnIdxs))))
	
	# Find the indices of rows in tstat and pval where all values are NA.
	# naIdxsT and naIdxsP are vectors of TRUEs and FALSEs for each row in tstat
	# or pval (TRUE if that row has all NA; FALSE if not).
	naIdxsT <- apply(is.na(tstat), 1, all)
	naIdxsP <- apply(is.na(pval), 1, all)
	
	# naIdxs is a vector of TRUEs and FALSEs for each row in tstat and pval
	# where either tstat or pval (or both) has all NA.
	naIdxs <- apply(cbind(naIdxsT, naIdxsP), 1, function(x){ x[1] || x[2]})
	
	# Log how many NA rows we found in pval.
	print(paste("length( NA rows ):", length(which(naIdxsP))))

	# allBadIdxs is a vector with TRUE if pval or tstat was NA or the
	# corresponding gene name was 0 for that row.
	allBadIdxs <- apply(cbind(naIdxs, zeroGnIdxs), 1, function(x){ x[1] || x[2] })

	# Now rank the genes based on each of five conditions ('statfilter's):
	# 	- "ANY": the top differentially expressed genes,
	# 	regardless of 'up' or 'down' call, as well as the non-differentially expressed genes.
	# 	- "UP_DOWN": the top differentially expressed genes, regardless of 'up'
	# 	or 'down' call.
	# 	- "UP": the top up-regulated genes.
	# 	- "DOWN": the top down-regulated genes.
	# 	- "NON-DE": the top non-differentially expressed genes. These are still
	# 	ranked based on p-value and t-statistic, so that we can see ones that
	# 	are 'nearly significant' based on our threshold.
	for (statfilter in c("ANY", "UP_DOWN", "UP", "DOWN", "NON_D_E")) {

		# ifelse(expr, if TRUE, if FALSE)
		# So if the statfilter is "ANY", give badIdxs only the zero gene
		# name row indices. Otherwise, give it the indices of rows where
		# gene name is 0, or pval or tstat is all-NA.
		ifelse (statfilter == "ANY",
			badIdxs <- zeroGnIdxs,
			badIdxs <- allBadIdxs
		)

		# Subset only the rows of tstat and pval that do not have indices matching ones in badIdxs.
		tstatGood <- filterMatrix(tstat, 1, !badIdxs)
		pvalGood <- filterMatrix(pval, 1, !badIdxs)

		# Log what we're doing now.
		print(paste("Sorting/filtering tstat and pval by filter:", statfilter))
	
		# idxs is a vector of numbers from 1 to the number of rows in tstat.
		idxs <- c(1:nrow(tstat))
		# Put NAs in the positions whose indices match to numbers in badIdxs
		idxs[badIdxs] <- NA

		# Rank the genes according to the current 'statfilter', using function
		# orderByStatfilter.
		# res is a data.frame containing the indices of p-values in ascending
		# order (under res$rowidxs; most significant first), the indices of
		# their corresponding t-statistics (under res$colidxs), the lowest
		# p-values (res$minpvals) and the highest t-statistics (res$maxtstats).
		res <- orderByStatfilter(statfilter, tstatGood, pvalGood)
		
		# Log how many p-values we've got back.
		print(paste("length( result ):", length(res$rowidxs)))

		# initial is a vector of the indices of all the non-NA values from idxs
		# (i.e., all the ones that have a gene name and (unless we're on "ANY")
		# pval and tstat rows that are not all NA.
		initial <- idxs[!is.na(idxs)]
		
		# filtered is a vector of numbers from initial with indices that match
		# numbers in res$rowidxs (which is the indices of the ranked p-values).
		filtered <- initial[res$rowidxs]
		
		# Make sure filtered is the same length as the number of rows in tstat.
		filtered <- filtered[1:nrow(tstat)]

		# Put zeros in in place of NA values in filtered.
		filtered[is.na(filtered)] <- 0

		# filtered now contains the indices of the genes ordered by p-value.
		# The first value in filtered is the index of the top differentially
		# expressed gene. E.g. if filtered[1] == 12421, this means that
		# geneIDs[12421] is the ID of the top gene.

		
		# vname is a string with e.g. "ORDER_ANY".
		vname <- paste("ORDER_", statfilter, sep = "")
		
		# Write the ranking order for this statfilter to the statistics NetCDF
		# object. Print the error if it doesn't work.
		tryCatch({
			print(paste(vname, "written..."))
			put.var.ncdf(statistics_nc, vname, filtered)
		}, error = function(e) print(e))
	
	}
	
	# Returning "OK" if everything worked.
	return("OK")
}




# replaceMissingValues()
# 	- Replace placeholders for missing expression values in NetCDF with 'NA'.
# ARGUMENTS:
# 	- m <- a matrix (of either normalized summarized expressions or
# 	t-statistics or p-values).
replaceMissingValues <<- function(m) {

	# Replace -1e6 with NA. At experiment loading, if we cannot parse an
	# expression value from a data file in the load experiment as a float, we
	# set it to an impossible expression value of -1000000 instead.
	m[m <= -1e6] = NA

	# set to NA the default float fill value
	m[m == 9.969209968386869e36] = NA

	return(m)
}




# transposeMatrix()
# 	- Returns a matrix: if given a matrix, just transposes it with t(); if not,
# 	creates a matrix with whatever it's given and returns that.
# 	- Transposition of matrices from the NetCDFs is required because they are
# 	stored in the NetCDF with each row corresponding to each assay, but for
# 	analysis with BioConductor packages we require each row to correspond to a
# 	design element.
# ARGUMENTS:
# 	- m <- a matrix (or vector).
# 	- nCols <- number of columns the returned matrix should have.
# 	- nRows <- number of rows the returned matrix should have.
transposeMatrix <<- function(m, nCols, nRows) {
	
	# If we've got a matrix, send it back transposed.
	if (is.matrix(m)) {
	   return(t(m))
	}

	# Otherwise, if nCols>0, make a matrix with nCols columns.
	# If nCols is 0, I guess we'll be given nRows instead, so make a matrix
	# with nRows rows.
	ifelse(nCols > 0, out <- matrix(m, ncol = nCols), out <- matrix(m, nrow = nRows))
	# Return the new matrix.
	return(out)
}




# fixMatrix()
# 	- Returns a matrix: if given a matrix, just returns it again. If not,
# 	creates a matrix with whatever it's given (probably a vector).
# ARGUMENTS:
# 	- m <- a matrix (or vector).
# 	- nCols <- number of columns the returned matrix should have.
# 	- nRows <- number of rows the returned matrix should have.
fixMatrix <<- function(m, nCols, nRows) {
     
	# If we've got a matrix, send it back as is.
	if (is.matrix(m)) {
		return(m)
	}

	# Otherwise, if nCols>0, make a matrix with nCols columns.
	# If nCols is 0, I guess we'll be given nRows instead, so make a matrix
	# with nRows rows.
	ifelse(nCols > 0, out <- matrix(m, ncol = nCols), out <- matrix(m, nrow = nRows))
	# Return the new matrix.
	return(out)
}




# filterMatrix()
# 	- Returns a subset of a matrix based on the contents of the 'filter' vector
# 	passed to it.
# ARGUMENTS:
# 	- m <- a matrix to take a subset of.
# 	- direction <- 1 = by rows; 2 = by columns.
# 	- filter <- a vector of numbers which will be used as row or column indices
# 	to subset the matrix m.
filterMatrix <<- function(m, direction = 1, filter) {
	
	# If direction is 1, byRows is TRUE and byColumns is FALSE. Vice versa if
	# direction is 2. (though in this file it's only ever called with direction
	# = 1).
	byRows <- (direction == 1)
	byColummns <- (direction == 2)
	
	# Get the number of rows and columns.
	nCols <- ncol(m)
	nRows <- nrow(m)

	# Copy m to m2
	m2 = m;

	# Subset using the numbers in filter, either by rows or columns. Also send
	# it to fixMatrix() to be made into a matrix if m2 is in fact a vector.
	if (byRows) {
		m2 <- m[filter, ]
		m2 <- fixMatrix(m2, nCols = nCols)
	} else if (byColummns) {
		m2 <- m[ ,filter]
		m2 <- fixMatrix(m2, nRows = nRows)
	}

	# Return the new matrix.
	return(m2)
}




# orderByStatfilter()
#	- Sorts t-statistcs and p-values according to any of 5 'statfilter's:
#	 	- "ANY": the top differentially expressed genes,
#	 	regardless of 'up' or 'down' call, as well as the non-differentially
#	 	expressed genes.
#	 	- "UP_DOWN": the top differentially expressed genes, regardless of 'up'
#	 	or 'down' call.
#	 	- "UP": the top up-regulated genes.
#	 	- "DOWN": the top down-regulated genes.
#	 	- "NON-DE": the top non-differentially expressed genes.
#	 	These are still ranked based on p-value and t-statistic, so that we can
#	 	see ones that are 'nearly significant' based on our threshold.
# ARGUMENTS:
# 	- statfilter <- one of the above strings
# 	- tstat <- the matrix of t-statistics
# 	- pval <- the matrix of p-values
orderByStatfilter <- function(statfilter, tstat, pval) {
	
	# How many rows does pval have
	nrows <- nrow(pval)
	
	# Set up three vectors the same length as there are rows in pval,
	# containing only -1's. These will be overwritten with new values later,
	# but in R it's quicker to set up a vector with the size you want and then
	# overwrite values rather than keep extending the vector every time you
	# want to add a new value.
	minpvals <- rep(-1, nrows)
	maxtstats <- rep(-1, nrows)
	maxtstatidxs <- rep(-1, nrows)

	# Copy tstat and pval to some new objects.
	f.tstat <- tstat
	f.pval <- pval

	# max.safe() function:
	# 	- Returns 1 if all values in x are NA, or the index of the maximum
	# 	value otherwise.
	max.safe <- function(x)ifelse(all(is.na(x)), 1, which.max(x))
	
	# If we are on statfilter "ANY", just get the indices of the maximum
	# t-statistic in each row of f.tstat and put them in maxtstatidxs. We use
	# the absolute value of each t-statistic (ignore minus signs) using abs().
	if (statfilter == "ANY") {
		maxtstatidxs <- apply(abs(f.tstat), 1, max.safe)

	} 
	
	# If we are on statfilter "UP_DOWN", we are interested in the genes with
	# p-values <= 0.05, so we filter out those which have p > 0.05 by setting
	# their p-values to 1 and their t-statistics to 0. We then call max.safe()
	# on the absolute values of the remaining values in f.tstat, to get the
	# indices of the maximum t-statistics.
	else if (statfilter == "UP_DOWN") {
		f.pval[pval > 0.05] <- 1
		f.tstat[pval > 0.05] <- 0
		maxtstatidxs <- apply(abs(f.tstat), 1, max.safe)

	} 
	
	# If we are on statfilter "UP", we are only interested in significantly
	# up-regulated genes. So we filter out any that have p > 0.05 OR a negative
	# t-statistic. Negative t-statistics mean 'down-regulated', positive ones
	# mean 'up-regulated'.
	# Then we call max.safe() to get the indices of the maximum t-statistics.
	# Don't have to worry about using absolute t-statistic values because they
	# will all be positive anyway.
	else if (statfilter == "UP") {
		f.pval[pval > 0.05 | tstat < 0] <- 1
		f.tstat[pval > 0.05 | tstat < 0] <- 0
		maxtstatidxs <- apply(f.tstat, 1, max.safe)

	} 
	
	# If we are on statfilter "DOWN", we are only interested in significantly
	# down-regulated genes. So we filter out any that have p > 0.05 OR a
	# positive t-statistic.
	# Then we call max.safe(), using '-' to make the (all-negative) remaining
	# values of f.tstat positive, instead of using abs().
	else if (statfilter == "DOWN") {
		f.pval[pval > 0.05 | tstat > 0] <- 1
		f.tstat[pval > 0.05 | tstat > 0] <- 0
		maxtstatidxs <- apply(-f.tstat, 1, max.safe)

	} 
	
	# If we are on statfilter "NON_D_E", we are only interested in genes that
	# are not differentially expressed. So we filter out all the
	# ones that have p <= 0.05 and then call max.safe() on the absolute
	# t-statistic values.
	else if(statfilter == "NON_D_E") {
		f.pval[pval <= 0.05] <- 1
		f.tstat[pval <= 0.05] <- 0
		maxtstatidxs <- apply(abs(f.tstat), 1, max.safe)
	}
	

	# Now we have a vector (maxtstatidxs) of the column indices of the maximum
	# t-statistic for each gene, from the tstat matrix (the column corresponds
	# to the factor value, the rows correspond to genes/design elements).
	
	
	# For i in 1 to length(maxtstatidxs)
	for (i in seq_along(maxtstatidxs)) {
		
		# Get the p-value from the f.pval matrix at row i and the column whose
		# index is the value at maxtstatidxs[i]. Put it in minpvals at index i.
		minpvals[i] <- f.pval[i, maxtstatidxs[i]]
		
		# Get the t-statistic from the f.tstat matrix at row i and the column whose
		# index is the value at maxtstatidxs[i]. Put it in maxtstats at index i.
		maxtstats[i] <- f.tstat[i, maxtstatidxs[i]]
	}
	
	# The values in minpvals and maxtstats are in the order of the genes/design
	# elements in the ExpressionSet (or the list from the NetCDF). Now we will
	# sort the values in minpvals in ascending order (lowest i.e. most
	# significant first). Note that here we don't actually change the order of
	# the values in minpvals or maxtstats. The order() function returns the
	# index of the lowest value first, then the index of the second-lowest,
	# etc. If values in minpvals are equal, their corresponding values from
	# maxtstats are used to try an resolve this.
	# What we end up with in idxs is the index of minpvals with the index of
	# the lowest value at idxs[1], the index of the second-lowest at idxs[2],
	# and so on. idxs[1] directly corresponds to the index of the most
	# significantly differentially expressed gene.
	idxs <- order(minpvals, -abs(maxtstats), na.last = TRUE)
	
	
	# If we're on statfilter "ANY", just get the indices of the ones that have
	# p-values < 1 and t-statistics that are non-zero.
	if (statfilter != 'ANY') {
		idxs <- idxs[which(minpvals[idxs] < 1 & maxtstats[idxs] != 0)]
	}
	
	# Return a data frame with:
	# 	- rowidxs: the sorted indices in idxs.
	# 	- colidxs: the indices of each column the max t-statistic was found in
	# 	(i.e. which factor value).
	# 	- minpvals: the values from minpvals above sorted according to the
	# 	values in idxs.
	# 	- maxtstats: the values from maxtstats above sorted according to the
	# 	values in idxs.
	return(
		data.frame(
			rowidxs = idxs,
			colidxs = maxtstatidxs[idxs],
			minpvals = minpvals[idxs],
			maxtstats = maxtstats[idxs]
		)
	)
}




})()

