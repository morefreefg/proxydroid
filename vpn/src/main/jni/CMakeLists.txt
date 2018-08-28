cmake_minimum_required(VERSION 3.4.1)
project(jni-helper)

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

ADD_SUBDIRECTORY(badvpn)

LIST(APPEND SOURCES
    src/main/jni/jni-helper.cpp
)
