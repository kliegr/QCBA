#' @import arc
#' @import utils
#' @importFrom methods as new
#' @importFrom rJava .jcall .jnew .jarray .jevalArray
#' @importFrom arules apriori inspect
#' @importFrom stats predict
#' @importFrom stats as.formula
#' @importFrom arulesCBA CBA CMAR CPAR PRM FOIL2
#' @importFrom methods is

library(arules)
library(rJava)
library(arc)
require(arulesCBA)

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


#' customCBARuleModel
#'
#' @description  This class represents a rule-based classifier, where rules are represented as string vectors in a data frame
#' @name customCBARuleModel-class
#' @rdname customCBARuleModel-class
#' @exportClass customCBARuleModel
#' @slot rules dataframe with rules
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
  classAtt="Class"
  appearance <- getAppearance(data_discr, classAtt)
  rules <- apriori(txns, parameter = list(confidence = 0.5, support= 3/nrow(data_discr), minlen=1, maxlen=3), appearance=appearance)
  print("Seed list of rules")
  inspect(rules)
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



#' @title Returns vector with confidences for the positive class (useful for ROC or AUC computation)
#' @description Methods for computing ROC curves require a vector of confidences
#' of the positive class, while in qCBA, the confidence returned by predict.qCBARuleModel with
#' outputProbabilies = TRUE returns confidence for the predicted class.
#' This method converts the values to confidences for the positive class
#' @export
#' @param confidences Vector of confidences
#' @param predictedClass Vector with predicted classes
#' @param positiveClass Positive class (String)
#'
#' @return Vector of confidence values
#'
#' @examples
#' predictedClass = c("setosa","virginica")
#' confidences = c(0.9,0.6)
#' baseClass="setosa"
#' getConfVectorForROC(confidences,predictedClass,baseClass)

getConfVectorForROC <- function(confidences, predictedClass, positiveClass)
{
  if (length(levels(as.factor(predictedClass))) != 2){
    warning("Binary classification expected")
  }
  return(abs(confidences - as.integer(predictedClass != positiveClass)))
}

#' @title rcbaModel2arcCBARuleModel Converts a model created by \pkg{rCBA} so that it can be passed to qCBA
#' @description Creates instance of CBAmodel class from the \pkg{arc} package
#' Instance of CBAmodel can then be passed to \link{qcba}
#' @export
#' @param rcbaModel object returned  by rCBA::build
#' @param cutPoints specification of cutpoints applied on the data before they were passed to  \code{rCBA::build}
#' @param rawDataset the raw data (before discretization). This dataset is used to guess attribute types if attTypes is not passed
#' @param classAtt the name of the class attribute
#' @param attTypes vector of attribute types of the original data.  If set to null, you need to pass rawDataset.

#' @examples
#' # this example takes about 10 seconds
#' if (! requireNamespace("rCBA", quietly = TRUE)) {
#'  message("Please install rCBA: install.packages('rCBA')")
#' } else
#' {
#' # This will run only outside a CRAN test, if the environment variable  NOT_CRAN is set to true
#' # This environment variable is set by devtools
#' if (identical(Sys.getenv("NOT_CRAN"), "true")) {
#'  library(rCBA)
#'  message(packageVersion("rCBA"))
#'  discrModel <- discrNumeric(iris, "Species")
#'  irisDisc <- as.data.frame(lapply(discrModel$Disc.data, as.factor))
#'  
#'  rCBAmodel <- rCBA::build(irisDisc,parallel=FALSE, sa=list(timeout=0.01))
#'  CBAmodel <- rcbaModel2CBARuleModel(rCBAmodel,discrModel$cutp,iris,"Species")
#'  qCBAmodel <- qcba(CBAmodel,iris)
#'  print(qCBAmodel@rules)
#'  }
#'}
#' 

#' 
rcbaModel2CBARuleModel <- function(rcbaModel, cutPoints, rawDataset, classAtt, attTypes)
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
#'  classAtt <- "Species"
#'  discrModel <- discrNumeric(iris, classAtt)
#'  irisDisc <- as.data.frame(lapply(discrModel$Disc.data, as.factor))
#'  arulesCBAModel <- arulesCBA::CBA(Species ~ ., data = irisDisc, supp = 0.1, 
#'   conf=0.9)
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
  ruleCount<-length(arulesCBAModel$rules)
  #check if last rule in the classifier has no conditions (default rule)
  #if it is not a default rule, try to add one.
  if (sum(arulesCBAModel$rules@lhs@data[,ruleCount])>0)
  {
    #Both LHS and RHS in arules have the same dimension. 
    #positions 1 to number of distinct items in LHS are used for RHS
    #remaining positions are used for RHS items
    if ("default" %in% attributes(arulesCBAModel)$names)
    {
      itemCount<-nrow(arulesCBAModel$rules@lhs@data) #total for LHS and RHS items
      emptyLHS<-rep(FALSE,itemCount)
      arulesCBAModel$rules@lhs@data <- as(cbind(arulesCBAModel$rules@lhs@data,emptyLHS),"nMatrix")
      RHSLevels<-nlevels(arulesCBAModel$default)
      rhs <-emptyLHS
      positionOfDefaultRuleInRHSLevels<-as.numeric(arulesCBAModel$default)
      #RHS for default rule has only one bit on which corresponds to the position
      #of the default rule in the item vector
      rhs[itemCount - RHSLevels+positionOfDefaultRuleInRHSLevels] <- TRUE
      arulesCBAModel$rules@rhs@data <- as(cbind(arulesCBAModel$rules@rhs@data,rhs),"nMatrix")
      #arules data frame does not contain quality metrics for the default rule,
      # we will add a vector with as many zeros as there are quality metrics (columns)
      arulesCBAModel$rules@quality <- rbind(arulesCBAModel$rules@quality, rep(0,ncol(arulesCBAModel$rules@quality)) )
      message("Last rule added based on default specification in the passed model ")
    }
    else
    {
      warning("Last rule is not a default rule with empty antecedent and could 
      not be automatically added as 'default' attribute is missing")
    }
  }
  CBAmodel@rules <- arulesCBAModel$rules
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
#' @description Creates instance of  CBAmodel class from the \pkg{arc} package. 
#' Instance of  CBAmodel can then be passed to \link{qcba}
#' @export
#' @param sbrl_model object returned  by arulesCBA::CBA()
#' @param cutPoints specification of cutpoints applied on the data before they were passed to \code{rCBA::build}
#' @param rawDataset the raw data (before discretization). This dataset is used to guess attribute types if attTypes is not passed
#' @param classAtt the name of the class attribute
#' @param attTypes vector of attribute types of the original data.  If set to null, you need to pass rawDataset.
#' @examples 
#' if (! requireNamespace("rCBA", quietly = TRUE)) {
#'   message("Please install rCBA to allow for sbrl model conversion")
#'   return()
#' } else if (! requireNamespace("sbrl", quietly = TRUE)) {
#'   message("Please install sbrl to allow for postprocessing of sbrl models")
#' } else
#' {
#' #' # This will run only outside a CRAN test, if the environment variable  NOT_CRAN is set to true
#' # This environment variable is set by devtools
#' if (identical(Sys.getenv("NOT_CRAN"), "true")) {
#'   library(sbrl)
#'   library(rCBA)
#'   # sbrl handles only binary problems, iris has 3 target classes - remove one class
#'   set.seed(111)
#'   allData <- datasets::iris[sample(nrow(datasets::iris)),]
#'   classToExclude<-"versicolor"
#'   allData <- allData[allData$Species!=classToExclude, ]
#'   # drop the removed level
#'   allData$Species <-allData$Species [, drop=TRUE]
#'   trainFold <- allData[1:50,]
#'   testFold <- allData[51:nrow(allData),]
#'   sbrlFixedLabel<-"label"
#'   origLabel<-"Species"
#' 
#'   orignames<-colnames(trainFold)
#'   orignames[which(orignames == origLabel)]<-sbrlFixedLabel
#'   colnames(trainFold)<-orignames
#'   colnames(testFold)<-orignames
#' 
#'   # to recode label to binary values:
#'   # first create dict mapping from original distinct class values to 0,1 
#'   origval<-levels(as.factor(trainFold$label))
#'   newval<-range(0,1)
#'   dict<-data.frame(origval,newval)
#'   # then apply dict to train and test fold
#'   trainFold$label<-dict[match(trainFold$label, dict$origval), 2]
#'   testFold$label<-dict[match(testFold$label, dict$origval), 2]
#' 
#'   # discretize training data
#'   trainFoldDiscTemp <- discrNumeric(trainFold, sbrlFixedLabel)
#'   trainFoldDiscCutpoints <- trainFoldDiscTemp$cutp
#'   trainFoldDisc <- as.data.frame(lapply(trainFoldDiscTemp$Disc.data, as.factor))
#' 
#'   # discretize test data
#'   testFoldDisc <- applyCuts(testFold, trainFoldDiscCutpoints, infinite_bounds=TRUE, labels=TRUE)
#'   # SBRL 1.4 crashes if features contain a space
#'   # even if these features are converted to factors,
#'   # to circumvent this, it is necessary to replace spaces
#'   trainFoldDisc <- as.data.frame(lapply(trainFoldDisc, function(x) gsub(" ", "", as.character(x))))
#'   for (name in names(trainFoldDisc)) {trainFoldDisc[name] <- as.factor(trainFoldDisc[,name])}
#'   # learn sbrl model, rule_minlen is increased to demonstrate the effect of postprocessing 
#'   sbrl_model <- sbrl(trainFoldDisc, iters=20000, pos_sign="0", 
#'    neg_sign="1", rule_minlen=3, rule_maxlen=5, minsupport_pos=0.05, minsupport_neg=0.05, 
#'    lambda=20.0, eta=5.0, nchain=25)
#'   # apply sbrl model on a test fold
#'   yhat <- predict(sbrl_model, testFoldDisc)
#'   yvals<- as.integer(yhat$V1>0.5)
#'   sbrl_acc<-mean(as.integer(yvals == testFoldDisc$label))
#'   message("SBRL RESULT")
#'   message(sbrl_model)
#'   rm_sbrl<-sbrlModel2arcCBARuleModel(sbrl_model,trainFoldDiscCutpoints,trainFold,sbrlFixedLabel) 
#'   message(paste("sbrl acc=",sbrl_acc,", sbrl rule count=",nrow(sbrl_model$rs), ",
#'   avg condition count (incl. default rule)", 
#'   sum(rm_sbrl@rules@lhs@data)/length(rm_sbrl@rules)))
#'   rmQCBA_sbrl <- qcba(cbaRuleModel=rm_sbrl,datadf=trainFold)
#'   prediction <- predict(rmQCBA_sbrl,testFold)
#'   acc_qcba_sbrl <- CBARuleModelAccuracy(prediction, testFold[[rmQCBA_sbrl@classAtt]])
#'   avg_rule_length <- rmQCBA_sbrl@rules$condition_count/nrow(rmQCBA_sbrl@rules)
#'   message("RESULT of QCBA postprocessing of SBRL")
#'   message(rmQCBA_sbrl@rules)
#'   message(paste("QCBA after SBRL acc=",acc_qcba_sbrl,", rule count=",
#'   rmQCBA_sbrl@ruleCount, ", avg condition count (incl. default rule)",  avg_rule_length))
#'   unlink("tdata_R.label") # delete temp files created by SBRL
#'   unlink("tdata_R.out")
#'  }
#' }

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
#' @param minImprovement parameter of qCBA extend procedure  (used when  \code{extensionStrategy=ConfImprovementAgainstLastConfirmedExtension} or \code{ConfImprovementAgainstSeedRule})
#' @param minCondImprovement parameter of qCBA extend procedure
#' @param minConf minimum confidence  to accept extension (used when  extensionStrategy=MinConf)
#' @param extensionStrategy possible values: \code{ConfImprovementAgainstLastConfirmedExtension}, \code{ConfImprovementAgainstSeedRule},\code{MinConf}
#' @param loglevel logger level from \code{java.util.logging}
#' @param createHistorySlot creates a history slot on the resulting \link{qCBARuleModel} model, which contains an ordered list of extensions
#' that were created on input rules during the extension process
#' @param timeExecution reports execution time of the extend step
#' @param computeOrderedStats appends orderedConf and orderedSupp quality metrics to the resulting dataframe. Setting this parameter to FALSE will reduce the training time.
#'
#' @return Object of class \link{qCBARuleModel}.
#'
#' @examples
#' allData <- datasets::iris[sample(nrow(datasets::iris)),]
#' trainFold <- allData[1:100,]
#' rmCBA <- cba(trainFold, classAtt="Species")
#' rmqCBA <- qcba(cbaRuleModel=rmCBA,datadf=trainFold)
#' print(rmqCBA@rules)

qcba <- function(cbaRuleModel,  datadf, extendType="numericOnly", defaultRuleOverlapPruning="transactionBased",attributePruning  = TRUE, trim_literal_boundaries=TRUE, continuousPruning=FALSE, postpruning="cba",fuzzification=FALSE, annotate=FALSE, ruleOutputPath, minImprovement=0,minCondImprovement=-1,minConf = 0.5,  extensionStrategy="ConfImprovementAgainstLastConfirmedExtension", loglevel = "WARNING", createHistorySlot=FALSE, timeExecution=FALSE, computeOrderedStats = TRUE)
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
  if (is(cbaRuleModel,"CBARuleModel"))
  {
    #  the passed object in rmCBA@rules was created by arules package, reshape necessary
    rules=cbaRuleModel@rules
    rulesFrame <- as(rules,"data.frame")
    rulesFrame$rules <- as.character(rulesFrame$rules)
  }
  else if (is(cbaRuleModel,"customCBARuleModel"))
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
    extRulesArray <- .jcall(hjw, "[[Ljava/lang/String;", "getRulesBasicStatsLength", evalArray=FALSE)
    extRules <- .jevalArray(extRulesArray,simplify=TRUE)
    colnames(extRules) <- c("rules","support","confidence","condition_count")
    extRulesFrame<-as.data.frame(extRules,stringsAsFactors=FALSE)
    extRulesFrame$support<-as.numeric(extRulesFrame$support)
    extRulesFrame$confidence<-as.numeric(extRulesFrame$confidence)
    extRulesFrame$condition_count<-as.numeric(extRulesFrame$condition_count)
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
    if (computeOrderedStats)
    {
      #message("computing orderedConf and orderedSupp")
      firingIDs_train <- predict(rm,datadf,outputFiringRuleIDs=TRUE)
      prediction_train <- predict(rm,datadf)
      ordered_conf <- c()
      ordered_supp <- c()
      for (i in 1:rm@ruleCount)
      {
        trans_currentrulefiring <- firingIDs_train==i
        coveredInstances_true <- datadf[[classAtt]][trans_currentrulefiring]
        correct <- coveredInstances_true == prediction_train[trans_currentrulefiring]
        correct_count <- sum(correct)
        covered_count <- sum(trans_currentrulefiring)
        ordered_conf <- c(ordered_conf,correct_count/covered_count)
        ordered_supp <- c(ordered_supp,correct_count)
        i<-i+1
      }
      rm@rules$orderedConf <- ordered_conf
      rm@rules$orderedSupp <- ordered_supp
    }  
    
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
#' @param outputConfidenceScores if set to TRUE, instead of predictions, the function will return confidences of the firing rule 
#' @param confScoreType applicable only if `outputConfidenceScores=TRUE`, possible values `ordered` for confidence computed only for training instances reaching this rule, or `global` for standard rule confidence computed from the complete training data
#' @param positiveClass This setting is only used if `outputConfidenceScores=TRUE`. It should be used only for binary problems. In this 
#' case, the confidence values are recalculated so that these are not confidence values of the predicted class (default behaviour of `outputConfidenceScores=TRUE`)
#' but rather confidence values associated with the class designated as positive 
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
predict.qCBARuleModel <- function(object, newdata, testingType,loglevel = "WARNING", outputFiringRuleIDs=FALSE, outputConfidenceScores=FALSE, confScoreType="ordered", positiveClass=NULL, ...)
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
  if (outputFiringRuleIDs | outputConfidenceScores)
  {
    ruleIDs <- .jcall(jPredict, "[Ljava/lang/String;", "getFiringRuleID")
    # the original IDs from Java are zero based, R works with one-based indices
    ruleIDs <- strtoi(ruleIDs)+1
  }
  
  if (outputFiringRuleIDs)
  {
    if(outputConfidenceScores)
    {
      warning("Illegal combination of parameters, ignoring outputConfidenceScores")
    }
    return(ruleIDs)
  }
  if (outputConfidenceScores)
  {
    if (confScoreType =="ordered" & !("orderedConf" %in% colnames(ruleModel@rules)))
    {
      message("orderedConf has not been precomputed, have you trained qcba with 
              computeOrderedStats=TRUE? Falling back to standard global confidence")
      confPositionInVector<-which(colnames(ruleModel@rules)=="confidence")
    }
    else if (confScoreType =="ordered")
    {
      confPositionInVector<-which(colnames(ruleModel@rules)=="orderedConf")
    }
    else
    {
      confPositionInVector<-which(colnames(ruleModel@rules)=="confidence")
      if (confScoreType !="global")
      {
        message("Unrecognized confScoreType, falling back to standard global confidence")
      }
    }
    # The method uses confidence of the firing rule (as was computed on the entire training data)
    # as the confidence estimate. 
    # This is not the best approximation of confidence, especially for rules lower in the list
      confidences <- vector()
      for (ruleId in ruleIDs)
      {
        confidence <-  ruleModel@rules[ruleId,confPositionInVector]
        confidences <- c(confidences, confidence)
      }
      if (!is.null(positiveClass))
      {
        confidences <- getConfVectorForROC(confidences,prediction,positiveClass)
      }
      return(confidences)
  }
  else
  {
    return(prediction)
  }
  
}


#' @title Learn and evaluate QCBA postprocessing on multiple rule learners. 
#' This can be, for example, used to automatically select the best model for a given
#' use case based on a combined preference for accuracy and model size.
#' 
#' @description Learn multiple rule models using base rule induction algorithms
#' from \pkg{arulesCBA} and apply QCBA to postprocess them. 
#' @export
#' @param train data frame with training data
#' @param test data frame with testing data before postprocessing
#' @param classAtt the name of the class attribute
#' @param train_disc prediscretized training data
#' @param test_disc prediscretized tet data
#' @param cutPoints specification of cutpoints applied on the data
#'  (ignored if train_disc is null)
#' @param algs vector with names of baseline rule learning algorithms. 
#' Names must correspond to function names from the \pkg{arulesCBA} library 
#' @param iterations number of executions over base learner, which is used for
#' obtaining a more precise estimate of build time
#' @param rounding_places statistics in the resulting dataframe will be rounded to
#' specified number of decimal places 
#' @param return_models boolean indicating if also learnt rule lists
#' (baseline and postprocessed) should be  included in model output 
#' @param debug_prints print debug information such as rule lists 
#' @param ... Parameters for base learners, the name of the argument is the base
#' learner (one of `algs` values) and value is a list of parameters to pass. 
#' To specify parameters for QCBA pass "QCBA". See also Example 3.
#' @return Outputs a dataframe with evaluation metrics and if `return_models==TRUE`
#' also the induced baseline and QCBA models (see also Example 3).  
#' Included metrics in the dataframe with statistics:
#' **accuracy**: percentage of correct predictions in the test set
#' **rulecount**: number of rules in the rule list. Note that for QCBA the 
#' count includes the default rule (rule with empty antecedent), while for 
#' base learners this rule may not be included (depending on the base learner) 
#' **modelsize**: total number of conditions in the antecedents of all rules in
#'  the model
#' **buildtime**: learning time for inference of the model. In case of QCBA, this 
#' excludes time for the induction of the base learner
#' 
#' @seealso [qcba()] which this function wraps.
#' @examples
#' # EXAMPLE 1: pass train and test folds, induce multiple base rule learners,
#' # postprocess each with QCBA and return benchmarking results.
#' 
#' # Define input dataset and target variable 
#' df_all <-datasets::iris
#' classAtt <- "Species"
#'
#' # Create train/test partition using built-in R functions
#' tot_rows<-nrow(df_all)  
#' train_proportion<-2/3
#' df_all <- df_all[sample(tot_rows),]
#' trainFold <- df_all[1:(train_proportion*tot_rows),]
#' testFold <- df_all[(1+train_proportion*tot_rows):tot_rows,]
#' # learn with default metaparameter values
#' stats<-benchmarkQCBA(trainFold,testFold,classAtt)
#' print(stats)
#' # print relative change of QCBA results over baseline algorithms 
#' print(stats[,6:10]/stats[,0:5]-1)
#' 
#' # EXAMPLE 2: As Example 1 but data are discretizated externally
#' # Discretize numerical predictors using built-in discretization
#' # This performs supervised, entropy-based discretization (Fayyad and Irani, 1993)
#' # of all numerical predictor variables with 3 or more distinct numerical values
#' # This example could run for more than 5 seconds
#' if (identical(Sys.getenv("NOT_CRAN"), "true")) {
#'   discrModel <- discrNumeric(trainFold, classAtt)
#'   train_disc <- as.data.frame(lapply(discrModel$Disc.data, as.factor))
#'   test_disc <- applyCuts(testFold, discrModel$cutp, infinite_bounds=TRUE, labels=TRUE)
#'   stats<-benchmarkQCBA(trainFold,testFold,classAtt,train_disc,test_disc,discrModel$cutp)
#'   print(stats)
#' }
#' # EXAMPLE 3: pass custom metaparameters to selected base rule learner,
#' # then postprocess with QCBA, evaluate, and return both models
#' # This example could run for more than 5 seconds
#' if (identical(Sys.getenv("NOT_CRAN"), "true")) {
#' # use only CBA as a base learner, return rule lists.
#'   output<-benchmarkQCBA(trainFold,testFold,classAtt,train_disc,test_disc,discrModel$cutp, 
#'                      CBA=list("support"=0.05,"confidence"=0.5),algs = c("CPAR"),
#'                      return_models=TRUE)
#'   message("Evaluation statistics")
#'   print(output$stats)
#'   message("CPAR model")
#'   inspect(output$CPAR[[1]])
#'   message("QCBA model")
#'   print(output$CPAR_QCBA[[1]])
#' }

benchmarkQCBA <- function(train,test, classAtt,train_disc=NULL, test_disc=NULL, cutPoints=NULL,
                          algs = c("CBA","CMAR","CPAR","PRM","FOIL2"), iterations=2, rounding_places=3, return_models = FALSE, debug_prints = FALSE, ...
){
  set.seed(1)
  algcombinations<-c(algs,paste0(algs,"_QCBA"))
  df_stats <- data.frame(matrix(rep(0,length(algs)*2), ncol = length(algcombinations), nrow = 4), row.names = c("accuracy","rulecount","modelsize", "buildtime"))
  returnList=list()
  colnames(df_stats)<-algcombinations
  
  if (is.null(train_disc))
  {
    message("Discretized data not passed (train_disc is NULL), performing
            discretization and ignoring passed value of cutPoints and test_dic")
    discrModel <- discrNumeric(train, classAtt)
    train_disc <- as.data.frame(lapply(discrModel$Disc.data, as.factor))
    cutPoints <- discrModel$cutp
    test_disc <- applyCuts(test, cutPoints, infinite_bounds=TRUE, labels=TRUE)
  }
  else{
    message("Using passed prediscretized data")
    if (sum(test_disc[[classAtt]] == test[[classAtt]]) != nrow(test_disc) | nrow(test_disc) != nrow(test))
    {
      stop("Values of class attribute in test_disc and test must be the same and both must have the same length")
    }
  }
  
  for (alg in algs)
  {
    algQCBA<-paste0(alg,"_QCBA")
    message(paste0("** STARTED learning model with ", alg, " **"))
    f <- match.fun(alg)
    start.time <- Sys.time()
    form <-as.formula(paste(classAtt, " ~ .",collapse = " "))
    params <- list(formula = form, data = train_disc)
    z <- list(...)
    if (alg %in% names(z))
    {
      params <- append(params,z[[alg]])
    }
    
    for (i in 1:iterations) arulesBaseModel <- do.call(f, params)  
    averageExecTime<-as.numeric((Sys.time()- start.time)/iterations,units="secs")
    message(paste0("** FINISHED learning model with ", alg, " **"))
    #Important: use predict function from arules library 
    yhat <- predict(arulesBaseModel, test_disc) # Use rule list for prediction
    
    if (return_models) returnList[[alg]] <- list(arulesBaseModel$rules)    
    baseModel_arc <- arulesCBA2arcCBAModel(arulesBaseModel, cutPoints,  train, classAtt)
    
    
    # Compute model statistics 
    df_stats["accuracy",alg] <- CBARuleModelAccuracy(yhat, test_disc[[classAtt]])
    df_stats["buildtime",alg] <- averageExecTime
    df_stats["rulecount",alg] <- length(arulesBaseModel$rules)
    df_stats["modelsize",alg] <- sum(arulesBaseModel$rules@lhs@data) 
    
    message(paste0("** STARTED QCBA POSTPROCESSING OF ", alg, "  **"))
    params<-list(cbaRuleModel=baseModel_arc,datadf=train)
    if ("QCBA" %in% names(z))
    {
      params <- append(params,z[["QCBA"]])
    }
    start.time <- Sys.time()
    for (i in 1:iterations) qCBAmodel <- do.call(qcba,params)
    if (alg %in% names(z))
    {
      params <- append(params,z[[alg]])
    }
    averageExecTime<-as.numeric((Sys.time() - start.time)/iterations,units="secs")
    # wrapping in list is necessary when dataframe is added to a list 
    if (return_models) returnList[[ algQCBA ]] <- list(qCBAmodel@rules)
    if (debug_prints) print(qCBAmodel@rules) #Rule list after postprocessing
    yhat <- predict(qCBAmodel, test) # Use postprocessed rule list for prediction
    # Compute model statistics 
    df_stats["accuracy",algQCBA]  <- CBARuleModelAccuracy(yhat, test[[classAtt]])
    df_stats["buildtime",algQCBA] <-averageExecTime
    df_stats["rulecount",algQCBA] <-qCBAmodel@ruleCount
    df_stats["modelsize",algQCBA] <- sum(qCBAmodel@rules$condition_count)
    message(paste0("** FINISHED POSTPROCESSING ", alg, " model with QCBA **"))
  }
  rounded_df <- as.data.frame(lapply(df_stats, function(x) round(x, digits = rounding_places)))
  rownames(rounded_df)=rownames(df_stats)
  if (return_models)
  {
    returnList[["stats"]]=df_stats
    return(returnList)
  }
  else
  {
    return(rounded_df)
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

