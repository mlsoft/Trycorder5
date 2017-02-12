#!/bin/bash
# convert ip parameter to city
#

export IPTOFIND="$1"

COMMAND="select country,city from dbipcity where fromip <= '$IPTOFIND' and toip >= '$IPTOFIND' limit 1;"

sqlite3 tryserver.sqlite "$COMMAND"

