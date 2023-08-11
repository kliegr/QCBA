library(arulesCBA)
library(qCBA)
set.seed(1)

#' @title Auto learn and evaluate QCBA postprocessing on multiple rule learners
#' 
#' @description Learn multiple rule models using other rule induction algorithms and apply
#' QCBA to postprocess them. 
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
#' @param iterations number of excecutions over base learner, whihc is used for
#' obtaining a more precise estimate of build time
#' @param rounding_places statistics in the resulting dataframe will be rounded to
#' specified number of decimal places 
#' @param return_models boolean indicating if also learnt rule lists should be 
#' included in model output 
#' @param debug_prints print debug information such as rule lists 
#' @param ... parameters for base learners, the name of the argument is the base
#' learner (one of algs values) and value is a list of parameters to pass. 
#' To specify paramaters for QCBA pass argument "QCBA".
#' @return Outputs a dataframe with evaluation metrics statistics. 
#' Included metrics:
#' **accuracy**: percentage of correct predictions for the test set
#' **buildtime**: learning time for inference of the model. In case of QCBA, this 
#' excludes time for the induction of the base learner
#' **rulecount**: number of rules in the rule list. Note that for QCBA the 
#' count includes the default rule (rule with empty antecedent), while for 
#' base learners this rule may not be included (depending on arules output) 
#' **modelsize**: total number of conditions in the antecedents of all rules in
#'  the model
#'
#' @examples
#'
benchmarkQCBA <- function(train,test, classAtt,train_disc=NULL, test_disc=NULL, cutPoints=NULL,
                algs = c("CBA","CMAR","CPAR","PRM","FOIL2"), iterations=2, rounding_places=3, return_models = FALSE, debug_prints = FALSE, ...
                ){
  
  algcombinations<-c(algs,paste0(algs,"_QCBA"))
  df_stats <- data.frame(matrix(rep(0,length(algs)*2), ncol = length(algcombinations), nrow = 4), row.names = c("accuracy","rulecount","modelsize", "buildtime"))
  returnList=list()
  colnames(df_stats)<-algcombinations

  if (is.null(train_disc))
  {
    message("Discretized data not passed (train_disc is NULL), performing
            discretization and ignoring passed value of cutPoints and test_dic")
    discrModel <- discrNumeric(trainFold, classAtt)
    train_disc <- as.data.frame(lapply(discrModel$Disc.data, as.factor))
    cutPoints <- discrModel$cutp
    test_disc <- applyCuts(test, cutPoints, infinite_bounds=TRUE, labels=TRUE)
  }
  else{
    message("Using passed prediscretized data")
    if (sum(test_disc[[classAtt]] == test[[classAtt]]) != nrow(test_disc) | nrow(test_disc) != nrow(test))
    {
      Exception("Values of class attribute in test_disc and test must be the same and both must have the same length")
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
# EXAMPLE 1 benchmarking only
#Define input dataset and target variable 
df_all <-datasets::iris
classAtt <- "Species"

# Create train/test partition using built-in R functions
tot_rows<-nrow(df_all)  
train_proportion<-2/3
df_all <- df_all[sample(tot_rows),]
trainFold <- df_all[1:(train_proportion*tot_rows),]
testFold <- df_all[(1+train_proportion*tot_rows):tot_rows,]


# learn with default metaparameter values
stats<-benchmarkQCBA(trainFold,testFold,classAtt)
print(stats)
# print relative change of QCBA results over baseline algorithms 
print(stats[,6:10]/stats[,0:5]-1)

# EXAMPLE 2 external discretization
# Discretize numerical predictors using built-in discretization
# This performs supervised, entropy-based discretization (Fayyad and Irani, 1993)
# of all numerical predictor variables with 3 or more distinct numerical values
discrModel <- discrNumeric(trainFold, classAtt)
train_disc <- as.data.frame(lapply(discrModel$Disc.data, as.factor))
test_disc <- applyCuts(testFold, discrModel$cutp, infinite_bounds=TRUE, labels=TRUE)
stats<-benchmarkQCBA(trainFold,testFold,classAtt,train_disc,test_disc,discrModel$cutp)
print(stats)
# EXAMPLE 3 pass custom metaparameters for base learners, 
# use only CBA as a base learner, return rule lists.
output<-benchmarkQCBA(trainFold,testFold,classAtt,train_disc,test_disc,discrModel$cutp, 
                     CBA=list("support"=0.05,"confidence"=0.5),algs = c("CPAR"),
                     return_models=TRUE)
message("Evaluation statistics")
print(output$stats)
message("CPAR model")
inspect(output$CPAR[[1]])
message("QCBA model")
print(output$CPAR_QCBA[[1]])
