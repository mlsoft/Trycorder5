sqlite3 /home/bin/tryserver.sqlite "select country,city,count() from trycorder group by country,city order by count(),country,city;" >trycount.txt

sqlite3 /home/bin/tryserver.sqlite "select ipaddr,name,connection from trycorder;" >trycorders.txt

sqlite3 /home/bin/tryserver.sqlite "select country,count() from trycorder group by country" >trycountry.txt

sqlite3 /home/bin/tryserver.sqlite "select state,count() from trycorder group by state" >trystate.txt

sqlite3 /home/bin/tryserver.sqlite "select city,count() from trycorder group by city" >trycity.txt

echo "Distinct trycorders : " `wc -l trycorders.txt`
echo "Distinct country : " `wc -l trycountry.txt`
echo "Distinct state : " `wc -l trystate.txt`
echo "Distinct city : " `wc -l trycity.txt`

