package joeapp.odb;
import java.io.*;
import java.net.*;
import java.util.*;
//
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.*;
/**
Message format: Owner_Msg_List. Cmd (Type): 1 byte. n bytes owner, n bytes message, n bytes list of nodes (cluster)
<br>owner: String. HostName:PortNumber or HostIP:Port and msg can be a node (like owner) or any String
<br>0:  Node down (offline). Format: 0, node, list&lt;alternative JODB&gt;
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
public class ODBEvent {
  /**
  Constructor
  @param bb byte array containing information and message
  */
  public ODBEvent(byte[] bb) {
    this.type = (int)(bb[0] & 0xFF);
    if (type == 13) {
      message = new String(bb, 1, bb.length-1).trim();
      nodes = null;
      node = null;
    } else {
      nodes = (new String(bb, 1, bb.length-1)).split(""+(char)0x01);
      message = nodes[1];
      node = nodes[0];
    }
  }
  /**
  getNodes
  @return String array containing all online nodes (format: hostName/IP:Port)
  */
  public String[] getNodes() {
    return nodes;
  }
  /**
  getEventType (or cmd. See above)
  @return String, the EventMessage
  */
  public int getEventType() {
    return type;
  }
  /**
  getMessage returns the event-message or dbName or even a node
  @return String, the EventMessage
  */
  public String getMessage() {
    return message;
  }
  /**
  getActiveNode
  @return String, the running node (Host:Port)
  */
  public String getActiveNode() {
    return node;
  }
  /** 
  onEvent: SWITCH connect to the next online host (fail-safe OODB) in the queue 
  @param pw Password
  @param uid UserID
  @param dbList ArrayList containing all OODB names
  @return ODBConnect object, null for no connection or no alternative host was found
  */
  public ODBConnect onEvent(String pw, String uid, List<String> dbList) {
    for (int i = 1; i < nodes.length; ++i) if (!nodes[i].equals(node)) try {
      String[] ip =  nodes[i].split(":");
      ODBConnect odbc = new ODBConnect(ip[0], Integer.parseInt(ip[1]), pw, uid);
      // connect to the alternative node. Ignore *
      for (String dbName : dbList) try {
        odbc.connect(dbName);
      } catch (Exception e) { }
      node = nodes[i];
      return odbc;
    } catch (Exception ex) { }
    return null;
  }
  //
  private String node, message;
  private String[] nodes;
  private int type;
}
