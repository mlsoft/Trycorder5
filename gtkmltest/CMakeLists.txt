cmake_minimum_required(VERSION 2.6)
project(gtkmltest)

find_package(PkgConfig)

pkg_check_modules(GTKMM gtkmm-3.0) # look into FindPkgConfig.cmake, 

link_directories(
    ${GTKMM_LIBRARY_DIRS}  )

include_directories(
    ${GTKMM_INCLUDE_DIRS}  )

add_executable(gtkmltest main.cpp helloworld.cpp)

target_link_libraries(gtkmltest 
    ${GTKMM_LIBRARIES}  )

install(TARGETS gtkmltest RUNTIME DESTINATION bin)
