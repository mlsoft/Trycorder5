sqlite3 tryserver.sqlite "select trycorder.name,dbipcity.country,dbipcity.city from trycorder,dbipcity where trycorder.ipaddr>=dbipcity.fromaddr and trycorder.ipaddr<=dbipcity.toaddr;"

