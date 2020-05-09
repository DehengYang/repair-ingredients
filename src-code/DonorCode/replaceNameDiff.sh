#!/bin/bash

for dirName in `ls`  # Chart
do
	echo "$dirName"  
	if [[ ! -d $dirName ]]; then
		echo "$dirName not a dir."
		continue
	fi

	for idName in `ls $dirName`  # 1, 2, 3, ...
	do
		path="$dirName/$idName"  # Chart/1
		if  [ ! -d $path ];then
			echo "$path is not a dir."
			continue
		fi

		# replace " CCI " with " change actions "
		#sed -i 's/deheng/apr/g' $path/bugDiff.txt
		sed -i 's/dale/apr/g' $path/bugDiff.txt
	done
done