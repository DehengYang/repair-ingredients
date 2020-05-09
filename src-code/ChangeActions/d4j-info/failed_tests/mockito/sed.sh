for((i=1;i<=38;i++))
do
	cat Mockito_${i}.txt |grep "::" | tr -s ' '|cut -d ' ' -f 3 > ${i}.txt
done
