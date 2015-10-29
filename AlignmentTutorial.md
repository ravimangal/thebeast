# Introduction #

This page will give a brief tutorial that covers some of thebeast's features. As task we look at a vanilla version of bilingual alignment: determine which tokens of a sentence in one language translate to which tokens in the same sentence written in another language.

For example, for the English sentence

> Haag plays Elianti

and the German sentence

> Haag spielt Elianti

an alignment could look like:

|         | Haag | plays | Elianti |
|:--------|:-----|:------|:--------|
| Haag    |  x   |       |         |
| spielt  |      |   x   |         |
| Elianti |      |       |    x    |


The MLN we describe here can be found in `examples/align/toy.pml`.

While `toy.pml` helps to get a rough overview, it does reflect the normal Machine Learning workflow:
usually we would train a model and then run tests with the same model on several datasets. However,
with toy.pml we would need to train again everytime we want to test our model on a new test corpus.

Thus this directory also contains three more files: `init.pml`, `train.pml` and `test.pml`.
Calling them in this order will first do some data preprocessing, then training and  finally testing.
This setup allows to do test the same model on different test tests. Moreover, unless the formulae
of the MLN have been changed the init.pml file does not need to be executed when we train
on the same data but change some training parameters.

# Starting thebeast #

You can execute the commands in this tutorial by simply calling
```
$ thebeast toy.pml
```
or by running thebeast in interactive mode and typing in the statements presented here. For this just start
```
$ thebeast
```

All assuming that you `cd` into `examples/align` before and that `thebeast/bin` is in your $PATH$.


# Overview #

This tutorial assumes that you already looked at the [first tutorial](SRLTutorial.md) and/or are familar with some of the features of `thebeast`. The structure of the main file, `toy.pml`,  is essentially the same. Instead of discussing this structure again we will focus on the actual MLN that captures our domain knowledge through a set of formulae.

The top level file that describes the alignment MLN is `align.pml`. It begins with some predicate definitions and goes on to include the files `align-global` which contains global formulae (relating two or more hidden ground atoms) and local ones (with only one ground atom).

```
/* Predicate definitions */

// The words of the source string
predicate src_word: Int x SourceWord;

// The words of the target string
predicate tgt_word: Int x TargetWord;

// the true alignments: align(src,tgt) means that token src in the source is aligned
// to token tgt in the target string
predicate align: Int x Int;

// Word to word translation probabilities from IBM model 4
// Note that in practice it might make more sense to provide these probabilties
// one a instance-by-instance basis. This table could become very large and as is
// thebeast would keep it in memory.
predicate model4: SourceWord x TargetWord x Double;

/* Loading the MLN formulae (local and global ones) */
include "align-global.pml";
include "align-local.pml";

/* Defining which predicates are hidden, observed and global. Do not forget this! */
observed: src_word, tgt_word;
hidden: align;
global: model4;
```

In this tutorial we will focus on the new types of local and global formulae introduced in `align-local.pml` and `align-global.pml`, respectively.

# Local Formulae #

The interesting new formula is the following:

```
weight w_model4: Double;
factor: for Int s, SourceWord w_s, Int t, TargetWord w_t, Double prob
  if src_word(s,w_s) & tgt_word(t,w_t) & model4(w_s,w_t,prob) add [align(s,t)] * prob * w_model4;
```

This is an example for real valued formulae. It tests whether the tokens of the word have a IBM model 4 translation probability (taken from the global predicate `model4` loaded from `global.atoms`). If so we add a score scaled by a weight and the probability for this token pair.

# Global Formulae #

This tutorial introduces weighted global formulae. While the [first tutorial](SRLTutorial.md) used deterministic constraints to enforce certain properties of a role labelling, in this tutorial we present an instance of a weighted global formula:

```
weight w_diag: Double+;
factor: for Int s, Int t 
	if src_word(s,_) & src_word(s+1,_) & tgt_word(t,_) & tgt_word(t+1,_)
	add [align(s,t) => align(s+1,t+1)] * w_diag;
```

It essentially rewards solutions where words are translated in a diagonal fashion. For many language pairs this is often true, but does not hold all the time. Thus we just assign a higher score to more diagonal solutions without forbidding nondiagonal ones.

Note the type of the weight `Double+`. This means that `w_diag` will be nonnegative. **This is crucial**. It has to do with the way Cutting Plane Inference works and is described in more detail in the manual. However, **it is important for you to remember the following rule of thumb**:

  * If most ground formulae are false in the actual gold solution the weight function must be nonpositive (Double-)

  * If most ground formulae are true in the actual gold solution the weight function must be nonpositive (Double+)

# Inspection #
The alignment example also shows how to inspect the local features for a particular ground atom:
```

// go to next instance, solve it, print the solution and a comparison with the gold standard
next; solve; print atoms.align; print eval;

// print the local features for ground atom align(1,1)
print solver.features.align(1,1);
```

The final command should give you the following output
```
for align(1, 1) add 2.0 * w_model4()[0.2301386981000224]
for align(1, 1) add 1.0 * w_bias()[-0.1857060643698165]
```
which shows the contribution of the model 4 feature and the contribution of a bias feature that penalizes the existence of every true align ground atom.



