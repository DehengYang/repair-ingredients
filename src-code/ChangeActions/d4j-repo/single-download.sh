source /etc/profile
#otherwise defects4j may not be recognized in the Root Terminal

proj=$1
id=$2
skipTest=$3
# 3 parameters

project="${proj^}"
#make sure proj is capitalized.

# at least two parameters. (proj, id)
if [ $# -lt 2 ];
then
    echo -e "\nparameter error. \nThe usage: ./single-download.sh <proj> <id> <optional:skipTest 1 or 0>"
    echo -e "e.g., ./single-download.sh  chart 3 1\n"
    exit
fi

echo "current script is: $0"

# get current dir.(for this sh)
curDir="$(cd $(dirname $0); pwd)"
echo "current dir is: $curDir"
#echo "current dir is $(pwd)" #alternetive

echo "current proj_id is: ${project}_${id}"

#-----check if the proj-name is valied.-----------------------------------------------------------

projs=(chart closure lang math mockito time)
ifD4Jbug=0
for i in ${projs[@]} ;
do
	if [ ${i^} = ${project} ];then
		ifD4Jbug=1
		break
	fi
done

if [ $ifD4Jbug -eq 0 ];then
	echo "the $proj is not in D4J projects list. Exit now."
	exit
fi

#exit

#------build repo and check if the proj-id is in repo already. ------------------------------------

projDir=repo/${project}

wd=repo/${project}/${project}_${id}

if [ ! -d "$projDir" ];then
	mkdir -p $projDir
fi

if [ ! -d "$wd" ];then
	echo "$wd does not exist. So download it now"
	defects4j checkout -p ${project} -v ${id}b -w ${wd}
else
	echo "$wd already exists. So skip the download. And also skipTests."
	skipTest=1
fi

#----check if need to run defects4j compile test------------------------------------------------------------
flag="0"

if [ "$skipTest" = "" ] || [ "$skipTest" = $flag ];then
#when skipTest is null or '0', skip test (defects4j test).
        echo "test now."
        cd ${wd}
        defects4j compile
        defects4j test | tee > testInfo.txt
        cd -
else
	echo "skip tests. Exit now."
fi

#----make a copy in curdir from repo.---

copyDir=${project}/${project}_${id}

if [ ! -d "${project}" ];then
        mkdir ${project}
fi

if [ -d "$copyDir" ];then
        echo "rm $copyDir and re-copy from repo/."
        rm -rf $copyDir
        cp -rf  $wd ${project} 
else
        echo "copy from repo/"
        cp -rf  $wd ${project}
fi

echo "Successfully downloads bug ${project[i]}_$id----------------"

#--- long comments ------------------------------------------------------------

:<<!
if [ ! -d "${project}" ];then 
        mkdir ${project}
fi

if [ -d "$wd" ]; then
        echo "$wd already exists. So skip the download."
#       cd ${wd}
#       defects4j compile
#       defects4j test | tee > testInfo.txt
#       cd -
else
        echo "$wd does not exist. So download it now."
        defects4j checkout -p ${project} -v ${id}b -w ${wd}
#       cd ${wd}
#       defects4j compile
#       defects4j test | tee > testInfo.txt
#       cd -
fi
!

