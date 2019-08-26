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
#' @description  This class represents a QCBA rule-based classifier.
#' @name qCBARuleModel-class
#' @rdname qCBARuleModel-class
#' @exportClass qCBARuleModel
#' @slot rules object of class rules from arules package postprocessed by \pkg{qCBA}
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
#' @description  This class represents an CBA rule-based classifier, where rules are represented as string vectors in a data frame
#' @name customCBARuleModel-class
#' @rdname customCBARuleModel-class
#' @exportClass customCBARuleModel
#' @slot rules dataframe output by \pkg{rCBA}
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

#' @title  Use the HumTemp dataset to test the one rule classification QCBA workflow.
#' @description Learns a CBA classifier and performs all QCBA postprocessing steps.
#'
#' @return QCBA model
#' @export
#'
qcbaHumTemp <- function()
{
  data_raw<-arc::humtemp
  data_discr <-arc::humtemp
  #custom discretization
  data_discr[,1]<-cut(data_raw[,1],breaks=seq(from=15,to=45,by=5))
  data_discr[,2]<-cut(data_raw[,2],breaks=c(0,40,60,80,100))
  #change interval syntax from (15,20] to (15;20], which is required by QCBA
  data_discr[,1]<-as.factor(unlist(lapply(data_discr[,1], function(x) {gsub(",", ";", x)})))
  data_discr[,2]<-as.factor(unlist(lapply(data_discr[,2], function(x) {gsub(",", ";", x)})))

  data_discr[,3] <- as.factor(data_raw[,3])

  txns <- as(data_discr, "transactions")
  rules <- apriori(txns, parameter = list(confidence = 0.5, support= 3/nrow(data_discr), minlen=1, maxlen=3), appearance=appearance)
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


#' @title  Use the \link{iris} dataset to the test QCBA workflow.
#' @description Learns a CBA classifier and performs all QCBA postprocessing steps
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

#' @title Use the Iris dataset to test the experimental multi-rule QCBA workflow.
#' @description Learns a CBA classifier, and then transforms it  to a multirule classifier,
#'  including rule annotation and fuzzification. Applies the learnt model with rule mixture classification.
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


#' @title rcbaModel2arcCBARuleModel Converts a model created by \pkg{rCBA} so that it can be passed to qCBA
#' @description Creates instance of CBAmodel class from the \pkg{arc} package
#' Instance of CBAmodel can then be passed to \link{qcba}
#' @export
#' @param rcbaModel object returned  by rCBA::build
#' @param cutPoints specification of cutpoints applied on the data before they were passed to  \code{rCBA::build}
#' @param classAtt the name of the class attribute
#' @param rawDataset the raw data (before discretization). This dataset is used to guess attribute types if attTypes is not passed
#' @param attTypes vector of attribute types of the original data.  If set to null, you need to pass rawDataset.

#' @examples
#' # this example takes about 10 seconds
#' if (! requireNamespace("rCBA", quietly = TRUE)) {
#'  message("Please install rCBA: install.packages('rCBA')")
#' } else
#' {
#'  library(rCBA)
#'  message(packageVersion("rCBA"))
#'  discrModel <- discrNumeric(iris, "Species")
#'  irisDisc <- as.data.frame(lapply(discrModel$Disc.data, as.factor))
#'  rCBAmodel <- rCBA::build(irisDisc,parallel=FALSE, sa=list(timeout=0.1))
#'  CBAmodel <- rcbaModel2CBARuleModel(rCBAmodel,discrModel$cutp,"Species",iris)
#'  qCBAmodel <- qcba(CBAmodel,iris)
#'  print(qCBAmodel@rules)
#'   }
#' 

#' 
rcbaModel2CBARuleModel <- function(rcbaModel, cutPoints, classAtt, rawDataset, attTypes)
{
  # note that the example for this function generates a notice
  # this should be fine according to https://cran.r-project.org/doc/manuals/r-release/R-exts.html#Suggested-packages
  CBArm <- CBARuleModel()
  CBArm@rules <- rcbaModel$model #as.character
  CBArm@cutp <- cutPoints
  CBArm@classAtt <- classAtt
  if (missing(attTypes))
  {
    CBArm@attTypes <- sapply(rawDataset, class)
  }
  else
  {
    CBArm@attTypes <- attTypes
  }
  return (CBArm)
}

#' @title arulesCBA2arcCBAModel Converts a model created by \pkg{arulesCBA} so that it can be passed to qCBA
#' @description Creates instance of arc CBAmodel class from the \pkg{arc} package
#' Instance of CBAmodel can then be passed to \link{qcba}
#' @export
#' @param arulesCBAModel aobject returned  by arulesCBA::CBA()
#' @param cutPoints specification of cutpoints applied on the data before they were passed to \code{rCBA::build}
#' @param rawDataset the raw data (before discretization). This dataset is used to guess attribute types if attTypes is not passed
#' @param classAtt the name of the class attribute
#' @param attTypes vector of attribute types of the original data.  If set to null, you need to pass rawDataset.
#' @examples 
#' 
#' if (! requireNamespace("arulesCBA", quietly = TRUE)) {
#'  message("Please install arulesCBA: install.packages('arulesCBA')")
#' }  else {
#'  message("The following code might cause the 'pruning exception' rCBA error on some installations")
#'  classAtt <- "Species"
#'  discrModel <- discrNumeric(iris, classAtt)
#'  irisDisc <- as.data.frame(lapply(discrModel$Disc.data, as.factor))
#'  arulesCBAModel <- arulesCBA::CBA(Species ~ ., data = irisDisc, supp = 0.05, 
#'   conf=0.9, lhs.support=TRUE)
#'  CBAmodel <- arulesCBA2arcCBAModel(arulesCBAModel, discrModel$cutp,  iris, classAtt)
#'  qCBAmodel <- qcba(cbaRuleModel=CBAmodel,datadf=iris)
#'  print(qCBAmodel@rules)
#'  }
#' 
#' 
arulesCBA2arcCBAModel <- function(arulesCBAModel, cutPoints, rawDataset, classAtt, attTypes )
{
  # note that the example for this function generates a notice
  # this should be fine according to https://cran.r-project.org/doc/manuals/r-release/R-exts.html#Suggested-packages
  
  CBAmodel <- CBARuleModel()
  #add default rule
  CBAmodel@rules <- arulesCBAModel$rules
  emptyrhs<-rep(FALSE,NROW(arulesCBAModel$class))
  emptylhs<-rep(FALSE,NROW(arulesCBAModel$rules@lhs@data)-NROW(arulesCBAModel$class))
  CBAmodel@rules@lhs@data<-as(cbind(arulesCBAModel$rules@lhs@data,c(emptylhs,emptyrhs)),"ngCMatrix")
  rhs<-emptyrhs
  rhs[which(arulesCBAModel$default  == arulesCBAModel$class ) ]<- TRUE
  CBAmodel@rules@rhs@data<-as(cbind(arulesCBAModel$rules@rhs@data,c(emptylhs,rhs)),"ngCMatrix")
  #arules data frame does not contain quality metrics for the default rule
  CBAmodel@rules@quality <- rbind(CBAmodel@rules@quality, c(0,0,0,0) )
  CBAmodel@cutp <- cutPoints
  CBAmodel@classAtt <- classAtt
  if (missing(attTypes))
  {
    CBAmodel@attTypes <- sapply(rawDataset, class)
  }
  else
  {
    CBAmodel@attTypes = attTypes
  }
  return (CBAmodel)
}
#' @title sbrlModel2arcCBARuleModel Converts a model created by \pkg{sbrl} so that it can be passed to qCBA
#' @description Creates instance of  CBAmodel class from the \pkg{arc} package
#' Instance of  CBAmodel can then be passed to \link{qcba}
#' @export
#' @param sbrl_model aobject returned  by arulesCBA::CBA()
#' @param cutPoints specification of cutpoints applied on the data before they were passed to \code{rCBA::build}
#' @param rawDataset the raw data (before discretization). This dataset is used to guess attribute types if attTypes is not passed
#' @param classAtt the name of the class attribute
#' @param attTypes vector of attribute types of the original data.  If set to null, you need to pass rawDataset.
#' @examples 
#' if (! requireNamespace("rCBA", quietly = TRUE)) {
#'   message("Please install rCBA to allow for sbrl model conversion")
#'   return()
#' } else
#' {
#'   library(rCBA)
#' }
#' if (! requireNamespace("sbrl", quietly = TRUE)) {
#'   message("Please install sbrl to allow for postprocessing of sbrl models")
#'   return()
#' } else
#' {
#'   library(sbrl)
#' }
#'  #sbrl handles only binary problems, iris has 3 target classes - remove one class
#' set.seed(111)
#' allData <- datasets::iris[sample(nrow(datasets::iris)),]
#' classToExclude<-"versicolor"
#' allData <- allData[allData$Species!=classToExclude, ]
#' # drop virginica level
#' allData$Species <-allData$Species [, drop=TRUE]
#' trainFold <- allData[1:50,]
#' testFold <- allData[51:nrow(allData),]
#' sbrlFixedLabel<-"label"
#' origLabel<-"Species"
#' 
#' orignames<-colnames(trainFold)
#' orignames[which(orignames == origLabel)]<-sbrlFixedLabel
#' colnames(trainFold)<-orignames
#' colnames(testFold)<-orignames
#' 
#' # to recode label to binary values:
#' # first create dict mapping from original distinct class values to 0,1 
#' origval<-levels(as.factor(trainFold$label))
#' newval<-range(0,1)
#' dict<-data.frame(origval,newval)
#' # then apply dict to train and test fold
#' trainFold$label<-dict[match(trainFold$label, dict$origval), 2]
#' testFold$label<-dict[match(testFold$label, dict$origval), 2]
#' 
#' # discretize training data
#' trainFoldDiscTemp <- discrNumeric(trainFold, sbrlFixedLabel)
#' trainFoldDiscCutpoints <- trainFoldDiscTemp$cutp
#' trainFoldDisc <- as.data.frame(lapply(trainFoldDiscTemp$Disc.data, as.factor))
#' 
#' # discretize test data
#' testFoldDisc <- applyCuts(testFold, trainFoldDiscCutpoints, infinite_bounds=TRUE, labels=TRUE)
#' 
#' # learn sbrl model
#' sbrl_model <- sbrl(trainFoldDisc, iters=30000, pos_sign="0", 
#'                    neg_sign="1", rule_minlen=1, rule_maxlen=10, 
#'                    minsupport_pos=0.10, minsupport_neg=0.10, 
#'                    lambda=10.0, eta=1.0, alpha=c(1,1), nchain=10)
#' # apply sbrl model on a test fold
#' yhat <- predict(sbrl_model, testFoldDisc)
#' yvals<- as.integer(yhat$V1>0.5)
#' sbrl_acc<-mean(as.integer(yvals == testFoldDisc$label))
#' message("SBRL RESULT")
#' sbrl_model
#' rm_sbrl<-sbrlModel2arcCBARuleModel(sbrl_model,trainFoldDiscCutpoints,trainFold,sbrlFixedLabel) 
#' message(paste("sbrl acc=",sbrl_acc,"sbrl rule count=",nrow(sbrl_model$rs), "avg rule length", 
#'  sum(rm_sbrl@rules@lhs@data)/length(rm_sbrl@rules)))
#' rmQCBA_sbrl <- qcba(cbaRuleModel=rm_sbrl,datadf=trainFold)
#' prediction <- predict(rmQCBA_sbrl,testFold)
#' acc_qcba_sbrl <- CBARuleModelAccuracy(prediction, testFold[[rmQCBA_sbrl@classAtt]])
#' if (! requireNamespace("stringr", quietly = TRUE)) {
#'   message("Please install stringr to compute average rule length for QCBA")
#'   avg_rule_length <- NA
#' } else
#' {
#'   library(stringr)
#'   avg_rule_length <- (sum(unlist(lapply(rmQCBA_sbrl@rules[1],str_count,pattern=",")))+
#'                         # assuming the last rule has antecedent length zero 
#'                         nrow(rmQCBA_sbrl@rules)-1)/nrow(rmQCBA_sbrl@rules)
#' }
#' message("QCBA RESULT")
#' rmQCBA_sbrl@rules
#' message(paste("QCBA after SBRL acc=",acc_qcba_sbrl,"rule count=",
#'  rmQCBA_sbrl@ruleCount, "avg rule length",  avg_rule_length))

sbrlModel2arcCBARuleModel <- function(sbrl_model, cutPoints, rawDataset, classAtt, attTypes)
{
  #rules in the list order with default rule missing
  lhs <- sbrl_model$rulenames[sbrl_model$rs$V1]
  #add defaut class antecedent
  lhs <- c(lhs,"{}")
  #class probabilities, incl. default rule
  classes<-as.integer(sbrl_model$rs$V2<0.5)
  rulecount<-length(classes)
  rhs<-paste0(rep("{label=",rulecount),classes,rep("}",rulecount))
  rules<-paste0(lhs, rep(" => ", rulecount), rhs)
  support<- rep(1,rulecount)
  confidence<- rep(1,rulecount)
  lift<- rep(1,rulecount)
  dfRules<-data.frame(rules,support,confidence, lift, stringsAsFactors=FALSE)
  
  rm_sbrl <- CBARuleModel()
  rm_sbrl@rules <- rCBA::frameToRules(dfRules)
  #rm_sbrl@rules <- as.item.matrix(dfRules,trainFold,classAtt)
  rm_sbrl@cutp <- cutPoints
  rm_sbrl@classAtt <- classAtt
  
  if (missing(attTypes))
  {
    rm_sbrl@attTypes <- sapply(rawDataset, class)
  }
  else
  {
    rm_sbrl@attTypes <- attTypes
  }
  
  return (rm_sbrl)
}  
  

#' @title qCBA Quantitative CBA
#' @description Creates QCBA model by from a CBA rule model.
#' The default values are set so that the function postprocesses CBA models, reducing their size. 
#' The resulting model has the same structure as CBA model: it is composed of an ordered list of crisp conjunctive rules,  intended to be applied for one-rule classification.
#' The experimental \code{annotate} and \code{fuzzification} parameters will trigger more complex postprocessing of CBA models: 
#' rules will be annotated with probability distributions and optionally fuzzy borders. The intended use of such models is multi-rule classification.
#' The \link{predict} function automatically determines whether the input model is a CBA model or an annotated model.
#' @export
#' @param cbaRuleModel a \link{CBARuleModel}
#' @param datadf data frame with training data
#' @param extendType  possible extend types - numericOnly or noExtend
#' @param defaultRuleOverlapPruning pruning removing rules made redundant by the default rule; possible values: \code{noPruning}, \code{transactionBased}, \code{rangeBased}, \code{transactionBasedAsFirstStep}
#' @param attributePruning remove redundant attributes
#' @param trim_literal_boundaries trimming of literal boundaries enabled
#' @param continuousPruning indicating continuous pruning is enabled
#' @param postpruning type of  postpruning (\code{none}, \code{cba} - data coverage pruning, \code{greedy} - data coverage pruning stopping on first rule with total error worse than default)
#' @param fuzzification boolean indicating if fuzzification is enabled. Multi-rule classification model is produced if enabled. Fuzzification without annotation is not supported.
#' @param annotate boolean indicating if annotation with probability distributions is enabled, multi-rule classification model is produced if enabled
#' @param ruleOutputPath path of file to which model will be saved. Must be set if multi rule classification is produced.
#' @param minImprovement parameter ofqCBA extend procedure  (used when  \code{extensionStrategy=ConfImprovementAgainstLastConfirmedExtension} or \code{ConfImprovementAgainstSeedRule})
#' @param minCondImprovement parameter ofqCBA extend procedure
#' @param minConf minimum confidence  to accept extension (used when  extensionStrategy=MinConf)
#' @param extensionStrategy possible values: \code{ConfImprovementAgainstLastConfirmedExtension}, \code{ConfImprovementAgainstSeedRule},\code{MinConf}
#' @param createHistorySlot creates a history slot on the resulting \link{qCBARuleModel} model, which contains an ordered list of extensions
#' that were created on input rules during the extension process
#' @param timeExecution reports execution time of the extend step

#' @param loglevel logger level from \code{java.util.logging}
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



qcba <- function(cbaRuleModel,  datadf, extendType="numericOnly", defaultRuleOverlapPruning="transactionBased",attributePruning  = TRUE, trim_literal_boundaries=TRUE, continuousPruning=FALSE, postpruning="cba",fuzzification=FALSE, annotate=FALSE, ruleOutputPath, minImprovement=0,minCondImprovement=-1,minConf = 0.5,  extensionStrategy="ConfImprovementAgainstLastConfirmedExtension", loglevel = "WARNING", createHistorySlot=FALSE, timeExecution=FALSE)
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
#' @description Applies \link{qcba} rule model on provided data. 
#' Automatically detects whether one-rule or  multi-rule classification is used
#' 
#'
#' @param object \link{qCBARuleModel} class instance
#' @param newdata data frame with data
#' @param testingType either \code{mixture} for multi-rule classification or \code{firstRule} for one-rule classification. Applicable only when model is loaded from file.
#' @param loglevel logger level from \code{java.util.logging}
#' @param outputFiringRuleIDs if set to TRUE, instead of predictions, the function will return one-based IDs of  rules used to classify each instance (one rule per instance). 
#' @param ... other arguments (currently not used)
#' @return vector with predictions.
#' @export
#' @method predict qCBARuleModel
#' @examples
#' allData <- datasets::iris[sample(nrow(datasets::iris)),]
#' trainFold <- allData[1:100,]
#' testFold <- allData[101:nrow(datasets::iris),]
#' rmCBA <- cba(trainFold, classAtt="Species")
#' rmqCBA <- qcba(cbaRuleModel=rmCBA, datadf=trainFold)
#' print(rmqCBA@rules)
#' prediction <- predict(rmqCBA,testFold)
#' acc <- CBARuleModelAccuracy(prediction, testFold[[rmqCBA@classAtt]])
#' message(acc)
#' firingRuleIDs <- predict(rmqCBA,testFold,outputFiringRuleIDs=TRUE)
#' message("The second instance in testFold was classified by the following rule")
#' message(rmqCBA@rules[firingRuleIDs[2],1])
#' message("The second instance is")
#' message(testFold[2,])
#' 
#' @seealso \link{qcba}
#'
#'
predict.qCBARuleModel <- function(object, newdata, testingType,loglevel = "WARNING", outputFiringRuleIDs=FALSE, ...)
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
  
  #pass data to QCBA Java implementation
  #the reason why we cannot use predict.RuleModel in \pkg{arc} package is that the items in the rules do not match the itemMatrix after R extend
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
    extRulesJArray <- .jarray(lapply(ruleModel@rules, .jarray))
    .jcall(jPredict, , "addRuleFrame", extRulesJArray)
    prediction <- .jcall(jPredict, "[Ljava/lang/String;", "predict")
  }
  if (outputFiringRuleIDs)
  {
    ruleIDs <- .jcall(jPredict, "[Ljava/lang/String;", "getFiringRuleID")
    # the original IDs from Java are zero based, R works with one-based indices
    return(strtoi(ruleIDs)+1)
  }
  else
  {
    return(prediction)
  }
  
}





#' @title Map R types to qCBA
#' @description The QCBA Java implementation uses different names of some data types than are used in this R wrapper.
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
