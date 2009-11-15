### Atlas Analytics Original Driver Functions
### Author: ostolop


### Reads data from Atlas NetCDF file and returns an ExpressionSet
### Post-AEW migration TODOs:
### * NChannelSet
### * Multiple array designs
read.atlas.nc <-
  function (filename, accnum=NULL) {
    require(ncdf)
    require(Biobase)
    require(gtools)

    nc = open.ncdf(filename)

    bdc = get.var.ncdf(nc,"BDC")
    as  = get.var.ncdf(nc,"AS")
    bs  = get.var.ncdf(nc,"BS")
    b2a = get.var.ncdf(nc,"BS2AS")

    ef  = get.var.ncdf(nc,"EF")
    efv = get.var.ncdf(nc,"EFV")
    sc  = get.var.ncdf(nc,"SC")
    scv = get.var.ncdf(nc,"SCV")

    de  = get.var.ncdf(nc,"DE")
    gn  = get.var.ncdf(nc,"GN")

    accnum = att.get.ncdf(nc,varid=0,"experiment_accession")$value
    qt     = att.get.ncdf(nc,varid=0,"quantitationType")$value
    adacc  = att.get.ncdf(nc,varid=0,"ADaccession")$value
    adname = att.get.ncdf(nc,varid=0,"ADname")$value

    colnames(efv) = ef
    rownames(efv) = as
    efv = data.frame(efv)

    colnames(scv) = sc
    rownames(scv) = bs
    scv = data.frame(scv)

    for ( i in 1:length(ef) ) {
      levels(efv[[i]]) = mixedsort(levels(efv[[i]]))
    }

    for ( i in 1:length(sc) ) {
      levels(scv[[i]]) = mixedsort(levels(scv[[i]]))
    }

    bdc[bdc<=-1e6] = NA

    bdc = t(bdc)
    rownames(bdc) = de
    colnames(bdc) = as
    
    rownames(b2a) = as
    colnames(b2a) = bs

    print(paste("Read in", accnum))
    print(paste("Read in BDC:",nrow(bdc),"x",ncol(bdc)))
    print(paste("Read in EFV:",nrow(efv),"x",ncol(efv)))
    print(paste("Read in SCV:",nrow(scv),"x",ncol(scv)))


    ncinfo = unlist(strsplit(basename(filename),"_|[.]"))
    exptid = ncinfo[1]
    arraydesignid = ncinfo[2]


    efscv <- efv
    for(sc in colnames(scv)) {
      scvj <- as.factor(unlist(lapply(rownames(efv), function(ef)
                                      paste(unique(scv[colnames(b2a)[as.logical(b2a[ef,])],sc]), 
                                            ## colnames(b2a)[as.logical(b2a[ef,])], 
                                            sep=":", collapse="|"))))
      
      ef <- sub("bs_","ba_",sc)
      if( !identical(efscv[[ef]], scvj)) {        
        efscv[[sc]] <- scvj
      }
    }
    
    fDataFrame = data.frame(gn=gn,de=de)
    fData = new("AnnotatedDataFrame", data=fDataFrame)
    featureNames(fData) = de
    pData  = new("AnnotatedDataFrame", data=efscv)
    scData = new("AnnotatedDataFrame", data=scv)
    eData = new("MIAME", 
      other=list(accession=accnum, 
        experimentid=exptid, 
        arraydesignid=arraydesignid, 
        qt=qt, 
        arraydesignaccnum=adacc, 
        arraydesignname=adname
        )
      )
    
    attr(eData, "scv") <- scv           
    attr(eData, "b2a") <- b2a
    
    print(paste("Computed phenoData for", paste(varLabels(pData),collapse=", "), nrow(pData), "x", ncol(pData)))
    return(new("ExpressionSet", exprs=bdc, phenoData=pData, featureData=fData, experimentData=eData))
  }

### Log function that does not return NAs
log2.safe <- function(x,log=T) {
  if(!log) 
    return(x)
  tmp=log2(x)
  tmp[!is.finite(tmp)]=0
  tmp
}

### Omnibus one-way ANOVA (with moderated t) F-statistic computation
fstat.eset <- function(eset, design=NULL, varLabel=NULL,lg=FALSE) {
  if(is.null(design) && !is.null(varLabel)) {
    print(paste("Calculating design matrix for", varLabel))
    design = model.matrix(as.formula(paste("~0+", varLabel)), data=eset)  
  }
  
  if(lg==TRUE) {
    exprs(eset)<-log2.safe(exprs(eset))
  }
  
  print("Fitting model...")
  fit=lmFit(eset,design)
  
  print("Re-fitting model to ANOVA contrasts...")
  pairs=design.pairs(colnames(design))
  cfit=contrasts.fit(fit,pairs)
  
  print("Moderating...")
  cfit=eBayes(cfit)
  fit=eBayes(fit)
  
  fit$F=cfit$F
  fit$F.p.value=cfit$F.p.value
  
  return(fit)
}

### Moderated t one-way ANOVA post-hoc testing with global FDR adjustment
allupdn <- function (eset, alpha=0.01, evars=varLabels(eset) ) {
  require(limma)
  
  exprs = exprs(eset)
  if(max(exprs,na.rm=TRUE)>1000 && min(exprs,na.rm=TRUE	)>=0) {
    print("Taking log2 of the expression matrix")
    exprs(eset) = log2.safe(exprs)
  }
  
  allFits = list()
  
  for(varLabel in evars){
    try({
      print(paste("Calculating lmFit and F-stats for", varLabel))
      if(length(levels(eset[[varLabel, exact=TRUE]]))<2 || length(levels(eset[[varLabel, exact=TRUE]])) == ncol(exprs) ) { next }
      thisFit = fstat.eset(eset,varLabel=varLabel)
      
      print("Adjusting p-values")
      pp = p.adjust(thisFit$F.p.value,method="fdr")
      w=which(pp<=alpha)
      
      thisFit$F.p.value.adj=pp
      
      n = ncol(thisFit$design)
      cm = diag(n)-1/n
      
      contr.fit=contrasts.fit(thisFit,cm)
      contr.fit=eBayes(contr.fit)
      
      dec = decideTests(contr.fit,method="global", adjust.method="fdr")
      colnames(dec) = levels(eset[[varLabel, exact=TRUE]])
      
      thisFit$which=w
      thisFit$boolupdn=dec
      thisFit$contr.fit=contr.fit
      
      allFits[[varLabel]] = thisFit
      print("Done.")
    })
  }
  
  allFits
}

### Atlas analytics processing driver: read the data, compute the linear fit, post-hoc test, adjust and write to tab-delim files
process.atlas.nc<-
  function (nc)
{
  eset = read.atlas.nc(nc)
  info = otherInfo(experimentData(eset))
  proc = allupdn(eset)

  print("Writing out the results")
  for(varLabel in varLabels(eset)) {
    if(!is.null(proc[[varLabel, exact=TRUE]]$contr.fit)) {
      fitfile <-  paste(info$accession,"_",info$experimentid,"_",info$arraydesignid,"_",varLabel,"_","fit.tab",sep="")
      tab <- list()
      tab$A <- proc[[varLabel, exact=TRUE]]$Amean
                                        #		    tab$Coef <- proc[[varLabel, exact=TRUE]]$contr.fit$coef
      tab$t <- proc[[varLabel, exact=TRUE]]$contr.fit$t
      tab$p.value <- as.matrix(proc[[varLabel, exact=TRUE]]$contr.fit$p.value)
      
      pv = tab$p.value
      o = !is.na(tab$p.value)
      pv[o] = p.adjust(pv[o], method="fdr")
      
      tab$p.value.adj = pv
      tab$Res <- unclass(proc[[varLabel, exact=TRUE]]$boolupdn)
      tab$F <- proc[[varLabel, exact=TRUE]]$F
      tab$F.p.value <- proc[[varLabel, exact=TRUE]]$F.p.value
      tab$F.p.value.adj = proc[[varLabel, exact=TRUE]]$F.p.value.adj
      tab$Genes <- proc[[varLabel, exact=TRUE]]$genes
      tab <- data.frame(tab, check.names = FALSE)
      write.table(tab, file = fitfile, quote = FALSE, row.names = FALSE, sep = "\t")
      print(paste("Wrote",fitfile)) 
    }
  }
}

### Atlas analytics, returns instead of writing
process.atlas.nc2 <-
function (nc)
{
  eset = read.atlas.nc(nc)
  info = otherInfo(experimentData(eset))
  proc = allupdn(eset)

  print("Writing out the results")
  result <- lapply(varLabels(eset), function(varLabel) {
    if(!is.null(proc[[varLabel, exact=TRUE]]$contr.fit)) { 
      fitfile <-  paste(info$accession,"_",info$experimentid,"_",info$arraydesignid,"_",varLabel,"_","fit.tab",sep="")
      tab <- list()
      tab$A <- proc[[varLabel, exact=TRUE]]$Amean
                                        #		    tab$Coef <- proc[[varLabel, exact=TRUE]]$contr.fit$coef
      tab$t <- proc[[varLabel, exact=TRUE]]$contr.fit$t
      tab$p.value <- as.matrix(proc[[varLabel, exact=TRUE]]$contr.fit$p.value)
      
      pv = tab$p.value
      o = !is.na(tab$p.value)
      pv[o] = p.adjust(pv[o], method="fdr")
      
      tab$p.value.adj = pv
      tab$Res <- unclass(proc[[varLabel, exact=TRUE]]$boolupdn)
      tab$F <- proc[[varLabel, exact=TRUE]]$F
      tab$F.p.value <- proc[[varLabel, exact=TRUE]]$F.p.value
      tab$F.p.value.adj = proc[[varLabel, exact=TRUE]]$F.p.value.adj
      tab$Genes <- proc[[varLabel, exact=TRUE]]$genes
      tab <- data.frame(tab, check.names = FALSE)
   ##   write.table(tab, file = fitfile, quote = FALSE, row.names = FALSE, sep = "\t")
     ## print(paste("Wrote",fitfile)) 
     return (tab)
    }
    else {
    	return(NULL)
    	}
  })
  
  names(result) <- varLabels(eset)
  return(result)
}

### Compute a design matrix for making all possible pairwise comparisons (one-way ANOVA F).
design.pairs <- function(levels) {
  n <- length(levels)
  design <- matrix(0,n,choose(n,2))
  rownames(design) <- levels
  colnames(design) <- 1:choose(n,2)
  k <- 0
  for (i in 1:(n-1))
    for (j in (i+1):n) {
      k <- k+1
      design[i,k] <- 1
      design[j,k] <- -1
      colnames(design)[k] <- paste(levels[i],"-",levels[j],sep="")
    }
  
   design
}

