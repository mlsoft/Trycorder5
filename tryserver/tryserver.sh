
killall tryserver

while true
do
	tryserver || echo wait
	sleep 5
done

