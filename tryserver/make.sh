# gcc -o tryserver -pthread -lsqlite3 tryserver.c

cd build
make
cd ..

cp build/tryserver .
cp tryserver /home/bin/
cp try*.sh /home/bin


