# Monotonicity Exploiting Association Rule Classification

# Quantitative CBA
Quantitative CBA (QCBA) is a postprocessing algorithm for association rule classification algorithm CBA, which implements a number of 
optimization steps to improve handling of quantitative (numerical) attributes. The viable properties of these rule lists that make CBA classification  models most comprehensible among all association rule classification algorithms, such as one-rule classification and crisp rules, are retained. The postprocessing is conceptually fast, because it is performed on a relatively small number of rules that passed the pruning steps, and  can be adapted also for multi-rule classification algorithms. Benchmark of our QCBA approach on 22 UCI datasets shows 40 to 58\% decrease in the total size of the model as measured by the total number of conditions in all rules. Model accuracy remains on the same level as for CBA with QCBA even providing small improvement over CBA on 11 of the 22 datasets. 

The reference for CBA:

 ```
 Liu, B. Hsu, W. and Ma, Y (1998). Integrating Classification and Association Rule Mining. Proceedings KDD-98, New York, 27-31 August. AAAI. pp 80-86.
 ```
 
The [arc](https://github.com/kliegr/arc) package is used for generation of the CBA classifier.
## Feature Tutorial
Look at the [tutorial](http://nb.vse.cz/~klit01/qcba/tutorial.html), which  visually demonstrates all the optimization steps in QCBA:

- **Refitting rules** to value grid. Literals originally aligned to borders of the discretized  regions are refit to finer grid.
- **Attribute pruning**. Remove redundant attributes from rules. 
- **Trimming.** Literals in discovered rules are trimmed so that they do not contain regions not covered by data.
- **Extension.** Ranges of literals in the body of each rule are extended, escaping from the coarse hypercubic created by discretization.
- **Data coverage pruning.** Remove some of the newly redundant rules
- **Default rule overlap pruning.** Some rules that classify into the same class as the default rule in the end of the classifier can be removed. 

## Installation
Package  can be installed from the R environment using the devtools package.
```R
devtools::install_github("kliegr/QCBA")
```

## Examples

```R
  library(qCBA)
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
  rmQCBA <- qcba(cbaRuleModel=rmCBA,datadf=trainFold)
  prediction <- predict(rmQCBA,testFold,"oneRule")
  acc <- CBARuleModelAccuracy(prediction, testFold[[rmMARC@classAtt]])
  print(paste("QCBA Model with ",rmQCBA@ruleCount, " rules and accuracy ",acc))
  print(rmQCBA@rules)
```

Output
```
[1] CBA Model with  53  rules and accuracy  0.720149253731343
[1] QCBA Model with  43  rules and accuracy  0.720149253731343
```
QCBA decreased the number of rules while keeping same accuracy.

If we actived the defaultRuleOverlapPruning option, it would result in aggressive pruning:


```R
  rmQCBA <- qcba(cbaRuleModel=rmCBA,datadf=trainFold,defaultRuleOverlapPruning="transactionBased")
  prediction <- predict(rmQCBA,testFold,"oneRule")
  acc <- CBARuleModelAccuracy(prediction, testFold[[rmMARC@classAtt]])
  print(paste("QCBA Model with ",rmQCBA@ruleCount, " rules and accuracy ",acc))
  print(rmQCBA@rules)
```
The resulting model has 44% less rules than the CBA model: 
```
[1] QCBA Model with  30  rules and accuracy  0.694029850746269
```