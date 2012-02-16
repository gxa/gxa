### Atlas Analytics Original Driver Functions
### Author: ostolop

(function() {

### Reads data from Atlas NetCDF file and returns an ExpressionSet
### Post-AEW migration TODOs:
### * NChannelSet
### * Multiple array designs
read.atlas.nc <<-
  function (filename, accnum = NULL) {
    
    require(ncdf)
    require(Biobase)
    require(gtools)

    nc = open.ncdf(filename)

    as = as.vector(get.var.ncdf(nc, "ASacc"))
    bs = as.vector(get.var.ncdf(nc, "BSacc"))
    b2a = fixMatrix(get.var.ncdf(nc, "BS2AS"), nRows = length(as), nCols = length(bs))

    de = as.vector(get.var.ncdf(nc, "DEacc"))
    bdc = fixMatrix(get.var.ncdf(nc, "BDC"), nRows = length(as), nCols = length(de))

    if ("EF" %in% names(nc$dim)) {
       ef = get.var.ncdf(nc, "EF")
       efv = get.var.ncdf(nc, "EFV")

       colnames(efv) = ef
       rownames(efv) = as
       efv = data.frame(efv)
       print(paste("Read in EFV:", nrow(efv), "x", ncol(efv)))
    }

    if ("SC" %in% names(nc$dim)) {
      sc = get.var.ncdf(nc, "SC")
      scv = get.var.ncdf(nc, "SCV")

      colnames(scv) = sc
      rownames(scv) = bs
      scv = data.frame(scv)
      print(paste("Read in SCV:", nrow(scv), "x", ncol(scv)))
    }

    # deacc = get.var.ncdf(nc, "DEacc")
    gn = get.var.ncdf(nc, "GN")

    # make de's unique
    #de[de == 0] <- -(1:length(which(de == 0)))

    accnum = att.get.ncdf(nc,varid = 0,"experiment_accession")$value
    qt = att.get.ncdf(nc,varid = 0,"quantitationType")$value
    adacc = att.get.ncdf(nc,varid = 0,"ADaccession")$value
    adname = att.get.ncdf(nc,varid = 0,"ADname")$value

    close.ncdf(nc)

    bdc = replaceMissingValues(bdc)

    if (length(as) == 1) {
      bdc = matrix(bdc, nrow = length(de))
      b2a = matrix(b2a, nrow = 1)
    } else {
      bdc = t(bdc)
    }
    rownames(bdc) = de
    colnames(bdc) = as

    rownames(b2a) = as
    colnames(b2a) = bs

    print(paste("Read in", accnum))
    print(paste("Read in BDC:", nrow(bdc), "x", ncol(bdc)))


    ncinfo = unlist(strsplit(basename(filename),"_|[.]"))
    exptid = ncinfo[1]
    arraydesignid = ncinfo[2]

    if (exists("efv")) {
      efscv <- efv
    } else {
      efscv <- data.frame(row.names=as)
    }

    fDataFrame = data.frame(gn = gn,de = de) #, deacc = deacc)
    fData = new("AnnotatedDataFrame", data = fDataFrame)
    featureNames(fData) = de
    pData = new("AnnotatedDataFrame", data = efscv)

    if(exists("scv")) {
      scData = new("AnnotatedDataFrame", data = scv)
    }

    eData = new("MIAME",
      other = list(accession = accnum,
        experimentid = exptid,
        arraydesignid = arraydesignid,
        qt = qt,
        arraydesignaccnum = adacc,
        arraydesignname = adname
        )
      )

    if (exists("scv")) {
      attr(eData, "scv") <- scv
    }
    attr(eData, "b2a") <- b2a

    aData <- assayDataNew(storage.mode = "lockedEnvironment", exprs = bdc)
    featureNames(aData) <- de
    sampleNames(aData) <- as

    print(paste("Computed phenoData for", paste(varLabels(pData),collapse = ", "), nrow(pData), "x", ncol(pData)))
    return(new("ExpressionSet", assayData = aData, phenoData = pData, featureData = fData, experimentData = eData))
  }

### Log function that does not return NAs
log2.safe <-
  function(x) {
    # Assumption is, expression levels are > 0 unless someone has _already_ applied log.
    # If the levels are < 1000, we're fine without log: moreover, that may as well mean
    # all the expression levels were > 1 (why not?), and someone has already applied log.
    # If the values are more than 1000, then log definitely was not used before, as 2^1000
    # is just way too much for expression level value.
    if (min(x, na.rm = TRUE) < 0 || max(x, na.rm = TRUE) < 1000)
      return(x)

    print("Taking log2 of the expression matrix")

    # For most values, 1e-10 is far smaller than the precision expected, so we can add it and be (almost) sure
    # we've got rid of zeros without disturbing the stats.
    tmp = log2(x + 1e-10)
    tmp[!(is.finite(tmp) | is.na(x))] <- 0
    tmp
}

### function to check if factor value is empty
isEmptyEFV <- function(value) return (is.null(value))

### Omnibus one-way ANOVA (with moderated t) F-statistic computation
fstat.eset <-
  function(eset, varLabel) {
    print(paste("Calculating design matrix for", varLabel))
    design = model.matrix(as.formula(paste("~0+", varLabel)), data = eset)

    print("Fitting model...")
    fit = lmFit(eset, design)

    fit = eBayes(fit)

    return(fit)
}

### Moderated t one-way ANOVA post-hoc testing with global FDR adjustment
allupdn <-
  function (eset, alpha = 0.01, evars = varLabels(eset) ) {

    require(limma)

    exprs = exprs(eset)
    exprs(eset) = log2.safe(exprs)

    allFits = list()

    for(varLabel in evars){
      try({
        print(paste("Calculating lmFit and F-stats for", varLabel))

        nonEmptyFactorValues = which(!sapply(eset[[varLabel, exact = TRUE]], isEmptyEFV))

        esetForVariable = eset[, nonEmptyFactorValues]
        esetForVariable[[varLabel, exact = TRUE]] = factor(esetForVariable[[varLabel, exact = TRUE]])

        numVariableFactorLevels <- nlevels(esetForVariable[[varLabel, exact=TRUE]])
        if (numVariableFactorLevels == ncol(exprs(esetForVariable)) || numVariableFactorLevels < 2) {
          print("Can't compute statistics for poorly conditioned data: too few or too many factor levels.")
          next
        }

        thisFit = fstat.eset(esetForVariable, varLabel = varLabel)

        n = ncol(thisFit$design)
        cm = diag(n) - 1/n

        contr.fit = contrasts.fit(thisFit, cm)
        contr.fit = eBayes(contr.fit)

        dec = decideTests(contr.fit, method = "global", adjust.method = "fdr")
        colnames(dec) = levels(esetForVariable[[varLabel, exact = TRUE]])

        thisFit$boolupdn = dec
        thisFit$contr.fit = contr.fit

        allFits[[varLabel]] = thisFit
        print("Done.")
      })
    }

    allFits
}

### Atlas analytics processing driver: read the data, compute the linear fit, post-hoc test, adjust and write to tab-delim files
process.atlas.nc<-
  function (nc) {

    eset = read.atlas.nc(nc)
    info = otherInfo(experimentData(eset))
    proc = allupdn(eset)

    print("Writing out the results")
    for (varLabel in varLabels(eset)) {
      if (!is.null(proc[[varLabel, exact = TRUE]]$contr.fit)) {
        fitfile <- paste(info$accession, "_", info$experimentid, "_", info$arraydesignid, "_", varLabel, "_", "fit.tab", sep = "")
        tab <- list()
        tab$A <- proc[[varLabel, exact = TRUE]]$Amean
        # tab$Coef <- proc[[varLabel, exact = TRUE]]$contr.fit$coef
        tab$t <- proc[[varLabel, exact = TRUE]]$contr.fit$t
        tab$p.value <- as.matrix(proc[[varLabel, exact = TRUE]]$contr.fit$p.value)

        pv = tab$p.value
        o = !is.na(tab$p.value)
        pv[o] = p.adjust(pv[o], method = "fdr")

        tab$p.value.adj = pv
        tab$Res <- unclass(proc[[varLabel, exact = TRUE]]$boolupdn)
        tab$F <- proc[[varLabel, exact = TRUE]]$F
        tab$F.p.value <- proc[[varLabel, exact = TRUE]]$F.p.value
        tab$F.p.value.adj = proc[[varLabel, exact = TRUE]]$F.p.value.adj
        tab$Genes <- proc[[varLabel, exact = TRUE]]$genes
        tab <- data.frame(tab, check.names = FALSE)
        write.table(tab, file = fitfile, quote = FALSE, row.names = FALSE, sep = "\t")
        print(paste("Wrote",fitfile))
      }
    }
  }

### Atlas analytics, returns instead of writing
computeAnalytics <<-
  function (data_ncdf, statistics_ncdf) {
    e <- try({
      eset = read.atlas.nc(data_ncdf)
      data_nc = open.ncdf(data_ncdf)
      statistics_nc = open.ncdf(statistics_ncdf, write = TRUE)

      if (dim(eset)[2] == 1) {
        return(sapply(varLabels(eset), function(i) "NOK"))
      }

      proc = allupdn(eset)

      propertyNAME = get.var.ncdf(statistics_nc, "propertyNAME")
      propertyVALUE = get.var.ncdf(statistics_nc, "propertyVALUE")
      pairs <- paste(propertyNAME, propertyVALUE, sep = "||")

      # initialize tstat and pval to NA
      tstat = matrix(NA, ncol = length(propertyNAME), nrow = nrow(eset)); #t(get.var.ncdf(statistics_nc, "TSTAT"))
      pval = matrix(NA, ncol = length(propertyNAME), nrow = nrow(eset)); #t(get.var.ncdf(statistics_nc, "PVAL"))

      colnames(tstat) <- make.names(pairs)
      colnames(pval) <- make.names(pairs)

      result <- sapply(varLabels(eset),
                       function(varLabel) {
                         print(paste("Processing",varLabel))
                         if (is.null(proc[[varLabel, exact = TRUE]]$contr.fit)) {
                           return("NOK")
                         }

                         tab <- list()
                         tab$A <- proc[[varLabel, exact = TRUE]]$Amean
                         tab$t <- proc[[varLabel, exact = TRUE]]$contr.fit$t
                         tab$p.value <- as.matrix(proc[[varLabel, exact = TRUE]]$contr.fit$p.value)

                         pv = tab$p.value
                         o = !is.na(tab$p.value)
                         pv[o] = p.adjust(pv[o], method = "fdr")

                         tab$p.value.adj = pv
                         tab$Res <- unclass(proc[[varLabel, exact = TRUE]]$boolupdn)

                         # tab$F <- proc[[varLabel, exact = TRUE]]$F
                         # tab$F.p.value <- proc[[varLabel, exact = TRUE]]$F.p.value
                         # tab$F.p.value.adj = proc[[varLabel, exact = TRUE]]$F.p.value.adj

                         tab$Genes <- proc[[varLabel, exact = TRUE]]$genes

                         colnames(tab$Res) <- make.names(paste(varLabel,colnames(tab$Res),sep = "||"))

                         colnames(tab$t) <- colnames(tab$Res)
                         colnames(tab$p.value.adj) <- colnames(tab$Res)

                         tstat[,which(colnames(tstat) %in% colnames(tab$t))] <<- tab$t[,colnames(tstat)[which(colnames(tstat) %in% colnames(tab$t))]]
                         pval[,which(colnames(pval) %in% colnames(tab$p.value.adj))] <<- tab$p.value.adj[,colnames(pval)[which(colnames(pval) %in% colnames(tab$p.value.adj))]]

                         return("OK")
                       })

      print(paste("Writing tstat and pval to NetCDF:", ncol(tstat), "x", nrow(tstat)))
      put.var.ncdf(statistics_nc, "TSTAT", t(tstat))
      put.var.ncdf(statistics_nc, "PVAL", t(pval))

      efsc = get.var.ncdf(data_nc, "EF")
      
      sync.ncdf(statistics_nc)
      updateStatOrder(data_nc, statistics_nc)

      close.ncdf(data_nc)
      close.ncdf(statistics_nc)

      names(result) <- efsc

      return(result)
    })

    return(e)
  }

# Computes and saves the order of design elements for each statfilter value
updateStatOrder <<-
  function(data_nc, statistics_nc) {
    nCols <- length(get.var.ncdf(statistics_nc, "propertyNAME"))
    pval <- transposeMatrix(get.var.ncdf(statistics_nc, "PVAL"), nCols)
    tstat <- transposeMatrix(get.var.ncdf(statistics_nc, "TSTAT"), nCols)
    gn <- get.var.ncdf(data_nc, "GN")

    print(paste("T(rows:", nrow(tstat), "cols:", ncol(tstat), ")"))
    print(paste("P(rows:", nrow(pval), "cols:", ncol(pval), ")"))

    tstat <- replaceMissingValues(tstat)
    pval <- replaceMissingValues(pval)

    # find rows of zero genes and NA values
    zeroGnIdxs <- (gn == 0)
    print(paste("length( Zero GN rows ):", length(which(zeroGnIdxs))))

    naIdxsT <- apply(is.na(tstat), 1, all)
    naIdxsP <- apply(is.na(pval), 1, all)
    naIdxs <- apply(cbind(naIdxsT, naIdxsP), 1, function(x){ x[1] || x[2]})
    print(paste("length( NA rows ):", length(which(naIdxsP))))

    allBadIdxs <- apply(cbind(naIdxs, zeroGnIdxs), 1, function(x){ x[1] || x[2] })
    
    for (statfilter in c("ANY", "UP_DOWN", "UP", "DOWN", "NON_D_E")) {
      ifelse (statfilter == "ANY",
              badIdxs <- zeroGnIdxs,
              badIdxs <- allBadIdxs
      )

      tstatGood <- filterMatrix(tstat, 1, !badIdxs)
      pvalGood <- filterMatrix(pval, 1, !badIdxs)

      print(paste("Sorting/filtering tstat and pval by filter:", statfilter))
      idxs <- c(1:nrow(tstat))
      idxs[badIdxs] <- NA

      res <- orderByStatfilter(statfilter, tstatGood, pvalGood)
      print(paste("length( result ):", length(res$rowidxs)))

      initial <- idxs[!is.na(idxs)]
      filtered <- initial[res$rowidxs]
      filtered <- filtered[1:nrow(tstat)]
      filtered[is.na(filtered)] <- 0
      vname <- paste("ORDER_", statfilter, sep = "")

      tryCatch({
        print(paste(vname, "written..."))
        put.var.ncdf(statistics_nc, vname, filtered)
      }, error = function(e) print(e))
    }
    return("OK")
  }

replaceMissingValues <<-
  function(m) {
    m[m <= -1e6] = NA
    m[m == 9.969209968386869e36] = NA # set to NA the default float fill value
    return(m)
  }

transposeMatrix <<-
  function(m, nCols, nRows) {
    if (is.matrix(m)) {
       return(t(m))
    }
    ifelse(nCols > 0, out <- matrix(m, ncol = nCols), out <- matrix(m, nrow = nRows))
    return(out)
  }

fixMatrix <<-
  function(m, nCols, nRows) {
     if (is.matrix(m)) {
       return(m)
     }
     ifelse(nCols > 0, out <- matrix(m, ncol = nCols), out <- matrix(m, nrow = nRows))
     return(out)
  }

filterMatrix <<-
    function(m, direction = 1, filter) {
        byRows <- (direction == 1)
        byColummns <- (direction == 2)

        nCols <- ncol(m)
        nRows <- nrow(m)

        m2 = m;
        if (byRows) {
            m2 <- m[filter, ]
            m2 <- fixMatrix(m2, nCols = nCols)
        } else if (byColummns) {
            m2 <- m[ ,filter]
            m2 <- fixMatrix(m2, nRows = nRows)
        }
        return(m2)
    }

### Compute a design matrix for making all possible pairwise comparisons (one-way ANOVA F).
design.pairs <-
  function(levels) {
    makeContrasts(contrasts = combn(levels, 2, paste, collapse = '-'),levels = levels)
  }

### Sorts T and P values by statfilter.
orderByStatfilter <-
  function(statfilter, tstat, pval) {
    nrows <- nrow(pval)

    minpvals <- rep(-1, nrows)
    maxtstats <- rep(-1, nrows)
    maxtstatidxs <- rep(-1, nrows)

    f.tstat <- tstat
    f.pval <- pval

    max.safe <- function(x)ifelse(all(is.na(x)), 1, which.max(x))
      
    if (statfilter == "ANY") {
      maxtstatidxs <- apply(abs(f.tstat), 1, max.safe)

    } else if (statfilter == "UP_DOWN") {
      f.pval[pval > 0.05] <- 1
      f.tstat[pval > 0.05] <- 0
      maxtstatidxs <- apply(abs(f.tstat), 1, max.safe)

    } else if (statfilter == "UP") {
      f.pval[pval > 0.05 | tstat < 0] <- 1
      f.tstat[pval > 0.05 | tstat < 0] <- 0
      maxtstatidxs <- apply(f.tstat, 1, max.safe)

    } else if (statfilter == "DOWN") {
      f.pval[pval > 0.05 | tstat > 0] <- 1
      f.tstat[pval > 0.05 | tstat > 0] <- 0
      maxtstatidxs <- apply(-f.tstat, 1, max.safe)

    } else if(statfilter == "NON_D_E") {
      f.pval[pval <= 0.05] <- 1
      f.tstat[pval <= 0.05] <- 0
      maxtstatidxs <- apply(abs(f.tstat), 1, max.safe)
    }

    for (i in seq_along(maxtstatidxs)) {
      minpvals[i] <- f.pval[i, maxtstatidxs[i]]
      maxtstats[i] <- f.tstat[i, maxtstatidxs[i]]
    }

    idxs <- order(minpvals, -abs(maxtstats), na.last = TRUE)

    if (statfilter != 'ANY') {
      idxs <- idxs[which(minpvals[idxs] < 1 & maxtstats[idxs] != 0)]
    }

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

