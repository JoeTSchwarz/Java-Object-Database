package joeapp.odb;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
//
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.*;
/**
Object Data Connect
@author Joe T. Schwarz (c)
*/
public class ODBConnect {
  /**
  contructor
  <br>Note:
  <br>- serialized Java object as key must have an implemented equals-method
  <br>- Leading or trailing spaces are consisdered as part of a string
  @param dbHost String, JODB Server hostname or IP
  @param port int, JODB server port
  @param pw String, User's password
  @param uID String, User's ID
  @exception Exception thrown by JAVA
  */
  // reserved command: x1F or 31 for GZIP if OO data size > 256
  public ODBConnect(String dbHost, int port, String pw, String uID) throws Exception {
    soc = SocketChannel.open(new InetSocketAddress(dbHost, port));
    soc.socket().setReceiveBufferSize(65536); // 32KB
    soc.socket().setSendBufferSize(65536);
    String enc = EnDecrypt.encrypt(pw+":"+uID)+"@";
    //
    ios.write(0);
    ios.writeToken(enc);
    ios.write(soc);
    ios.read(soc); // read the content from soc
    if (ios.readBool()) {
      enc = ios.readMsg();
      priv = enc.charAt(0) & 0x0F;
      user = enc.substring(1).split("/");
      listener = new ODBEventListener(user[1]);
      pool.execute(listener);
      panicShutdown();
      dis = false;
      return;
    }
    close();
    throw new Exception("Unable to connect to:"+dbHost+":"+port+". Check your Password/UserID.");
  }
  /**
  disconnect() the connection to JODB Server and closes all opened DBs
  */
  // disconnect: 2. Don't clear dbLst for the case of ODBEvent's SWITCH node
  // send to worker and must wait for reply 
  public void disconnect( ) {
    try {
      ios.reset();
      ios.write(2);
      ios.writeToken("*");
      ios.write(soc);
      ios.read(soc); // a must
    } catch (Exception ex) { }
    dis = true;
    close();
  }
  /**
  getPrivilege of user of this connection
  <br>Privileg: 0: read only, 1: read/write, 2: read/write/delete, 3: 2 + superuser
  @return int, UserPrivilege: 0, 1, 2, 3
  */
  public int getPrivilege() {
    return priv;
  }
  /**
  getID of this connection
  @return String, ID of this connection, ID or null on NO connection to this dbName
  */
  public String getID() {
    return user[0];
  }
  /**
  getDBList of this connection
  @return ArrayList of all connected DB-Names
  */
  public ArrayList<String> getDBList() {
    return dbLst;
  }
  /**
  add an ODBEventListening implementation to ODBListener
  @param odbe Object with implemented ODBEventListening
  */
  public void addEvent(ODBEventListening odbe) {
    listener.addListener(odbe);
  }
  /**
  register an ODBEventListening implementation to ODBListener
  @param odbe Object with implemented ODBEventListening
  */
  public void register(ODBEventListening odbe) {
    listener.addListener(odbe);
  }
  /**
  getKeys() returns all keys of dbName
  @param dbName String, the DB name.
  @return ArrayList of Object (String or serialized POJO)
  */
  public ArrayList<Object> getKeys(String dbName) {
    try {
      send(dbName, 3);
      return ios.readKeys();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }
  /**
  getLocalKeys() returns local keys of dbName
  @param dbName String, the DB name.
  @return ArrayList of Object (String or serialized POJO)
  */
  public ArrayList<Object> getLocalKeys(String dbName) {
    try {
      send(dbName, 4);
      return ios.readKeys();
    } catch (Exception ex) { }
    return null;
  }
  /**
  getClusterKeys() returns cluster keys of dbName
  @param dbName String, the DB name.
  @param node String, the node of format HostName:Port or HostIP:Port
  @return ArrayList of Object (String or serialized POJO)
  */
  public ArrayList<Object> getClusterKeys(String dbName, String node) {
    try {
      send(dbName, 5);
      return ios.readKeys();
    } catch (Exception ex) { }
    return null;
  }
  /**
  add() a seralized object to JODB dbName
  @param dbName String, the DB name.
  @param key String
  @param obj serialized object to be added
  @exception Exception thrown by JAVA
  */
  public void add(String dbName, Object key, Object obj) throws Exception {
    if (priv > 0) send(dbName, 6, key, obj);
    else throw new Exception("ADD Privilege: 1. Yours: 0");
  }
  /**
  add() a seralized object byte array to JODB dbName
  @param dbName String, the DB name.
  @param key String
  @param obj serialized POJO object to be added
  @exception Exception thrown by JAVA
  */
  public void add(String dbName, Object key, byte[] obj) throws Exception {
    if (priv > 0) send(dbName, 6, key, obj);
    else throw new Exception("ADD Privilege: 1. Yours: 0");
  }
  /**
  unlock all locked keys of this dbName (Note: NO Rollback, NOR Commit is then possible)
  @param dbName String, the DB name.
  @return boolean true: key is unlocked, false if unknown key or key wasn't locked
  */
  public boolean unlock(String dbName) {
    if (priv > 0) try {
      send(dbName, 7);
      return ios.readBool();
    } catch (Exception ex) { }
    return false;
  }
  /**
  update() a seralized object of JODB dbName
  @param dbName String, the DB name.
  @param key String
  @param obj serialized object to be replaced
  @return boolean true if success.
  @exception Exception thrown by JAVA
  */
  public boolean update(String dbName, Object key, Object obj) throws Exception {
    if (priv == 0) throw new Exception("UPDATE Privilege: 1. Yours: 0");
    send(dbName, 8, key, obj);
    return ios.readBool();
  }
  /**
  update() a seralized object byte array of JODB dbName
  @param dbName String, the DB name.
  @param key String
  @param obj serialized POJO object to be replaced
  @return boolean true if success
  @exception Exception thrown by JAVA
  */
  public boolean update(String dbName, Object key, byte[] obj) throws Exception {
    if (priv == 0) throw new Exception("UPDATE Privilege: 1. Yours: 0");
    send(dbName, 8, key, obj);
    return ios.readBool();
  }
  /**
  delete() a seralized object of JODB dbName
  @param dbName String, the DB name.
  @param key String
  @return boolean true if success
  @exception Exception thrown by JAVA
  */
  public boolean delete(String dbName, Object key) throws Exception {
    if (priv < 2) throw new Exception("DELETE Privilege: 2. Yours: "+priv);
    send(dbName, 9, key);
    return ios.readBool();
    
  }
  /**
  read() a seralized object from JODB dbName
  @param dbName String, the DB name.
  @param key String
  @return serialized object or a serialized POJO object if data aren't serialized. See getBytes().
  @exception Exception thrown by JAVA
  */
  public Object read(String dbName, Object key) throws Exception {
    send(dbName, 10, key);
    byte[] bb = ios.readObj();
    try (ObjectInputStream oi = new ObjectInputStream(new ByteArrayInputStream(bb))) {
      Object obj = oi.readObject();
      oi.close();
      return obj;
    } catch (Exception ex) { }
    // must return byte array
    return bb;
  }
  /**
  readBytes() a seralized object as byte array of JODB dbName
  @param dbName String, the DB name.
  @param key String
  @return serialized POJO object of (serialized) object
  @exception Exception thrown by JAVA
  */
  public byte[] readBytes(String dbName, Object key) throws Exception {
    send(dbName, 10, key);
    byte[] bb = ios.readObj();
    if (bb != null) return bb;
    throw new Exception("Exception due to unknown reason at KEY:"+key);
  }
  /**
  isExisted()
  @param dbName String, the DB name (abs.path, without any suffix)
  @param key String
  @return boolean true: key exists, false if key is unknown
  */
  public boolean isExisted(String dbName, Object key) {
    try {
      send(dbName, 11, key);
      return ios.readBool();
    } catch (Exception ex) { }
    return false;
  }
  /**
  isLocked()
  @param dbName String, the DB name.
  @param key String
  @return boolean true: key is locked, false if key is unknown or unlocked
  */
  public boolean isLocked(String dbName, Object key) {
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
  @param key String
  @return boolean true: key is locked, false if unknown key or key was locked
  */
  public boolean lock(String dbName, Object key) {
    if (priv > 0) try {
      send(dbName, 13, key);
      return ios.readBool();
    } catch (Exception ex) { }
    return false;
  }
  /**
  unlock() unlock key (Note: NO Rollback, NOR Commit is then possible)
  @param dbName String, the DB name.
  @param key String
  @return boolean true: key is unlocked, false if unknown key or key wasn't locked
  */
  public boolean unlock(String dbName, Object key) {
    if (priv > 0) try {
      send(dbName, 14, key);
      return ios.readBool();
    } catch (Exception ex) { }
    return false;
  }
  /**
  rollback() key must be locked before ROLLBACK
  @param dbName String, the DB name.
  @param key String, key of object to be rolled back
  @return boolean true if rollback is successful, false: unknown key, no rollback
  @exception Exception thrown by JAVA
  */
  public boolean rollback(String dbName, Object key) throws Exception {
    if (priv > 0) try {
      send(dbName, 15, key);
      return ios.readBool();
    } catch (Exception ex) {
      throw new Exception(ex.toString());
    }
    return false;
  }
  /**
  rollback() ALL modified objects regardless of locked or unlocked
  @param dbName String, the DB name.
  @return boolean true if rollback is successful, false: nothing to rollback
  @exception Exception thrown by JAVA if cls not found
  */
  public boolean rollback(String dbName) throws Exception {
    if (priv > 0) try {
      send(dbName, 15);
      return ios.readBool();
    } catch (Exception ex) {
      throw new Exception(ex.toString());
    }
    return false;
  }
  /**
  connect(String dbName) to JODB Server
  @param dbName String, the DB name.
  @exception Exception thrown by JAVA
  */
  public void connect(String dbName) throws Exception {
    if (dbName == null || dbName.length() == 0) throw new Exception(dbName+" is null or empty");
    if (!dbLst.contains(dbName)) {
      dbLst.add(dbName);
      send(dbName, 17);
    }
  }
  /**
  close. All locked keys will be freed. Data may get lost if commitAll wasn't executed before close
  @param dbName String, the DB name.
  @exception Exception thrown by JAVA
  */
  public void close(String dbName) throws Exception {
    send(dbName, 18);
    dbLst.remove(dbName);
  }
  /**
  save. Save and do all commits, then free all the locked keys.
  @param dbName String, the DB name.
  @exception Exception thrown by JAVA
  */
  public void save(String dbName) throws Exception {
    if (priv > 0) send(dbName, 19);
    else throw new Exception("Insufficient Privilege.");
  }
  /**
  xDelete locks, deletes and completes a commit if nothing happened in-between
  @param dbName String, DB name
  @param key Object key of/for the object
  @return boolean true if success
  @exception Exception thrown by JAVA if cls not found
  */
  public boolean xDelete(String dbName, Object key) throws Exception {
    if (priv < 2) throw new Exception("DELETE Privilege: 2. Yours: "+priv);
    send(dbName, 20, key);
    return ios.readBool();
  }
  /**
  xUpdate locks, wpdates and completes it with a commit if nothing happened in-between
  @param dbName String, DB name
  @param key Object key of/for the object
  @param object Java Object
  @return boolean true if success
  @exception Exception thrown by JAVA if cls not found
  */
  public boolean xUpdate(String dbName, Object key, Object object) throws Exception {
    if (priv < 1) throw new Exception("UPDATE Privilege: 1. Yours: 0");
    send(dbName, 21, key, object);
    return ios.readBool();
  }
  /**
  commit() Key must be locked before COMMIT
  @param dbName String, the DB name.
  @param key String, key of object to be rolled back
  @return boolean true if commit is successful, false: unknown key, no commit
  @exception Exception thrown by JAVA if cls not found
  */
  public boolean commit(String dbName, Object key) throws Exception {
    if (priv > 0) try {
      send(dbName, 22, key);
      return ios.readBool();
    } catch (Exception ex) {
      throw new Exception(ex.toString());
    }
    return false;
  }
  /**
  commit() ALL modified objects (regardless of locked or unlocked)
  @param dbName String, the DB name.
  @return boolean true if commit is successful, false: unknown dbName, no commit
  @exception Exception thrown by JAVA if cls not found
  */
  public boolean commit(String dbName) throws Exception {
    if (priv > 0) try {
      send(dbName, 23);
      return ios.readBool();
    } catch (Exception ex) {
      throw new Exception(ex.toString());
    }
    return false;
  }
  /**
  lockedBy()
  @param dbName String, the DB name.
  @param key String
  @return String of userID or null if key is NOT locked by anyone
  */
  public String lockedBy(String dbName, Object key) {
    try {
      send(dbName, 24, key);
      String uid = ios.readMsg();
      if (uid.charAt(0) != '?') return uid;
    } catch (Exception ex) { }
    return null;
  }
  /**
  isAutoCommit( )
  @return boolean, true: autoCommit is set
  */
  public boolean isAutoCommit( ) {
    return autoCommit;
  }
  /**
  autoCommit( ) set AutoCommit to all dbs belonged to this Connection.
  @param mode  boolean, true: set autoCommit (1A/26), false: reset autoCommit (1B/27)
  @return boolean, true: successful, else FAILED
  */
  public boolean autoCommit(boolean mode) {
    if (priv > 0) try {
      ios.reset();
      if (mode) ios.write(26);
      else ios.write(27);
      ios.writeToken("*");
      ios.write(soc);
      ios.read(soc); // read the content from soc
      autoCommit = ios.readBool();
      ios.ifException();
    } catch (Exception ex) {
      autoCommit = false;
    }
    return autoCommit;
  }
  /**
  isKeyLocked()
  @param dbName String, the DB name.
  @param key String
  @return boolean true key is unlocked or locked by owner
  */
  public boolean isKeyFree(String dbName, Object key) {
    try {
      send(dbName, 28, key);
      return ios.readBool();
    } catch (Exception ex) { }
    return false;
  }
  /**
  add() a serialized object to dbName on the specifiied node
  @param dbName String, the DB name.
  @param key String
  @param obj serialized object to be added
  @param node String, format hostName:port or hostIP:port
  @exception Exception thrown by JAVA
  */
  public void add(String dbName, Object key, Object obj, String node) throws Exception {
    if (priv > 0) {
      send(dbName, 29, key+"+"+node, obj);
    } else throw new Exception("ADD Privilege: 1. Yours: 0");
  }
  /**
  add() serialized object byte array to dbName on the specifiied node
  @param dbName String, the DB name.
  @param key String
  @param obj serialized POJO object to be added
  @param node String, format hostName:port or hostIP:port
  @exception Exception thrown by JAVA
  */
  public void add(String dbName, Object key, byte[] obj, String node) throws Exception {
    if (priv > 0) {
      send(dbName, 29, key+"+"+node, obj);
    } else throw new Exception("ADD Privilege: 1. Yours: 0");
  }
  /**
  changePassword(oldPW, newPW)
  @param oldPW String, old Password
  @param newPW String, new Password
  @return boolean, true: successful, else FAILED
  @exception Exception thrown by JAVA
  */
  public boolean changePassword(String oldPW, String newPW) throws Exception {
   if (oldPW == null || newPW == null) throw new Exception("oldPW:"+oldPW+" or newPW:"+newPW);
    ios.reset();
    ios.write(31);
    ios.writeToken(EnDecrypt.encrypt(oldPW));
    ios.writeToken(EnDecrypt.encrypt(newPW));
    ios.write(soc);
    ios.read(soc); // read the content from soc
    ios.ifException(); // check for Error
    return ios.readBool();
  }
  //--------------------------------------------------------------------------
  protected void send(String dbName, int cmd, Object key, Object obj) throws Exception {
    if (dis) return;
    if (key == null || obj == null || !dbLst.contains(dbName)) 
      throw new Exception("Invalid dbName or obj or key");
    //
    ios.reset();
    ios.write(cmd);
    ios.writeToken(dbName);
    ios.writeKey(key);
    ios.write(obj);
    ios.write(soc);
    ios.read(soc); // read the content from soc
    ios.ifException(); // check for Error
  }
  // Check dbName and key
  protected void send(String dbName, int cmd, Object key) throws Exception {
    if (dis) return;
    if (key == null || !dbLst.contains(dbName)) throw new Exception("Invalid dbName or key");
    //
    ios.reset();
    ios.write(cmd);
    ios.writeToken(dbName);
    ios.writeKey(key);
    ios.write(soc);
    ios.read(soc); // read the content from soc
    ios.ifException(); // check for Error
  }
  //
  protected void send(String dbName, int cmd) throws Exception {
    if (dis) return;
    if (!dbLst.contains(dbName)) throw new Exception("Invalid dbName.");
    //
    ios.reset();
    ios.write(cmd);
    ios.writeToken(dbName);
    ios.write(soc);
    ios.read(soc); // read the content from soc
    ios.ifException(); // check for Error
  }
  //--------------------------------------------------------------------------
  private void close() {
    try {     
      soc.shutdownOutput();
      soc.shutdownInput();
      soc.close();
    } catch (Exception ex) { }
    listener.exitListening();
    pool.shutdownNow();
  }
  // hook the Shutdown watchdog.
  private void panicShutdown(){
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        if (!dis) disconnect();
      }
    });
  }
  // data area
  private int priv;
  private String[] user;
  private SocketChannel soc;
  private ODBEventListener listener;
  protected ODBIOStream ios = new ODBIOStream();
  private ArrayList<String> dbLst = new ArrayList<>();
  private boolean dis = true, autoCommit = false, event = false;
  private ExecutorService pool = Executors.newFixedThreadPool(10);
}
