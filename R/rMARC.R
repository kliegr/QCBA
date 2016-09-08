#' @import rJava 
#' @import arules
test <- function(){
   train<-data("iris")
   irisdf <- data.frame(train, check.names=FALSE)
   train <- sapply(iris, as.factor)
   train <- data.frame(train, check.names=FALSE)
   txns <- as(train,"transactions")
  
   rules = apriori(txns, parameter=list(support=0.03, confidence=0.03, minlen=2),
   appearance = list(rhs=c("Species=setosa", "Species=versicolor", "Species=virginica"),default="lhs"))
   rulesFrame <- as(rules,"data.frame")
   rulesFrame$rules <- as.character(rulesFrame$rules)
   rulesArray <- .jarray(lapply(rulesFrame, .jarray))
   irisArray <-  .jarray(lapply(irisdf, .jarray))
   trainConverted <- data.frame(lapply(train, as.character), stringsAsFactors=FALSE)
   trainArray <-  .jarray(lapply(trainConverted, .jarray))
   
   cNames <- .jarray(colnames(train))
   att_types <- .jarray(unname(sapply(iris, class)))
   targetColName <-"Species" 
   IDcolumnName <- ""
   
  
   
   hjw <- .jnew("eu.kliegr.ac1.R.RinterfaceExtend", att_types,targetColName,IDcolumnName)
   out <- .jcall(hjw, , "addDataFrame", trainArray,cNames)
   out <- .jcall(hjw, , "addRuleFrame", rulesArray)
   out <- .jcall(hjw, , "extend")
   
   extRulesArray <- .jcall(hjw, "[[Ljava/lang/String;", "getRules", evalArray=FALSE)
   extRules <- .jevalArray(extRulesArray,simplify=TRUE)   
   colnames(extRules) <- c("rules","support","confidence")
   extRulesFrame<-as.data.frame(extRules,stringsAsFactors=FALSE)
   extRulesFrame$support<-as.numeric(extRulesFrame$support)
   extRulesFrame$confidence<-as.numeric(extRulesFrame$confidence)
   
  
   jPredict <- .jnew("eu.kliegr.ac1.R.RinterfacePredict", att_types,targetColName,IDcolumnName)
   .jcall(jPredict, , "addDataFrame", trainArray,cNames)
   
   extRulesJArray <- .jarray(lapply(extRulesFrame, .jarray))
   .jcall(jPredict, , "addRuleFrame", extRulesJArray)
   
   prediction <- .jcall(jPredict, "[Ljava/lang/String;", "predict")
   return(out)
}


