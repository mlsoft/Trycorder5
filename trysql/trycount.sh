sqlite3 /home/bin/tryserver.sqlite "select country,city,count() from trycorder group by country,city order by count(),country,city;" >trycount.txt

sqlite3 /home/bin/tryserver.sqlite "select ipaddr,name,connection from trycorder;" >trycorders.txt

sqlite3 /home/bin/tryserver.sqlite "select distinct country from trycorder" >trycountry.txt

sqlite3 /home/bin/tryserver.sqlite "select distinct city from trycorder" >trycity.txt

echo "Distinct trycorders : " `wc -l trycorders.txt`
echo "Distinct country : " `wc -l trycountry.txt`
echo "Distinct city : " `wc -l trycity.txt`

