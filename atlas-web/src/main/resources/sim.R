library(metarank)

sim.nc <-
function (de, nc, n=5, dist=c("euclidean", "pearson")) {
    eset <- read.aew.nc(nc)
    sim(de,eset,n,dist)
}


sim <-
function (de, eset, n=5, dist=c("euclidean", "pearson")) {
	corfn <- function(r1,r2) cor(r1,r2)
	eucfn <- function(r1,r2) -sqrt(sum(abs(r1-r2)^2))

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