monqjfa
=======

GitHub version of http://svn.berlios.de/wsvn/monqjfa


The Java class library monq.jfa provides fast and flexible text filtering with regular expressions. In contrast to java.util.regex, monq.jfa allows a regular expression to be bound to an action that is automatically called whenever a match is spotted in an input stream.

In addition it is possible to combine several tenthousand regex/action pairs into one machinery (called DFA). The DFA filters input to output by looking for matches of all regular expressions in parallel, calling their actions to reformat the text or to incrementally built up a data structure.

If you need a GPL free version of the software to use in closed source projects, please contact the [Text Mining Group at the EBI](http://www.ebi.ac.uk/Rebholz/contact.html).

Design ©2007 by [Harald Kirsch](mailto:Harald.Kirsch@pifpafpuf.de)
