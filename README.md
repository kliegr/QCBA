# Quantitative CBA

[![](http://cranlogs.r-pkg.org/badges/qCBA)](http://cran.rstudio.com/web/packages/qCBA/index.html)
[![Travis-CI Build Status](https://travis-ci.org/kliegr/QCBA.svg?branch=master)](https://travis-ci.org/kliegr/QCBA)
[![](https://www.r-pkg.org/badges/version/qCBA)](https://cran.r-project.org/web/packages/qCBA/index.html)




[Quantitative CBA (QCBA)](https://arxiv.org/abs/1711.10166) is a postprocessing algorithm for association rule classification algorithm CBA, which implements a number of 
optimization steps to improve handling of quantitative (numerical) attributes. The viable properties of these rule lists that make CBA classification  models most comprehensible among all association rule classification algorithms, such as one-rule classification and crisp rules, are retained. The postprocessing is conceptually fast, because it is performed on a relatively small number of rules that passed the pruning steps, and  can be adapted also for multi-rule classification algorithms. Benchmarks show about 50% decrease in the total size of the model as measured by the total number of conditions in all rules. Model accuracy generally remains on the same level as for CBA with QCBA even providing small improvement over CBA on 11 of the 22 datasets involved in our benchmark. 


 ```
Kliegr, Tomas. "Quantitative CBA: Small and Comprehensible Association Rule Classification Models." arXiv preprint arXiv:1711.10166 (2017).
 ```
 
The [arc](https://github.com/kliegr/arc) package is used for generation of the CBA classifier, which is postprocessed by the QCBA R package.

## Feature Tutorial
The [tutorial](http://nb.vse.cz/~klit01/qcba/tutorial.html)  visually demonstrates all the optimization steps in QCBA:

- **Refitting rules** Literals originally aligned to borders of the discretized  regions are refit to finer grid.
- **Attribute pruning** Remove redundant attributes from rules. 
- **Trimming** Literals in discovered rules are trimmed so that they do not contain regions not covered by data.
- **Extension** Ranges of literals in the body of each rule are extended, escaping from the coarse hypercubic created by discretization.
- **Data coverage pruning** Remove some of the newly redundant rules
- **Default rule overlap pruning** Some rules that classify into the same class as the default rule in the end of the classifier can be removed. 

The R Markdown source for this tutorial is located [here](https://github.com/kliegr/QCBA/blob/master/man/tutorial.Rmd). Note that while GitHub displays the syntax, it does not run the code or even display the knitted HTML. For this reason, it is recommended to view the tutorial [outside github](http://nb.vse.cz/~klit01/qcba/tutorial.html).

## Prerequisites
The qCBA package depends on Java 8, and correctly installed [rJava](https://cran.r-project.org/web/packages/rJava/index.html) package. On Linux, even if you have Java installed, it [might be necessary](https://stackoverflow.com/questions/3311940/r-rjava-package-install-failing) to install it again with
```bash
apt-get install r-cran-rjava
```

For instructions on how to setup rJava please refer to [rJava documentation](https://cran.r-project.org/web/packages/rJava/index.html) .
## Installation
The package version available on CRAN is [![](https://www.r-pkg.org/badges/version/qCBA)](https://cran.r-project.org/web/packages/qCBA/index.html).

The latest version can be installed from the R environment using the devtools package.
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
rmCBAiris <- cba(trainFold, classAtt="Species")
inspect(rmCBAiris@rules)
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
library(stringr)
prediction_iris <- predict(rmCBAiris,testFold)
acc <- CBARuleModelAccuracy(prediction_iris, testFold[[rmCBAiris@classAtt]])
avgRuleLengthCBA <- sum(rmCBAiris@rules@lhs@data)/length(rmCBAiris@rules)
print(paste("Number of rules: ",length(rmCBAiris@rules),", average number of conditions per rule :",round(avgRuleLengthCBA,2), ", accuracy on test data: ",round(acc,2)))
```
Returns:

      Number of rules:  5 , average number of conditions per rule : 1.6 , accuracy on test data:  0.94

### QCBA model
Learn a QCBA model.
```R
library(qCBA)
rmCBA4QCBAiris <- cba(trainFold, classAtt="Species",pruning_options=list(default_rule_pruning=FALSE))
rmqCBAiris <- qcba(cbaRuleModel=rmCBA4QCBAiris,datadf=trainFold)
print(rmqCBAiris@rules)
``` 
The model:

        lhs                                                    rhs                  support confidence lift     lhs_length
    [1] {Petal.Width=[-Inf;0.6]}                            => {Species=setosa}     0.32    1.00       3.125000 2         
    [2] {Petal.Length=[5.2;Inf]}                            => {Species=virginica}  0.25    1.00       3.125000 2         
    [3] {Sepal.Width=[-Inf;3.1],Petal.Width=[1.8;Inf]}      => {Species=virginica}  0.20    1.00       3.125000 2         
    [4] {}                                                  => {Species=versicolor} 0.36    0.36       1.000000 0 

The statistics:
```R
prediction_iris <- predict(rmqCBAiris,testFold)
acc <- CBARuleModelAccuracy(prediction_iris, testFold[[rmqCBAiris@classAtt]])
avgRuleLengthQCBA <- (sum(unlist(lapply(rmqCBAiris@rules[1],str_count,pattern=",")))+
                              # assuming the last rule has antecedent length zero - not counting its length
                              nrow(rmqCBAiris@rules)-1)/nrow(rmqCBAiris@rules)
print(paste("Number of rules: ",nrow(rmqCBAiris@rules),", average number of conditions per rule :",avgRuleLengthQCBA, ", accuracy on test data: ",round(acc,2)))
``` 
Returns:

    Number of rules:  4 , average number of conditions per rule : 1 , accuracy on test data:  0.96

QCBA:
- Improved accuracy from 0.94 to 0.96
- Reduced number of rules from 5 to 4
- Reduced number of conditions in the rules from 1.6 to 1
- Unlike other ARC approaches retains interpretability of CBA models by performing one rule classification.


