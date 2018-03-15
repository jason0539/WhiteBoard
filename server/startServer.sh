#!/bin/bash
echo "build start"

JAR_PATH=libs
BIN_PATH=bin
SRC_PATH=src

# java文件列表临时存储文件
SRC_FILE_LIST_PATH=src/sources.list

# 删除临时文件，重新罗列所有需要编译的java文件
rm -f $SRC_FILE_LIST_PATH
find $SRC_PATH/ -name *.java > $SRC_FILE_LIST_PATH

#删除旧的编译文件 生成bin目录
rm -rf $BIN_PATH/
mkdir $BIN_PATH/

#生成依赖jar包 列表
for file in  ${JAR_PATH}/*.jar;
do
jarfile=${jarfile}:${file}
done
echo "jarfile = "$jarfile

#编译
javac -d $BIN_PATH/ -cp $jarfile @$SRC_FILE_LIST_PATH

#运行
java -cp $BIN_PATH$jarfile com.jzj.socket.ClsMainServer