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
    HashMap<String, String> map = ODBParser.odbProperty(config);
    parms = new ODBParms(map.get("ODB_PATH").replace(File.separator, "/")+"/");
    //
    LocalDateTime now = LocalDateTime.now();
    // create a LOG txt file with DayOfWeek day Month Year Hour minute Second
    int d = now.getDayOfMonth(), month = now.getMonthValue(), year = now.getYear();
    logName = map.get("LOG_PATH")+String.format("%s_%02d_%02d_%04d_%02dH%02dMin%02dSec.txt",
              LocalDate.of(year, month,  d).getDayOfWeek().name().substring(0, 3),
              d, month, year, now.getHour(), now.getMinute(), now.getSecond());
    parms.logger = new BufferedOutputStream(new FileOutputStream(logName, false));
    log = map.get("LOGGING").charAt(0) == '1';
    parms.log = log;
    //
    d = Integer.parseInt(map.get("DELAY"));
    parms.delay = d > 1000? 1000:d;
    parms.userlist =  map.get("USERLIST");
    // load cluster nodes
    hostName = map.get("WEB_HOST/IP");
    parms.primary = map.get("PORT"); // port
    parms.webHostName = hostName+":"+parms.primary;
    d = Integer.parseInt(map.get("MAX_THREADS"));
    if (d < 1014) d = 1024; // min. 1K threads
    parms.pool = Executors.newFixedThreadPool(d);
    // Start JODB server for listening
    parms.pool.execute(() -> {
      boolean running = false;
      try {
        dbSvr = ServerSocketChannel.open();
        dbSvr.socket().bind(new InetSocketAddress(hostName, Integer.parseInt(parms.primary)));
        dbSvr.setOption(StandardSocketOptions.SO_RCVBUF, 65536);
        running = true; // loop until Shutdown....
        while (true) parms.pool.execute(new ODBWorker(dbSvr.accept(), parms));
      } catch (Exception e) { }
      if (!running) { // something wrong with dbSvr: hostName  Port?
        System.err.println("Cannot start JODB. Pls. check "+parms.webHostName);
        System.exit(0);
      }
    });
    // start ODBBroadcaster and ODBEventListener
    parms.broadcaster =  map.get("MULTICASTING");
    parms.BC = new ODBBroadcaster(parms.broadcaster);
    parms.listener = new ODBEventListener(parms.broadcaster);
    // start Broadcaster, Listener and ODBManager
    parms.pool.execute(parms.BC);
    parms.pool.execute(parms.listener);
    odMgr = new ODBManager(parms);
    // Start listening for shutdown or catastrophe...
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        if (dbSvr != null) shutdown( );
      }
    });
  }
  /**
  getPool
  @return ExecutorService pool
  */
  public ExecutorService getPool() {
    return parms.pool;
  }
  /**
  @param enabled boolean, true: log is enabled, false: log is disabled
  */
  public void setLog(boolean enabled) {
    if (!log) log = enabled;
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
    if (!parms.remList.contains(node)) parms.BC.broadcast(7, node, parms.nodes);
  }
  /**
  addNode a new node to the cluster ring
  @param node String with the foemat HostName:Port or HostIP:Port
  */
  public void addNode(String node) {
    if (parms.remList.contains(node)) parms.BC.broadcast(6, node, parms.nodes);
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
    boolean b = odMgr.forcedClose(dbName);
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
    boolean b = odMgr.restoreKey("*", dbName, ODBIOStream.odbKey(key), true);
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
    boolean b = odMgr.restoreKey("*", dbName, ODBIOStream.odbKey(key), false);
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
    boolean b = odMgr.restoreKeys("*", dbName, true);
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
    boolean b = odMgr.restoreKeys("*", dbName, false);
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
    return odMgr.removeClient(uID);
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
    return odMgr.dbList();
  }
  /**
  activeClients returns a list of active DB userID
  @return List of userID
  */
  public ArrayList<String> activeClients() {
    return odMgr.activeClients();
  }
  /**
  activeUsers of active DB
  @param dbName String, dbName
  @return list of active users of this dbName
  */
  public ArrayList<String> activeWorkers(String dbName) {
    return odMgr.activeWorkers(dbName);
  }
  /**
  keyOwners
  @param dbName String, dbName
  @return List of user ID who owns the db keys (null if dbName is unknown)
  */
  public ArrayList<String> lockedKeyList(String dbName) {
    return odMgr.lockedKeyList("*", dbName);
  }
  /**
  Grateful shutdown ODBServer
  */
  public void shutdown() {
    try {
      dbSvr.close();
      dbSvr = null;
      odMgr.shutdown();
      parms.listener.exit(); // stop Listener
      parms.BC.exit(); // stop ODBBroadcaster
      parms.logging("ODBService is down.");
      parms.logger.close();
      if (!log) (new File(logName)).delete();
      parms.pool.shutdownNow(); // close Pool
      TimeUnit.MILLISECONDS.sleep(parms.delay);
    } catch (Exception ex) { }
  }
  //
  private ODBIOStream ios = new ODBIOStream();
  private ServerSocketChannel dbSvr;
  private String hostName, logName;
  private ODBManager odMgr;
  private ODBParms parms;
  private boolean log;
}
