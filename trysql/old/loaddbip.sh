
# rm -f tryserver.sqlite

sqlite3 tryserver.sqlite "create table dbipcity(fromip,toip,country,state,city,fromaddr,toaddr);"

sqlite3 tryserver.sqlite ".import dbip-city-2017-02.csv dbipcity"

sqlite3 tryserver.sqlite "update dbipcity set fromaddr=fromip,toaddr=toip;"

