esetToTextFile = function(infname, outfname) {

   require(Biobase)

   objnames = load(infname);

   expressions = exprs(get(objnames[1]));

   dimensionnames = dimnames(expressions);

   write(c("scanref", dimensionnames$sample), file = outfname,
           ncolumns = length(dimensionnames$sample) + 1, sep = "\t");

   write.table(expressions, file = outfname, sep = "\t",
           quote = FALSE, row.names = TRUE, col.names = FALSE, append = TRUE)
}