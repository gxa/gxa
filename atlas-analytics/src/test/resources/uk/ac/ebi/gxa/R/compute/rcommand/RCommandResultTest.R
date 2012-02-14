asDataFrame <-
   function(list) {
       res <- data.frame(
          testColumn = list,
          numericColumn = as.numeric(list),
          integerColumn = as.integer(list),
          stringColumn = as.character(rep('test', length(list)))
       )
       attr(res, "intAttr") <- as.integer(10)
       attr(res, "nullAttr") <- NULL
       attr(res, "emptyAttr") <- c()
       return(res);
   }

returnEmptyDataFrame <<-
   function() {
      return(asDataFrame(c()))
   }

returnNonEmptyDataFrame <<-
   function() {
      return(asDataFrame(c(1.2, 2.3, 3.4)))
   }