
export DATABASE="/home/bin/tryserver.sqlite"

sqlite3 $DATABASE "drop table dbipcity;"

sqlite3 $DATABASE "create table dbipcity(fromip,toip,country,state,city,fromaddr,toaddr);"

sqlite3 $DATABASE ".import dbipcity.csv dbipcity"

