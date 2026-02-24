package joeapp.odb;

import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.math.BigInteger;
import java.util.concurrent.*;
//
import java.nio.*;
import java.nio.channels.*;
// @author Joe T. Schwarz (c)
/**
ODB Services server
@author Joe T. Schwarz (c)
*/
public class ODBService {
  /**
  Constructor. This is the base for a customized JODB server
  @param config String, Server's config file name
  @exception Exception thrown by JAVA
  */
  public ODBService(String config) throws Exception {
    parms = new ODBParms( );
    LocalDateTime now = LocalDateTime.now();
    HashMap<String, String> map = ODBParser.odbProperty(config);
    parms.db_path = map.get("ODB_PATH").replace(File.separator, "/")+"/";
    // create a LOG txt file with DayOfWeek day Month Year Hour minute Second
    int d = now.getDayOfMonth(), month = now.getMonthValue(), year = now.getYear();
    logName = map.get("LOG_PATH")+String.format("%s_%02d_%02d_%04d_%02dH%02dMin%02dSec.txt",
              LocalDate.of(year, month,  d).getDayOfWeek().name().substring(0, 3),
              d, month, year, now.getHour(), now.getMinute(), now.getSecond());
    parms.logger = new BufferedOutputStream(new FileOutputStream(logName, false));
    parms.limit = Integer.parseInt(map.get("MAX_CACHE_LIMIT")) * 0x100000;
    if (parms.limit > 0x40000000)  parms.limit = 0x40000000; // 1 GB
    else if (parms.limit < 0x100000) parms.limit = 0x100000; // 1 MB
    parms.nodeList = Collections.synchronizedList(new ArrayList< >());
    parms.remList = Collections.synchronizedList(new ArrayList< >());
    // load cluster nodes
    for (int i = 1; ; ++i) {
      String node = map.get("NODE_"+i);
      if (node == null) break;
      parms.nodeList.add(node.toLowerCase());
    }
    parms.log = map.get("LOGGING").charAt(0) == '1';
    parms.userlist =  map.get("USERLIST");
    hostName = map.get("WEB_HOST/IP");
    parms.primary = map.get("PRIMARY");
    parms.webHostName = hostName+":"+parms.primary;
    parms.broadcaster =  map.get("MULTICASTING");
    parms.BC = new ODBBroadcaster(parms.broadcaster);
    parms.listener = new ODBEventListener(parms.broadcaster);
    // launch Broadcaster, Listener, ODBManager and server
    pool = Executors.newFixedThreadPool(64);
    pool.execute(parms.listener);
    pool.execute(parms.BC);
    // Start JODB server for listening
    pool.execute(() -> {
      try {
        dbSvr = ServerSocketChannel.open();
        dbSvr.socket().bind(new InetSocketAddress(hostName, Integer.parseInt(parms.primary)));
        dbSvr.setOption(StandardSocketOptions.SO_RCVBUF, 65536);
        while (parms.loop) (new ODBWorker(dbSvr.accept(), parms)).start();
      } catch (Exception e) { }
      if (parms.loop) { // something wrong with dbSvr: hostName  Port?
        System.err.println("Cannot start JODB. Pls. check "+parms.webHostName);
        System.exit(0);
      }
    });
    parms.odMgr = new ODBManager(parms);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        if (parms.loop) shutdown( );
      }
    });
  }
  /**
  getPool
  @return ExecutorService pool
  */
  public ExecutorService getPool() {
    return pool;
  }
  /**
  @param enabled boolean, true: log is enabled, false: log is disabled
  */
  public void setLog(boolean enabled) {
    parms.log = enabled;
  }
  /**
  @return ODBParms
  */
  public ODBParms getODBParms() {
    return parms;
  }
  
  /**
  removeNode a node from the cluster ring
  @param node String with the foemat HostName:Port or HostIP:Port
  */
  public void removeNode(String node) {
    if (!parms.remList.contains(node)) parms.BC.broadcast(7, node, parms.nodeList);
  }
  /**
  addNode a new node to the cluster ring
  @param node String with the foemat HostName:Port or HostIP:Port
  */
  public void addNode(String node) {
    if (parms.remList.contains(node)) parms.BC.broadcast(6, node, parms.nodeList);
  }
  /**
  ping a cluster node
  @param node String in format HostName:Port or IP:Port (see Config file)
  @return long the rounding trip in milliseconds or -1 if Node is unreachable
  */
  public long ping(String node) {
    try {
      String[] ip = ODBParser.split(node, ":");
      if (ip.length == 2) {
        SocketChannel soc = SocketChannel.open(new InetSocketAddress(ip[0], Integer.parseInt(ip[1])));
        ByteBuffer buf = ByteBuffer.allocate(32);
        //
        ios.reset();
        // send(99, "*")
        ios.writeInt((99<<24)+0x100+0x58);
        long beg = System.currentTimeMillis();
        soc.write(ByteBuffer.wrap(ios.toByteArray()));
        int p = soc.read(buf); // wait for the reply from the node
        beg = Long.parseLong(new String(buf.array(), 0, p)) - beg;
        soc.shutdownInput();
        soc.shutdownOutput();
        soc.close();
        return beg;
      }
    } catch (Exception ex) { }
    return -1;
  }
  /**
  forcedClose(dbName) forces to close dbName on local node
  @param dbName String
  @return boolean true if key is freed
  @exception Exception thrown by JAVA
  */
  public boolean forcedClose(String dbName) throws Exception {
    boolean b = parms.odMgr.forcedClose(dbName);
    if (b) parms.BC.broadcast(9, parms.webHostName,
                                 Arrays.asList(dbName+" is forced to close."));
    return b;
  }
  /**
  forcedFreeKey(dbName, key) forces to commit and free a locked key
  @param dbName String
  @param key Object
  @return boolean true if key is committed freed
  @exception Exception thrown by JAVA
  */
  public boolean forcedFreeKey(String dbName, Object key) throws Exception {
    boolean b = parms.odMgr.restoreKey("*", dbName, ODBIOStream.odbKey(key), true);
    if (b) parms.BC.broadcast(4, 
                              parms.webHostName,
                              Arrays.asList(key+" of "+dbName+" is forced to unlock."));
    return b;
  }
  /**
  forcedRollbackKey(dbName, key) forces to rollback and free a locked key
  @param dbName String
  @param key Object
  @return boolean true if key is rollbacked and freed
  @exception Exception thrown by JAVA
  */
  public boolean forcedRollbackKey(String dbName, Object key) throws Exception {
    boolean b = parms.odMgr.restoreKey("*", dbName, ODBIOStream.odbKey(key), false);
    if (b) parms.BC.broadcast(4, 
                              parms.webHostName,
                              Arrays.asList(key+" of "+dbName+" is forced to rollback."));
    return b;
  }
  /**
  forcedFreeKeys(dbName) forces to free ALL locked keys on local node
  @param dbName String
  @return boolean true if keys are committed and freed
  @exception Exception thrown by JAVA
  */
  public boolean forcedFreeKeys(String dbName) throws Exception {
    boolean b = parms.odMgr.restoreKeys("*", dbName, true);
    if (b) parms.BC.broadcast(4, parms.webHostName,
                              Arrays.asList("All keys of "+dbName+" are forced to unlock."));
    return b;
  }
  /**
  forcedRollback(dbName) forces to rollback ALL objects on local node
  @param dbName String
  @return boolean true if keys are rolled back and freed
  @exception Exception thrown by JAVA
  */
  public boolean forcedRollback(String dbName) throws Exception {
    boolean b = parms.odMgr.restoreKeys("*", dbName, false);
    if (b) parms.BC.broadcast(4, parms.webHostName,
                              Arrays.asList("All keys of "+dbName+" are forced to rollback."));
    return b;
  }
  /**
  removeClient(id) remove a Worker or an Agent (* or asterik is for all)
  @param uID String Worker/Agent ID
  @return boolean true if successful
  */
  public boolean removeClient(String uID) {
    return parms.odMgr.removeClient(uID);
  }
  /**
  register() to JODB's server for ODBEventListening
  @param odbe ODBEvent implemented App
  */
  public void register(ODBEventListening odbe) {
    parms.listener.addListener(odbe);
  }
  /**
  broadcast() a message (superuser)
  @param msg String, message
  */
  public void broadcast(String msg) {
    parms.BC.broadcast(4, parms.webHostName, Arrays.asList(msg));
  }
  /**
  dbList returns a list of active DB
  @return List of DB names
  */
  public ArrayList<String> dbList() {
    return parms.odMgr.dbList();
  }
  /**
  activeClients returns a list of active DB userID
  @return List of userID
  */
  public ArrayList<String> activeClients() {
    return parms.odMgr.activeClients();
  }
  /**
  activeUsers of active DB
  @param dbName String, dbName
  @return list of active users of this dbName
  */
  public ArrayList<String> activeWorkers(String dbName) {
    return parms.odMgr.activeWorkers(dbName);
  }
  /**
  keyOwners
  @param dbName String, dbName
  @return List of user ID who owns the db keys (null if dbName is unknown)
  */
  public ArrayList<String> lockedKeyList(String dbName) {
    return parms.odMgr.lockedKeyList("*", dbName);
  }
  /**
  Grateful shutdown ODBServer
  */
  public void shutdown() {
    try {
      parms.odMgr.shutdown( );
      parms.loop = false;
      dbSvr.close();
      if (parms.logger != null) {
        parms.logger.write(("ODBService is down."+System.lineSeparator()).getBytes());
        parms.logger.close();
      }
      if (!parms.log) (new File(logName)).delete();
      parms.listener.exit(); // stop Listener
      parms.BC.exit(); // stop ODBBroadcaster
    } catch (Exception ex) { }
    pool.shutdownNow(); // close Pool
  }
  //
  private ODBIOStream ios = new ODBIOStream();
  private ServerSocketChannel dbSvr;
  private String hostName, logName;
  private ExecutorService pool;
  private ODBParms parms;
}
