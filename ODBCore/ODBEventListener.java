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
<br>owner: String. HostName@Port or HostIP@Port and msg can be a node (like owner) or any String
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
<br>10: ODBManager, received node replies to UP node
<br>11: Notify add/delete/update. Format: 11, userID|dbName, list&lt;message&gt; 
<br>12: Client sends msg to JODB. Format: 12, node, list&lt;message&gt; 
<br>13: Customized message. Format: 13, message; 
<br>rest is reserved for future use
@author Joe T. Schwarz
*/
public class ODBEventListener implements Runnable {
  /**
  @param host_port string, HostName@Port or HostIP@Port. Example 224.0.0.3@9990
  */
  public ODBEventListener(String host_port, ODBEventListening odbe) {
    ip = ODBParser.split(host_port, "@");
    this.odbe = odbe;
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
    try {
      mcs = new MulticastSocket(Integer.parseInt(ip[1]));
      mcs.joinGroup(InetAddress.getByName(ip[0]));
      while (true) {
        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
        mcs.receive(packet); // wait for the incoming msg
        odbe.odbEvent(new ODBEvent(packet));
      }
    } catch (Exception ex) { }
    exit();
  }
  private String ip[];
  private MulticastSocket mcs;
  private ODBEventListening odbe;
}