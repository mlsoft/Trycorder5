fgrep -v "Jan " /var/log/trycorder.log | fgrep -v "Dec" | sort -u >trycorder.list
less trycorder.list
