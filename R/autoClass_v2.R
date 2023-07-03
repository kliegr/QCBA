library(arulesCBA)
allData <- datasets::iris[sample(nrow(datasets::iris)),]
trainFold <- allData[1:100,]
testFold <- allData[101:nrow(allData),]
classAtt <- "Species"
discrModel <- discrNumeric(trainFold, classAtt)
train_disc <- as.data.frame(lapply(discrModel$Disc.data, as.factor))



algs<- c("CBA","CMAR","CPAR","PRM","FOIL2")
algcombinations<-c(algs,paste0(algs,"_QCBA"))
df <- data.frame(matrix(rep(0,10), ncol = length(algcombinations), nrow = 5), row.names = c("accuracy","rulecount","rulelength","modelsize", "buildtime"))
colnames(df)<-algcombinations

count_strings <- function(input_string,pattern)
{
  matches <- gregexpr(pattern, input_string)
  all_matches <- regmatches(input_string, matches)
  count <- sum(lengths(all_matches))
  return(count)
}
autoQCBA <- function(train,train_disc,test_raw, classAtt,cutp,algs = c("CBA","CMAR","CPAR","PRM","FOIL2"), iterations=1  ){
  
  for (alg in algs)
  {
    algQCBA<-paste0(alg,"_QCBA")
    message(paste0("*** processing ", alg, " ***"))
    f_rule_model<-get(alg)
    message(paste0("** STARTED learning model with ", alg, " **"))
    start.time <- Sys.time()
    if (alg=="CMAR")
    {
      for (i in 1:iterations) arulesCBAModel <- CMAR(as.formula(paste(classAtt, " ~ .")), data = train_disc, supp = 0.1, conf=0.9)
    }
    else if (alg=="CPAR")
    {
      for (i in 1:iterations) arulesCBAModel <- CPAR(as.formula(paste(classAtt, " ~ .")), data = train_disc)
    }
    else if (alg=="PRM")
    {
      for (i in 1:iterations) arulesCBAModel <- PRM(as.formula(paste(classAtt, " ~ .")), data = train_disc)
    }  
    else if (alg=="FOIL2")
    {
      for (i in 1:iterations) arulesCBAModel <- FOIL2(as.formula(paste(classAtt, " ~ .")), data = train_disc)
    }  
    else if (alg=="CBA")
    {
      for (i in 1:iterations) arulesCBAModel <- CBA(formula=as.formula(paste(classAtt, " ~ .")), data = train_disc)
    }    
    end.time <- Sys.time()
    averageExecTime<-round(as.numeric((end.time - start.time)/iterations,units="secs"),2)

    message(paste(alg,"took:",averageExecTime, "seconds per iteration"))
    message(paste0("** FINISHED learning model with ", alg, " **"))
    message("** Original list of rules **")
    inspect(arulesCBAModel$rules)
    rulecount<-length(arulesCBAModel$rules)
    rulelength <- sum(arulesCBAModel$rules@lhs@data)/length(arulesCBAModel$rules)
    CBAmodel <- arulesCBA2arcCBAModel(arulesCBAModel, cutp,  train, classAtt)
    message("** List of rules after conversion to the new data structure **")
    inspect(CBAmodel@rules)
    message("** USE OF MODEL FOR PREDICTION **")    
    yhat <- predict(CBAmodel, test_raw)
    # acc<-mean(as.integer(as.character(yhat) == as.character(test_raw[, ncol(test_raw)])))
    acc<-CBARuleModelAccuracy(yhat, test_raw[[classAtt]])
    
    message(paste("Accuracy of ",alg,  "on test data is", acc, " with number of rules:",rulecount, "and average rule length",rulelength))
    df["accuracy",alg] <-round(acc,2)
    df["buildtime",alg] <-round(averageExecTime,2)
    df["rulecount",alg]<-round(rulecount,2)
    df["rulelength",alg]<-round(rulelength,2)    
    df["modelsize",alg]<-round(rulelength*rulecount,2)   
    message(paste0("** STARTED POSTPROCESSING ", alg, " model with QCBA **"))
    start.time <- Sys.time()
    qCBAmodel <- qcba(cbaRuleModel=CBAmodel,datadf=train)
    end.time <- Sys.time()
    rulecount<-qCBAmodel@ruleCount
    rulelength<- (sum(unlist(lapply(qCBAmodel@rules[1],count_strings,pattern=",")))+
                                      # assuming the last rule has antecedent length zero - not counting its length
                                      nrow(qCBAmodel@rules)-1)/nrow(qCBAmodel@rules)
    yhat <- predict(qCBAmodel, test_raw)
    acc<-CBARuleModelAccuracy(yhat, test_raw[[classAtt]])
    message(paste("Accuracy of ",alg,  "+QCBA on test data is", acc, " with number of rules:",rulecount, "and average rule length",rulelength))
    df["accuracy",algQCBA] <-acc
    df["buildtime",algQCBA] <-averageExecTime
    df["rulecount",algQCBA]<-rulecount    
    df["rulelength",algQCBA]<-rulelength    
    df["modelsize",algQCBA]<-round(rulelength*rulecount,2)   
    message(paste0("** FINISHED POSTPROCESSING ", alg, " model with QCBA **"))
    print(qCBAmodel@rules)
  }
  return(df)
}
stats<-autoQCBA(trainFold,train_disc,testFold,classAtt,discrModel$cutp)
print(stats)
