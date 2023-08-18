# Quantitative CBA: Small and Comprehensible Association Rule Classification Models 

[![](https://www.r-pkg.org/badges/version/qCBA)](https://cran.r-project.org/web/packages/qCBA/index.html)
 
[Quantitative CBA (QCBA)](https://link.springer.com/article/10.1007/s10489-022-04370-x) is a postprocessing algorithm intended for some machine learning models generating rule-based classifiers based on pre-discretized data.  QCBA implements a number of 
optimization steps to improve handling of quantitative (numerical) attributes. Depending on input dataset and used rule classifier, the postprocessed models may become smaller and/or more accurate. 
The QCBA approach is described in:
 ```
Tomas Kliegr and Ebroul Izquierdo. "Quantitative CBA: Small and Comprehensible Association Rule Classification Models." Applied Intelligence https://link.springer.com/article/10.1007/s10489-022-04370-x (2023).
 ```

## Inputs for  QCBA
- Rule model (rule list/set) learnt by a supported rule learning algorithm on prediscretized data
- Raw dataset (before discretization) 

## Output of QCBA
- Rule list: rules are sorted. The first  rule matching a given instances is used to classify it.

## Which postprocessing steps are performed by QCBA?

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
This example shows how to learn the standard association-rule based classification model 
using the CBA algorithm (as implemented by the [arc package](https://github.com/kliegr/arc)) 
and then how to postprocess it with QCBA.

### Baseline CBA model

Learn a CBA classifier.
```R
library(arc)
set.seed(25)
allData <- datasets::iris[sample(nrow(datasets::iris)),]
trainFold <- allData[1:100,]
testFold <- allData[101:nrow(datasets::iris),]
classAtt<-"Species"
y_true <-testFold[[classAtt]]
rmCBA <- cba(trainFold, classAtt=classAtt)
predictionBASE <- predict(rmCBA,testFold)
inspect(rmCBA@rules)
```
The model:

    lhs                                                   rhs                  support confidence coverage lift     count lhs_length orderedConf orderedSupp cumulativeConf
    [1] {Petal.Length=(5.05; Inf]}                         => {Species=virginica}  0.34    1.00       0.34     2.702703 34    1          1.0        34          1.00      
    [2] {Petal.Length=[-Inf;2.45]}                         => {Species=setosa}     0.32    1.00       0.32     3.125000 32    1          1.0        32          1.00        
    [3] {Petal.Length=(2.45;5.05], Petal.Width=(0.8;1.55]} => {Species=versicolor} 0.29    1.00       0.29     3.225806 29    2          1.0        29          1.00         
    [4] {}                                                 => {Species=virginica}  0.37    0.37       1.00     1.000000 37    0          0.6        3         0.98        

The statistics:
```R
print(paste0(
  "Number of rules: ",length(rmCBA@rules), ", ",
  "Total conditions: ",sum(rmCBA@rules@lhs@data), ", ", 
  "Accuracy: ", round(CBARuleModelAccuracy(predictionBASE, y_true),2)))
```
Returns:

      Number of rules: 4, Total conditions: 4, Accuracy: 0.92

### Postprocessing CBA model with QCBA
Learn a QCBA model.
```R
library(qCBA)
rmQCBA <- qcba(cbaRuleModel=rmCBA,datadf=trainFold)
predictionQCBA <- predict(rmQCBA,testFold)
print(rmQCBA@rules)
``` 
The model:

                                                                  rules support confidence condition_count orderedConf orderedSupp
    1                         {Petal.Length=[-Inf;1.9]} => {Species=setosa}    0.32       1.00               1   1.0000000          32
    2 {Petal.Length=[-Inf;5.5],Petal.Width=[1;1.6]} => {Species=versicolor}    0.29       1.00               2   1.0000000          29
    3                                             {} => {Species=virginica}    0.37       0.37               0   0.9487179          37
The statistics:
```R
 print(paste0(
 "Number of rules: ",nrow(rmQCBA$rules), ", ",
 "Total conditions: ",sum(rmQCBA@rules$condition_count), ", ", 
 "Accuracy: ", round(CBARuleModelAccuracy(predictionQCBA, y_true),2)))
```
Returns:

    Number of rules: 3, Total conditions: 3, Accuracy:  0.96

Effect of QCBA:
- Improved accuracy from 0.92 to 0.96
- Reduced number of rules from 4 to 3
- Reduced total conditions in the rules from 4 to 3

### Postprocessing other rule models with QCBA
The QCBA package is compatible also with other software generating rule models.
This example shows how it can be used to postprocess models generated by
`arulesCBA` package, which offers a range of rule models including 
CPAR, CMAR, FOIL2,  and PRM.

When using `arulesCBA`, it is required to perform discretization of data externally 
and pass the generated cutpoints to QCBA:

```R
set.seed(54)
library(arulesCBA)
allData <- datasets::iris[sample(nrow(datasets::iris)),]
trainFold <- allData[1:100,]
testFold <- allData[101:nrow(datasets::iris),]
classAtt <- "Species"
discrModel <- discrNumeric(trainFold, classAtt)
train_disc <- as.data.frame(lapply(discrModel$Disc.data, as.factor))
cutPoints <- discrModel$cutp
test_disc <- applyCuts(testFold, cutPoints, infinite_bounds=TRUE, labels=TRUE)
y_true <-testFold[[classAtt]]
```
Learn and evaluate CPAR model:
```R
rmBASE <- CPAR(train_disc, formula=as.formula(paste(classAtt,"~ .")))
predictionBASE <- predict(rmBASE,test_disc) # CPAR (arulesCBA) predict function 
```
Learn and evaluate QCBA model:
```R
# Convert CPAR model to QCBA ecosystem datastructure
baseModel_arc <- arulesCBA2arcCBAModel(rmBASE, cutPoints,  trainFold, classAtt)

rmQCBA <- qcba(cbaRuleModel=baseModel_arc,datadf=trainFold)
predictionQCBA <- predict(rmQCBA,testFold) 
```
Compare CPAR and CPAR+QCBA:
```R
print(paste("CPAR: Number of rules: ",length(rmBASE$rules),", Total conditions:",sum(rmBASE$rules@lhs@data), ", Accuracy: ",round(CBARuleModelAccuracy(predictionBASE, y_true),2)))

print(paste("QCBA+CPAR: Number of rules: ",nrow(rmQCBA@rules),", Total conditions:",sum(rmQCBA@rules$condition_count), ", Accuracy: ",round(CBARuleModelAccuracy(predictionQCBA, y_true),2)))
```

Returns:

    CPAR: Number of rules:  8, total conditions: 10, Accuracy:  0.98
    QCBA+CPAR: Number of rules:  3, total conditions: 2, Accuracy:  1

Effect of QCBA:
- Improved accuracy from 0.98 to 1.00
- Reduced number of rules from 8 to 3
- Reduced total conditions in the rules from 10 to 2

Note that QCBA is not generally guaranteed to improve the accuracy of input model or reduce it size.

### Documentation
 - [Reference manual](https://cran.r-project.org/web/packages/qCBA/qCBA.pdf)
 - [Research article](https://link.springer.com/article/10.1007/s10489-022-04370-x)
 - [Tutorial](http://nb.vse.cz/~klit01/qcba/tutorial.html)(sources [here](https://github.com/kliegr/QCBA/blob/master/man/tutorial.Rmd)).