# Introduction #

This is a thebeast tutorial. We use a semantic tagger as example application. The task is to label words with semantic slots. For example we want to get a tagging such as

Show me all flights from Hamburg<sub>FROM_LOC</sub> to Chicago<sub>TO_LOC</sub>

Often a system needs more information than this. We may also be interested in the fact that both Hamburg and Chicago are city names. So in fact we are looking for the following labeling:

Show me all flights from Hamburg<sub>FROM_LOC & CITY_NAME</sub> to Chicago<sub>TO_LOC & CITY_NAME</sub>

In fact, there can be several slots per token.

Note that using a simple classifier or (single layer) sequential model would be problematic here: it either had to pick always just exactly one slot type or create all possible type combinations and introduce these as new classes. While the first solution will never be able to predict more than one slot type, the second will run into sparsity problems. Finally, we could define a set of binary classifiers, one for each type, but this modelling would not be able to exploit the fact that there are dependencies between the slot types.

In PML this task can be easily modeled using a predicate `slot` that relates each token with a set of slot types. Using formulas we can then also define dependencies between slot types of the same token or even different tokens.

# Installation #

## Download ##

Go to "Source" and use svn for now

## compile ##

cd into the thebeast directory and call

```
ant -f thebeast.xml
```

to compile the source.

`thebeast` can be started by calling

```
bin/linux/thebeast 
```

on linux machines and

```
bin/mac/thebeast 
```

on macs.

# Defining the Signature #

First we need to define the signature of our model. A signature consists of a set of types and predicates over these types.

## Types ##

To define a type we write the following:
```
type Slot: From_loc, To_loc, NONE;
type Word: ... "a", "man";
```
The first row shows a type (the Slot type) that uses capitalized constants (`From_loc`). Note that `from_loc` (with lower case intial"f") will result in a syntax error, because lowercased strings are reserved for predicates and functions.

The second row shows the Word type. It uses string literals in quotes in order to represent lower case words. Also note the `...`! This is a keyword that indicates that the type is "open-ended"; that is, we might encounter strings not defined here. For these we won't be able to learn any parameters, however, we also won't generate an error message if we see them.

## Predicates ##
Based on these types we are now ready to define some predicates. In our example we define the slot and the word as follows:

```
predicate slot: Int x Slot;
predicate word: Int x Word;
```

Note that `Int` represents a built-in integer type.

# Defining the Model #

First we define which predicates are query or hidden predicates and which predicates are observed. In this task the only hidden predicate is `slot`, `word` is a observed predicate. We define this fact via

```
hidden: slot;
observed: word;
```

note that you can define more than one hidden or observed predicate by simply enumerating them in a comma-separated list
```
observed: word, pos;
```

## Local Formulas/Features ##
The weight function (maps different feature instantiations to weights)
```
weight w_word: Word x Slot -> Double;
```
The formula/factor/feature for the current word
```
factor: for Int t, Word w, Slot s if word(t,w) add [slot(t,s)] * w_word(w,s);
```
Basically this does what it says: for each token `t`, word `w` and slot type `s` for which `w` is the word at `t` we add a score `w_word(w,s)` if the atom `slot(t,s)` is true, and 0 otherwise. This is a local formula because the delta-function represented by the brackets `[` and `]` only contains one hidden decision to make: `slot(t,s)`. Understanding this format becomes easier if we read the bracket as follows
```

[ formula ] = 1 if formula holds
              0 otherwise.

```

While factors can share weights (untested) we usually have one unique weight function for each factor. If we want to take the next word into account, we need to define a new weight function:
```
weight w_word_p1: Word x Slot -> Double;
```
and a new formula
```
factor: for Int t, Word w, Slot s if word(t+1,w) & word(t,_) add [slot(t,s)] * w_word_p1(w,s);
```
Note the `_`: this indicates a _don't care_ variable. We only want to make sure that there is a word at `t`, we don't care what this word is, thus we write `word(t,_)`.


# Loading Data #

## File Format ##

First, you need to define a file with the examples for the task. The following is an example for a the semantic tagging task.

```
>>
>word
0 "from"
1 "NewYork"
2 "to"
3 "Chicago"

>slot
0 NONE
1 From_Loc
1 City_Name
2 |NONE
3 To_Loc
3 City_Name

>>
>word
0 "from"
1 "Chicago"
2 "to"
3 "NewYork"

>slot
0 NONE
1 From_Loc
1 City_Name
2 NONE
3 To_Loc
3 City_Name

```

the `>>` starts a new database/sentence, the `>predicate-name` a table with true ground atoms for the given predicate.

## Load corpus ##

Once defined the file (e.g. corpus1.txt), you can load it into the beast with:

```
load corpus from "corpus1.txt";
```

## check data ##

So far we have loaded the corpus, but not into RAM. It will be streamed from files in a sequential manner when we learn or test our model. To inspect the corpus it needs to be loaded into ram by
```
save corpus to ram;
```

Now you can move around the corpus using
```
next;
```
and
```
prev;
```
and
```
print atoms.words;
```
to print all words of the current sentence/database.

# Learning #

For learning we follow the next sequence of actions:
  * Define the signature
  * Define the model
  * Load corpus
  * Collect features
  * Setting learning parameters
  * Begin learning

## Collect Features ##

As seen above, factors/features have parametrized weights. For example, the first factor we presented has a different weight for each possible argument vector to the weight function `w_word`. We we call each of these argument vectors an _instantiation_ of the factor.

Often we do not want to estimate weights for each factor instantiation. It might make sense to only use such instantiations that we have actually observed in the training set. To this end we introduce the feature _collection_ phase before training. In this phase we determine which instantiations we should learn weights for. Or in other words: what instantiations should have non-zero weights.

The feature collection process is triggered by calling
```
collect;
```
By default, this will select all features we have seen in the data. If we want to use all possible instantiations of a factor we need to set
```
set collector.all.w_word = true;
```
where `w_word` should be replaced by the corresponding weight function of the factor we want to fully instantiate.

## Training Instances ##

In online learning we need to extract the same features of the same training instances over and over again. thebeast allows(actually forces) us to cache these features for the training process. For this need to convert the training corpus into a data structure we will call _training instances_.

```
save corpus to instances "/tmp/instances.dmp";
```

This extracts all required features in advance and saves these to a file on disk . During training this file is streamed in and out in blocks, avoiding large memory overhead (so make sure this file is fast to access).

We can now go on to train the model. However, if we wish to stop thebeast now we can continue later without ever needing to re-extract features by simply calling
```
load instances from "/tmp/instances.dmp";
```
This allows us to evaluate different learning parameters (independent of the model/features) without doing all the extraction again. However, once we add new features this process (and the collection process) needs to be repeated and the old instances file becomes invalid.

## Setting parameters for learning ##

There are several parameters to set-up for the learner. We can set them using the command `set`. For instance:
```
set learner.update = "mira";
```
The `learner` has a `solver` object which has another set of parameters. These are set with:
```
set learner.solver.integer = true;
```
TODO: Put reference with list of parameters

## Learning process ##

To begin the learing process use:

```
learn for 10 epochs;
```
where `10` can be any other number of epochs.

Once the learning epochs are finalized, save the weights into a file (e.g. weights.dmp).
```
save weights to dump "/tmp/weights.dmp";
```

Note: The beast saves the weights for each epoch unter the `/tmp` directory whith the names `epoch_n.dmp` where n is the number of the epoch. These files are good to find the which number of epochs produce the best model.

# Testing #

For testing we follow the next sequence of actions:
  * Define the signature (similar to learning)
  * Define the model (similar to learging)
  * Load weights
  * Load corpus (similar to learning)
  * Setting solver parameters
  * Begin testing process (similar to learning)

## Loading weights ##

To load the weights we use the following command:
```
load weights from dump "/tmp/weights.dmp";
```

## Setting parameters for the solver ##

There are several parameters to set-up for the solver (notice that the learner has its own solver which parameters are independent of this). We can set them using the command `set`. For instance:
```
set solver.integer = true;
```
TODO: Put reference with list of parameters

## Testing process ##

To begin the testing procedure (i.e., tagging) we use the command:
```
test to "/tmp/output.txt";
```

## Inspecting the results ##

Once the testing is finish, it's possible to inspect the results, for this we save the corpus into ram, as previously done:
```
save corpus to ram;
```
And, we use the triplet of commands:
```
 next; solve; print eval;
```
We print the atoms to identify the errors:
```
 print atoms;
```

## Using scripts ##

It is possible to define a sequence of commands into a script file and then loaded into the beast with the `include` command. This is useful to define the signature and the model files and the training, testing, and inspectig sequences we have seen here. An example of the command is:
```
include "model.pml";
```

# Extending the model #

Here we review some extensions to the models.

## Global formulae ##

The true advantage of statistical relation learning engine such as thebeast are global features. While classifiers such as Maxent only provide local features and CRF packages features up to some small markov order n, thebeast can make use of correlations of far distant variables at a low computational cost.

For example, we know that there are dependencies between slots within a sentence on a non-local level. One `TOLOC` slot at the beginning of a sentence makes further `TOLOC` slots at any position less likely, because it is more probable to only mention on location as a destination. This correlation can be modeled using the following bits of code:
```
weight w_pair: Slot x Slot -> Double-;
factor: for Int t1, Int t2, Word w1, Word w2, Slot s1, Slot s2 
   if word(t,w1) & word(t+1,w2) & s1 != NONE & s2 != NONE
   add [slot(t1,s1) & slot(t2,s2] * w_pair(s1,s2);
```


### Hard constraints ###

Deterministic formulae/hard constraints are constraints that the solution has to satisfy by all means. For instance, we might want force at least least one slot per word (and introduce a "NONE" slot type). This constraint is represented via
```
factor atLeastOne: for Int t, Word w if word(t,w) | Slot s: slot(t,s) | >= 1;
```
The symbols `|` denotes the cardinality of its argument, in this case, the set of all `s` for which `slot(t,s)` holds. Note that this is basically a short form for writing
```
factor atLeastOne: for Int t, Word w if word(t,w) add [| Slot s: slot(t,s) | >= 1] * infinity;
```
Note that we have given the factor a name (`atLeastOne`). We will see in a bit for what this can be used.

When using this constraint during inference, the incremental (cutting plane) solver has two options: either it incorporates this constraint from the beginning, or it adds it only if its violated. Both ways have advantages and disadvantages but return the same solution.

By default, the constraint will only enforced if violated. To use it from the start we need to tell the solver:

```
set solver.ground.atLeastOne = true;
```
Here we see the use of named factor: we can refer to it later on inform the solver about its special status.

The online learner maintains an own solver with own parameters. If we want to configure this solver as the testing solver above, we write:
```
set learner.solver.ground.atLeastOne = true;
```


# More on inspecting the features #

An importan feature of thebeast is to inspect the weights for the features for testing. We can query for them using:
```
 print solver.features.slot(6,'NONE');
```
where `6` represents the position we are interested, and `NONE` the particular value of the slot we are interested. This command will display all factors which are attached to the node `slot(6,'NONE')`.


Add your content here.  Format your content with:
  * Text in **bold** or _italic_
  * Headings, paragraphs, and lists
  * Automatic links to other wiki pages