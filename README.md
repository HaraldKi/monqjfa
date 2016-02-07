monqjfa 
=======

Nondeterministic and Deterministic Finita Automata for Java (NFA, DFA)

The Java class library monq.jfa provides fast and flexible text
filtering with regular expressions. In contrast to java.util.regex,
monq.jfa allows a regular expression to be bound to an action that is
automatically called whenever a match is spotted in an input stream.

In addition it is possible to combine several tenthousand regex/action
pairs into one machinery (called DFA). The DFA filters input to output
by looking for matches of all regular expressions in parallel, calling
their actions to reformat the text or to incrementally built up a data
structure.

Homepage: http://pifpafpuf.de/Monq.jfa/

Release Notes:
https://raw.githubusercontent.com/HaraldKi/monqjfa/master/Documentation/RELNOTES

For some recent blog entry of how the speed improved recently, read
http://pifpafpuf.de/2015-11/generated-index.html

Javadoc: http://haraldki.github.io/monqjfa/monqApiDoc/index.html

regular expression syntax: http://haraldki.github.io/monqjfa/monqApiDoc/monq/jfa/doc-files/resyntax.html

test-coverage: http://haraldki.github.io/monqjfa/monq-test-coverage/

©2007--2016 by [Harald Kirsch](mailto:pifpafpuf@gmx.de)


