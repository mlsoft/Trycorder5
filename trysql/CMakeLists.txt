cmake_minimum_required(VERSION 2.6)
project(trysql)

add_executable(trysql trysql.c)

target_link_libraries(trysql PUBLIC pthread sqlite3)

install(TARGETS trysql RUNTIME DESTINATION bin)
