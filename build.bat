@echo off
cd ODBCore
javac -g:none -d ./classes *.java
cd classes
jar -cvf ../../../joodb.jar joeapp/odb/*.class  > ../log.txt
cd ../../..
