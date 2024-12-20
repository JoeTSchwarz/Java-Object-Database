# Java-Object-Database 
Java serialized Object Database or JODB
This JODB package is a Java Object Database where the objects are usually Java serialized objects. More: see JODB.pdf

NOTE: The EnDecrypt API is only secure if you and only you know the encryption/decryption algorithm, so I leave this API's encrypt and decrypt method empty and only provide you with the EnDecrypt.class if you want to use my EnDecrypt algorithm for your JODB. The file joodb.jar (and joemvc.jar) was built and tested with JDK 9.0.4.

How to run the examples and the Servers:

1) open a Command Window (CMD)
2) move to the JODB directory (or the directory where you unzip JODB.zip)
3) run the bat file "build.bat" to build the ODBCore
4) modify the setCLASSPATH.bat according to your JAVA environment then run this setCLASSPATH
5) run the ODBServer either with SWING
   - javaw -jar JODBServer.jar odbConfig_node1.txt (and 2. with obdConfig_node2.txt)
   or with JFX
   - javaw -jar JfxODBServer.jar odbConfig_node1.txt (and 2. with obdConfig_node2.txt)
   or mixed SWING/JFX
6) go to UserMainternance with the default Superuser with ID: admin and PW: system (both
   in lower cases) and create some userID/PW for yourself. The delivered userlist contains
   TWO default users: Superuser (admin/system) and user test/tester with privileg 2.
   Note that the default Superuser admin/system CANNOT access to the ODB Server.
7) run "java ThePeople" (People-Example) or "java eDict" (dDict/eDict/pDict-Example) etc. using your new´
   created userID and userPW and see how JODB works

