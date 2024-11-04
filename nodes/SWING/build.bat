@echo off
cd .\classes
jar -cvfme ../JODBServer.jar ../resources/manifest.mf JOODBServer *.class ../resources/*.txt ../icons/*.jpg > ../log.txt
cd ..\
