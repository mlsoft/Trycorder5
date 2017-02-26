
# rm -f tryserver.sqlite

sqlite3 tryserver.sqlite "drop table trycorder"

sqlite3 tryserver.sqlite "create table trycorder(ipaddr,name,android,tryversion,localaddr,connection INTEGER,country,city);"

sqlite3 tryserver.sqlite ".import trycorder.list trycorder"

sqlite3 tryserver.sqlite "update trycorder set localaddr = ipaddr, connection=1;"

sqlite3 tryserver.sqlite "create index ipaddr on trycorder (ipaddr);"

