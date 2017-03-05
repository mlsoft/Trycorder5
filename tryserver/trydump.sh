
echo ".output trycorder.dump
.dump trycorder
.output" | sqlite3 /home/bin/tryserver.sqlite

echo ".output dbipcity.dump
.dump dbipcity
.output" | sqlite3 /home/bin/tryserver.sqlite

