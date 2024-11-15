package joeapp.odb;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import java.util.concurrent.*;
// ODBEventListener wait for broacasted message (on client site)
// as an implementation of ODBEventListening
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
<br>rest is reserved for future use
@author Joe T. Schwarz
*/
public class ODBEventListener implements Runnable {
  /**
  @param host_port string containing MultiCast IP and port. Example 224.0.0.3:9990
  */
  public ODBEventListener(String host_port) {
    this.host_port = host_port;
  }
  /**
  @param odbe an implemented ODBListening object
  */
  public void addListener(ODBEventListening odbe) {
    if (!set.contains(odbe)) set.add(odbe);
  }
  /**
  @param odbe an implemented ODBListening object
  */
  public void removeListener(ODBEventListening odbe) {
    set.remove(odbe);
  }
  // called by ODBService
  public void exit() {
    if (mcs != null) try {
      mcs.close();
      mcs = null;
    } catch (Exception ex) { }
  }
  //
  public void run() {
    boolean running = false;
    try {
      String[] ip = host_port.split(":");
      mcs = new MulticastSocket(Integer.parseInt(ip[1]));
      mcs.joinGroup(InetAddress.getByName(ip[0]));
      running = true;
      while (true) {
        DatagramPacket packet = new DatagramPacket(new byte[256], 256);
        mcs.receive(packet); // wait for the incoming msg
        pool.submit(() -> {
          byte[] buf =  packet.getData(); 
          ODBEvent event = buf[0] > (byte)0x1F ? new ODBEvent(new String(buf, 0, packet.getLength())):
                           new ODBEvent((int) (buf[0] & 0xFF), new String(buf, 1, packet.getLength()-1));
          for (ODBEventListening odbe : set) odbe.odbEvent(event);
        });
      }
    } catch (Exception ex) { }
    if (!running) {
      System.err.println("Unable to start ODBBroadcaster:"+host_port);
      System.exit(0);
    }
    pool.shutdownNow();
  }
  private String host_port;
  private MulticastSocket mcs;
  private ExecutorService pool = Executors.newFixedThreadPool(16);
  private volatile Set<ODBEventListening> set = new CopyOnWriteArraySet<ODBEventListening>( );
}