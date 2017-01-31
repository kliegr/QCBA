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
#' @slot rulePath path to file with rules, has priority over the rules slot
MARCRuleModel <- setClass("MARCRuleModel",
                      slots = c(
                        rules = "data.frame",
                        classAtt ="character",
                        attTypes = "vector",
                        rulePath ="character"
                      )
)


#' @title Test simple MARC Workflow with CBA input on Iris Dataset, , the result of MARC is printed
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
  rmMARC <- marcExtend(cbaRuleModel=rmCBA,datadf=trainFold,continuousPruning=TRUE, postpruning=TRUE, fuzzification=FALSE, annotate=FALSE)
  prediction <- predict(rmMARC,testFold,"mixture")
  acc <- CBARuleModelAccuracy(prediction, testFold[[rmMARC@classAtt]])
  print(rmMARC@rules)
  return(acc)
}

#' @title Test MARC more complex workflow with annotation and fuzzification, the result of MARC is saved to file
#' @description Test workflow on iris dataset: learns a CBA classifier on one "train set" fold, and applies it to the second  "test set" fold.
#'
#' @return Accuracy.
#' @export
#'
#'
marcIris2 <- function()
{
  set.seed(111)
  allData <- datasets::iris[sample(nrow(datasets::iris)),]
  trainFold <- allData[1:100,]
  testFold <- allData[101:nrow(datasets::iris),]
  rmCBA <- cba(trainFold, classAtt="Species")
  rmMARC <- marcExtend(cbaRuleModel=rmCBA,datadf=trainFold,continuousPruning=TRUE, postpruning=TRUE, fuzzification=TRUE, annotate=TRUE,ruleOutputPath="rules.xml")
  prediction <- predict(rmMARC,testFold,"mixture")
  acc <- CBARuleModelAccuracy(prediction, testFold[[rmMARC@classAtt]])

  return(acc)
}


#' @title MARC extend and prune
#' @description Processes CBA rule set  with MARC (extend+prune).
#' @export
#' @param cbaRuleModel a \link{CBARuleModel}
#' @param datadf data frame with training data
#' @param continuousPruning boolean indicating if transactions covered by an extended rule should be removed and thus not available for extensionn of rules with lower priority
#' @param postpruning boolean indicating if rules should be pruned after extension and before annotation
#' @param fuzzification boolean indicating if rules should be fuzzified after extension and before annotation
#' @param annotate boolean indicating if rules should be annotated with distributions
#' @param ruleOutputPath path to write resulting rules to
#' @param loglevel logger level from java.util.logging
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

marcExtend <- function(cbaRuleModel,  datadf, continuousPruning=FALSE, postpruning=TRUE, fuzzification=FALSE, annotate=FALSE, ruleOutputPath, loglevel = "FINEST")
{
  
  if (fuzzification & !annotate)
  {
    stop("Fuzzification without annotation is not supported")
  }
  if (missing(ruleOutputPath) & ( annotate | fuzzification))
  {
    print("ruleOutputPath must be set when annotation or fuzzification is enabled")
    ruleOutputPath <- tempfile(pattern = "marc-rules", tmpdir = tempdir(),fileext=".xml")
    print(paste("setting it to '",ruleOutputPath,"'"))
  }
  
  
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
  
  hjw <- .jnew("eu.kliegr.ac1.R.RinterfaceExtend", attTypesArray,classAtt,idAtt, loglevel)
  out <- .jcall(hjw, , "addDataFrame", dataArray,cNames)
  out <- .jcall(hjw, , "addRuleFrame", rulesArray)
  
  #execute MARC extend
  out <- .jcall(hjw, , "extend", continuousPruning, postpruning, fuzzification, annotate)  
  
  rm <- MARCRuleModel()
  
  rm@classAtt <- classAtt
  rm@attTypes <- attTypes
  
  if (annotate)
  {
    out <- .jcall(hjw, , "saveToFile", ruleOutputPath)    
    rm@rulePath <- ruleOutputPath
  }
  else
  {
    
    #parse results into R structures
    extRulesArray <- .jcall(hjw, "[[Ljava/lang/String;", "getRules", evalArray=FALSE)
    extRules <- .jevalArray(extRulesArray,simplify=TRUE)   
    colnames(extRules) <- c("rules","support","confidence")
    extRulesFrame<-as.data.frame(extRules,stringsAsFactors=FALSE)
    extRulesFrame$support<-as.numeric(extRulesFrame$support)
    extRulesFrame$confidence<-as.numeric(extRulesFrame$confidence)
    
    rm@rulePath <- ""
    rm@rules <- extRulesFrame
    
    if (!missing(ruleOutputPath))
    {
      write.csv(extRulesFrame, ruleOutputPath, row.names=TRUE,quote = TRUE)
      
    }
    
  }
  
  return(rm)
}



#' Apply Rule Model
#' @description Method that matches rule model against test data.
#'
#' @param object a \link{MARCRuleModel} class instance
#' @param newdata a data frame with data
#' @param testingType either "mixture" or "firstRule", applicable only when rule set is an annotated model stored in a file
#' @param loglevel logger level from java.util.logging
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
predict.MARCRuleModel <- function(object, newdata, testingType, loglevel = "INFO", ...) 
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
  
  
  #pass data to MARC Java
  #the reason why we cannot use e.g. predict.RuleModel in arc package is that the items in the rules do not match the itemMatrix after R extend
  idAtt <- ""
  jPredict <- .jnew("eu.kliegr.ac1.R.RinterfacePredict", attTypesArray, ruleModel@classAtt, idAtt,loglevel)
  .jcall(jPredict, , "addDataFrame", testArray,cNames)
  
  
  if (nchar(ruleModel@rulePath)>0)
  {
    print(paste("Loading rule model from file:",ruleModel@rulePath ))
    prediction <- .jcall(jPredict, "[Ljava/lang/String;", "predictWithRulesFromFile", ruleModel@rulePath, testingType)
  }
  else
  {
    # using ruls stored in  ruleModel
    extRulesJArray <- .jarray(lapply(ruleModel@rules, .jarray))
    .jcall(jPredict, , "addRuleFrame", extRulesJArray)
    #execute predict
    prediction <- .jcall(jPredict, "[Ljava/lang/String;", "predict")
  }

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