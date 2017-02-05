fgrep -v " 2017" /var/log/trycorder.log | sort -u >trycorder.list
less trycorder.list
