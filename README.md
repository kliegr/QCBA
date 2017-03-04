# Monotonicity Exploiting Association Rule Classification

This package for R implements the MARC algorithm aimed at postprocesing output of a CBA classifier.

 ```Liu, B. Hsu, W. and Ma, Y (1998). Integrating Classification and Association Rule Mining. Proceedings KDD-98, New York, 27-31 August. AAAI. pp 80-86.```
 
The [arc](https://github.com/kliegr/arc) package is used for generation of the CBA classifier.

## Features 
- Java implementation with R wrapper
- Supports one rule as well as multi rule classification
- Optional fuzzification of rules
- Optional annotation of rules with probability distributions
- Optional Post pruning -- less aggressive, generally slightly decreases accuracy as well as rule count
- Optional Continuous pruning -- more aggressive, generally higher decrease in accuracy as well as rule count

## Installation
Package  can be installed from the R environment using the devtools package.
```R
devtools::install_github("kliegr/marc")
```


## Examples

### One rule classification
```R
  library(rMARC)
  library(mlbench)
  data("PimaIndiansDiabetes")
  
  set.seed(111)
  allData <- PimaIndiansDiabetes[sample(nrow(PimaIndiansDiabetes)),]
  trainFold <- allData[1:500,]
  testFold <- allData[501:nrow(PimaIndiansDiabetes),]
  rmCBA <- cba(trainFold, classAtt="diabetes")
  prediction <- predict(rmCBA,testFold)
  acc <- CBARuleModelAccuracy(prediction, testFold[[rmCBA@classAtt]])
  print(paste("CBA Model with ",length(rmCBA@rules), " rules and accuracy ",acc))
  rmMARC <- marcExtend(cbaRuleModel=rmCBA,datadf=trainFold,continuousPruning=FALSE, postpruning=TRUE, fuzzification=FALSE, annotate=FALSE,ruleOutputPath="rules.xml")
  prediction <- predict(rmMARC,testFold,"oneRule")
  acc <- CBARuleModelAccuracy(prediction, testFold[[rmMARC@classAtt]])
  print(paste("MARC Model with ",rmMARC@ruleCount, " rules and accuracy ",acc))
  print(rmMARC@rules)
```

Output
```
[1] CBA Model with  53  rules and accuracy  0.753731343283582
[1] MARC Model with  47  rules and accuracy  0.753731343283582
```
MARC decreased the number of rules while keeping same accuracy.

If we actived the continuousPruning option, it would result in aggressive pruning:
```
[1] MARC Model with  28  rules and accuracy  0.67910447761194
```
### Multi rule classification
```R
  library(rMARC)
  library(mlbench)
  data("Ionosphere")
  library(rMARC)
  set.seed(111)
  allData <- Ionosphere[sample(nrow(Ionosphere)),]
  trainFold <- allData[1:300,]
  testFold <- allData[301:nrow(Ionosphere),]
  rmCBA <- cba(trainFold, classAtt="Class")
  prediction <- predict(rmCBA,testFold)
  acc <- CBARuleModelAccuracy(prediction, testFold[[rmCBA@classAtt]])
  print(paste("CBA Model with ",length(rmCBA@rules), " rules and accuracy ",acc))
  rmMARC <- marcExtend(cbaRuleModel=rmCBA,datadf=trainFold,continuousPruning=TRUE, postpruning=TRUE, fuzzification=FALSE, annotate=TRUE,ruleOutputPath="rules.xml")
  prediction <- predict(rmMARC,testFold,"mixture")
  acc <- CBARuleModelAccuracy(prediction, testFold[[rmMARC@classAtt]])
  print(paste("MARC Model with ",rmMARC@ruleCount, " rules and accuracy ",acc))
  print(rmMARC@rulePath)
```


Output
```
[1] CBA Model with  18  rules and accuracy  0.92156862745098
[1] MARC Model with  18  rules and accuracy  0.941176470588235
```

MARC improved accuracy while keeping the number of rules.
Rules in MARC multi rule model cannot be currently  visualized - they are stored in a file.
