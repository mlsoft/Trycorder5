
killall tryserver

while true
do
	/home/bin/tryserver || echo wait
	sleep 5
done

