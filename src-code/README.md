## Project 1: identifying change actions from commit history (in ChangeActions folder)

This project is based on the framework of [PatchParser](https://github.com/AutoProRepair/PatchParser) to investigate *RQ-1: To what extent change actions available in the buggy program are relevant to repair it?* as described in our paper. 

The run.sh in *ChangeActions* folder can be directly executed to replicate our experiments once the following prerequisites are achieved:
+ [Defects4J v1.4.0](https://github.com/rjust/defects4j/tree/v1.4.0)
+ Java 1.8

## Project 2: searching and identifying donor code (in DonorCode folder)

This project employs code clone techniques for donor code search and identification, in order to study **RQ-2: Where could the donor code be found to generate patches?** mentioned in our paper.

The prerequisites for running the run.sh in *DonorCode* folder are as follows:
+ [Defects4J v1.4.0](https://github.com/rjust/defects4j/tree/v1.4.0)
+ Java 1.7

Note that if you would like to re-build the *CodeMatch.jar* and *CodeSearch.jar*, it is necessary to configure Eclipse and [FatJar Eclipse plug-in](http://fjep.sourceforge.net/) in your machine. 

## Tips for configuring Defects4J in Ubuntu
First, prepare necessary tools:
```
apt-get update \
apt-get install git -y \
&& apt-get install subversion -y \
&& apt-get install wget -y \
&& apt-get install unzip -y \
&& apt-get install gcc -y \
&& apt-get install cmake -y \
&& cpan Bundle::CSV DBD::CSV DBI 
```

Then, download Defects4J:
`git clone https://github.com/rjust/defects4j.git`

Finally, run *init.sh* in downloaded Defects4J folder to finish the remaining steps of Defects4J configuration.