#' @import rJava 
#' @import arules
#' @import arc
#' 
library(arules)
library(rJava)
library(arc)

#' MARCRuleModel
#'
#' @description  This class represents a MARC rule-based classifier.


#' @name MARCRuleModel-class
#' @rdname MARCRuleModel-class
#' @exportClass MARCRuleModel
#' @slot rules an object of class rules from arules package enhanced by MARC
#' @slot classAtt name of the target class attribute
#' @slot attTypes attribute types
MARCRuleModel <- setClass("MARCRuleModel",
                      slots = c(
                        rules = "data.frame",
                        classAtt ="character",
                        attTypes = "vector"
                      )
)


#' @title Test MARC Workflow with CBA input on Iris Dataset
#' @description Test workflow on iris dataset: learns a cba classifier on one "train set" fold , and applies it to the second  "test set" fold.
#'
#' @return Accuracy.
#' @export
#'
#'
marcIris <- function()
{
  set.seed(111)
  allData <- datasets::iris[sample(nrow(datasets::iris)),]
  trainFold <- allData[1:100,]
  testFold <- allData[101:nrow(datasets::iris),]
  rmCBA <- cba(trainFold, classAtt="Species")
  rmMARC <- marcExtend(cbaRuleModel=rmCBA,datadf=trainFold)
  prediction <- predict(rmMARC,testFold)
  acc <- CBARuleModelAccuracy(prediction, testFold[[rmMARC@classAtt]])
  print(rmMARC@rules)
  return(acc)
}


#' @title MARC extend and prune
#' @description Processes CBA rule set  with MARC (extend+prune).
#' @export
#' @param cbaRuleModel a \link{CBARuleModel}
#' @param datadf data frame with training data
#'
#' @return Object of class \link{MARCRuleModel}.
#'
#' @examples
#' allData <- datasets::iris[sample(nrow(datasets::iris)),]
#' trainFold <- allData[1:100,]
#' testFold <- allData[101:nrow(datasets::iris),]
#' rmCBA <- cba(trainFold, classAtt="Species")
#' rmMARC <- marcExtend(cbaRuleModel=rmCBA,datadf=trainFold)
#' print(rmMARC@rules)

marcExtend <- function(cbaRuleModel,  datadf)
{
  #ensure that any NA or null values are replaced by empty string
  datadf[is.na(datadf)] <- ''
  datadf[is.null(datadf)] <- ''
  
  rules=cbaRuleModel@rules
  classAtt=cbaRuleModel@classAtt
  
  #reshape R data for Java call
  rulesFrame <- as(rules,"data.frame")
  rulesFrame$rules <- as.character(rulesFrame$rules)
  rulesArray <- .jarray(lapply(rulesFrame, .jarray))
  datadfConverted <- data.frame(lapply(datadf, as.character), stringsAsFactors=FALSE)
  
  #cast R data to Java structures
  dataArray <-  .jarray(lapply(datadfConverted, .jarray))
  cNames <- .jarray(colnames(datadf))
  
  attTypes <- mapDataTypes(cbaRuleModel@attTypes)
  attTypesArray <- .jarray(unname(attTypes))
  
  
  #pass data to MARC in Java
  idAtt <- ""
  loglevel <- "INFO"
  hjw <- .jnew("eu.kliegr.ac1.R.RinterfaceExtend", attTypesArray,classAtt,idAtt, loglevel)
  out <- .jcall(hjw, , "addDataFrame", dataArray,cNames)
  out <- .jcall(hjw, , "addRuleFrame", rulesArray)
  
  #execute MARC extend
  out <- .jcall(hjw, , "extend")  
  
  #parse results into R structures
  extRulesArray <- .jcall(hjw, "[[Ljava/lang/String;", "getRules", evalArray=FALSE)
  extRules <- .jevalArray(extRulesArray,simplify=TRUE)   
  colnames(extRules) <- c("rules","support","confidence")
  extRulesFrame<-as.data.frame(extRules,stringsAsFactors=FALSE)
  extRulesFrame$support<-as.numeric(extRulesFrame$support)
  extRulesFrame$confidence<-as.numeric(extRulesFrame$confidence)
  
  rm <- MARCRuleModel()
  rm@rules <- extRulesFrame
  rm@classAtt <- classAtt
  rm@attTypes <- attTypes
  return(rm)
}


#' Apply Rule Model
#' @description Method that matches rule model against test data.
#'
#' @param object a \link{MARCRuleModel} class instance
#' @param newdata a data frame with data
#' @param ... other arguments (currently not used)
#' @return A vector with predictions.
#' @export
#' @method predict MARCRuleModel
#' @examples
#' allData <- datasets::iris[sample(nrow(datasets::iris)),]
#' trainFold <- allData[1:100,]
#' testFold <- allData[101:nrow(datasets::iris),]
#' rmCBA <- cba(trainFold, classAtt="Species")
#' rmMARC <- marcExtend(cbaRuleModel=rmCBA,datadf=trainFold)
#' print(rmMARC@rules)
#' prediction <- predict(rmMARC,testFold)
#' acc <- CBARuleModelAccuracy(prediction, testFold[[rmMARC@classAtt]])
#' message(acc)
#' 
#' @seealso \link{marcExtend}
#'
#'
predict.MARCRuleModel <- function(object, newdata,...) 
{
  ruleModel <- object
  
  newdata[is.na(newdata)] <- ''
  newdata[is.null(newdata)] <- ''
  
  #reshape and cast test data to Java structures
  testConverted <- data.frame(lapply(newdata, as.character), stringsAsFactors=FALSE)
  cNames <- .jarray(colnames(newdata))
  
  #reusing attribute types from training data
  attTypes <- ruleModel@attTypes
  attTypesArray <- .jarray(unname(attTypes))
  
  #attTypesArray <- .jarray(unname(sapply(newdata, class)))
  testArray <-  .jarray(lapply(testConverted, .jarray))
  extRulesJArray <- .jarray(lapply(ruleModel@rules, .jarray))
  
  #pass data to MARC Java
  #the reason why we cannot use e.g. predict.RuleModel in arc package is that the items in the rules do not match the itemMatrix after R extend
  idAtt <- ""
  loglevel <- "INFO"
  jPredict <- .jnew("eu.kliegr.ac1.R.RinterfacePredict", attTypesArray, ruleModel@classAtt, idAtt,loglevel)
  .jcall(jPredict, , "addDataFrame", testArray,cNames)
  .jcall(jPredict, , "addRuleFrame", extRulesJArray)
  
  #execute predict
  prediction <- .jcall(jPredict, "[Ljava/lang/String;", "predict")
  return(prediction)
}


#' @title Map R types to MARC
#' @description Maps  data types from R data frame to data types used in R
#' @export
#' @param Rtypes a vector with R data  types
#'
#' @return Vector with MARC data types
#'
#' @examples
#' mapDataTypes(unname(sapply(iris, class)))


mapDataTypes<- function (Rtypes)
{
  newTypes<-Rtypes
  newTypes[TRUE]<-"nominal"
  newTypes[Rtypes=="numeric"] <-"numerical"
  newTypes[Rtypes=="integer"] <-"numerical"
  return(newTypes)
}