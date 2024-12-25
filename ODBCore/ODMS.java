package joeapp.odb;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.*;
import java.nio.file.*;
import java.nio.charset.*;
import java.nio.channels.*;
import java.util.concurrent.*;
/**
Object Data Management System. Instantiated and maintained by ODBManager
@author Joe T. Schwarz (c)
*/
class ODMS {
  /**
  Constructor with default UTF-8
  @param dbName String the object file name (auto create if non existed)
  */
  protected ODMS(String dbName) {
    this.dbName = dbName;
    cache = new ConcurrentHashMap<String, byte[]>();
    oCache = new ConcurrentHashMap<String, List<byte[]>>();    
  }
  /**
  Constructor
  @param dbName String the object file name (auto create if non existed)
  @param csName String the character set
  */
  protected ODMS(String dbName, String csName) {
    this.dbName = dbName;
    this.csName = csName;
    cache = new ConcurrentHashMap<String, byte[]>();
    oCache = new ConcurrentHashMap<String, List<byte[]>>();    
  }
  /**
  getKeys() returns an ArrayList of all ODB keys
  @return ArrayList of strings (as keys)
  */
  protected ArrayList<String> getKeys() {
    // don't directly return this lst !
    ArrayList<String> lst = new ArrayList<>(cache.keySet());
    return lst;
  }
  /**
  isExisted. Excl. deleted key
  @param key String key
  @return boolean true if querried key exists (deleted keys won't count)
  */
  protected boolean isExisted(String key) {
    return cache.containsKey(key);
  }
  /**
  isExisted. Excl. deleted key
  @param key String key
  @return boolean true if querried key exists (deleted keys won't count)
  */
  protected boolean isKeyDeleted(String key) {
    return oCache.containsKey(key);
  }
  /**
  readObject
  @param key String key of object to be read
  @return byte array or null if key is unknown
  */
  protected byte[] readObject(String key) {
    return cache.get(key);
  }
  /**
  deleteObject - do nothing if key is unknown
  @param key String key of object to be deleted
  @return boolean true if success
  */
  protected boolean deleteObject(String key) {
    if (!cache.containsKey(key)) return false;
    List<byte[]> list = oCache.get(key);
    if (list == null) list = Collections.synchronizedList(new ArrayList<byte[]>());
    list.add(0, cache.remove(key)); // on the top like push() of Stack<>
    oCache.put(key, list);
    return true;
  }
  /**
  modifyObject - do nothing if key is unknown
  @param key String the key
  @param Obj modified byte array
  @return boolean true if success
  */
  protected boolean updateObject(String key, byte[] Obj) {
    if (!cache.containsKey(key)) return false;
    List<byte[]> list = oCache.get(key);
    if (list == null) list = Collections.synchronizedList(new ArrayList<byte[]>());
    list.add(0, cache.remove(key)); // on the top like push() of Stack<>
    oCache.put(key, list);
    cache.put(key, Obj);
    return true;
  }
  /**
  addObject - NO WRITE if key is existed.
  @param key String the key
  @param obj byte array to be written
  */
  protected void addObject(String key, byte[] obj) {
    oCache.put(key, Collections.synchronizedList(new ArrayList<byte[]>()));
    cache.put(key, obj);
  }
  /**
  commit transaction of the given key. Locked key will be remove
  @param key String
  @return boolean true if successful
  */
  protected boolean commit(String key) {
    boolean ok = oCache.remove(key) != null;
    if (ok) committed = true; // must be!
    return true;
  }
  /**
  commit all transaction on this ODB
  @param keys String list of committing keys
  */
  protected void commit(List<String> keys) {
    int i = oCache.size();
    for (String key : keys) oCache.remove(key);
    if (i > oCache.size()) committed = true;
  }
  /** 
  rollback() rollbacks the LAST modified/added action
  @param key String, key of object to be rollbacked
  @return boolean true if rollback is successful, false: unknown key, no rollback
  */
  protected boolean rollback(String key) {
    List<byte[]> list = oCache.get(key);
    if (list == null) return false;
    if (list.size() > 0) {
      cache.put(key, list.remove(0));
      oCache.put(key, list);     
    } else { // addbject ?
      oCache.remove(key);
      cache.remove(key);
    }
    return true;
  }
  /** 
  rollback() rollbacks all the LAST modified/added actions
  @param kLst String arraylist of keys of object to be rollbacked
  */
  protected void rollback(List<String> kLst) {
    for (String key : kLst) rollback(key);
  }
  /**
  close() commit all uncommitted Transactions and close ODB. Otherwise the data could be lost
  */
  protected void close( ) {
    save();
    cache.clear();
  }
  /**
  save() commit all uncommitted Transactions and save the current cache to ODB
  */
  protected void save( ) {
    if (committed) try {
      List<String> kLst = new ArrayList<>(oCache.keySet());
      // rollback all uncommited keys
      for (String key : kLst) {
        // all keys are known. No null check
        List<byte[]> list = oCache.get(key);
        if (list.size() == 0) cache.remove(key);
        else cache.put(key, list.get(0));
      }
      kLst = new ArrayList<>(cache.keySet());
      GZIPOutputStream go = new GZIPOutputStream(new FileOutputStream(dbName, false), true);
      ODBIOStream ios = new ODBIOStream( );
      // format: KeyLength ObjectLength Key      Object
      //         2 bytes   4 bytes      n bytes  n bytes
      for (String key : kLst) {
        byte[] obj = cache.get(key);    // committed Obj
        ios.writeShort(key.length());   // key length
        ios.writeInt(obj.length);       // obj length
        ios.writeString(key);           // key
        ios.write(obj);                 // object
      }
      go.write(ios.toByteArray());
      go.flush( );
      go.close( );
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    committed = false;
    oCache.clear();
  }
  /**
  open() must be invoked before any IO action can be invoked
  */
  // format: KeyLength ObjectLength Key      Object
  //         2 bytes   4 bytes      n bytes  n bytes
  protected void open( ) {
    try {
      if (!(new File(dbName)).exists()) {
        FileOutputStream fo = new FileOutputStream(dbName, false);
        fo.close(); // auto-create a new ODB
        return;
      }
      // NIO for the performance
      byte[] buf = Files.readAllBytes((new File(dbName)).toPath());
      if (buf.length == 0) return;
      GZIPInputStream gi = new GZIPInputStream(new ODBInputStream(buf));
      ODBIOStream ios = new ODBIOStream();
      buf = new byte[65536]; // 64K
      for (int p = gi.read(buf); p > 0; p = gi.read(buf)) ios.write(buf, 0, p);
      gi.close();
      //
      ODBInputStream ois = new ODBInputStream(ios.toByteArray());
      ois.setCharset(csName);
      //
      while (ois.remainderLength() > 0) {
        int kL  = ois.readShort();
        int oL  = ois.readInt();
        String key = ois.readString(kL);
        cache.put(key, ois.readBytes(oL));
      }
    } catch (Exception ex) {
      //ex.printStackTrace();
    }
  }
  // Data area
  private boolean committed = false;
  private String dbName, csName = "UTF-8";
  private ConcurrentHashMap<String, byte[]> cache;
  private ConcurrentHashMap<String, List<byte[]>> oCache;
}
