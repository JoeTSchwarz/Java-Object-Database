import java.io.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.text.*;
import java.util.concurrent.*;
import java.text.NumberFormat;
//
import joeapp.odb.*;
// 
/**
MVC-Modelling with SWINGLoader Package
@author Joe T. Schwarz (C)
*/
public class ServerTab implements ODBEventListening {
  /**
  ServerTab an implementation of ODBListening
  @param map HashMap with String as keys (defined in the model) and Object as values (J Components)
  @param prop Proprieties map
  @param uList UserList
  @param serverController ServerController
  */
  @SuppressWarnings("unchecked")
  public ServerTab(HashMap<String, Object> map, HashMap<String, String> prop,
                   UserList uList, ServerController serverController, String config) {
    this.map = map;
    this.prop = prop;
    this.uList = uList;
    this.config = config;
    this.serverController = serverController;
    //
    frame = (JFrame)map.get("frame");
    log = ((JButton)map.get("log"));
    logOn = prop.get("LOGGING").charAt(0) == '1';
    if (logOn) log.setText("LOG Disable");
    else log.setText("LOG Enable");
    webHost = prop.get("WEB_HOST/IP")+":"+prop.get("PORT");
    report = (JTextArea) map.get("area1");
    report.setText("Report Area.\n");
    report.setEditable(false);
    //
    log.setEnabled(false);
    ((JButton)map.get("clients")).setEnabled(false);
    ((JButton)map.get("workers")).setEnabled(false);
    ((JButton)map.get("bcast")).setEnabled(false);
    ((JButton)map.get("keys")).setEnabled(false);
    ((JButton)map.get("list")).setEnabled(false);
    //
    ((JButton) map.get("start")).addActionListener(e -> {
      try {
        odbService = new ODBService(config);
        report.append("ODBServer is started.\nPrimary Node: "+webHost+" is ONLINE\n");
        if (!registered) {
          odbService.register(This);
          registered = true;
        }
        log.setEnabled(true);
        pool = odbService.getPool();
        serverController.setService(odbService);
        ((JButton)map.get("keys")).setEnabled(true);
        ((JButton)map.get("list")).setEnabled(true);
        ((JButton)map.get("bcast")).setEnabled(true);
        ((JButton)map.get("clients")).setEnabled(true);
        ((JButton)map.get("workers")).setEnabled(true);
        ((JButton) map.get("start")).setEnabled(false);
        ((JComboBox<String>) map.get("ping")).setEnabled(true);
      } catch (Exception ex) {
        ex.printStackTrace();
        System.exit(0);
      }
    });
    log.addActionListener(e -> {
      String[] aut = JOptions.login(frame, "SuperUser Authentication");
      if(aut[0].length() > 0 && aut[1].length() > 0 && uList.isSuperuser(aut[1], aut[0])) {
        logOn = !logOn;
        if (logOn) log.setText("LOG Disable");
        else log.setText("LOG Enable");
        odbService.setLog(logOn);
      } else {
        report.append("Illegally try to change LOG mode.\n");
        return;
      }
    });
    ((JButton)map.get("list")).addActionListener(e->{
      ArrayList<String> lst = odbService.dbList();
      if (lst.size() > 0) {
        report.append("Following ODB"+(lst.size() > 1?"s are":" is")+" active.\n");
        for (String db:lst) report.append("- ODB:"+db+"\n");
      } else report.append("No ODB is active.\n");
    });
    ((JButton)map.get("clients")).addActionListener(e->{
      ArrayList<String> lst = odbService.activeClients();
      if (lst.size() > 0) {
        report.append("Following Client"+(lst.size() > 1?"s are":" is")+" active.\n");
        for (String uid:lst) if (uid.charAt(0) != '+')
             report.append("- WorkerID:"+uid+"\n");
        else report.append("- AgentID: "+uid.substring(1, uid.length()-1)+"\n");
      } else report.append("No Client is active.\n");
    });
    ((JButton)map.get("workers")).addActionListener(e -> {
      String dbName = JOptions.input(frame, "DB Name");
      if (dbName == null) return;
      ArrayList<String> lst = odbService.activeWorkers(dbName);
      int s = lst.size();
      if (s > 0) {
        report.append("Following "+dbName+"-Worker"+(s > 1?"s are":" is")+" active:\n");
        for (int i = 0; i < s; ) {
          String S = lst.get(i++);
          if (S.charAt(0) != '+') report.append(i+". ClientWorker: "+S+"\n");
          else report.append(i+". AgentWorker: "+S.substring(1, S.length()-1)+"\n");
        }
      } else report.append("No Worker is active. Or "+dbName+" is inactive.\n");
    });
    ((JButton)map.get("keys")).addActionListener(e->{
      String dbName = JOptions.input(frame, "DB Name");
      if (dbName == null) return;
      ArrayList<String> lst = odbService.lockedKeyList(dbName);
      int s = lst.size();
      if (s > 0) {
        report.append("Following "+dbName+"-key"+(s > 1?"s are":" is")+" locked:\n");
        for (int i = 0; i < s; ++i) report.append((i+1)+". Key: "+lst.get(i)+"\n");
      } else report.append("No Key is in ownership. Or "+dbName+" is inactive.\n");
    });
    ((JButton)map.get("bcast")).addActionListener(e -> {
      String msg = JOptions.input(frame, "Your Message");
      if (msg != null) odbService.broadcast(msg);
    });
    bPing = (JComboBox<String>) map.get("ping");
    bPing.addActionListener(e -> {
      String node = (String) ((JComboBox<String>) map.get("ping")).getSelectedItem();
      long time = odbService.ping(node);
      if (time > 0) report.append("RoundTrip: "+time+" mSec.\n");
      else report.append("Cluster Node: "+node+" is unreachable.\n");
      return;
    });
    ((JButton) map.get("exit")).addActionListener(e -> {
      if (odbService != null) {
        String[] aut = JOptions.login(frame, "SuperUser Authentication");
        if(aut[0].length() > 0 && aut[1].length() > 0 && uList.isSuperuser(aut[1], aut[0])) {
          odbService.shutdown();        
          registered = false;
          odbService = null;
        } else {
          report.append("Illegally try to shutdown ODBServer.\n");
          return;
        }
      }
      if (odbService == null) System.exit(0);
    });
  }
  // implement the ODBEvent-----------------------------------------------------
  /**
  ODBListening implementation
  @param e ODBEvent
  */
  public void odbEvent(ODBEvent e) {
    pool.execute(() -> {
      String node = e.getActiveNode();
      int type = e.getEventType();
      if (type == 13 || type != 12 && node.equals(webHost)) return;
      switch (type) {
        case 0:
          report.append(node+" is DOWN\n");
          nodes.remove(node);
          bPing.removeItem((Object)node);
          return;
        case 1:
          if (nodes.contains(node)) return;
          report.append(node+" is ONLINE\n");
          nodes.add(node);
          bPing.addItem(node);
          return;
        case 2:
          report.append(node+" is READY\n");
          nodes.add(node);
          return;
        case 3:
          nodes.remove(node);
          report.append(node+" is UNKNOWN or UNAVAILABLE\n");
          return;
        case 4:
          report.append(node+": "+e.getMessage()+"\n");
          return;
        case 5:
          nodes.remove(node);
          bPing.removeItem((Object)node);
          report.append(node+" was removed from Cluster\n");
          return;
        case 6:
          bPing.setEnabled(true);
          if (nodes.contains(node)) return;
          //
          nodes.add(node);
          bPing.addItem(node);
          report.append(node+" is added to Cluster\n");
          return;
        case 7:
          if (!nodes.contains(node)) return;
          //
          nodes.remove(node);
          bPing.removeItem((Object)node);
          report.append(node+" is removed from Cluster\n");
          return;
        case 9:
          report.append(node+": "+e.getMessage()+" was forced to close.\n");
          return;
        case 10: // reply from online server
          if (!nodes.contains(node) && webHost.equals(e.getMessage())) {
            report.append(node+" is ONLINE\n");
            nodes.add(node);
            bPing.addItem(node);
          }
          return;
        case 12:
          report.append(e.getMessage());
      }
    });
  }
  /**
  isOnline
  @return boolean
  */
  public boolean isOnline() {
    return registered;
  }
  /**
  getNode
  return String for this node: hostName/IP:Port
  */
  public String getNode() {
    return webHost;
  }
  //----------------------------------------------------------------------------
  private JButton log;
  private JFrame frame;
  private UserList uList;
  private JTextArea report;
  private ExecutorService pool;
  private ServerTab This = this;
  private ODBService odbService;
  private String webHost, config;
  private JComboBox<String> bPing;
  private HashMap<String, Object> map;
  private HashMap<String, String> prop;
  private ServerController serverController;
  private boolean registered = false, logOn;
  private ArrayList<String> nodes = new ArrayList<>();
}

