
rm -f dbipcity.sqlite

sqlite3 dbipcity.sqlite "create table dbipcity(fromip,toip,country,state,city,fromaddr,toaddr);"

sqlite3 dbipcity.sqlite ".import dbipcity.csv dbipcity"

