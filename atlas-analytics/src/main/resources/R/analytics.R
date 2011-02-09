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

    as = get.var.ncdf(nc, "AS")
    bs = get.var.ncdf(nc, "BS")
    b2a = fixMatrix(get.var.ncdf(nc, "BS2AS"), nRows = length(as), nCols = length(bs))

    de = get.var.ncdf(nc, "DE")
    bdc = fixMatrix(get.var.ncdf(nc, "BDC"), nRows = length(as), nCols = length(de))
    
    ef = get.var.ncdf(nc, "EF")
    efv = get.var.ncdf(nc, "EFV")

    try({
      sc = get.var.ncdf(nc, "SC")
      scv = get.var.ncdf(nc, "SCV")

      colnames(scv) = sc
      rownames(scv) = bs
      scv = data.frame(scv)
   })

    # deacc = get.var.ncdf(nc, "DEacc")
    gn = get.var.ncdf(nc, "GN")

    # make de's unique
    de[de == 0] <- -(1:length(which(de == 0)))

    accnum = att.get.ncdf(nc,varid = 0,"experiment_accession")$value
    qt = att.get.ncdf(nc,varid = 0,"quantitationType")$value
    adacc = att.get.ncdf(nc,varid = 0,"ADaccession")$value
    adname = att.get.ncdf(nc,varid = 0,"ADname")$value

    close.ncdf(nc)

    colnames(efv) = ef
    rownames(efv) = as
    efv = data.frame(efv)

    bdc = misValCheck(bdc)

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
    print(paste("Read in EFV:", nrow(efv), "x", ncol(efv)))

    if (exists("scv")) {
      print(paste("Read in SCV:", nrow(scv), "x", ncol(scv)))
    }

    ncinfo = unlist(strsplit(basename(filename),"_|[.]"))
    exptid = ncinfo[1]
    arraydesignid = ncinfo[2]

    efscv <- efv
#   MK: remove this for now, to prevent analytics running on SC's
#    for(sc in colnames(scv)) {
#      scvj <- as.factor(unlist(lapply(rownames(efv), function(ef)
#                                      paste(unique(scv[colnames(b2a)[as.logical(b2a[ef,])],sc]),
#                                            ## colnames(b2a)[as.logical(b2a[ef,])],
#                                            sep = ":", collapse = "|"))))
#
#      ef <- sub("bs_","ba_",sc)
#      if( !identical(efscv[[ef]], scvj)) {
#        efscv[[sc]] <- scvj
#      }
#    }

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
  function(x,log = T) {
    if (!log)
      return(x)

    tmp = log2(x)
    tmp[!(is.finite(tmp) | is.na(x))] <- 0
    tmp
}

### Omnibus one-way ANOVA (with moderated t) F-statistic computation
fstat.eset <-
  function(eset, design = NULL, varLabel = NULL,lg = FALSE) {
    if (is.null(design) && !is.null(varLabel)) {
      print(paste("Calculating design matrix for", varLabel))
        design = model.matrix(as.formula(paste("~0+", varLabel)), data = eset)
    }

    if (lg == TRUE) {
        exprs(eset)<-log2.safe(exprs(eset))
    }

    print("Fitting model...")
    fit = lmFit(eset,design)

    #  print("Re-fitting model to ANOVA contrasts...")
    #  pairs = design.pairs(colnames(design))
    #  cfit = contrasts.fit(fit,pairs)

    #  print("Moderating...")
    #  cfit = eBayes(cfit)
    fit = eBayes(fit)

    #  fit$F = cfit$F
    #  fit$F.p.value = cfit$F.p.value

    return(fit)
}

### Moderated t one-way ANOVA post-hoc testing with global FDR adjustment
allupdn <-
  function (eset, alpha = 0.01, evars = varLabels(eset) ) {

    require(limma)

    exprs = exprs(eset)
    if (max(exprs,na.rm = TRUE) > 1000 && min(exprs, na.rm = TRUE) >= 0) {
      print("Taking log2 of the expression matrix")
      exprs(eset) = log2.safe(exprs)
    }

    allFits = list()

    for(varLabel in evars){
      try({
        print(paste("Calculating lmFit and F-stats for", varLabel))
          if (length(levels(eset[[varLabel, exact = TRUE]])) < 2
            || length(levels(eset[[varLabel, exact = TRUE]])) == ncol(exprs) ) { next }

          esetForVariable = eset[,which(eset[[varLabel, exact = TRUE]] != "")]
          esetForVariable[[varLabel, exact = TRUE]] = factor(esetForVariable[[varLabel, exact = TRUE]])

          thisFit = fstat.eset(esetForVariable, varLabel = varLabel)

          print("Adjusting p-values")
          #   pp = p.adjust(thisFit$F.p.value, method = "fdr")
          #   w = which(pp <= alpha)
          #
          #   thisFit$F.p.value.adj = pp

          n = ncol(thisFit$design)
          cm = diag(n) - 1/n

          contr.fit = contrasts.fit(thisFit, cm)
          contr.fit = eBayes(contr.fit)

          dec = decideTests(contr.fit, method = "global", adjust.method = "fdr")
          colnames(dec) = levels(esetForVariable[[varLabel, exact = TRUE]])

          #   thisFit$which = w
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
        fitfile <-  paste(info$accession, "_", info$experimentid, "_", info$arraydesignid, "_", varLabel, "_", "fit.tab", sep = "")
        tab <- list()
        tab$A <- proc[[varLabel, exact = TRUE]]$Amean
        #    tab$Coef <- proc[[varLabel, exact = TRUE]]$contr.fit$coef
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
  function (nc) {
    e <- try({
      eset = read.atlas.nc(nc)
      ncd = open.ncdf(nc, write = TRUE)

      if (dim(eset)[2] == 1) {
        return(sapply(varLabels(eset), function(i) "NOK"))
      }

      proc = allupdn(eset)

      uEFV = get.var.ncdf(ncd, "uEFV")

      # initialize tstat and pval to NA
      tstat = matrix(NA, ncol = length(uEFV), nrow = nrow(eset)); #t(get.var.ncdf(ncd, "TSTAT"))
      pval = matrix(NA, ncol = length(uEFV), nrow = nrow(eset)); #t(get.var.ncdf(ncd, "PVAL"))

      colnames(tstat) <- make.names(uEFV)
      colnames(pval)  <- make.names(uEFV)

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
      put.var.ncdf(ncd, "TSTAT", t(tstat))
      put.var.ncdf(ncd, "PVAL",  t(pval))

      ef = get.var.ncdf(ncd, "EF")
      
      close.ncdf(ncd)

      names(result) <- ef

      updateStatOrder(nc)

      return(result)
    })

    return(e)
  }

# Computes and saves the order of design elements for each statfilter value
updateStatOrder <<-
  function(filename) {
    
    ncd <- open.ncdf(filename, write = TRUE)
    on.exit(close.ncdf(ncd))

    nCols <- length(get.var.ncdf(ncd, "uEFV"))
    pval <- transposeMatrix(get.var.ncdf(ncd, "PVAL"), nCols)
    tstat <- transposeMatrix(get.var.ncdf(ncd, "TSTAT"), nCols)
    gn <- get.var.ncdf(ncd, "GN")

    print(paste("T(rows:", nrow(tstat), "cols:", ncol(tstat), ")"))
    print(paste("P(rows:", nrow(pval), "cols:", ncol(pval), ")"))

    tstat <- misValCheck(tstat)
    pval <- misValCheck(pval)
    
    # ignore NA values and zero genes
    naIdxsT <- apply(is.na(tstat), 1, all)
    naIdxsP <- apply(is.na(pval), 1, all)
    zeroGnIdxs <- (gn == 0)
    badIdxs <- apply(cbind(naIdxsT, naIdxsP, zeroGnIdxs), 1, function(x){ x[1] || x[2] || x[3] })

    print(paste("length( NA T rows ):", length(which(naIdxsT))))
    print(paste("length( NA P rows ):", length(which(naIdxsP))))
    print(paste("length( Zero GN rows ):", length(which(zeroGnIdxs))))
    
    tstatGood <- filterMatrix(tstat, 1, !badIdxs)
    pvalGood <- filterMatrix(pval, 1, !badIdxs)

    for (statfilter in c("ANY", "UP_DOWN", "UP", "DOWN", "NON_D_E")) {
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
        print(paste("Write:", vname))
        put.var.ncdf(ncd, vname, filtered)
      }, error = function(e) print(e))
    }
    return("OK")
  }

misValCheck <<-
  function(m) {
    m[m <= -1e6] = NA
    m[m == 9.969209968386869e36] = NA # set to NA the default float fill value
    return(m)
  }

transposeMatrix <<-
  function(m, nCols) {
    if (is.matrix(m))
      return(t(m))
    return(matrix(m, ncol=nCols))
  }

fixMatrix <<-
  function(m, nCols, nRows) {
     if (is.matrix(m))
       return(m)
     return(matrix(m, ncol=nCols, nrow=nRows))
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

    if (statfilter == "ANY") {
      maxtstatidxs <- apply(abs(f.tstat), 1, which.max)

    } else if (statfilter == "UP_DOWN") {
      f.pval[pval > 0.05] <- 1
      f.tstat[pval > 0.05] <- 0
      maxtstatidxs <- apply(abs(f.tstat), 1, which.max)

    } else if (statfilter == "UP") {
      f.pval[pval > 0.05 | tstat < 0] <- 1
      f.tstat[pval > 0.05 | tstat < 0] <- 0
      maxtstatidxs <- apply(f.tstat, 1, which.max)

    } else if (statfilter == "DOWN") {
      f.pval[pval > 0.05 | tstat > 0] <- 1
      f.tstat[pval > 0.05 | tstat > 0] <- 0
      maxtstatidxs <- apply(-f.tstat, 1, which.max)

    } else if(statfilter == "NON_D_E") {
      f.pval[pval <= 0.05] <- 1
      f.tstat[pval <= 0.05] <- 0
      maxtstatidxs <- apply(abs(f.tstat),1, which.max)
    }

    for (i in seq_along(maxtstatidxs)) {
      minpvals[i]  <- f.pval[i, maxtstatidxs[i]]
      maxtstats[i] <- f.tstat[i, maxtstatidxs[i]]
    }

    idxs <- order(minpvals, -abs(maxtstats))

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

###  Returns T and P values for selected genes and factors.
###  If nothing is specified it returns the best genes arcording the statfilter (default is ANY).
find.best.design.elements <<-
  function(ncdf, gnids = NULL, ef = NULL, efv = NULL, statfilter = c('ANY','UP_DOWN','DOWN','UP','NON_D_E'), statsort = "PVAL", from = 1, rows = 10) {

    require(ncdf)

    options(digits.secs = 6)
    print(Sys.time())
   
    from <- max(1, from)
    to <- (from + rows -1)

    statfilter = match.arg(statfilter)

    nc <- open.ncdf(ncdf)

    gn <- get.var.ncdf(nc, "GN")

    de <- nc$dim$DE$vals

    wde <- which(gn > 0)

    uefv  <- nc$dim$uEFV$vals
    wuefv <- c()

    if ((!is.null(ef) && ef != "") && (is.null(efv) || efv == "")) {
      wuefv <- grep(paste(ef,"||",sep = ""), uefv, fixed = TRUE)

    } else if ((!is.null(ef)  && ef != "") && (!is.null(efv) && efv != "")) {
      efv <- paste(ef, efv, sep = "||")
      wuefv <- which(uefv %in% efv)

    } else {
      wuefv <- rep(1:length(uefv))
    }

    if (!is.null(gnids) && gnids != "") {
      wde <- which(gn %in% gnids)
    } else if (length(wuefv) == length(uefv)) { # if no params
      rowOrder <- tryCatch(get.var.ncdf(nc, paste("ORDER_", statfilter, sep = "")), error = function(e) NULL)
      if (!is.null(rowOrder)) {
         rowOrder <- rowOrder[rowOrder > 0]
         to <- min(to, length(rowOrder))
         from <- min(from, to)
         wde <- rowOrder[from:to]
         from <- 1
      }
    }

    tstat <- matrix(nrow = length(wde), ncol = length(wuefv))
    pval <- matrix(nrow = length(wde), ncol = length(wuefv))

    if (length(wuefv) < length(uefv)) {
      for (i in seq_along(wuefv)) {
        tstat[,i] <- get.var.ncdf(nc, "TSTAT", start = c(wuefv[i],1), count = c(1,-1))[wde]
        pval[,i] <- get.var.ncdf(nc, "PVAL",  start = c(wuefv[i],1), count = c(1,-1))[wde]
      }
    } else {
      if (length(wde) < 0.2 * nc$dim$DE$len) {
        for (i in seq_along(wde)) {
          tstat[i,] <- get.var.ncdf(nc, "TSTAT", start = c(1,wde[i]), count = c(-1,1))
          pval[i,] <- get.var.ncdf(nc, "PVAL", start = c(1,wde[i]), count = c(-1,1))
        }
      } else {
        tstat <- transposeMatrix(get.var.ncdf(nc, "TSTAT"))[wde, ]
        pval <- transposeMatrix(get.var.ncdf(nc, "PVAL"))[wde, ]
      }
    }
    close(nc)
    print(Sys.time())

    idxs <- c()
    uefvidxs <- c()
    minpvals <- c()
    maxtstats <- c()

    if (nrow(tstat) > 0) {
      idxsT <- apply(!is.na(tstat), 1, any)
      idxsP <- apply(!is.na(pval), 1, any)
      idxsBoth <- apply(cbind(idxsT, idxsP), 1, function(x){x[1]&x[2]})

      pval <- filterMatrix(pval, 1, idxsBoth)
      tstat <- filterMatrix(tstat, 1, idxsBoth)

      to <- min(nrow(pval), to)

      result <- orderByStatfilter(statfilter, tstat, pval);
      residxs <- result$rowidxs

      if (length(residxs) > 0) {
        to <- min(length(residxs), to)
        idxs <- result$rowidxs[from:to]
        uefvidxs <- result$colidxs[from:to]
        minpvals <- result$minpvals[from:to]
        maxtstats <- result$maxtstats[from:to]
      }
    }

    uefvs <- c()

    for (i in seq_along(uefvidxs)) {
      if (length(wuefv) > 1) {
        uefvs[i] <- uefv[wuefv[uefvidxs[i]]]
      } else {
        uefvs[i] <- uefv[wuefv]
      }
    }

    print(Sys.time())

    return(
      data.frame(
        deindexes = as.integer(wde[idxs]),
        geneids = as.integer(gn[wde[idxs]]),
        designelements = as.integer(de[wde[idxs]]),
        minpvals = minpvals,
        maxtstats = maxtstats,
        uefvs = uefvs
      )
   )
 }
})()
