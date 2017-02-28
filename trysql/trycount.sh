sqlite3 /home/bin/tryserver.sqlite "select country,city,count() from trycorder group by country,city order by count(),country,city;" >trycount.txt

sqlite3 /home/bin/tryserver.sqlite "select ipaddr,name,connection from trycorder order by connection;" >trycorders.txt

sqlite3 /home/bin/tryserver.sqlite "select country,count() from trycorder group by country order by count()" >trycountry.txt

sqlite3 /home/bin/tryserver.sqlite "select state,count() from trycorder group by state order by count()" >trystate.txt

sqlite3 /home/bin/tryserver.sqlite "select city,count() from trycorder group by city order by count()" >trycity.txt

sqlite3 /home/bin/tryserver.sqlite "select name,count() from trycorder group by name order by count()" >trymodel.txt

sqlite3 /home/bin/tryserver.sqlite "select android,count() from trycorder group by android order by android" >tryandroid.txt

sqlite3 /home/bin/tryserver.sqlite "select tryversion,count() from trycorder group by tryversion order by tryversion" >tryversion.txt

cat tryandroid.txt
cat tryversion.txt
echo ""
echo "Distinct trycorders : " `wc -l trycorders.txt`
echo "Distinct country : " `wc -l trycountry.txt`
echo "Distinct state : " `wc -l trystate.txt`
echo "Distinct city : " `wc -l trycity.txt`
echo "Distinct models : " `wc -l trymodel.txt`
echo "Distinct android : " `wc -l tryandroid.txt`
echo "Distinct tryversion : " `wc -l tryversion.txt`


