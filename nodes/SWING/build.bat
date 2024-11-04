@echo off
cd .\classes
jar -cvfme ../JODBServer.jar manifest.txt JOODBServer *.class ../resources/*.txt ../icons/*.jpg > ../log.txt
cd ..\
