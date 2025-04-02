# [PSSC](https://doi.org/XXXXX)

Original work: https://link.springer.com/article/10.1007/s10732-025-09552-7
Original repo: https://github.com/LmR308/PSSC

## Abstract
The partial set covering problem (PSCP) is a significant combinatorial optimization problem that finds applications in numerous real-world scenarios. The objective of PSCP is to encompass a minimum number of subsets while ensuring the coverage of at least n elements. Due to its NP-hard nature, solving large-scale PSCP efficiently remains a critical issue in computational intelligence. 

## Authors
Authors involved in this work and their respective contributions:
- Person1.
- Person2.
## Datasets

Instances are categorized in different datasets inside the 'instances' folder. All instances are from the [LmR308/PSSC]([https://archive.ics.uci.edu/ml/index.php](https://github.com/LmR308/PSSC)).

### Instance format

(Explain instance format so other users may easily use them even if not using your code.)


## Compiling

You can easily compile and build an executable artifact of this project using Maven and a recent version of Java (17+):
```text
mvn clean package
```

## Executing

Executable artifacts are generated inside the `target` directory. For ease of use, there is an already executable JAR inside the target folder.
To review a full list of configurable parameters, see the `application.yml`, or review the [configuration section of the Mork documentation](https://docs.mork-optimization.com/en/latest/features/config/).

Example 1: execute default experiment with the default set of instances
```text
java -jar target/PSSC.jar 
```

Example: execute the IteratedGreedyExperiment using a different set of instances, located inside the `newinstances` folder.
```
java -jar target/PSSC.jar --instances.path.default=newinstances --solver.experiment=IteratedGreedyExperiment
```

## Cite

Consider citing our paper if used in your own work:
(Fill with the references to your own published work)

### DOI
https://doi.org/XXXXXXX

### Bibtex
```bibtex
@article{
...
}
```

## Powered by MORK (Metaheuristic Optimization framewoRK)
| ![Mork logo](https://user-images.githubusercontent.com/55482385/233611563-4f5c91f2-af36-4437-a4b5-572b6655487a.svg) | Mork is a Java framework for easily solving hard optimization problems. You can [create a project](https://generator.mork-optimization.com/) and try the framework in under one minute. See the [documentation](https://docs.mork-optimization.com/en/latest/) or the [source code](https://github.com/mork-optimization/mork). |
|--|--|
