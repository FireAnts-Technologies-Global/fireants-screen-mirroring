cmake_minimum_required(VERSION 3.22.1)
project("signature_native")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Werror -O3")

set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -Wl,-z,max-page-size=16384")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

add_library(signature_native SHARED
    signature_native.cpp
    crypto_utils.cpp
)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(signature_native
    ${log-lib}
    ${android-lib}
)
