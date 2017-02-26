#!/bin/bash
# convert ip parameter to city
#

export IPTOFIND="$1"

COMMAND="select fromaddr,toaddr,country,city,state from dbipcity where fromaddr <= '$IPTOFIND' and toaddr >= '$IPTOFIND' ;"

sqlite3 /home/bin/tryserver.sqlite "$COMMAND"

