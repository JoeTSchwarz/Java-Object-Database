package joeapp.odb;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import java.util.concurrent.*;
// Joe T. Schwarz
/**
Message format: Owner_Msg_List. Cmd (Type): 1 byte. n bytes owner, n bytes message, n bytes list of nodes (cluster)
<br>owner: String. HostName:PortNumber or HostIP:Port and msg can be a node (like owner) or any String
<br>0:  Node down (offline). Format: 0, node, list&lt;talternative JODB&gt;
<br>1:  Node up (online). Format: 1, node, list&lt;alternative JODB&gt;
<br>2:  Node ready- Format: 2, node, list&lt;alternative JODB&gt;
<br>3:  node unavailble. Format: 3, node, list&lt;message&gt;
<br>4:  superuser msg which can be: forcedFreeKey, forcedRollabck, etc. Format: 4, node, list&lt;message&gt;
<br>5:  SuperUser, internal, removeNode's Agents  broadcast by ODBManager. Format: 5, node, list&lt;message&gt;
<br>6:  superUser, internal, addNode invoked by ODBService. Format: 6, node, list&lt;alternative JODB&gt;
<br>7:  SuperUser, internal, removeNode invoked by ODBService. Format: 7, node, list&lt;alternative JODB&gt;
<br>8:  SuperUser, internal, removeNode issued by ODBManage onEvent(). Format: 8, node, list&lt;alternative JODB&gt;
<br>9:  SuperUser, forcedClose, invoked by ODBService. Format: 9, node, list&lt;message&gt;
<br>10: SuperUser, internal, joinNode, invoked by ODBWorker. Format: 10, node, list&lt;message&gt;
<br>11: Notify add/delete/update. Format: 11, userID|dbName, list&lt;message&gt; 
<br>12: Client sends msg to JODB. Format: 12, node, list&lt;message&gt; 
<br>13: Customized message. Format: 13, message; 
<br>rest is reserved for future use
@author Joe T. Schwarz
*/
public class ODBBroadcaster implements Runnable {
  /**
  ODBBroadcaster is purposed for messaging the state of a node
  @param host_port string, HostName:Port or HostIP:Port
  */
  public ODBBroadcaster(String host_port) {
    this.host_port = host_port;
  }
  /**
  customized message Broadcasting
  @param msg String, message
  */
  public void broadcast(String msg) {
    msgLst.add(((char)0x0D+msg).getBytes( ));
  }
  /**
  ODB internal message broadcasting
  @param cmd int, send command type
  @param msg String, message
  @param nodes String, arrayList of nodes on cluster
  */
  public void broadcast(int cmd, String msg, List<String> nodes) {
    StringBuilder sb = new StringBuilder((char)(cmd & 0xFF)+msg+(char)0x01);
    if (nodes.size() > 0) for (String node:nodes) sb.append(node+(char)0x01);
    else sb.append("*"+(char)0x01); // dummy node
    msgLst.add(sb.toString().getBytes( ));
  }
  // called by ODBService
  public void exit() {
    if (mcs != null) try {
      mcs.close();
      mcs = null;
    } catch (Exception ex) { }
  }
  public void run( ) {
    boolean running = false;
    try {
      String[] ip = host_port.split(":");
      int port = Integer.parseInt(ip[1]);
      mcs = new MulticastSocket(port);
      InetAddress group = InetAddress.getByName(ip[0]);
      running = true;
      while (true) {
        while (msgLst.size() > 0) {
          byte[] msg = msgLst.remove(0);
          mcs.send(new DatagramPacket(msg, msg.length, group, port));
        }
        // pause 0.1 milliSecond or 100 microSeconds
        java.util.concurrent.TimeUnit.MICROSECONDS.sleep(100);
      }
    } catch (Exception ex) { }
    if (!running) {
      System.err.println("Unable to start ODBBroadcaster:"+host_port);
      System.exit(0);
    }
  }
  private String host_port;
  private MulticastSocket mcs;
  private volatile List<byte[]> msgLst = Collections.synchronizedList(new ArrayList<byte[]>());
}
