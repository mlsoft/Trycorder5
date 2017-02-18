# gcc -o trysql -lsqlite3 -pthread trysql.c

cd build
make
cd ..

cp build/trysql .

cp trysql trysql.sh trycount.sh /home/bin

