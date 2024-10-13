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
public class ODBEventListener implements Runnable {
  /**
  @param host_port string containing MultiCast IP and port. Example 224.0.0.3:9990
  */
  public ODBEventListener(String host_port) {
    ip = host_port.split(":");
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
  /**
  exit and close Listening
  */
  public void exitListening() {
    listened = false;
  }
  //
  public void run() {
    try (MulticastSocket mcs = new MulticastSocket(Integer.parseInt(ip[1]))){
       mcs.joinGroup(InetAddress.getByName(ip[0]));
       while (listened) {
         DatagramPacket packet = new DatagramPacket(new byte[256], 256);
         mcs.receive(packet); // wait for the incoming msg
         pool.submit(() -> {
           byte[] buf =  packet.getData();
           ODBEvent event = new ODBEvent((int) (buf[0] & 0xFF), new String(buf, 1, packet.getLength()-1));
           for (ODBEventListening odbe : set) odbe.odbEvent(event);
         });
       }
    } catch (Exception ex) { }
    pool.shutdownNow();
  }
  private String[] ip;
  private volatile boolean listened = true;
  private ExecutorService pool = Executors.newFixedThreadPool(16);
  private volatile Set<ODBEventListening> set = new CopyOnWriteArraySet<ODBEventListening>( );
}