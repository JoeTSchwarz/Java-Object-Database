package joeapp.odb;

import java.io.*;
import java.util.*;
import java.time.*;
import java.util.zip.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
//
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.Charset;
/**
Object Data Manager
@author Joe T. Schwarz (c)
*/
public class ODBManager implements ODBEventListening {
  /**
  contructor. ODBManager is responsible for the coomunication between ODBWorker/ODBCluster and NanoDB
  @param config String, Server's config file name
  @exception Exception thrown by JAVA
  */
  public ODBManager(String config) throws Exception {
    LocalDateTime now = LocalDateTime.now();
    HashMap<String, String> map = ODBParser.odbProperty(config);
    db_path = map.get("ODB_PATH").replace(File.separator, "/")+"/";
    // create a LOG txt file with DayOfWeek day Month Year Hour minute Second
    int d = now.getDayOfMonth(), month = now.getMonthValue(), year = now.getYear();
    logName = map.get("LOG_PATH")+String.format("%s_%02d_%02d_%04d_%02dH%02dMin%02dSec.txt",
              LocalDate.of(year, month,  d).getDayOfWeek().name().substring(0, 3),
              d, month, year, now.getHour(), now.getMinute(), now.getSecond());
    logger = new BufferedOutputStream(new FileOutputStream(logName, false));
    //                 ODB_Nano only
    limit = Integer.parseInt(map.get("MAX_CACHE_LIMIT"));
    if (limit > 4096) limit = 4096 * 0x100000; // 4GB
    else if (limit < 1) limit = 0x100000;      // 1MB
    else limit *= 0x100000;
    //
    userList = new UserList(map.get("USERLIST"));
    log = map.get("LOGGING").charAt(0) == '1';
    broadcaster =  map.get("MULTICASTING");
    userFile = map.get("USERLIST");
    primary = map.get("PRIMARY");
    //
    userList = new UserList(userFile);
    webHostName = map.get("WEB_HOST/IP")+"@"+primary;
    listener = new ODBEventListener(broadcaster);
    BC = new ODBBroadcaster(broadcaster);
    listener.addListener(this);
  }
  // ODBEventListening Implementation
  // Check only for 
  // 1:  Node is up or added
  // 5:  Node is removed
  // 6:  SuperUser addNode
  // 7:  SuperUser removeNode
  // 8:  SuperUser detachedNode
  // 10: reply to UP node
  // ignore other events
  /**
  a required ODBListening implementation
  @param event ODBEvent
  */
  public synchronized void odbEvent(final ODBEvent event) {
    String node = event.getActiveNode();
    if (node.equals(webHostName)) return;
    switch(event.getEventType()) {
    case 0: // node down
    case 7: // removeNode
    case 8: // detachedNode
      removeNode(node);
    case 5:  // removeNode's Agents
      List<String> uLst = new ArrayList<String>(dbOwner.keySet());
      for (String uID:uLst) if (uID.charAt(0) == '+') removeAgent(uID);
      else if (node.equals(webHostName)) BC.broadcast(8, node, nodeList);
      return;
    case 1: // node up
      joinNode(node);
      BC.broadcast(10, webHostName, nodeList);
      return;
    case 6: // addNode
      joinNode(node);
      return;
    case 10:
      ForkJoinPool.commonPool().execute(()->{
        if (!nodes.containsKey(node)) try {
          ODBCluster odbc = new ODBCluster(node);
          nodes.put(node, odbc);
          nodeList.add(node);
          cluster.add(odbc);
        } catch (Exception ex) { }
      });
    }
  }
  /**
  connect to the specified dbName (ODBCluster and ODBConnect)
  @param uID    String, userID
  @param dbName String, the DB name
  @param cs     String, Charset name
  @exception Exception thrown by JAVA
  */
  public void connect(String uID, String dbName, String cs) throws Exception {
    if (nanoMap.get(dbName) == null) {
      NanoDB nano = new NanoDB(db_path+dbName, cs, limit);
      nanoMap.put(dbName, nano);
      nano.open();
    }
    charsets.put(dbName, cs);
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
  @param aID String, agent ID
  @param cSet String, character set name
  */
  public synchronized void bindAgent(String aID, String cSet) {
    for (ODBCluster odbc:cluster) odbc.connect(aID, cSet);
  }
  /**
  unbindAgent() - unbind dbAgents. Invoked by ODBWorker-ODBConnect/disconnect
  @param uID String
  */
  public synchronized void unbindAgent(String uID) {
    for (ODBCluster odbc:cluster) odbc.removeAgent(uID);
  }
  /**
  autoCommit -set or reset autoCommit
  @param uID String, user ID
  @param mode boolean, true: autoCommit, false: reset AutoCommit
  */
  public synchronized void autoCommit(String uID, boolean mode) {
    if (mode) { // set if true, else reset
      autoCom.put(uID, true);
      List<String> lst = dbOwner.get(uID);
      if (lst != null) for (String dbName:lst) dbCommit.put(dbName, true);
      if (uID.charAt(0) != '+') // tell all Agents autoommit
        for (ODBCluster odbc:cluster) odbc.autoCommit("+"+uID+"|", true);
    } else {
      autoCom.remove(uID); // set if true, else reset
      List<String> lst = dbOwner.get(uID);
      if (lst != null) for (String dbName:lst) dbCommit.remove(dbName);
      if (uID.charAt(0) != '+') // tell all Agents autoommit
        for (ODBCluster odbc:cluster) odbc.autoCommit("+"+uID+"|", false);
    }
  }
  /**
  isAutoCommit
  @param uID  String, user ID
  @return boolean  true: autoCommit is set, false: NO autoCommit
  */
  public synchronized boolean isAutoCommit(String uID) {
    return autoCom.get(uID) != null;
  }
  /**
  getDBCommit
  @param dbName String
  @return boolean true: autoCommit
  */
  public synchronized boolean getDBCommit(String dbName) {
    return dbCommit.get(dbName) != null;
  }
  /**
  getCharset()
  @param dbName String
  @return String Charset Name if set, else null
  */
  public synchronized String getCharset(String dbName) {
    return charsets.get(dbName);
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
      dbName = "+"+uID+"|"+dbName;
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
  public synchronized ArrayList<String> getLocalKeys(String uID, String dbName) {
    NanoDB nano = nanoMap.get(dbName);
    // nano must be check for null because of non-existent on other node.
    if (nano != null) return nano.getKeys();
    return new ArrayList<>();
  }
  /**
  getClusterKeys() return all cluster keys of the specified dbName (without the deleted)
  @param uID    String, userID
  @param dbName String, dbName to be unlocked
  @param node   String, the node in cluster
  @return ArrayList of String, null if uID is not owner of or shared with dbName
  */
  public synchronized ArrayList<String> getClusterKeys(String uID, String dbName, String node) {
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
  public synchronized void add(String uID, String dbName, String key, byte[] obj) throws Exception {
    NanoDB nano = nanoMap.get(dbName);
    if (nano == null || nano.isExisted(key)) throw new Exception(key+"  exists. Or unknown "+dbName);
    if (autoCom.get(uID) == null) {  // lock key
      nano.addObject(key, obj);
      ConcurrentHashMap<String, String> kMap = kOwner.get(dbName);
      kMap.put(key, uID);
      kOwner.put(dbName, kMap);
    } else nano.add(key, obj);
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
  public synchronized boolean update(String uID, String dbName, String key, byte[] obj) throws Exception {
    NanoDB nano = nanoMap.get(dbName);
    if (nano == null) return false;
    if (isLocked(uID, dbName, key)) {
      if (autoCom.get(uID) == null) {
        if (nano.updateObject(key, obj)) return true;
      } else { // autoCommit -> unlock key 
        if (nano.update(key, obj)) {
          ConcurrentHashMap<String, String> kMap = kOwner.get(dbName);
          if (uID.equals(kMap.get(key)) || uID.charAt(0) == '*') {
            kMap.remove(key); // unlock key
            kOwner.put(dbName, kMap);
          }
        }
        return true;
      }
    }
    if (uID.charAt(0) != '+') {
      dbName = "+"+uID+"|"+dbName;
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
  public synchronized boolean delete(String uID, String dbName, String key) throws Exception {
    NanoDB nano = nanoMap.get(dbName);
    if (nano == null) return false;
    if (isLocked(uID, dbName, key)) {
      if (autoCom.get(uID) == null) {
        if (nano.deleteObject(key)) return true; 
      } else { // autoCommit -> free key
        if (nano.delete(key)) { 
          ConcurrentHashMap<String, String> kMap = kOwner.get(dbName);
          if (uID.equals(kMap.get(key)) || uID.charAt(0) == '*') {
            kMap.remove(key); // unlock key
            kOwner.put(dbName, kMap);
          }
        }
        return true;
      }
    }
    if (uID.charAt(0) != '+') {
      dbName = "+"+uID+"|"+dbName;
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

  public synchronized byte[] read(String uID, String dbName, String key) throws Exception {
    NanoDB nano = nanoMap.get(dbName);
    if (nano == null) return null;
    String uid = kOwner.get(dbName).get(key);
    if (nano.isExisted(key) && (uID.equals(uid) || uid == null)) {
      byte[] bb = nano.readObject(key);
      if (bb != null) return bb;
    }
    if (uID.charAt(0) != '+') {
      dbName = "+"+uID+"|"+dbName;
      for (ODBCluster odbc:cluster) {
        byte[] bb = odbc.getByteArray(dbName, key);
        if (bb != null) return bb;
      }
    }
    throw new Exception("Read "+dbName+" failed. Probably "+key+" is locked or unknown.");
  }
  /**
  close() closes all connected db opened by uID
  @param uID    String, userID
  @exception Exception thrown by JAVA
  */
  // owner Check is done on ODBConnect
  public synchronized void close(String uID) throws Exception {
    List<String> lst = dbOwner.get(uID);
    for (String dbName : lst) {
      charsets.remove(dbName);
      close(uID, dbName);
    }
  }
  /**
  close() closes a connected dbName opened by uID
  @param uID    String, userID
  @param dbName String
  @exception Exception thrown by JAVA
  */
  // owner Check is done on ODBConnect
  public synchronized void close(String uID, String dbName) throws Exception {
    boolean auto = autoCom.get(uID) != null;
    restoreKeys(uID, dbName, auto);
    List<String> list = dbOwner.get(uID);
    charsets.remove(dbName);
    list.remove(dbName);
    dbOwner.put(uID, list);
    onActive(uID, dbName);
    //
    if (uID.charAt(0) != '+') {
      dbName = "+"+uID+"|"+dbName;
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
  public synchronized boolean forcedClose(String dbName) {
    if (dbList.contains(dbName)) {
      List<String> uLst = new ArrayList<String>(dbOwner.keySet());
      for (String uID:uLst) {
        List<String> lst = dbOwner.get(uID);
        if (lst.remove(dbName)) { // remove this dbWorker/dbAgent
          dbOwner.put(uID, lst);
          removeAgent(uID);
          dbList.remove(dbName);
          dbWorker.remove(dbName);
          NanoDB nano = nanoMap.remove(dbName);
          if (nano != null) nano.close();
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
  public synchronized void save(String uID, String dbName) throws Exception {
    NanoDB nano = nanoMap.get(dbName);
    if (nano == null) return;
    boolean auto = autoCom.get(uID) != null;
    restoreKeys("*", dbName, auto);
    nano.save();
    //
    if (uID.charAt(0) != '+') {
      dbName = "+"+uID+"|"+dbName;
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
  public synchronized boolean isExisted(String uID, String dbName, String key) {
    NanoDB nano = nanoMap.get(dbName);
    if (nano == null) return false;
    if (nano.isExisted(key)) return true;
    if (uID.charAt(0) != '+') {
      dbName = "+"+uID+"|"+dbName;
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
  public synchronized String lockedBy(String dbName, String key) {
    NanoDB nano = nanoMap.get(dbName);
    if (nano == null) return null;
    if (nano.isExisted(key)) {
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
  public synchronized boolean isKeyFree(String uID, String dbName, String key) {
    NanoDB nano = nanoMap.get(dbName);
    if (nano == null) return false;
    if (nano.isExisted(key)) {
      String uid = kOwner.get(dbName).get(key);
      if (uID.equals(uid) || uid == null) return true;
      return false;
    }
    if (uID.charAt(0) != '+') {
      dbName = "+"+uID+"|"+dbName;
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
  public synchronized boolean isLocked(String uID, String dbName, String key) {
    if (kOwner.get(dbName).get(key) != null) return true;
    if (uID.charAt(0) != '+') {
      dbName = "+"+uID+"|"+dbName;
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
  public synchronized boolean lock(String uID, String dbName, String key) {
    NanoDB nano = nanoMap.get(dbName);
    if (nano == null) return false;
    if (nano.isExisted(key)) {
      ConcurrentHashMap<String, String> kMap = kOwner.get(dbName);
      String uid = kMap.get(key); // is already locked ?
      if (uid != null) return uID.equals(uid);
      kMap.put(key, uID); // lock key
      kOwner.put(dbName, kMap);
      return true;
    }
    if (uID.charAt(0) != '+') {
      dbName = "+"+uID+"|"+dbName;
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
  public synchronized boolean unlock(String uID, String dbName, String key) {
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
      dbName = "+"+uID+"|"+dbName;
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
  public synchronized boolean unlock(String uID, String dbName) {
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
      dbName = "+"+uID+"|"+dbName;
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
  public synchronized boolean restoreKey(String uID, String dbName, String key, boolean mode) {
    ConcurrentHashMap<String, String> kMap = kOwner.get(dbName);
    if (kMap.size() > 0) {
      NanoDB nano = nanoMap.get(dbName);
      if (nano == null) return false;
      if (uID.equals(kMap.get(key)) || uID.indexOf("*") >= 0 || nano.isKeyDeleted(key)) {
        kMap.remove(key); // unlock key
        kOwner.put(dbName, kMap);
        if (mode) nano.commit(key);
        else nano.rollback(key);
        return true;
      }
    }
    if (uID.charAt(0) != '+') {
      dbName = "+"+uID+"|"+dbName;
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
  public synchronized boolean restoreKeys(String uID, String dbName, boolean mode) {
    ConcurrentHashMap<String, String> kMap = kOwner.get(dbName.trim());
    if (kMap.size() > 0) {
      NanoDB nano = nanoMap.get(dbName);
      if (nano == null) return false;
      ArrayList<String> lst = new ArrayList<>();
      List<String> kLst = new ArrayList<String>(kMap.keySet());
      for (String key : kLst) 
      if (uID.equals(kMap.get(key)) || "*".equals(uID) || nano.isKeyDeleted(key)) {
        if (kMap.remove(key) != null) lst.add(key); // unlock key
      }
      if (lst.size() > 0) {
        kOwner.put(dbName, kMap);
        if (mode) nano.commit(lst);
        else nano.rollback(lst);
        return true;
      }
    }
   return false;
  }
  /**
  remove a node from cluster
  @param node String with the format HostName@Port or HostIP@Port
  */
  public synchronized void removeNode(String node) {
    ODBCluster odbc = nodes.remove(node);
    if (odbc == null) return; // unknown node
    // remove all agents TO this node on Cluster
    List<String> uLst = new ArrayList<String>(dbOwner.keySet());
    for (String uID:uLst) if (uID.charAt(0) != '+') odbc.disconnect("+"+uID+"|");
    cluster.remove(odbc); // remove odbc from cluster
    //
    nodes.remove(node);
    nodeList.remove(node);
    BC.broadcast(5, node, Arrays.asList(node+" is removed ftom Cluster"));
  }
  /**
  joinNode
  @param node String as HostName@Port or HostIP@Port
  */
  public synchronized void joinNode(String node) {
    try {
      cluster.remove(nodes.get(node));
      ODBCluster odbc = new ODBCluster(node);
      cluster.add(odbc);  // new cluster (instance)
      nodes.put(node, odbc); // reload the opened ODBs
      if (dbList.size() > 0) {
        List<String> uList = new ArrayList<String>(dbOwner.keySet());
        for (String dbName:dbList) for (String uID:uList)  // attach to DBs
        if (uID.charAt(0) != '+') odbc.connect("+"+uID+"|"+dbName, charsets.get(dbName));
      }
      nodeList.add(node);
      BC.broadcast(2, node, nodeList); // is ready
    } catch (Exception ex) {
      BC.broadcast(3, node, Arrays.asList(node+" failed to join Cluster"));
    }
  }
  /**
  getAgent returns ODBCluster of given node
  @param node String, node name
  @return ODBCluster null if node is unkmown
  */
  public synchronized ODBCluster getAgent(String node) {
    return nodes.get(node);
  }
  /**
  dbList returns a list of active DB
  @return List of DB names
  */
  public synchronized ArrayList<String> dbList() {
   return new ArrayList<String>(dbList);
  }
  /**
  userDBList returns a DB list of active user uID
  @param uID String userID
  @return ArrayList of DB names
  */
  public synchronized ArrayList<String> userDBList(String uID) {
    List<String> list = dbOwner.get(uID);
    return new ArrayList<String>(list);
  }
  /**
  activeWorkers of active dbName
  @param dbName String, dbName
  @return list of active users of this dbName
  */
  public synchronized ArrayList<String> activeWorkers(String dbName) {
    List<String> list = dbWorker.get(dbName);
    if (list == null) return new ArrayList<String>();
    return new ArrayList<String>(list);
  }
  /**
  activeClients of active DB
  @return ArrayList of String containing the UserID of JODB Clients
  */
  public synchronized ArrayList<String> activeClients( ) {
    return new ArrayList<>(dbOwner.keySet());
  }
  /**
  keyOwners 
  @param uID String, userID
  @param dbName String, dbName
  @return List of user ID who owns the db keys (null if dbName is unknown)
  */
  public synchronized ArrayList<String> lockedKeyList(String uID, String dbName) {
    ArrayList<String> keys = new ArrayList<String>(); 
    if (kOwner.size() > 0) {
      ConcurrentHashMap<String, String> kMap = kOwner.get(dbName);
      if (kMap != null) keys = new ArrayList<>(kMap.keySet());
    }
    if (uID.charAt(0) != '+') {
      dbName = "+"+uID+"|"+dbName;
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
  public synchronized boolean removeClient(String uID) {
    if (uID.charAt(0) == '*') {
      List<String> uLst = new ArrayList<String>(dbOwner.keySet());
      for (String uid:uLst) removeAgent(uid);
      return true;
    }
    if (uID.charAt(0) != '+') for (ODBCluster odbc:cluster) odbc.removeAgent("+"+uID+"|");
    return removeAgent(uID);
  }
  /**
  removeAgent(String uID) or user disconnect()
  @param uID String, ID of worker/agent
  @return boolean true if successful
  */
  public synchronized boolean removeAgent(String uID) {
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
  /**
  shutdown() closes ALL open db and Agents
  @exception Exception thrown by JAVA
  */
  public void shutdown( ) {
    try {
      // broadcast this webHostName node is down
      BC.broadcast(0, webHostName, nodeList);
      for (String uID:uIDList) {
        for (ODBCluster odbc:cluster) odbc.disconnect("+"+uID+"|");
        removeAgent(uID);
      }
      for (ODBWorker w:workers) w.exit();
      if (log) {
        logging("ODBService is down."+System.lineSeparator());
        logger.close();
      } else (new File(logName)).delete();
      listener.exit(); // stop Listener
      BC.exit(); // stop ODBBroadcaster
    } catch (Exception ex) { }
  }
  /**
  @param msg, String -message to be logged
  */
  public synchronized void logging(String msg) {
    try {
      logger.write((msg+System.lineSeparator()).getBytes());
      logger.flush();
    } catch (Exception ex) {
      System.err.println("Unable to log: "+msg);
    }
  }
  //
  private synchronized void onActive(String uID, String dbName) {
    List<String> list = dbWorker.get(dbName);
    list.remove(uID);
    dbWorker.put(dbName, list);
    if (list.size() > 0) return;
    NanoDB nano = nanoMap.remove(dbName);
    if (nano != null) nano.close();
    dbWorker.remove(dbName);
    dbList.remove(dbName);
  }
  // public data area-----------------------------------------------------------
  // ODB_Nano only
  public int limit;
  public UserList userList;
  public ODBBroadcaster BC;
  public OutputStream logger;
  public volatile boolean log;
  public ODBEventListener listener;
  public String db_path, broadcaster, primary, webHostName, userFile;
  //
  public List<String> uIDList = Collections.synchronizedList(new ArrayList<>());
  public List<String> nodeList = Collections.synchronizedList(new ArrayList<>());
  //
  public List<ODBWorker> workers = Collections.synchronizedList(new ArrayList<>());
  // private data area-----------------------------------------------------------
  protected String userID, logName;
  //
  private List<String> dbList = Collections.synchronizedList(new ArrayList<>());
  private List<ODBCluster> cluster = Collections.synchronizedList(new ArrayList<>());
  //
  private ConcurrentHashMap<String, NanoDB> nanoMap = new ConcurrentHashMap<>();
  //
  private ConcurrentHashMap<String, Boolean> autoCom = new ConcurrentHashMap<>();
  private ConcurrentHashMap<String, Boolean> dbCommit = new ConcurrentHashMap<>();
  //
  private ConcurrentHashMap<String, String> charsets = new ConcurrentHashMap<>();
  //
  private ConcurrentHashMap<String, ODBCluster> nodes = new ConcurrentHashMap<>();
  //
  private ConcurrentHashMap<String, List<String>> dbOwner = new ConcurrentHashMap<>();
  private ConcurrentHashMap<String, List<String>> dbWorker = new ConcurrentHashMap<>();
  //
  private ConcurrentHashMap<String, ConcurrentHashMap<String, String>> kOwner = new ConcurrentHashMap<>();
}
