package joeapp.odb;

import java.io.*;
import java.net.*;
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
    odbMgr = new ODBManager(config);
    // launch Broadcaster, Listener, ODBManager and server
    pool = Executors.newFixedThreadPool(8);
    pool.execute(odbMgr.listener);
    pool.execute(odbMgr.BC);
    // Start ODB server
    pool.execute(()->{
      boolean ok = false;
      try {
        dbSvr = ServerSocketChannel.open();
        dbSvr.socket().bind(new InetSocketAddress(odbMgr.webHostName.substring(0, odbMgr.webHostName.indexOf(":")),
                                                  Integer.parseInt(odbMgr.primary)));
        dbSvr.setOption(StandardSocketOptions.SO_RCVBUF, 65536);
        ok = true; // set OK
        while (true) (new ODBWorker(dbSvr.accept(), odbMgr)).start();
      } catch (Exception e) {
        if (!ok) { // something wrong with dbSvr: hostName  Port?
          System.err.println("Cannot start ODB Server. Pls. check: "+odbMgr.webHostName);
          System.exit(0);
        }
      }
    });
    odbMgr.BC.broadcast(1, odbMgr.webHostName, odbMgr.nodeList);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        if (dbSvr != null) shutdown( );
      }
    });
  }
  /**
  @param enabled boolean, true: log is enabled, false: log is disabled
  */
  public void setLog(boolean enabled) {
    odbMgr.log = enabled;
  }
  /**
  @return ODBManager
  */
  public ODBManager getODBManager() {
    return odbMgr;
  } 
  /**
  addNode a new node to the cluster ring
  @param node String with the foemat HostName:Port or HostIP:Port
  */
  public void addNode(String node) {
    if (!odbMgr.nodeList.contains(node)) odbMgr.BC.broadcast(6, node, odbMgr.nodeList);
  }
  /**
  removeNode a node from the cluster ring
  @param node String with the foemat HostName:Port or HostIP:Port
  */
  public void removeNode(String node) {
    if (odbMgr.nodeList.contains(node)) odbMgr.BC.broadcast(7, node, odbMgr.nodeList);
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
    boolean b = odbMgr.forcedClose(dbName);
    if (b) odbMgr.BC.broadcast(9, odbMgr.webHostName, Arrays.asList(dbName+" is forced to close."));
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
    boolean b = odbMgr.restoreKey("*", dbName, ODBIOStream.odbKey(key), true);
    if (b) odbMgr.BC.broadcast(4, odbMgr.webHostName, Arrays.asList(key+" of "+dbName+" is forced to unlock."));
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
    boolean b = odbMgr.restoreKey("*", dbName, ODBIOStream.odbKey(key), false);
    if (b) odbMgr.BC.broadcast(4, odbMgr.webHostName, Arrays.asList(key+" of "+dbName+" is forced to rollback."));
    return b;
  }
  /**
  forcedFreeKeys(dbName) forces to free ALL locked keys on local node
  @param dbName String
  @return boolean true if keys are committed and freed
  @exception Exception thrown by JAVA
  */
  public boolean forcedFreeKeys(String dbName) throws Exception {
    boolean b = odbMgr.restoreKeys("*", dbName, true);
    if (b) odbMgr.BC.broadcast(4, odbMgr.webHostName, Arrays.asList("All keys of "+dbName+" are forced to unlock."));
    return b;
  }
  /**
  forcedRollback(dbName) forces to rollback ALL objects on local node
  @param dbName String
  @return boolean true if keys are rolled back and freed
  @exception Exception thrown by JAVA
  */
  public boolean forcedRollback(String dbName) throws Exception {
    boolean b = odbMgr.restoreKeys("*", dbName, false);
    if (b) odbMgr.BC.broadcast(4, odbMgr.webHostName, Arrays.asList("All keys of "+dbName+" are forced to rollback."));
    return b;
  }
  /**
  removeClient(id) remove a Worker or an Agent (* or asterik is for all)
  @param uID String Worker/Agent ID
  @return boolean true if successful
  */
  public boolean removeClient(String uID) {
    return odbMgr.removeClient(uID);
  }
  /**
  register() to JODB's server for ODBEventListening
  @param odbe ODBEvent implemented App
  */
  public void register(ODBEventListening odbe) {
    odbMgr.listener.addListener(odbe);
  }
  /**
  broadcast() a message (superuser)
  @param msg String, message
  */
  public void broadcast(String msg) {
    odbMgr.BC.broadcast(4, odbMgr.webHostName, Arrays.asList(msg));
  }
  /**
  dbList returns a list of active DB
  @return List of DB names
  */
  public ArrayList<String> dbList() {
    return odbMgr.dbList();
  }
  /**
  activeClients returns a list of active DB userID
  @return List of userID
  */
  public ArrayList<String> activeClients() {
    return odbMgr.activeClients();
  }
  /**
  activeUsers of active DB
  @param dbName String, dbName
  @return list of active users of this dbName
  */
  public ArrayList<String> activeWorkers(String dbName) {
    return odbMgr.activeWorkers(dbName);
  }
  /**
  keyOwners
  @param dbName String, dbName
  @return List of user ID who owns the db keys (null if dbName is unknown)
  */
  public ArrayList<String> lockedKeyList(String dbName) {
    return odbMgr.lockedKeyList("*", dbName);
  }
  /**
  Grateful shutdown ODBServer
  */
  public void shutdown() {
    try {
      odbMgr.shutdown( );
      dbSvr.close();
    } catch (Exception ex) { }
    pool.shutdownNow(); // close Pool
  }
  //
  private ODBIOStream ios = new ODBIOStream();
  private ServerSocketChannel dbSvr;
  private ExecutorService pool;
  private ODBManager odbMgr;
}
