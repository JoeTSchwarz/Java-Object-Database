package joeapp.odb;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
//
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.*;
import java.nio.charset.Charset;
/**
ODBAgent Connection. Internal communication between 2 nodes (or ODBManagers)
@author Joe T. Schwarz (c)
*/
class ODBCluster {
  /**
  contructor
  @param host_port String, JODB Server. Format: hostName:hostPort or hostIP:hostPort
  @exception Exception thrown by JAVA
  */
  protected ODBCluster(String host_port) throws Exception {
    String[] ip =  ODBParser.split(host_port, ":");
    soc = SocketChannel.open(new InetSocketAddress(ip[0], Integer.parseInt(ip[1])));
    soc.socket().setReceiveBufferSize(65536); // 32KB
    soc.socket().setSendBufferSize(65536);
    send("*", 1);
  }
  /**
  closeAgent and disconnect
  */
  protected void closeAgent(String uID) {
    try {
      send("*", 25, uID);
      soc.shutdownInput();
      soc.shutdownOutput();
      soc.close();
    } catch (Exception ex) { }
  }
  /**
  autoCommit(dbName) effective only in case of disconnect, close, etc.
  @param aID String, the Agent ID.
  @param mode boolean, true: autoCommit, false: reset autoCommit.
  */
  protected void autoCommit(String aID, boolean mode) {
    try {
      if (mode) send(aID, 26);
      else send(aID, 27);
    } catch (Exception ex) { }
  }
  /**
  getKeys() returns all keys of dbName
  @param dbName String, the DB name.
  @return ArrayList of String
  */
  protected ArrayList<String> getKeys(String dbName) {
    try {
      send(dbName, 3);
      return ios.readList();
    } catch (Exception ex) { }
    return null;
  }
  /**
  getLocalKeys() returns local keys of dbName
  @param dbName String, the DB name.
  @return ArrayList of String
  */
  protected ArrayList<String> getLocalKeys(String dbName) {
    try {
       send(dbName, 4);
       return ios.readList();
    } catch (Exception ex) { }
    return null;
  }
  /**
  getClusterKeys() returns cluster keys of dbName
  @param dbName String, the DB name.
  @param node String, the node of format HostName:Port or HostIP:Port
  @return ArrayList of String
  */
  protected ArrayList<String> getClusterKeys(String dbName, String node) {
    try {
      send(dbName, 5);
      return ios.readList();
    } catch (Exception ex) { }
    return null;
  }
  /**
  add() object
  @param dbName String, the DB name.
  @param key String, key name
  @param obj byte array to be added
  @exception Exception thrown by JAVA
  */
  protected void add(String dbName, String key, byte[] obj) throws Exception {
    send(dbName, 6, key, obj);
  }
  /**
  unlock all locked keys of this dbName
  @param dbName String, the DB name.
  @return boolean true: key is unlocked, false if unknown key or key wasn't locked
  */
  protected boolean unlock(String dbName) {
    try {
      send(dbName, 7);
      return ios.readBool();
    } catch (Exception ex) { }
    return false;
  }
  /**
  update() object
  @param dbName String, the DB name.
  @param key String, key name
  @param obj byte array to be replaced
  @return boolean true if success
  @exception Exception thrown by JAVA
  */
  protected boolean update(String dbName, String key, byte[] obj) throws Exception {
    send(dbName, 8, key, obj);
    return ios.readBool();
  }
  /**
  delete() object
  @param dbName String, the DB name.
  @param key String, key name
  @return boolean true if success
  @exception Exception thrown by JAVA
  */
  protected boolean delete(String dbName, String key) throws Exception {
    send(dbName, 9, key);
    return ios.readBool();
  }
  /**
  getBytes() byte array (of object)
  @param dbName String, the DB name.
  @param key String, key name
  @return byte array of (serialized) object
  @exception Exception thrown by JAVA
  */
  protected byte[] getByteArray(String dbName, String key) throws Exception {
    send(dbName, 10, key);
    return ios.readObj();
  }
  /**
  isExisted()
  @param dbName String, the DB name (abs.path, without any suffix)
  @param key String, key name
  @return boolean true: key exists, false if key is unknown
  */
  protected boolean isExisted(String dbName, String key) {
    try {
      send(dbName, 11, key);
      return ios.readBool();
    } catch (Exception ex) { }
    return false;
  }
  /**
  isLocked()
  @param dbName String, the DB name.
  @param key String, key name
  @return boolean true: key is locked, false if key is unknown or unlocked
  */
  protected boolean isLocked(String dbName, String key) {
    try {
       send(dbName, 12, key);
       return ios.readBool();
    } catch (Exception ex) { }
    return false;
  }
  /**
  lock() lock key. Must be invoked before delete or update.
  <br>By delete: locked key is released if delete was successful
  @param dbName String, the DB name.
  @param key String, key name
  @return boolean true: key is locked, false if unknown key or key was locked
  */
  protected boolean lock(String dbName, String key) {
    try {
      send(dbName, 13, key);
      return ios.readBool();
    } catch (Exception ex) { }
    return false;
  }
  /**
  unlock() unlock key
  @param dbName String, the DB name.
  @param key String, key name
  @return boolean true: key is unlocked, false if unknown key or key wasn't locked
  */
  protected boolean unlock(String dbName, String key) {
    try {
      send(dbName, 14, key);
      return ios.readBool();
    } catch (Exception ex) { }
    return false;
  }
  /**
  rollback()
  @param dbName String, the DB name.
  @param key String, key of object to be rolled back
  @return boolean true if rollback is successful, false: unknown key, no rollback
  */
  protected boolean rollback(String dbName, String key) {
    try {
      send(dbName, 15, key);
      return ios.readBool();
    } catch (Exception ex) { }
    return false;
  }
  /**
  rollback() ALL modified objects
  @param dbName String, the DB name.
  @return boolean true if rollback is successful, false: nothing to rollback
  */
  protected boolean rollback(String dbName) {
    try {
      send(dbName, 15);
      return ios.readBool();
    } catch (Exception ex) { }
    return false;
  }
  /**
  connect(String aID) to JODB Server
  @param aID String, agent ID.
  @param cs String, Character set name.
  */
  protected void connect(String aID, String cs) {
    if (!cList.contains(aID)) try {
      if (!charsets.containsKey(aID))try {
        charsets.put(aID, cs);
      } catch (Exception ex) {
        charsets.put(aID, "UTF-8");
        cs = "UTF-8";
      }
      cList.add(aID);
      send(aID, 16, cs);
    } catch (Exception ex) { }
  }
  /**
  save. Save and do all commits, then free all the locked keys.
  @param dbName String, the DB name.
  @exception Exception thrown by JAVA
  */
  protected void save(String dbName) throws Exception {
    send(dbName, 19);
  }
  /**
  commit()
  @param dbName String, the DB name.
  @param key String, key of object to be rolled back
  @return boolean true if commit is successful, false: unknown key, no commit
  */
  protected boolean commit(String dbName, String key) {
    try {
      send(dbName, 22, key);
      return ios.readBool();
    } catch (Exception ex) { }
    return false;
  }
  /**
  commit()
  @param dbName String, the DB name.
  @return boolean true if commit is successful, false: unknown dbName, no commit
  */
  protected boolean commit(String dbName) {
    try {
      send(dbName, 23);
      return ios.readBool();
    } catch (Exception ex) { }
    return false;
  }
  /**
  lockedBy()
  @param dbName String, the DB name.
  @param key String
  @return String of userID
  */
  protected String lockedBy(String dbName, String key) {
    try {
      send(dbName, 24, key);
      return ios.readMsg();
    } catch (Exception ex) { }
    return null;
  }
  /**
  isKeyFree()
  @param dbName String, the DB name.
  @param key String
  @return boolean true key is unlocked or locked by host_port
  */
  protected boolean isKeyFree(String dbName, String key) {
    try {
      send(dbName, 28, key);
      return ios.readBool();
    } catch (Exception ex) { }
    return false;
  }
  /**
  lockedKeys(String dbName)
  @param dbName String, the DB name.
  @return List of keys (String)
  */
  protected List<String> lockedKeys(String dbName) {
    try {
      send(dbName, 30);
      return ios.readList();
    } catch (Exception ex) { }
    return null;
  }

  /**
  disconnect
  */
  protected void disconnect( ) {
    try {
      soc.shutdownInput();
      soc.shutdownOutput();
      soc.close();
      charsets.clear();
    } catch (Exception ex) { }
  }
  //----------------------Superuser---------------------------------------------
  /**
  restoreKey()
  @param dbName String, the DB name.
  @param key String, key name
  @param mode boolean, true:commit, false:rollback
  @return boolean true if success
  @exception Exception thrown by JAVA
  */
  protected boolean restoreKey(String dbName, String key, boolean mode) {
    try {
      send(dbName, 96, key, ""+mode);
      return ios.readBool();
    } catch (Exception ex) { }
    return false;
  }
  /**
  joinNode, cmd: 97
  @param node String node (hostname:port) to be added
  */
  protected void joinNode(String node, String owner) {
    try {
      send(node, 97, owner);
   } catch (Exception ex) { }
  }
  /**
  removeClient, cmd: 93
  @param uID String UserID
  */
  protected void removeClient(String uID) {
    try {
      send(uID, 98);
    } catch (Exception ex) { }
  }
  //--------------------------------------------------------------------------
  // note: writeToken for key as ""+key because key is here has already
  // the tag 0x00 for String, x01 for ser. Object key, x02 for long, x03 for BigInteger
  private void send(String dbName, int cmd, Object key, Object obj) throws Exception {
    String cs = charsets.get(dbName);
    if (cs == null) ios.setCharset("UTF-8");
    else ios.setCharset(cs);
    ios.reset();
    ios.write(cmd);
    ios.writeToken(dbName);
    ios.writeToken(""+key);
    ios.write(obj);
    ios.write(soc);
    ios.getSoc(soc); // read the content from soc
  }
  //
  private void send(String dbName, int cmd, Object key) throws Exception {
    String cs = charsets.get(dbName);
    if (cs == null) ios.setCharset("UTF-8");
    else ios.setCharset(cs);
    ios.reset();
    ios.write(cmd);
    ios.writeToken(dbName);
    ios.writeToken(""+key);
    ios.write(soc);
    ios.getSoc(soc); // read the content from soc
  }
  //
 private void send(String dbName, int cmd) throws Exception {
    String cs = charsets.get(dbName);
    if (cs == null) ios.setCharset("UTF-8");
    else ios.setCharset(cs);
    ios.reset();
    ios.write(cmd);
    ios.writeToken(dbName);
    ios.write(soc);
    ios.getSoc(soc); // read the content from soc
  }
  //--------------------------------------------------------------------------
  // data area
  private SocketChannel soc;
  private ODBIOStream ios = new ODBIOStream();
  private ArrayList<String> cList = new ArrayList<>();
  private HashMap<String, String> charsets = new HashMap<>();

}
