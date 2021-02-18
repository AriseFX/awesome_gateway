cmake_minimum_required(VERSION 3.10)
project(c C)

set(CMAKE_C_STANDARD 99)

#add_library(awesome_linux_socket SHARED library.c library.h)
add_executable(${PROJECT_NAME} com_arise_linux_NativeSupport.c)