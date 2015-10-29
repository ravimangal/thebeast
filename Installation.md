# Download #

Click on the featured download link on the project home page or go to the Downloads tab and find the newest version there.

# Unpacking #

Go to the directory your downloaded the distribution to and call
```
$ tar xvzf thebeast-x.y.z.tar.gz
```
You may move the extracted directory to a location of your choice.

# Compilation #

`cd` into the extracted directory and execute
```
$ ant
```
this will compile the sources and create a file `bin/thebeast`

# Running #

Call
```
$ bin/thebeast
```
for interactive mode or
```
$ bin/thebeast <filename>
```
to execute the script in `filename`

Find more information in `examples/srl/README` and `doc/manual.pdf`