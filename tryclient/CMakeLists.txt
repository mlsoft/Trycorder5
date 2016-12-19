cmake_minimum_required(VERSION 2.6)
project(tryclient)

add_executable(tryclient tryclient.c)

target_link_libraries(tryclient PUBLIC pthread)

install(TARGETS tryclient RUNTIME DESTINATION bin)
