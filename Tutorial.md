# Introduction #

This page will give a brief tutorial that covers some of thebeast's features. As task we look at a vanilla version of Semantic Role Labelling (SRL): finding out which are arguments of a verb and what roles do they play. For example, in

> Haag<sub>A0</sub> plays Elianti<sub>A1</sub>

Haag is the agent ("A0") and Elianti the patient ("A1") of the verb "play".

The MLN we describe here can be found in `examples/srl/toy.pml`.

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

All assuming that you `cd` into `examples/srl` before and that `thebeast/bin` is in your $PATH$.

# Defining Types #
First we define the types of constants that will appear in our world. We will save these definitions in an external file `srl-types.pml` (this is not mandatory, they could also be in `toy.pml`.
```
type Word: ...;
type Pos: ...;
type Role: ...;
```
Here `Word` is a set of words, `Pos` a set of Part Of Speech tags and `Role` the set of possible roles arguments can have.

Note that Instead of explicitely defining the constants we use the "..." notation to indicate that constants should be added when seen in the data. We could also write
```
type Role: "A0","A1";
```

In `toy.pml` we include the type definitions by calling
```
include "srl-types.pml";
```

# Defining Predicates #
We will the describe a sentence and possible role labelling using logical predicates. To define these we start a new file called `srl.pml`. Again it is possible to specify these predicates inline in the top level `toy.pml` file.
```
/* Predicate definitions */

// The word predicate maps token indices (Int=Integer) to words: word(t,w) means that token t has word w.
// Note that when we say "maps" we mean "implicitly maps" -- a predicate itself only maps to boolean values.
predicate word: Int x Word;

// The pos predicate maps token indices to Part of Speech tags. I.e: pos(t,p) means that token t has tag p.
predicate pos: Int x Pos;

// The role predicate role(p,a,r) indicates that the token a is an argument of token p with role r.
predicate role: Int x Int x Role;

// the unique predicate denotes roles which cannot appear more than once (like "A0")
predicate unique: Role;
```

# Defining Formulae #
We are now ready to define some MLN formulae that describes our intuition of Semantic Role Labelling.

## Global Formulae ##
The main aim of languages such as Markov Logic is to allow in incorporation of global correlations between
decisions that go beyond the linear dependencies of linear-chain CRFs etc. In this tutorial we will use two of such correlations and save them in `srl-global.pml`.
```
// this formula ensures that an argument cannot be have more than one role wrt to
// one predicate. 
factor: for Int p, Int a if word(p,_) & word(a,_) : |Role r: role(p,a,r)| <=1;

// this formula ensures that an SRL predicate cannot have more than one argument with
// a unique role, such as "A0"
factor: for Int p, Role r if word(p,_) & unique(r) : |Int a: word(a,_) & role(p,a,r)| <=1;
```
Note that both of these formulae are deterministic. In future tutorials we will also introduce global soft formulae.
Also note that `thebeast` allows cardinality constraints.

We include this file using
```
include "srl-global.pml";
```
in `srl.pml`

## Local Formulae ##
We also define a set of formulae that only consider a single hidden `role(p,a,r)` decision. In Natural Language Processing such formulae are often extremely powerful and not many global ones are actually needed. We write these formulae into `srl-local.pml`.
```
// This formula tests whether the argument is to the left of the (SRL) predicate
weight w_left: Role -> Double;
factor: for Int p, Int a, Role r
  if word(p,_) & word(a,_) & a < p add [role(p,a,r)] * w_left(r);

// This formula checks the POS tag of the predicate and argument token
weight w_pos_pa: Pos x Pos x Role -> Double;
factor: for Int p, Int a, Pos p_pos, Pos a_pos, Role r
  if pos(p,p_pos) & pos(a,a_pos) add [role(p,a,r)] * w_pos_pa(p_pos,a_pos,r);

// This formula checks the POS tag of the predicate token
weight w_pos_p: Pos x Role -> Double;
factor: for Int p, Int a, Pos p_pos, Role r
  if pos(p,p_pos) & pos(a,_) add [role(p,a,r)] * w_pos_p(p_pos,r);
```
By default features are only considered if they have been seen at least once in the training set. For the formula above this means that we cannot learn from negative examples. The command below enforces that every possible POS tag & role combination (argument to w\_pos\_p) can have a nonzero weight:

```
set collector.all.w_pos_p = true;
```

Finally we add
```
include "srl-global.pml";
```
to `srl.pml`.

# Defining Hidden, Observed and Global Predicates #
Now we specify which information has to be inferred (hidden), which is given but different for every possible world (observed) and which is static and holds for all possible worlds (global).
```
observed: word,pos;
hidden: role;
global: unique;
```

This concludes our model of the SRL task in `srl.pml`

# Defining Global Static Data #
Which arguments roles have to be unique with respect to one verb is information that is static for all worlds. Thus we defined `unique` as a global predicate. In order to specify which roles are supposed to be unique we create a file `global.atoms` with
```
>unique
"A0"
"A1"
```
Note that we don't need `>>` lines because there is only one world we need to specify.

We load this data by calling
```
load global from "global.atoms";
```
in `toy.pml`


# Adding Training Data #
Now we will add some training data in order to train the weights of our MLN. In Markov Logic data is defined in terms of possible worlds: collections of ground atoms. We write a set of possible worlds into a `train.atoms` file:
```
>>
>word
0 "Haag"
1 "plays"
2 "Elianti"
3 "."

>role
1 0 "A0"
1 2 "A1"

>pos
0 "NNP"
1 "VBZ"
2 "NNP"
3 "P"

>>
>word
0 "He"
1 "plays"
2 "the"
3 "fool"
4 "."

>role
1 0 "A0"
1 3 "A1"

>pos
0 "NNP"
1 "VBZ"
2 "DT"
3 "NN"
4 "P"
```

We load this data by calling
```
load corpus from "train.atoms";
```
in `toy.pml`

# Instantiating Weights #
Different weights are assigned to different groundings of our formulae. For example in the second local formulae we use different weights for different combinations of POS tags of the verb and argument and the role given to the argument. Before we learn this weights we would like to define which of these weights are allowed to be nonzero. For example, it is common to only use features that have been seen in training data at least once (or `k` times).

To define which weights are allowed to be nonzero we collect all active features from the training set and count them using
```
collect;
```

Depending on the settings we made before (such as the `set collect.all...` statement in `srl-local.pml` we now get a set of weights which are allowed to be nonzero. We can print them out by calling
```
print weights;
```

This should produce the following screen output
```
>w_left
"A0"            0.000000        

>w_pos_p
"NNP"           "A0"            0.000000        
"NNP"           "A1"            0.000000        
"VBZ"           "A0"            0.000000        
"VBZ"           "A1"            0.000000        
"P"             "A0"            0.000000        
"P"             "A1"            0.000000        
"DT"            "A0"            0.000000        
"DT"            "A1"            0.000000        
"NN"            "A0"            0.000000        
"NN"            "A1"            0.000000        

>w_pos_pa
"VBZ"           "NNP"           "A0"            0.000000        
"VBZ"           "NNP"           "A1"            0.000000        
"VBZ"           "NN"            "A1"            0.000000     
```
Notice that for `w_pos_p` all possible argument combinations have weights before we set `set collector.all.w_pos_p=true` before. For the other weights we only see those argument tuples that exist in the training corpus.

# Estimating Weights #
Now we save the corpus we loaded above as training instances to a temporary file  because some preprocessing is done to speed up training. You can also reuse this in later sessions.
```
save corpus to instances "srl.instances";
```
We are finally ready to estimate the weights by online learning using
```
learn for 10 epochs;
```
By default this uses 1-best MIRA using the number of false hidden ground atoms as Loss function. Other options are possible.

Let us print out the weights again:
```
print weights;
```
Now we should see
```
>w_left
"A0"            0.829904        

>w_pos_p
"NNP"           "A0"            -1.138654       
"NNP"           "A1"            0.000000        
"VBZ"           "A0"            0.400000        
"VBZ"           "A1"            0.400000        
"P"             "A0"            -1.965481       
"P"             "A1"            0.000000        
"DT"            "A0"            -1.965481       
"DT"            "A1"            0.000000        
"NN"            "A0"            -1.965481       
"NN"            "A1"            0.000000        

>w_pos_pa
"VBZ"           "NNP"           "A0"            0.400000        
"VBZ"           "NNP"           "A1"            0.400000        
"VBZ"           "NN"            "A1"            0.453077        
```

# Loading the test data #
Now we add some test data to see how our model is doing. We create a file called `test.atoms` and write
```
>>
>word
0 "Haag"
1 "sings"
2 "a"
3 "shanty"
4 "."

>role
1 0 "A0"
1 3 "A1"

>pos
0 "NNP"
1 "VBZ"
2 "DT"
3 "NN"
4 "P"

>>
>word
0 "Elianti"
1 "was"
2 "played"
3 "by"
4 "Haag"
5 "."

>role
2 0 "A1"
2 4 "A0"

>pos
0 "NNP"
1 "VBD"
2 "VBN"
3 "IN"
4 "NNP"
5 "P"
```

# Inspecting the model #
We can use the beast to manually inspect our model to see what is going wrong and where:
```
/* Load a test corpus */
load corpus from "test.atoms";

/* Just loading makes the corpus available for
   the processors within thebeast but not for random
   access through the shell. This is achieved using ... */
save corpus to ram;

/* Now we want to manually see how our model does on the
   test data. */

// go to the next world in the corpus (the first in this case)
next;

// use the current model (the one we trained) and find the most likely world
solve;

// print the resulting labels to the screen
print atoms.role;

// compare the results to the gold labels
print eval;

// do the same thing for the next world
next; solve; print atoms.role; print eval;
```

# Testing the model #
Finally let us write out the results our model produces for the test data.

```
/* Now we want to apply our MLN to all worlds in the test corpus
   and write out the result. Note that this will also print out
   some statistics. */
test to "system.atoms";
```

This should give you screen output like
```
Processed:     2
Time:          44.0ms
Avg. time:     22.0ms
Memory use:    6.1mb
Loss           0.500
Iterations     2.000
Global
-------------------------
Recall              0.500
Precision           0.400
F1                  0.444
Correct             0.500
role
-------------------------
Recall              0.500
Precision           0.400
F1                  0.444
```
and a new file `system.atoms` that contains the predictions of our MLN.