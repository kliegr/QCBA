#' @import arc
#' @import utils
#' @importFrom methods as new
#' @importFrom rJava .jcall .jnew .jarray .jevalArray
#' @importFrom arules apriori inspect
#' @importFrom stats predict

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
#' @slot history extension history
#' @slot classAtt name of the target class attribute
#' @slot attTypes attribute types
#' @slot rulePath path to file with rules, has priority over the rules slot
#' @slot ruleCount number of rules
qCBARuleModel <- setClass("qCBARuleModel",
                      slots = c(
                        rules = "data.frame",
                        history = "data.frame",
                        classAtt ="character",
                        attTypes = "vector",
                        rulePath ="character",
                        ruleCount ="integer"
                      )
)



#' rCBARuleModel
#'
#' @description  This class represents an CBA rule-based classifier, where rules are represented as string vector in a data frame
#' @name customCBARuleModel-class
#' @rdname customCBARuleModel-class
#' @exportClass customCBARuleModel
#' @slot rules dataframe output by rCBA
#' @slot cutp list of cutpoints
#' @slot classAtt name of the target class attribute
#' @slot attTypes attribute types
customCBARuleModel <- setClass("customCBARuleModel",
                          slots = c(
                            rules = "data.frame",
                            cutp = "list",
                            classAtt ="character",
                            attTypes = "vector"
                          )
)

#' @title  Use the Humidity-Temperature toy dataset from the arc package to test one rule classification QCBA workflow.
#' @description TODO
#'
#' @return QCBA model
#' @export
#'
#'
qcbaHumTemp <- function()
{
  data_raw<-arc::humtemp
  data_discr <-arc::humtemp
  #custom discretization
  data_discr[,1]<-cut(data_raw[,1],breaks=seq(from=15,to=45,by=5))
  data_discr[,2]<-cut(data_raw[,2],breaks=c(0,40,60,80,100))
  #change interval syntax from (15,20] to (15;20], which is required by MARC
  data_discr[,1]<-as.factor(unlist(lapply(data_discr[,1], function(x) {gsub(",", ";", x)})))
  data_discr[,2]<-as.factor(unlist(lapply(data_discr[,2], function(x) {gsub(",", ";", x)})))

  data_discr[,3] <- as.factor(data_raw[,3])

  txns <- as(data_discr, "transactions")
  rules <- apriori(txns, parameter = list(confidence = 0.75, support= 3/nrow(data_discr), minlen=1, maxlen=5))
  print("Seed list of rules")
  inspect(rules)

  classAtt="Class"
  appearance <- getAppearance(data_discr, classAtt)
  rmCBA <- cba_manual(data_raw,  rules, txns, appearance$rhs, classAtt, cutp= list(), pruning_options=NULL)
  print("CBA classifier")
  inspect(rmCBA@rules)
  prediction_cba<-predict(rmCBA,data_discr,discretize=FALSE)
  acc_cba <- CBARuleModelAccuracy(prediction_cba, data_discr[[classAtt]])
  print(paste("Accuracy (CBA):",acc_cba))

  rmqCBA <- qcba(cbaRuleModel=rmCBA,datadf=data_raw, trim_literal_boundaries=TRUE, attributePruning  = FALSE, extendType="numericOnly", postpruning="cba", defaultRuleOverlapPruning="transactionBased") 
  prediction <- predict(rmqCBA,data_raw)

  acc <- CBARuleModelAccuracy(prediction, data_raw[[rmqCBA@classAtt]])
  print("QCBA classifier")
  print(rmqCBA@rules)
  
  print(paste("Accuracy (QCBA):",acc))
  return(rmqCBA)
}


#' @title  Use the Iris dataset to test one rule classification QCBA workflow.
#' @description Learns a CBA classifier, performs all qCBA postprocessing steps for one rule classification.
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
  rmqCBA <- qcba(cbaRuleModel=rmCBA,datadf=trainFold, trim_literal_boundaries=TRUE, attributePruning  = TRUE, extendType="numericOnly", postpruning="cba", defaultRuleOverlapPruning="transactionBased")
  prediction <- predict(rmqCBA,testFold)
  acc <- CBARuleModelAccuracy(prediction, testFold[[rmqCBA@classAtt]])
  print(rmqCBA@rules)
  print(paste("Rule count:",rmqCBA@ruleCount))
  return(acc)
}

#' @title Use the Iris dataset to test multi rule qCBA workflow.
#' @description Learns a CBA classifier, performs qCBA with the experimental multi rule classification workflow including annotation and fuzzification. Applies the model with rule mixture classification.
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
  rmqCBA <- qcba(cbaRuleModel=rmCBA,datadf=trainFold,extendType="numericOnly",trim_literal_boundaries=TRUE, postpruning="cba", defaultRuleOverlapPruning = "rangeBased", fuzzification=TRUE, annotate=TRUE,ruleOutputPath=paste(tempdir(),"rules.xml",sep=.Platform$file.sep))
  prediction <- predict(rmqCBA,testFold,"mixture")
  acc <- CBARuleModelAccuracy(prediction, testFold[[rmqCBA@classAtt]])
  print(paste("Rule count:",rmqCBA@ruleCount))
  return(acc)
}


#' @title rcbaModel2CustomCBAModel Converts a model created by rCBA so that it can be passed to qCBA
#' @description Creates instance of CustomCBAModel class based on model created by the rCBA package.
#' This instance can then be passed to qcba() instead of CBARuleModel created with the arc package.
#' @export
#' @param rcbaModel aobject returned  by rCBA::build
#' @param cutPoints specification of cutpoints applied on the data before they were passed to rCBA::build
#' @param classAtt the name of the class attribute
#' @param rawDataset the raw data (before discretization). This dataset is used to guess attribute types if attTypes is not passed
#' @param attTypes vector of attribute types of the original data.  If set to null, you need to pass rawDataset.
#' @examples
#' if (! requireNamespace("rCBA", quietly = TRUE)) {
#'  message("Please install rCBA: install.packages('rCBA')")
#' } else {
#'  discrModel <- discrNumeric(iris, "Species")
#'  irisDisc <- as.data.frame(lapply(discrModel$Disc.data, as.factor))
#'  rCBAmodel <- rCBA::build(irisDisc)
#'  cCBAmodel <- rcbaModel2CustomCBAModel(rCBAmodel,discrModel$cutp,"Species",iris)
#'  qCBAmodel <- qcba(cCBAmodel,iris)
#'  print(qCBAmodel@rules)
#' }
#' 
rcbaModel2CustomCBAModel <- function(rcbaModel, cutPoints, classAtt, rawDataset, attTypes)
{
  # note that the example for this function generates a notice
  # this should be fine according to https://cran.r-project.org/doc/manuals/r-release/R-exts.html#Suggested-packages
  cCBA <- customCBARuleModel()
  cCBA@rules <- rcbaModel$model #as.character
  cCBA@cutp <- cutPoints
  cCBA@classAtt <- classAtt
  if (missing(attTypes))
  {
    cCBA@attTypes <- sapply(rawDataset, class)
  }
  else
  {
    cCBA@attTypes <- attTypes
  }
  return (cCBA)
}


#' @title arulesCBAModel2CustomCBAModel Converts a model created by arulesCBA so that it can be passed to qCBA
#' @description Creates instance of CustomCBAModel class based on model created by the rCBA package.
#' This instance can then be passed to qcba() instead of CBARuleModel created with the arc package.
#' @export
#' @param arulesCBAModel aobject returned  by arulesCBA::CBA()
#' @param cutPoints specification of cutpoints applied on the data before they were passed to rCBA::build
#' @param rawDataset the raw data (before discretization). This dataset is used to guess attribute types if attTypes is not passed
#' @param attTypes vector of attribute types of the original data.  If set to null, you need to pass rawDataset.
#' @examples
#' if (! requireNamespace("arulesCBA", quietly = TRUE)) {
#'  message("Please install arulesCBA: install.packages('arulesCBA')")
#' }  else {
#'  discrModel <- discrNumeric(iris, "Species")
#'  irisDisc <- as.data.frame(lapply(discrModel$Disc.data, as.factor))
#'  arulesCBAModel <- arulesCBA::CBA(Species ~ ., data = irisDisc, supp = 0.05, 
#'   conf=0.9, lhs.support=TRUE)
#'  cCBAmodel <- arulesCBAModel2CustomCBAModel(arulesCBAModel, discrModel$cutp, iris)
#'  qCBAmodel <- qcba(cbaRuleModel=cCBAmodel,datadf=iris)
#'  print(qCBAmodel@rules)
#' }
#' 
arulesCBAModel2CustomCBAModel <- function(arulesCBAModel, cutPoints, rawDataset, attTypes )
{
  # note that the example for this function generates a notice
  # this should be fine according to https://cran.r-project.org/doc/manuals/r-release/R-exts.html#Suggested-packages
  
  cCBA <- customCBARuleModel()
  rulesFrame<-as(arulesCBAModel$rules,"data.frame")
  #default rule is stored separately, we need to add it 
  defRule<- paste("{} => {",arulesCBAModel$default,"}",sep="")
  df<-data.frame(defRule,0.0,0.0,0.0,0.0)
  names(df)  <- names(rulesFrame)
  newdf <- rbind(rulesFrame, df)
  newdf$rules <- as.character(newdf$rules)
  cCBA@rules <- newdf
  
  cCBA@cutp <- cutPoints
  cCBA@classAtt <- arulesCBAModel$class
  if (missing(attTypes))
  {
    cCBA@attTypes <- sapply(rawDataset, class)
  }
  else
  {
    cCBA@attTypes = attTypes
  }
  return (cCBA)
}

#' @title qCBA Quantitative CBA
#' @description Creates qCBA one rule or multi rule classication model from a CBA rule model
#' @export
#' @param cbaRuleModel a \link{CBARuleModel}
#' @param datadf data frame with training data
#' @param extendType  possible extend types - numericOnly or noExtend
#' @param defaultRuleOverlapPruning pruning removing rules made redundant by the default rule - possible values: noPruning,transactionBased,rangeBased,transactionBasedAsFirstStep
#' @param attributePruning remove redundant attributes
#' @param trim_literal_boundaries trimming of literal boundaries enabled
#' @param continuousPruning indicating continuous pruning is enabled
#' @param postpruning type of  postpruning (none, cba - data coverage pruning, greedy - data coverage pruning stopping on first rule with total error worse than default)
#' @param fuzzification boolean indicating if fuzzification is enabled. Multi rule classification model is produced if enabled. Fuzzification without annotation is not supported.
#' @param annotate boolean indicating if annotation with probability distributions is enabled, multi rule classification model is produced if enabled
#' @param ruleOutputPath path of file to which model will be saved. Must be set if multi rule classification is produced.
#' @param minImprovement parameter ofqCBA extend procedure  (used when  extensionStrategy=ConfImprovementAgainstLastConfirmedExtension or ConfImprovementAgainstSeedRule)
#' @param minCondImprovement parameter ofqCBA extend procedure
#' @param minConf minimum confidence  to accept extension (used when  extensionStrategy=MinConf)
#' @param extensionStrategy possible values: ConfImprovementAgainstLastConfirmedExtension, ConfImprovementAgainstSeedRule,MinConf
#' @param createHistorySlot creates a history slot on the resulting qCBARuleModel model, which with contains an ordered list of extensions
#' that were created on each rule during the extension process
#' @param timeExecution reports execution time of the extend step

#' @param loglevel logger level from java.util.logging
#'
#' @return Object of class \link{qCBARuleModel}.
#'
#' @examples
#' allData <- datasets::iris[sample(nrow(datasets::iris)),]
#' trainFold <- allData[1:100,]
#' testFold <- allData[101:nrow(datasets::iris),]
#' rmCBA <- cba(trainFold, classAtt="Species")
#' rmqCBA <- qcba(cbaRuleModel=rmCBA,datadf=trainFold)
#' print(rmqCBA@rules)



qcba <- function(cbaRuleModel,  datadf, extendType="numericOnly",defaultRuleOverlapPruning="transactionBased",attributePruning  = TRUE, trim_literal_boundaries=TRUE, continuousPruning=FALSE, postpruning="cba",fuzzification=FALSE, annotate=FALSE, ruleOutputPath, minImprovement=0,minCondImprovement=-1,minConf = 0.5,  extensionStrategy="ConfImprovementAgainstLastConfirmedExtension", loglevel = "WARNING", createHistorySlot=FALSE, timeExecution=FALSE)
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

  classAtt=cbaRuleModel@classAtt

  #reshape R data for Java call IF necessary
  if (class(cbaRuleModel)=="CBARuleModel")
  {
    #  the passsed object in rmCBA@rules was created by arules package, reshape necessary
    rules=cbaRuleModel@rules
    rulesFrame <- as(rules,"data.frame")
    rulesFrame$rules <- as.character(rulesFrame$rules)
  }
  else if (class(cbaRuleModel)=="customCBARuleModel")
  {
    rulesFrame=cbaRuleModel@rules
    message("Using customCBARuleModel")
  }
  else {
    stop("Unsupported rule model")
  }
  
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
  out <- .jcall(hjw, , "extend", extendType, defaultRuleOverlapPruning, attributePruning, trim_literal_boundaries, continuousPruning, postpruning, fuzzification, annotate,minImprovement,minCondImprovement,minConf,  extensionStrategy)
  end.time <- Sys.time()
  if (timeExecution)
  {
    message (paste("qCBA Model building took:", round(end.time - start.time, 2), " seconds"))
  }


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

    if (createHistorySlot)
    {
      extRulesHistoryArray <- .jcall(hjw, "[[Ljava/lang/String;", "getRuleHistory", evalArray=FALSE)
      extRulesHistory <- .jevalArray(extRulesHistoryArray,simplify=TRUE)
      colnames(extRulesHistory) <- c("RID","ERID","rules","support","confidence")
      extRulesHistoryFrame<-as.data.frame(extRulesHistory,stringsAsFactors=FALSE)
      extRulesHistoryFrame$support<-as.numeric(extRulesHistoryFrame$support)
      extRulesHistoryFrame$confidence<-as.numeric(extRulesHistoryFrame$confidence)
      rm@history <- extRulesHistoryFrame
    }
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
#' rmqCBA <- qcba(cbaRuleModel=rmCBA,datadf=trainFold)
#' print(rmqCBA@rules)
#' prediction <- predict(rmqCBA,testFold)
#' acc <- CBARuleModelAccuracy(prediction, testFold[[rmqCBA@classAtt]])
#' message(acc)
#'
#' @seealso \link{qcba}
#'
#'
predict.qCBARuleModel <- function(object, newdata, testingType,loglevel = "WARNING", ...)
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
    message(paste("Loading rule model from file:",ruleModel@rulePath ))
    prediction <- .jcall(jPredict, "[Ljava/lang/String;", "predictWithRulesFromFile", ruleModel@rulePath, testingType)
  }
  else
  {
    message("Using rules stored in the passed model")
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
