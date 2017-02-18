sqlite3 /home/bin/tryserver.sqlite "select trycorder.ipaddr,trycorder.name,dbipcity.country,dbipcity.city from trycorder,dbipcity where trycorder.localaddr>=dbipcity.fromaddr and trycorder.localaddr<=dbipcity.toaddr ORDER by dbipcity.country,dbipcity.city;"

