# Quantitative CBA

[![](https://www.r-pkg.org/badges/version/qCBA)](https://cran.r-project.org/web/packages/qCBA/index.html)
 
The [arc](https://github.com/kliegr/arc) package is used for generation of the CBA classifier, which is postprocessed by the QCBA R package.

[Quantitative CBA (QCBA)](https://link.springer.com/article/10.1007/s10489-022-04370-x) is a postprocessing algorithm for association rule classification algorithm CBA, which implements a number of 
optimization steps to improve handling of quantitative (numerical) attributes. As a result, the rule-based classifiers become typically smaller and often more accurate. The QCBA approach is described in:
 ```
Tomas Kliegr and Ebroul Izquierdo. "Quantitative CBA: Small and Comprehensible Association Rule Classification Models." Applied Intelligence https://link.springer.com/article/10.1007/s10489-022-04370-x (2023).
 ```

## Inputs for  QCBA
- Rule model (rule list/set) learnt by an arbitrary rule learning algorithm on prediscretized data
- Raw dataset (before discretization) 

## Output for  QCBA
- Rule list: rules are sorted. Tthe first  rule matching a given instances is used to classify it.

## How QCBA works?
QCBA obtains a rule model from an arbitrary rule learning algorithm and postprocesses using the following algorithms:

- **Refitting rules** Literals originally aligned to borders of the discretized  regions are refit to finer grid.
- **Attribute pruning** Remove redundant attributes from rules. 
- **Trimming** Literals in discovered rules are trimmed so that they do not contain regions not covered by data.
- **Extension** Ranges of literals in the body of each rule are extended, escaping from the coarse hypercubic created by discretization.
- **Data coverage pruning** Remove some of the newly redundant rules
- **Default rule overlap pruning** Some rules that classify into the same class as the default rule in the end of the classifier can be removed. 

These algorithms are explained in the [article](https://link.springer.com/article/10.1007/s10489-022-04370-x) and in an interactive [tutorial](http://nb.vse.cz/~klit01/qcba/tutorial.html)(sources [here](https://github.com/kliegr/QCBA/blob/master/man/tutorial.Rmd)).

## Installation
First, you need to have correctly installed [rJava](https://cran.r-project.org/web/packages/rJava/index.html) package. 
For instructions on how to setup rJava please refer to [rJava documentation](https://cran.r-project.org/web/packages/rJava/index.html).
Note a common issue with installing and configuring rJava can be resolved according to these [instructions](https://stackoverflow.com/questions/3311940/r-rjava-package-install-failing).

The latest version can be installed from the R environment with:
```R
devtools::install_github("kliegr/QCBA")
```
## Example

### Baseline CBA model

Learn a CBA classifier.
```R
library(arc)
set.seed(111)
allData <- datasets::iris[sample(nrow(datasets::iris)),]
trainFold <- allData[1:100,]
testFold <- allData[101:nrow(datasets::iris),]
classAtt<-"Species"
y_true <-testFold[[classAtt]]
rmCBA <- cba(trainFold, classAtt=class)
predictionBASE <- predict(rmCBA,testFold)
inspect(rmCBA@rules)
```
The model:

        lhs                                                    rhs                  support confidence lift     lhs_length
    [1] {Petal.Length=[-Inf;2.6],Petal.Width=[-Inf;0.8]}    => {Species=setosa}     0.32    1.00       3.125000 2         
    [2] {Petal.Length=(2.6;4.75],Petal.Width=(0.8;1.75]}    => {Species=versicolor} 0.30    1.00       2.777778 2         
    [3] {Sepal.Length=(5.85; Inf],Petal.Length=(5.15; Inf]} => {Species=virginica}  0.25    1.00       3.125000 2         
    [4] {Sepal.Width=[-Inf;3.05],Petal.Width=(1.75; Inf]}   => {Species=virginica}  0.18    1.00       3.125000 2         
    [5] {}                                                  => {Species=versicolor} 0.36    0.36       1.000000 0 

The statistics:
```R
 print(paste0(
 "Number of rules: ",length(rmCBA$rules), ", ",
 "Total conditions: ",rmCBA$rules@lhs@data, ", ", 
 "Accuracy: ", round(CBARuleModelAccuracy(predictionBASE, y_true),2)))
```
Returns:

      Number of rules: 5, Total conditions: 8, Accuracy: 0.94

### QCBA model
Learn a QCBA model.
```R
library(qCBA)
rmQCBA <- qcba(cbaRuleModel=rmCBA,datadf=trainFold)
predictionQCBA <- predict(rmQCBA,testFold)
print(rmQCBA@rules)
``` 
The model:

        lhs                                                    rhs                  support confidence lift     lhs_length
    [1] {Petal.Width=[-Inf;0.6]}                            => {Species=setosa}     0.32    1.00       3.125000 2         
    [2] {Petal.Length=[5.2;Inf]}                            => {Species=virginica}  0.25    1.00       3.125000 2         
    [3] {Sepal.Width=[-Inf;3.1],Petal.Width=[1.8;Inf]}      => {Species=virginica}  0.20    1.00       3.125000 2         
    [4] {}                                                  => {Species=versicolor} 0.36    0.36       1.000000 0 

The statistics:
```R
 print(paste0(
 "Number of rules: ",length(rmQCBA$rules), ", ",
 "Total conditions: ",rmQCBA@rules$condition_count, ", ", 
 "Accuracy: ", round(CBARuleModelAccuracy(predictionQCBA, y_true),2)))
```
Returns:

    Number of rules:  4 , average number of conditions per rule : 1 , accuracy on test data:  0.96

QCBA:
- Improved accuracy from 0.94 to 0.96
- Reduced number of rules from 5 to 4
- Reduced number of conditions in the rules from 1.6 to 1
- Unlike other ARC approaches retains interpretability of CBA models by performing one rule classification.

### New feature - ROC and AUC curves
```R
library(ROCR)
library(qCBA)
twoClassIris<-datasets::iris[1:100,]
twoClassIris <- twoClassIris[sample(nrow(twoClassIris)),]
#twoClassIris$Species<-as.factor(as.character(iris$Species))
trainFold <- twoClassIris[1:75,]
testFold <- twoClassIris[76:nrow(twoClassIris),]
rmCBA <- cba(trainFold, classAtt="Species")
rmqCBA <- qcba(cbaRuleModel=rmCBA, datadf=trainFold)
print(rmqCBA@rules)
prediction <- predict(rmqCBA,testFold)
acc <- CBARuleModelAccuracy(prediction, testFold[[rmqCBA@classAtt]])
message(acc)
confidences <- predict(rmqCBA,testFold,output,outputConfidenceScores=TRUE,positiveClass="setosa")
#it is importat that the first level is different from positiveClass specified in the line above
target<-droplevels(factor(testFold[[rmqCBA@classAtt]],ordered = TRUE,levels=c("versicolor","setosa")))

pred = ROCR::prediction(confidences, target)
roc = ROCR::performance(pred, "tpr", "fpr")
plot(roc, lwd=2, colorize=TRUE)
lines(x=c(0, 1), y=c(0, 1), col="black", lwd=1)
auc = ROCR::performance(pred, "auc")
auc = unlist(auc@y.values)
auc
``` 
