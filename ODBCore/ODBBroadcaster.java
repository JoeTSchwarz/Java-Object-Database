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
<br>0:  Node down (offline)
<br>1:  Node up (online)
<br>2:  Node ready
<br>3:  node unavailble
<br>4:  superuser msg which can be: forcedFreeKey, forcedRollabck, etc.
<br>5:  SuperUser, internal, removeNode's Agents  broadcast by ODBManager
<br>6:  superUser, internal, addNode invoked by ODBService
<br>7:  SuperUser, internal, removeNode invoked by ODBService
<br>8:  SuperUser, internal, removeNode issued by ODBManage onEvent()
<br>9:  SuperUser, forcedClose, invoked by ODBService
<br>10: SuperUser, internal, joinNode, invoked by ODBWorker
<br>rest: reserved
@author Joe T. Schwarz
*/
public class ODBBroadcaster implements Runnable {
  /**
  ODBBroadcaster is purposed for messaging the state of a node
  @param host_port string, HostName:Port or HostIP:Port
  */
  public ODBBroadcaster(String host_port) {
    ip = host_port.split(":");
  }
  /**
  @param cmd int, send command type
  @param msg String, message
  @param nodes String, arrayList of nodes on cluster
  */
  public void broadcast(int cmd, String msg, List<String> nodes) {
    StringBuilder sb = new StringBuilder((char)(cmd & 0xFF)+msg+(char)0x01);
    if (nodes.size() > 0) for (String node:nodes) sb.append(node+(char)0x01);
    else sb.append("*"+(char)0x01); // dummy node
    msgLst.add(sb.toString().getBytes());
  }
  // stopped by ODBService
  public void halt() {
    loop = false;
  }
  public void run( ) {
    int port = Integer.parseInt(ip[1]);
    try (MulticastSocket mcs = new MulticastSocket(port)) {
      InetAddress group = InetAddress.getByName(ip[0]);
      while (loop) {
        while (msgLst.size() > 0) {
          byte[] msg = msgLst.remove(0);
          mcs.send(new DatagramPacket(msg, msg.length, group, port));
        }
        // pause 0.1 milliSecond or 100 microSeconds
        java.util.concurrent.TimeUnit.MICROSECONDS.sleep(100);
      }
    } catch (Exception ex) { }
  }
  private String[] ip;
  private volatile boolean loop = true;
  private volatile List<byte[]> msgLst = Collections.synchronizedList(new ArrayList<byte[]>());
}
