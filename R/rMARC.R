#' @import rJava 
#' @import arules
#' @import arc

library(arules)
library(rJava)
library(arc)

#' qCBARuleModel
#'
#' @description  This class represents a qCBA rule-based classifier.
#' @name qCBARuleModel-class
#' @rdname qCBARuleModel-class
#' @exportClass qCBARuleModel
#' @slot rules object of class rules from arules package enhanced by qCBA
#' @slot classAtt name of the target class attribute
#' @slot attTypes attribute types
#' @slot rulePath path to file with rules, has priority over the rules slot
#' @slot ruleCount number of rules
qCBARuleModel <- setClass("qCBARuleModel",
                      slots = c(
                        rules = "data.frame",
                        classAtt ="character",
                        attTypes = "vector",
                        rulePath ="character",
                        ruleCount ="integer"
                      )
)


#' @title  Use the Iris dataset to test to test one rule classification qCBA workflow.
#' @description Learns a CBA classifier, performs qCBA Extension with continuous pruning and postpruning. Applies the model in one rule classification.
#'
#' @return Accuracy.
#' @export
#'
#'
qcbaIris <- function()
{
  set.seed(111)
  allData <- datasets::iris[sample(nrow(datasets::iris)),]
  trainFold <- allData[1:100,]
  testFold <- allData[101:nrow(datasets::iris),]
  rmCBA <- cba(trainFold, classAtt="Species")
  rmqCBA <- qcbaExtend(cbaRuleModel=rmCBA,datadf=trainFold,continuousPruning=TRUE, postpruning=TRUE, fuzzification=FALSE, annotate=FALSE)
  prediction <- predict(rmqCBA,testFold,"firstRule")
  acc <- CBARuleModelAccuracy(prediction, testFold[[rmqCBA@classAtt]])
  print(rmqCBA@rules)
  print(paste("Rule count:",rmqCBA@ruleCount))
  return(acc)
}

#' @title Use the Iris dataset to test multi rule qCBA workflow. 
#' @description Learns a CBA classifier, performs qCBA Extension with continuous pruning, postpruning, annotation and fuzzification. Applies the model in one rule classification.
#' The model  is saved to a temporary file. 
#'
#' @return Accuracy.
#' @export
#'
#'
qcbaIris2 <- function()
{
  set.seed(111)
  allData <- datasets::iris[sample(nrow(datasets::iris)),]
  trainFold <- allData[1:100,]
  testFold <- allData[101:nrow(datasets::iris),]
  rmCBA <- cba(trainFold, classAtt="Species")
  rmqCBA <- qcbaExtend(cbaRuleModel=rmCBA,datadf=trainFold,continuousPruning=TRUE, postpruning=TRUE, fuzzification=TRUE, annotate=TRUE,ruleOutputPath="rules.xml")
  prediction <- predict(rmqCBA,testFold,"mixture")
  acc <- CBARuleModelAccuracy(prediction, testFold[[rmqCBA@classAtt]])
  print(paste("Rule count:",rmqCBA@ruleCount))
  return(acc)
}


#' @title qCBA Extension with pruning options
#' @description Creates qCBA one rule or multi rule classication model from a CBA rule model
#' @export
#' @param cbaRuleModel a \link{CBARuleModel}
#' @param datadf data frame with training data
#' @param continuousPruning indicating  if continuous pruning is enabled
#' @param postpruning boolean indicating if postpruning is enabled
#' @param fuzzification boolean indicating if fuzzification is enabled. Multi rule classification model is produced if enabled. Fuzzification without annotation is not supported.
#' @param annotate boolean indicating if annotation with probability distributions is enabled, multi rule classification model is produced if enabled 
#' @param ruleOutputPath path of file to which model will be saved. Must be set if multi rule classification is produced.
#' @param loglevel logger level from java.util.logging
#'
#' @return Object of class \link{qCBARuleModel}.
#'
#' @examples
#' allData <- datasets::iris[sample(nrow(datasets::iris)),]
#' trainFold <- allData[1:100,]
#' testFold <- allData[101:nrow(datasets::iris),]
#' rmCBA <- cba(trainFold, classAtt="Species")
#' rmqCBA <- qcbaExtend(cbaRuleModel=rmCBA,datadf=trainFold)
#' print(rmqCBA@rules)

qcbaExtend <- function(cbaRuleModel,  datadf, continuousPruning=FALSE, postpruning=TRUE, fuzzification=FALSE, annotate=FALSE, ruleOutputPath, loglevel = "FINEST")
{
  if (fuzzification & !annotate)
  {
    stop("Fuzzification without annotation is not supported")
  }
  if (missing(ruleOutputPath) & ( annotate | fuzzification))
  {
    print("ruleOutputPath must be set when annotation or fuzzification is enabled")
    ruleOutputPath <- tempfile(pattern = "qcba-rules", tmpdir = tempdir(),fileext=".xml")
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
  
  #pass data to qCBA in Java
  idAtt <- ""
  
  hjw <- .jnew("eu.kliegr.ac1.R.RinterfaceExtend", attTypesArray,classAtt,idAtt, loglevel)
  out <- .jcall(hjw, , "addDataFrame", dataArray,cNames)
  out <- .jcall(hjw, , "addRuleFrame", rulesArray)
  
  #execute qCBA extend
  start.time <- Sys.time()
  out <- .jcall(hjw, , "extend", continuousPruning, postpruning, fuzzification, annotate)  
  end.time <- Sys.time()
  message (paste("qCBA Model building took:", round(end.time - start.time, 2), " seconds"))  
  
  rm <- qCBARuleModel()
  
  rm@classAtt <- classAtt
  rm@attTypes <- attTypes
  rm@ruleCount <- .jcall(hjw, "I" , "getRuleCount")
  
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

#' @title Aplies qCBARuleModel
#' @description Method that matches qCBA rule model. Supports both one rule and multi rule classification.
#'
#' @param object \link{qCBARuleModel} class instance
#' @param newdata data frame with data
#' @param testingType either "mixture" for multi rule classification or "firstRule" for one rule classification. Applicable only when model is loaded from file.
#' @param loglevel logger level from java.util.logging
#' @param ... other arguments (currently not used)
#' @return vector with predictions.
#' @export
#' @method predict qCBARuleModel
#' @examples
#' allData <- datasets::iris[sample(nrow(datasets::iris)),]
#' trainFold <- allData[1:100,]
#' testFold <- allData[101:nrow(datasets::iris),]
#' rmCBA <- cba(trainFold, classAtt="Species")
#' rmqCBA <- qcbaExtend(cbaRuleModel=rmCBA,datadf=trainFold)
#' print(rmqCBA@rules)
#' prediction <- predict(rmqCBA,testFold)
#' acc <- CBARuleModelAccuracy(prediction, testFold[[rmqCBA@classAtt]])
#' message(acc)
#' 
#' @seealso \link{qcbaExtend}
#'
#'
predict.qCBARuleModel <- function(object, newdata, testingType, loglevel = "INFO", ...) 
{
  start.time <- Sys.time()
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
  
  
  #pass data to qCBA Java
  #the reason why we cannot use predict.RuleModel in arc package is that the items in the rules do not match the itemMatrix after R extend
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
    print("Using rules stored in the passed model")
    extRulesJArray <- .jarray(lapply(ruleModel@rules, .jarray))
    .jcall(jPredict, , "addRuleFrame", extRulesJArray)
    prediction <- .jcall(jPredict, "[Ljava/lang/String;", "predict")
  }
  end.time <- Sys.time()
  message (paste("Prediction (qCBA model application) took:", round(end.time - start.time, 2), " seconds"))  
  return(prediction)
}

#' @title Map R types to qCBA
#' @description Map data types between R and qCBA
#' @export
#' @param Rtypes Vector with R data types
#'
#' @return Vector with qCBA data types
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