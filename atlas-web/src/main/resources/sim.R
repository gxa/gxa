### Atlas Analytics Original Driver Functions
### Author: ostolop

### Reads data from Atlas NetCDF file and returns an ExpressionSet
read.atlas.nc <- function (filename, accnum=NULL) {
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
    # deacc = get.var.ncdf(nc,"DEacc")
    gn  = get.var.ncdf(nc,"GN")

    # make de's unique
    de[de==0] <- -(1:length(which(de==0)))


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
#   MK: remove this for now, to prevent analytics running on SC's
#    for(sc in colnames(scv)) {
#      scvj <- as.factor(unlist(lapply(rownames(efv), function(ef)
#                                      paste(unique(scv[colnames(b2a)[as.logical(b2a[ef,])],sc]),
#                                            ## colnames(b2a)[as.logical(b2a[ef,])],
#                                            sep=":", collapse="|"))))
#
#      ef <- sub("bs_","ba_",sc)
#      if( !identical(efscv[[ef]], scvj)) {
#        efscv[[sc]] <- scvj
#      }
#    }

    fDataFrame = data.frame(gn=gn,de=de) #, deacc=deacc)
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

    aData <- assayDataNew(storage.mode="lockedEnvironment", exprs=bdc)
    featureNames(aData) <- de
    sampleNames(aData) <- as

    print(paste("Computed phenoData for", paste(varLabels(pData),collapse=", "), nrow(pData), "x", ncol(pData)))
    return(new("ExpressionSet", assayData=aData, phenoData=pData, featureData=fData, experimentData=eData))
  }

sim.nc <-
function (de, nc, n=5, dist=c("euclidean", "pearson")) {
    eset <- read.atlas.nc(nc)
    sim(de,eset,n,dist)
}


sim <-
function (de, eset, n=5, dist=c("euclidean", "pearson")) {
	corfn <- function(r1,r2) cor(r1,r2)
	eucfn <- function(r1,r2) - sqrt(sum(abs(r1-r2)^2))

	distfn <- match.arg(dist)
	if(distfn=="euclidean")
		distfn <- eucfn
	else
		distfn <- corfn

	des   <- featureNames(eset)
	exprs <- exprs(eset)

    w <- which(des==de)
    stopifnot(length(w)== 1)

	thisde <- exprs[w,]

	dists <- apply(exprs, 1, distfn, thisde)
	dists <- sort(dists, decreasing=TRUE)[1:n]

	data.frame(
		fData(eset)[which(des %in% names(dists)),],
		dist=abs(dists)
	)
}