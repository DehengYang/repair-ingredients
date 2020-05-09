# Repair Ingredients

This repository includes the experimental data of our study on the impact of repair ingredient retrieval on the performance of 24 program repair systems, as well as the source code for replication. 

## How to replicate the experiment

Please refer to the [README](./src-code/README.md) in the subfolder.


## Structure of the project
 ```powershell
  |--- README.md                              : user guidance
  |--- src-code
  |------ ChangeActions                          : source code for identification of change actions
  |------ DonorCode
  |--------- CodeSearch                          : source code for donor code search
  |--------- CodeMatch                           : source code for donor code match
  |--------- run.sh                              : shell script for running donor code search and match 
  |--- results
  |------ Change Actions
  |--------- Chart                                
  |------------ 1
  |--------------- bugDiff.txt                : `diff` info of Chart-1
  |--------------- changeAction.txt           : change actions of Chart-1
  |--------------- CommitId-<commitId>.txt    : commit id and its commit time
  |--------------- matchResult.txt            : identification results of change actions for Chart-1 
  |--------- ... (result data of other Defects4J bugs with same layout)
  |------ Donor Code
  |--------- logs                                
  |------------ Chart
  |--------------- 3
  |------------------ lines_org.jfree.data.time.TimeSeries_1057-1058_fixed.log : similar code snippet
  |------------------ org.jfree.data.time.TimeSeries_1057-1058_matched.log     : matched donor code
  |------------ ... (result data of other Defects4J bugs with same layout)
  |--------- results
  |------------ Chart
  |--------------- Chart_3_donor.txt          : identification results of donor code
  |------------ ... (result data of other Defects4J bugs with same layout)
  |--------- donor-code.xlsx                  : a collection of results for donor code
```
