# Changes in 0.0.2 #

## Enhancements ##
  * improved manual.pdf
  * new example mln: alignment for machine translation
  * added inspection scripts for the example mlns that can be used as prototypes for user projects
  * Perceptron learning rate and decay can be specified

## Bugfixes ##
  * Online learner creates temporary files in a platform independent way now

# Version 0.0.1 #

## Features ##
  * MAP Inference using Cutting Plane Inference and two possible base solvers
    * Integer Linear Programming using lp\_solve
    * MaxWalkSAT
  * Online Learning with three possible update rules:
    * MIRA
    * Perceptron
    * Passive-Aggressive
  * Interactive shell to inspect data and model