package joeapp.odb;

import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.zip.*;
import java.util.concurrent.*;
import java.nio.charset.Charset;
//
import java.nio.*;
import java.nio.channels.*;
// @author Joe T. Schwarz (c)
/**
Object Data Management System
@author Joe T. Schwarz (c)
*/
public class ODBService implements Runnable {
  /**
  Constructor. This is the base for a customized JODB server
  @param config String, config file name
  @exception Exception thrown by JAVA
  */
  public ODBService(String config) throws Exception {
    HashMap<String, String> map = ODBParser.odbProperty(config);
    LocalDateTime now = LocalDateTime.now();
    // create a LOG txt file with DayOfWeek day Month Year Hour minute Second
    int day = now.getDayOfMonth(), month = now.getMonthValue(), year = now.getYear();
    if (map.get("LOGGING").charAt(0) == '1')
      logger = new BufferedOutputStream(new FileOutputStream(map.get("LOG_PATH")+
               String.format("%s, %2d %s %4d_%2dH%2dMin%2dSec.txt",
               LocalDate.of(year, month,  day).getDayOfWeek().name().substring(0, 3),
               day, month(month), year, now.getHour(), now.getMinute(), now.getSecond()), false));
    //
    parms = new ODBParms(logger, map.get("ODB_PATH").replace(File.separator, "/")+"/", true);
    String charset = map.get("CHARSET");
    if (charset != null) parms.charset = Charset.forName(charset.trim());
    parms.userlist =  map.get("USERLIST");
    // load cluster nodes
    hostName = map.get("WEB_HOST/IP");
    parms.primary = map.get("PRIMARY"); // port
    parms.webHostName = hostName+":"+parms.primary;
    if (!"US_ASCII".equals(charset)) parms.charset = Charset.forName(charset);
    parms.pool = Executors.newFixedThreadPool(Integer.parseInt(map.get("MAX_THREADS")));
    // this.start();
    parms.pool.execute(this);
    // start ODBBroadcaster and ODBEventListener
    parms.broadcaster =  map.get("MULTICASTING");
    parms.BC = new ODBBroadcaster(parms.broadcaster);
    parms.listener = new ODBEventListener(parms.broadcaster);
    // start Broadcaster, Listener and ODBManager
    parms.pool.execute(parms.BC);
    parms.pool.execute(parms.listener);
    odMgr = new ODBManager(parms);
    panicShutdown( );
  }
  // the implemented run()
  public void run() {
    try {
      dbSvr = ServerSocketChannel.open();
      int port = Integer.parseInt(parms.primary);
      dbSvr.socket().bind(new InetSocketAddress(hostName, port));
      dbSvr.setOption(StandardSocketOptions.SO_RCVBUF, 65536);
      //
      while (running) (new ODBWorker(dbSvr.accept(), parms)).start();
    } catch (Exception e) {
      //e.printStackTrace();
      if (running) {
        System.err.println("Cannot start OODB server :"+parms.webHostName);
        System.exit(0);
      }
    }
    if (dbSvr != null) try {
      dbSvr.close();
    } catch (Exception ex) { }
  }
  /**
  getPool
  @return ExecutorService pool
  */
  public ExecutorService getPool() {
    return parms.pool;
  }
  /**
  @param mode boolean, true: log is enabled, false: log is disabled
  */
  public void setLog(boolean mode) {
    parms.log = mode;
    if (mode && parms.logger == null) logger = System.out;
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
      String[] ip = node.split(":");
      if (ip.length == 2) {
        SocketChannel soc = SocketChannel.open(new InetSocketAddress(ip[0], Integer.parseInt(ip[1])));
        ByteBuffer buf = ByteBuffer.allocate(32);
        //
        ios.reset();
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
    if (b) parms.BC.broadcast(9, parms.webHostName+(char)0x01+dbName, parms.nodes);
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
    boolean b = odMgr.restoreKey("*", dbName, ios.toODBKey(key), true);
    if (b) parms.BC.broadcast(4, 
                              parms.webHostName+(char)0x01+key+" of "+dbName+" is forced to unlock.",
                              parms.nodes);
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
    boolean b = odMgr.restoreKey("*", dbName, ios.toODBKey(key), false);
    if (b) parms.BC.broadcast(4, 
                              parms.webHostName+(char)0x01+key+" of "+dbName+" is forced to rollback.",
                              parms.nodes);
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
    if (b) parms.BC.broadcast(4, parms.webHostName+(char)0x01+"All keys of "+dbName+" are forced to unlock.", parms.nodes);
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
    if (b) parms.BC.broadcast(4, parms.webHostName+(char)0x01+"All keys of "+dbName+" are forced to rollback.", parms.nodes);
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
    parms.BC.broadcast(4, parms.webHostName+(char)0x01+msg, parms.nodes);
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
      odMgr.shutdown( );
      running = false;
      dbSvr.close();
      if (logger != null) {
        logger.write(("ODBManager is down."+System.lineSeparator()).getBytes());
        logger.close();
      }
      // wait for the last broadcasting....
      TimeUnit.MICROSECONDS.sleep(250);
      // stop ODBBroadcaster
      parms.BC.halt();
    } catch (Exception ex) { }
    parms.pool.shutdownNow();
  }
  /**
  hook the panicShutdown watching dog.
  */
  private void panicShutdown(){
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        shutdown( );
      }
    });
  }
  private String month(int m) {
    switch(m) {
    case 1: return "Jan";
    case 2: return "Feb";
    case 3: return "Mar";
    case 4: return "Apr";
    case 5: return "May";
    case 6: return "Jun";
    case 7: return "Jul";
    case 8: return "Aug";
    case 9: return "Sep";
    case 10: return "Oct";
    case 11: return "Nov";
    }
    return "Dec";
  }
  //
  private ODBIOStream ios = new ODBIOStream();
  private volatile boolean running = true;
  private OutputStream logger = null;
  private ServerSocketChannel dbSvr;
  private ODBManager odMgr;
  private String hostName;
  private ODBParms parms;
}
