package joeapp.odb;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
//
import java.nio.*;
import java.nio.channels.*;
/**
Object Data Manager
@author Joe T. Schwarz (c)
*/
public class ODBManager implements ODBEventListening {
  /**
  contructor. ODBManager is responsible for the coomunication between ODBWorker/ODBCluster and ODMS
  @param parms ODBParms, JODB Object as a generic Parameter
  */
  public ODBManager(ODBParms parms) {
    this.parms = parms;
    parms.odMgr = this;
    uIDList = Collections.synchronizedList(new ArrayList< >());
    cluster = Collections.synchronizedList(new ArrayList< >());
    dbList = Collections.synchronizedList(new ArrayList< >());
    dbWorker = new ConcurrentHashMap<>();
    dbCommit = new ConcurrentHashMap<>();
    dbOwner = new ConcurrentHashMap<>();
    autoCom = new ConcurrentHashMap<>();
    odmsLst = new ConcurrentHashMap<>();
    kOwner = new ConcurrentHashMap<>();
    nodes = new ConcurrentHashMap<>();
    parms.listener.addListener(this);
    // try to setup dbAgent in all nodes
    parms.pool.execute(()->{      
      for (String node:parms.nodes) try {
        ODBCluster odbc = new ODBCluster(node);
        nodes.put(node, odbc);
        cluster.add(odbc);
      } catch (Exception e) { }
      parms.BC.broadcast(1, parms.webHostName, parms.nodes);
    });
  }
  /**
  setID -set User ID
  @param uID String user ID after connect
  */
  public void setID(String uID) {
    uIDList.add(uID);
  }
  // ODBEventListening Implementation
  // Check only for 
  // 1: Node is up or added
  // 5: Node is removed
  // 6: SuperUser addNode
  // 7: SuperUser removeNode
  // 8: SuperUser detachedNode
  // ignore other events
  /**
  a required ODBListening implementation
  @param event ODBEvent
  */
  public void odbEvent(final ODBEvent event) {
    parms.pool.execute(()->{
      String node = event.getActiveNode();
      if (node.equals(parms.webHostName)) return;
      //
      int type = event.getEventType();
      // 0: node down, 7: removeNode, 8: detachedNode
      if (type == 0 || type == 7 || type == 8) {
        removeNode(node);
        parms.remList.add(node);
        parms.nodes.remove(node);
        List<String> uLst = new ArrayList<String>(dbOwner.keySet());
        for (String uID:uLst) if (uID.charAt(0) == '+') removeAgent(uID);
        else if (node.equals(parms.webHostName)) 
        parms.BC.broadcast(8, node, parms.nodes);
        return;
      }
      // 1: node up, 6: addNode
      if (type == 1 || type == 6) {
        try {
          if (joinNode(node)) {
            parms.BC.broadcast(2, node, parms.nodes);
            new ODBCluster(node).joinNode(parms.webHostName);
          } else parms.BC.broadcast(3, node, parms.nodes); // failed
        } catch (Exception ex) { }
        return;
      }
      if (type == 5) try { // removeNode's Agents
        List<String> uLst = new ArrayList<String>(dbOwner.keySet());
        for (String uID:uLst) if (uID.charAt(0) == '+') removeAgent(uID);
      } catch (Exception e) {
        parms.BC.broadcast(3, node, parms.nodes);
      }
    });
  }
  /**
  connect to the specified dbName (ODBCluster and ODBConnect)
  @param uID    String, userID
  @param dbName String, the DB name
  @exception Exception thrown by JAVA
  */
  public void connect(String uID, String dbName) throws Exception {
    if (odmsLst.get(dbName) == null) {
      ODMS odms = new ODMS(parms.db_path+dbName);
      odms.setCharset(parms.charset);
      odmsLst.put(dbName, odms);
      odms.open();
    }
    dbCommit.put(dbName, autoCom.get(uID) != null);
    if (!dbList.contains(dbName)) dbList.add(dbName);
    if (kOwner.get(dbName) == null) kOwner.put(dbName, new ConcurrentHashMap<String, String>());
    List<String> lst = dbOwner.get(uID);
    if (lst == null) lst = Collections.synchronizedList(new ArrayList<String>());
    if (!lst.contains(dbName)) lst.add(dbName);
    dbOwner.put(uID, lst);
    // active Workers
    lst = dbWorker.get(dbName);
    if (lst == null) lst = Collections.synchronizedList(new ArrayList<String>());
    if (!lst.contains(dbName)) lst.add(uID);
    dbWorker.put(dbName, lst);
  }
  /**
  bindAgent() - bind dbAgents. Invoked by ODBWorker-ODBConnect/connect
  @param aID String
  */
  public void bindAgent(String aID) {
    for (ODBCluster odbc:cluster) odbc.connect(aID);
  }
  /**
  unbindAgent() - unbind dbAgents. Invoked by ODBWorker-ODBConnect/disconnect
  @param uID String
  */
  public void unbindAgent(String uID) {
    for (ODBCluster odbc:cluster) odbc.removeClient(uID);
  }
  /**
  autoCommit -set or reset autoCommit
  @param uID String, user ID
  @param mode boolean, true: autoCommit, false: reset AutoCommit
  */
  public void autoCommit(String uID, boolean mode) {
    if (mode) {
      autoCom.put(uID, true); // set if true, else reset
      List<String> lst = dbOwner.get(uID);
      if (lst != null) for (String dbName:lst) dbCommit.put(dbName, true);
      if (uID.charAt(0) != '+') // tell all Agents autoommit
        for (ODBCluster odbc:cluster) odbc.autoCommit('+'+uID+'|', true);
    } else {
      autoCom.remove(uID); // set if true, else reset
      List<String> lst = dbOwner.get(uID);
      if (lst != null) for (String dbName:lst) dbCommit.remove(dbName);
      if (uID.charAt(0) != '+') // tell all Agents autoommit
        for (ODBCluster odbc:cluster) odbc.autoCommit('+'+uID+'|', false);
    }
  }
  /**
  isAutoCommit
  @param uID  String, user ID
  @return boolean  true: autoCommit is set, false: NO autoCommit
  */
  public boolean isAutoCommit(String uID) {
    return autoCom.get(uID) != null;
  }
  /**
  getDBCommit
  @param dbName String
  @return boolean true: autoCommit
  */
  public boolean getDBCommit(String dbName) {
    return dbCommit.get(dbName) != null;
  }
  /**
  getKeys() return all keys of the specified dbName (without the deleted)
  @param uID    String, userID
  @param dbName String, dbName to be unlocked
  @return ArrayList of String, null if uID is not owner of or shared with dbName
  */
  // existence of dbName is checked on ODBConnect site.
  public synchronized ArrayList<String> getKeys(String uID, String dbName) {
    ArrayList<String> keys = getLocalKeys(uID, dbName);
    if (uID.charAt(0) != '+') {
      dbName = '+'+uID+'|'+dbName;
      for (ODBCluster odbc : cluster) {
        ArrayList<String> local = odbc.getLocalKeys(dbName);
        // null && size must be checked. Otherwise exception
        if (local != null && local.size() > 0) keys.addAll(local);
      }
    }
    return keys;
  }
  /**
  getLocalKeys() return all loacl keys of the specified dbName (without the deleted)
  @param uID    String, userID
  @param dbName String, dbName to be unlocked
  @return ArrayList of String, null if uID is not owner of or shared with dbName
  */
  public ArrayList<String> getLocalKeys(String uID, String dbName) {
    ODMS odms = odmsLst.get(dbName);
    // odms must be check for null because of non-existent on other node.
    if (odms != null) return odms.getKeys();
    return new ArrayList<>();
  }
  /**
  getClusterKeys() return all cluster keys of the specified dbName (without the deleted)
  @param uID    String, userID
  @param dbName String, dbName to be unlocked
  @param node   String, the node in cluster
  @return ArrayList of String, null if uID is not owner of or shared with dbName
  */
  public ArrayList<String> getClusterKeys(String uID, String dbName, String node) {
    return nodes.get(node).getLocalKeys(dbName);
  }
  /**
  add() serialized object at the given key to dbName only on the primary Host
  @param uID    String, userID
  @param dbName String, dbName
  @param key String, the key name
  @param obj byte array of Serialized object to be added
  @exception Exception thrown by JAVA (only with Client request)
  */
  // owner Check is done on ODBConnect
  public void add(String uID, String dbName, String key, byte[] obj) throws Exception {
    ODMS odms = odmsLst.get(dbName);
    if (odms == null || odms.isExisted(key)) throw new Exception(key+"  exists. Or unknown "+dbName);
    odms.addObject(key, obj);
    if (autoCom.get(uID) == null) {  // lock key
      ConcurrentHashMap<String, String> kMap = kOwner.get(dbName);
      kMap.put(key, uID);
      kOwner.put(dbName, kMap);
    } else odms.commit(key); //  and commit
  }
  /**
  update() serialized object with obj at the given key to dbName (local or remote)
  @param uID    String, userID
  @param dbName String, dbName
  @param key String, the key name
  @param obj byte array of Serialized object to be replaced
  @return boolean true if success
  @exception Exception thrown by JAVA
  */
  // owner Check is done on ODBConnect
  public boolean update(String uID, String dbName, String key, byte[] obj) throws Exception {
    ODMS odms = odmsLst.get(dbName);
    if (odms == null) return false;
    if (isLocked(uID, dbName, key)) {
      if (odms.updateObject(key, obj)) {
        if (autoCom.get(uID) != null) {  // unlock key and commit
          ConcurrentHashMap<String, String> kMap = kOwner.get(dbName);
          if (uID.equals(kMap.get(key)) || uID.charAt(0) == '*') {
            kMap.remove(key); // unlock key
            kOwner.put(dbName, kMap);
            odms.commit(key);
          }
        }
        return true;
      }
    }
    if (uID.charAt(0) != '+') {
      dbName = '+'+uID+'|'+dbName;
      for (ODBCluster odbc : cluster) if (odbc.update(dbName, key, obj)) return true;
    }
    return false;
  }
  /**
  delete() serialized object at the given key from dbName
  @param uID    String, userID
  @param dbName String, dbName
  @param key String, the key name
  @return boolean true if success
  @exception Exception thrown by JAVA
  */
  // owner Check is done on ODBConnect
  public boolean delete(String uID, String dbName, String key) throws Exception {
    ODMS odms = odmsLst.get(dbName);
    if (odms == null) return false;
    if (isLocked(uID, dbName, key)) {
      if (odms.deleteObject(key)) { 
        if (autoCom.get(uID) != null) {  // unlock key and commit
          ConcurrentHashMap<String, String> kMap = kOwner.get(dbName);
          if (uID.equals(kMap.get(key)) || uID.charAt(0) == '*') {
            kMap.remove(key); // unlock key
            kOwner.put(dbName, kMap);
            odms.commit(key);
          }
        }
        return true;
      }
    }
    if (uID.charAt(0) != '+') {
      dbName = '+'+uID+'|'+dbName;
      for (ODBCluster odbc : cluster) if (odbc.delete(dbName, key)) return true;
    }
    return false;
 }
  /**
  read() serialized object at the given key from dbName
  @param uID    String, userID
  @param dbName String, dbName
  @param key String, the key name
  @return byte array of serialized object
  @exception Exception thrown by JAVA
  */
  // owner Check is done on ODBConnect

  public byte[] read(String uID, String dbName, String key) throws Exception {
    ODMS odms = odmsLst.get(dbName);
    if (odms == null) return null;
    if (isKeyFree(uID, dbName, key)) {
      byte[] bb = odms.readObject(key);
      if (bb != null) return bb;
    }
    if (uID.charAt(0) != '+') {
      dbName = '+'+uID+'|'+dbName;
      for (ODBCluster odbc:cluster) {
        byte[] bb = odbc.getByteArray(dbName, key);
        if (bb != null) return bb;
      }
    }
    throw new Exception("Read "+dbName+" faield. Probably "+key+" is locked or unknown.");
  }
  /**
  close() closes all connected db opened by uID
  @param uID    String, userID
  @exception Exception thrown by JAVA
  */
  // owner Check is done on ODBConnect
  public void close(String uID) throws Exception {
    List<String> lst = dbOwner.get(uID);
    for (String dbName:lst) close(uID, dbName);
  }
  /**
  close() closes a connected dbName opened by uID
  @param uID    String, userID
  @param dbName String
  @exception Exception thrown by JAVA
  */
  // owner Check is done on ODBConnect
  public void close(String uID, String dbName) throws Exception {
    boolean auto = autoCom.get(uID) != null;
    restoreKeys(uID, dbName, auto);
    List<String> list = dbOwner.get(uID);
    list.remove(dbName);
    dbOwner.put(uID, list);
    onActive(uID, dbName);
    //
    if (uID.charAt(0) != '+') {
      dbName = '+'+uID+'|'+dbName;
      for (ODBCluster odbc:cluster) {
        if (auto) odbc.commit(dbName);
        else odbc.rollback(dbName);
      }
    }
  }
  /**
  forcedClose() closes a connected db
  <br>Note: LOCAL node only
  @param dbName String
  @return boolean true if successful
  */
  public boolean forcedClose(String dbName) {
    if (dbList.contains(dbName)) {
      List<String> uLst = new ArrayList<String>(dbOwner.keySet());
      for (String uID:uLst) {
        List<String> lst = dbOwner.get(uID);
        if (lst.remove(dbName)) { // remove this dbWorker/dbAgent
          dbOwner.put(uID, lst);
          removeAgent(uID);
          dbList.remove(dbName);
          dbWorker.remove(dbName);
          ODMS odms = odmsLst.remove(dbName);
          if (odms != null) odms.close();
        }
      }
      return true;
    }
    return false;
  }
  /**
  save() do the CommitAll and save the JODB cache
  @param uID    String, userID
  @param dbName String
  @exception Exception thrown by JAVA
  */
  // owner Check is done on ODBConnect
  public void save(String uID, String dbName) throws Exception {
    ODMS odms = odmsLst.get(dbName);
    if (odms == null) return;
    boolean auto = autoCom.get(uID) != null;
    restoreKeys("*", dbName, auto);
    odms.save();
    //
    if (uID.charAt(0) != '+') {
      dbName = '+'+uID+'|'+dbName;
      for (ODBCluster odbc:cluster) {
        if (auto) odbc.commit(dbName);
        else odbc.rollback(dbName);
        odbc.save(dbName);
      }
    }
  }
  /**
  isExisted() return true if key of dbName exists
  @param uID String, UserID
  @param dbName String
  @param key String
  @return boolean
  */
  // owner Check is done on ODBConnect
  public boolean isExisted(String uID, String dbName, String key) {
    ODMS odms = odmsLst.get(dbName);
    if (odms == null) return false;
    if (odms.isExisted(key)) return true;
    if (uID.charAt(0) != '+') {
      dbName = '+'+uID+'|'+dbName;
      for (ODBCluster odbc : cluster) if (odbc.isExisted(dbName, key)) return true;
    }
    return false;
  }
  /**
  lockedBy() return userID that locks the key
  @param dbName String
  @param key String
  @return String uID
  */
  // owner Check is done on ODBConnect
  public String lockedBy(String dbName, String key) {
    ODMS odms = odmsLst.get(dbName);
    if (odms == null) return null;
    if (odms.isExisted(key)) {
      return kOwner.get(dbName).get(key);
    }
    for (ODBCluster odbc : cluster) {
      String uid = odbc.lockedBy(dbName, key);
      if (uid != null) return uid;
    }
    return null;
  }
  /**
  isKeyFree()
  @param uID String, UserID
  @param dbName String
  @param key String
  @return boolean true key is unlocked or locked by uID
  */
  // owner Check is done on ODBConnect
  public boolean isKeyFree(String uID, String dbName, String key) {
    ODMS odms = odmsLst.get(dbName);
    if (odms == null) return false;
    if (odms.isExisted(key)) {
      String uid = kOwner.get(dbName).get(key);
      if (uID.equals(uid) || uid == null) return true;
      return false;
    }
    if (uID.charAt(0) != '+') {
      dbName = '+'+uID+'|'+dbName;
      for (ODBCluster odbc : cluster) if (odbc.isKeyFree(dbName, key)) return true;
    }
    return false;
  }
  /**
  isLocked() return true if key of dbName is locked
  @param uID String, UserID
  @param dbName String
  @param key String
  @return boolean
  */
  // owner Check is done on ODBConnect
  public boolean isLocked(String uID, String dbName, String key) {
    if (kOwner.get(dbName).get(key) != null) return true;
    if (uID.charAt(0) != '+') {
      dbName = '+'+uID+'|'+dbName;
      for (ODBCluster odbc : cluster) 
        if (odbc.isLocked(dbName, key)) return true;
    }
    return false;
  }
  /**
  lock() return true if key of dbName is successfully locked
  @param uID String, UserID
  @param dbName String
  @param key String
  @return boolean, true key is locked by uID
  */
  // owner Check is done on ODBConnect
  public boolean lock(String uID, String dbName, String key) {
    ODMS odms = odmsLst.get(dbName);
    if (odms == null) return false;
    if (odms.isExisted(key)) {
      ConcurrentHashMap<String, String> kMap = kOwner.get(dbName);
      String uid = kMap.get(key); // is already locked ?
      if (uid != null) return uID.equals(uid);
      kMap.put(key, uID); // lock key
      kOwner.put(dbName, kMap);
      return true;
    }
    if (uID.charAt(0) != '+') {
      dbName = '+'+uID+'|'+dbName;
      for (ODBCluster odbc : cluster) if (odbc.lock(dbName, key)) return true;
    }
    return false;
  }
  /**
  unlock() return true if key of dbName is successfully unlosked
  @param uID String, UserID
  @param dbName String
  @param key String
  @return boolean, true key is unlocked by key owner
  */
  // owner Check is done on ODBConnect
  public boolean unlock(String uID, String dbName, String key) {
    ConcurrentHashMap<String, String> kMap = kOwner.get(dbName);
    if (kMap.size() > 0) {
      String uid = kMap.get(key); // is already locked ?
      if (uID.equals(kMap.get(key)) || uID.charAt(0) == '*'){
        kMap.remove(key); // unlock key
        kOwner.put(dbName, kMap);
        return true;
      }
    }
    if (uID.charAt(0) != '+') {
      dbName = '+'+uID+'|'+dbName;
      for (ODBCluster odbc : cluster) if (odbc.unlock(dbName, key)) return true;
    }
    return false;
  }
  /**
  unlock all locked keys of userID
  @param uID String, user ID
  @param dbName String
  @return boolean, true key is unlocked by key owner
  */
  // owner Check is done on ODBConnect
  public boolean unlock(String uID, String dbName) {
    ConcurrentHashMap<String, String> kMap = kOwner.get(dbName);
    if (kMap.size() > 0) {
      ArrayList<String> kLst = new ArrayList<>(kMap.keySet());
      for (String key : kLst) if (uID.equals(kMap.get(key)) || uID.charAt(0) == '*') {
        kMap.remove(key); // unlock key
      }
      kOwner.put(dbName, kMap);
      return true;
    }
    if (uID.charAt(0) != '+') {
      dbName = '+'+uID+'|'+dbName;
      for (ODBCluster odbc : cluster) if (odbc.unlock(dbName)) return true;
    }
    return false;
  }
  /**
  restoreKey() returns true if transaction is commited/rollbacked successfully and key is unlocked
  @param uID String, UserID
  @param dbName String
  @param key String
  @param mode boolean, true: commit, false: rollback
  @return boolean, true key is unlocked by key owner
  */
  public boolean restoreKey(String uID, String dbName, String key, boolean mode) {
    ConcurrentHashMap<String, String> kMap = kOwner.get(dbName);
    if (kMap.size() > 0) {
      ODMS odms = odmsLst.get(dbName);
      if (odms == null) return false;
      if (uID.equals(kMap.get(key)) || uID.indexOf("*") >= 0 || odms.isKeyDeleted(key)) {
        kMap.remove(key); // unlock key
        kOwner.put(dbName, kMap);
        if (mode) odms.commit(key);
        else odms.rollback(key);
        return true;
      }
    }
    if (uID.charAt(0) != '+') {
      dbName = '+'+uID+'|'+dbName;
      for (ODBCluster odbc : cluster) if (odbc.restoreKey(dbName, key, mode)) return true;
    }
    return false;
  }
  /**
  restoreKeys() returns true if transaction is commited/rollbacked successfully and all keys are unlocked
  <br>Note: LOCAL node only
  @param uID String, UserID
  @param dbName String
  @param mode booelan, true: commit, false: rollback
  @return boolean, true key is unlocked by key owner
  */
  public boolean restoreKeys(String uID, String dbName, boolean mode) {
    ConcurrentHashMap<String, String> kMap = kOwner.get(dbName.trim());
    if (kMap.size() > 0) {
      ODMS odms = odmsLst.get(dbName);
      if (odms == null) return false;
      ArrayList<String> lst = new ArrayList<>();
      List<String> kLst = new ArrayList<String>(kMap.keySet());
      for (String key : kLst) 
      if (uID.equals(kMap.get(key)) || "*".equals(uID) || odms.isKeyDeleted(key)) {
        if (kMap.remove(key) != null) lst.add(key); // unlock key
      }
      if (lst.size() > 0) {
        kOwner.put(dbName, kMap);
        if (mode) odms.commit(lst);
        else odms.rollback(lst);
        return true;
      }
    }
   return false;
  }
  /**
  shutdown() closes ALL open db
  @exception Exception thrown by JAVA
  */
  public void shutdown( ) throws Exception {
    StringBuilder sb = new StringBuilder(parms.webHostName+(char)0x01);
    for (String uID:uIDList) sb.append('+'+uID+"|;");
    parms.BC.broadcast(0, sb.toString(), parms.nodes);
    for (ODBCluster odbc:cluster) odbc.closeAgent( );
    // wait for broadcasting and closing of all agents
    TimeUnit.MICROSECONDS.sleep(250);
    parms.listener.exitListening();
    parms.active = false;
  }
  /**
  remove a node from cluster
  @param node String with the format HostName:Port or HostIP:Port
  */
  public void removeNode(String node) {
    ODBCluster odbc = nodes.remove(node);
    if (odbc == null) return; // unknown node
    // remove all agents TO this node on Cluster
    List<String> uLst = new ArrayList<String>(dbOwner.keySet());
    for (String uID:uLst) if (uID.charAt(0) != '+') odbc.removeClient('+'+uID+'|');
    cluster.remove(odbc); // remove odbc from cluster
    odbc.disconnect(); // disconnect and broadcast Message
    parms.nodes.remove(node); // remove this node
    parms.BC.broadcast(5, node, parms.nodes);
  }
  /**
  joinNode
  @param node String as hostname:port
  @return boolean true for success. 
  */
  public boolean joinNode(String node) {
    try {
      ODBCluster odbc = new ODBCluster(node);
      cluster.add(odbc);  // new cluster (instance)
      nodes.put(node, odbc); // reload the opened ODBs
      if (dbList.size() > 0) {
        List<String> uList = new ArrayList<String>(dbOwner.keySet());
        for (String dbName:dbList) for (String uID:uList)  // attach to DBs
        if (uID.charAt(0) != '+') odbc.connect('+'+uID+'|'+dbName);
      }
      parms.nodes.add(node);
      return true;
    } catch (Exception ex) { }
    return false;
  }
  /**
  getAgent returns ODBCluster of given node
  @param node String, node name
  @return ODBCluster null if node is unkmown
  */
  public ODBCluster getAgent(String node) {
    return nodes.get(node);
  }
  /**
  dbList returns a list of active DB
  @return List of DB names
  */
  public ArrayList<String> dbList() {
   return new ArrayList<String>(dbList);
  }
  /**
  userDBList returns a DB list of active user uID
  @param uID String userID
  @return ArrayList of DB names
  */
  public ArrayList<String> userDBList(String uID) {
    List<String> list = dbOwner.get(uID);
    return new ArrayList<String>(list);
  }
  /**
  activeWorkers of active dbName
  @param dbName String, dbName
  @return list of active users of this dbName
  */
  public ArrayList<String> activeWorkers(String dbName) {
    List<String> list = dbWorker.get(dbName);
    if (list == null) return new ArrayList<String>();
    return new ArrayList<String>(list);
  }
  /**
  activeClients of active DB
  @return ArrayList of String containing the UserID of JODB Clients
  */
  public ArrayList<String> activeClients( ) {
    return new ArrayList<>(dbOwner.keySet());
  }
  /**
  keyOwners 
  @param uID String, userID
  @param dbName String, dbName
  @return List of user ID who owns the db keys (null if dbName is unknown)
  */
  public ArrayList<String> lockedKeyList(String uID, String dbName) {
    ArrayList<String> keys = new ArrayList<String>(); 
    if (kOwner.size() > 0) {
      ConcurrentHashMap<String, String> kMap = kOwner.get(dbName);
      if (kMap != null) keys = new ArrayList<>(kMap.keySet());
    }
    if (uID.charAt(0) != '+') {
      dbName = '+'+uID+'|'+dbName;
      for (ODBCluster odbc:cluster) {
        List<String> lst = odbc.lockedKeys(dbName);
        if (lst != null) keys.addAll(lst);
      }
    }
    return keys;
  }
  /**
  removeClient(String uID)
  @param uID String, ID of worker/agen
  @return boolean true if successful
  */
  public boolean removeClient(String uID) {
    if (uID.charAt(0) == '*') {
      List<String> uLst = new ArrayList<String>(dbOwner.keySet());
      for (String uid:uLst) { // all Workers and Agents
        boolean auto = autoCom.remove(uid) != null;       
        List<String> list = dbOwner.remove(uid);
        if (list != null && list.size() > 0) for (String dbName:list) {
          restoreKeys("*", dbName, auto);
          onActive(uid, dbName);
        }
      }
      return true;
    }
    if (removeAgent(uID)) return true;
    return removeAgent('+'+uID+'|');
  }
  /**
  removeAgent(String uID) or user disconnect()
  @param uID String, ID of worker/agent
  @return boolean true if successful
  */
  public boolean removeAgent(String uID) {
    List<String> list = dbOwner.remove(uID);
    if (list == null || list.size() == 0) return false;
    boolean auto = autoCom.remove(uID) != null;
    for (String dbName:list) {
      if (auto) restoreKeys(uID, dbName, true);
      else restoreKeys(uID, dbName, false);
      onActive(uID, dbName);
    }
    return true;
  }
  //
  private void onActive(String uID, String dbName) {
    List<String> list = dbWorker.get(dbName);
    list.remove(uID);
    dbWorker.put(dbName, list);
    if (list.size() > 0) return;
    ODMS odms = odmsLst.remove(dbName);
    if (odms != null) odms.close();
    dbWorker.remove(dbName);
    dbList.remove(dbName);
  }
  // data area
  private String userID;
  private ODBParms parms;
  private List<ODBCluster> cluster;
  private List<String> dbList, uIDList;
  private ConcurrentHashMap<String, ODMS> odmsLst;
  private ConcurrentHashMap<String, ODBCluster> nodes;
  private ConcurrentHashMap<String, Boolean> autoCom, dbCommit;
  private ConcurrentHashMap<String, List<String>> dbOwner, dbWorker;
  private ConcurrentHashMap<String, ConcurrentHashMap<String, String>> kOwner;
}