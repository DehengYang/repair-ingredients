#!/bin/bash

# proj info
bugProjList=(Chart Closure Lang Math Mockito Time)
bugNumberList=(26 133 65 106 38 27) 

#for bugProj in ${bugProjList[@]}
for((i=0;i<${#bugProjList[@]};i++))
do 
	bugProj=${bugProjList[i]}
	oriProj=${oriProjList[i]}
	bugNumber=${bugNumberList[i]}

	# run for each bug
	for id in $(seq 1 $bugNumber) 
	do
		# donor code search
		cd CodeSearch/
		echo -e "------  Start to do donor code search for ${bugProj}-$id. -----\n"
		java -jar CodeSearch.jar \
			-proj $bugProj -id $id \
			-buggy_src_path d4j-repo/$bugProj/${bugProj}_$id \
			-fixed_src_path d4j-repo/fixed_bugs_dir/$bugProj/${bugProj}_$id \
			-target_source_path d4j-repo/$bugProj/${bugProj}_$id
		echo -e "\n------  Donor code search for ${bugProj}-$id is done. -----\n\n"

		# donor code match
		cd ../CodeMatch/
		echo -e "******  Start to do donor code match for ${bugProj}-$id. ******\n"
		java -jar CodeMatch.jar \
			-proj $bugProj -id $id \
			-projDir ../CodeSearch/d4j-repo/fixed_bugs_dir/$bugProj/ \
			-searchDir ../CodeSearch/search-log/${bugProj,}/$id
		echo -e "\n******  Donor code match for ${bugProj}-$id is done. ******\n\n"		
		break
	done
done














