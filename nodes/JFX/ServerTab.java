import java.util.*;
// JFX
import javafx.fxml.FXML;
import javafx.application.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.*;
import javafx.collections.*;
import javafx.fxml.Initializable;
//
import joeapp.odb.*;
/**
 the ServerTab is an implementation of Initializable and ODBListening
 @author Joe T. Schwarz (C)
*/
public class ServerTab implements Initializable, ODBEventListening {
  @FXML private Button Start, LogEnable, Odblist, Odbworker, Allclients,
                       Lockedkeylist, Broadcast, Exit;
  @FXML private ComboBox<String> PingNode;
  @FXML private TextArea report;
  /**
  initialize
  @param location URL instance
  @param resources ResourceBundle instance
  */
  @FXML public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
    LogEnable.setDisable(true);
    Odblist.setDisable(true);
    Odbworker.setDisable(true);
    Allclients.setDisable(true);
    Lockedkeylist.setDisable(true);
    Broadcast.setDisable(true);
    PingNode.setDisable(true);
    Start.setStyle("-fx-background-color: #7fff00; -fx-text-fill: #228b22"); // green/darkgreen
    Exit.setStyle("-fx-background-color: #ffe4c4; -fx-text-fill: red;"); // bisque/red
    report.appendText("Report Area\n");
  }
  @FXML private void start() {
    try {
      Start.setDisable(true);
      odbService = new ODBService(config);
      jfxController.setODBService(odbService);
      ODBController.setParm(odbService); // load Tab ODBMaintenance
      Start.setStyle("-fx-background-color: #7fff00; -fx-text-fill: blue"); // green/blue
      report.appendText("ODBServer is started.\nPrimary Node: "+webHost+" is ONLINE\n");
      if (!registered) {
        odbService.register(this);
        registered = true;
      }
      ODBController.setNode(webHost); 
      // enable other buttons
      LogEnable.setDisable(false);
      Odblist.setDisable(false);
      Odbworker.setDisable(false);
      Allclients.setDisable(false);
      Lockedkeylist.setDisable(false);
      Broadcast.setDisable(false);
      PingNode.setDisable(false);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(0);
    }
  }
  @FXML private void logEnable() {
    if (odbService == null) return;
    String[] aut = JFXController.jfx.login("Superuser Authentication");
    if (aut[0] == null || aut[1] == null || !uList.isSuperuser(aut[1], aut[0])) {
      report.appendText("Illegally try to change LOG mode.\n");
      return;
    }
    logOn = !logOn;
    if (logOn) LogEnable.setText("LOG Disable");
    else LogEnable.setText("LOG Enable");
    odbService.setLog(logOn);
  }
  @FXML private void odblist() {
    ArrayList<String> lst = odbService.dbList();
    if (lst.size() > 0) {
      report.appendText("Following ODB"+(lst.size() > 1?"s are":" is")+" active.\n");
      for (String db:lst) report.appendText("- ODB:"+db+"\n");
    } else report.appendText("No ODB is active.\n");    
  }
  @FXML private void odbworker() {
    String dbName = JFXController.jfx.input("DB Name");
    if (dbName == null) return;
    ArrayList<String> lst = odbService.activeWorkers(dbName);
    int s = lst.size();
    if (s > 0) {
      report.appendText("Following "+dbName+"-Worker"+(s > 1?"s are":" is")+" active:\n");
      for (int i = 0; i < s; ) {
        String S = lst.get(i++);
        if (S.charAt(0) != '+') report.appendText(i+". ClientWorker: "+S+"\n");
        else report.appendText(i+". AgentWorker: "+S.substring(1, S.length()-1)+"\n");
      }
    } else report.appendText("No Worker is active. Or "+dbName+" is inactive.\n");
  }
  @FXML private void allclients() {
    ArrayList<String> lst = odbService.activeClients();
    if (lst.size() > 0) {
      report.appendText("Following Client"+(lst.size() > 1?"s are":" is")+" active.\n");
      for (String uid:lst) if (uid.charAt(0) != '+')
           report.appendText("- WorkerID:"+uid+"\n");
      else report.appendText("- AgentID: "+uid.substring(1, uid.length()-1)+"\n");
    } else report.appendText("No Client is active.\n");
  }
  @FXML private void lockedkeylist() {
    String dbName = JFXController.jfx.input("DB Name");
    if (dbName == null) return;
    ArrayList<String> lst = odbService.lockedKeyList(dbName);
    int s = lst.size();
    if (s > 0) {
      report.appendText("Following "+dbName+"-key"+(s > 1?"s are":" is")+" locked:\n");
      for (int i = 0; i < s; ++i) report.appendText((i+1)+". Key: "+lst.get(i)+"\n");
    } else report.appendText("No Key is in ownership. Or "+dbName+" is inactive.\n");
  }
  @FXML private void broadcast() {
    String msg = JFXController.jfx.input("Your Message");
    if (msg != null) odbService.broadcast(msg);
  }
  @FXML private void pingnode() {
    String node = PingNode.getValue();
    long time = odbService.ping(node);
    if (time > 0) report.appendText("RoundTrip to Node "+node+": "+time+" mSec.\n");
    else report.appendText("Cluster Node "+node+" is unreachable.\n");
  }
  @FXML private void exit() {
    if (odbService != null) {
      String[] aut = JFXController.jfx.login("Superuser Authentication");
      if (aut[0] == null || aut[1] == null || !uList.isSuperuser(aut[1], aut[0])) {
        report.appendText("Illegally try to shutdown ODBServer.\n");
        return;
      }
      odbService.shutdown();        
      registered = false;
      odbService = null;
    }
    if (odbService == null) {
      Platform.exit();
      System.exit(0);
    }
  }
  // implement the ODBEvent-----------------------------------------------------
  /**
  odbEvent - ODBListening implementation
  @param e ODBEvent
  */
  public void odbEvent(ODBEvent e) {
    Platform.runLater(() -> {
      String node = e.getActiveNode();
      int type = e.getEventType();
      if (type == 13 || type != 12 && node.equals(webHost)) return;
      //
      switch (type) {
        case 0:
          report.appendText(node+" is DOWN\n");
          nodes.remove(node);
          break;
        case 1:
          if (nodes.contains(node)) return;
          report.appendText(node+" is ONLINE\n");
          nodes.add(node);
          break;
        case 2:
          report.appendText(node+" is READY\n");
          nodes.add(node);
          return;
        case 3:
          nodes.remove(node);
          report.appendText(node+" is UNKNOWN or UNAVAILABLE\n");
          return;
         case 4:
          report.appendText(node+": "+e.getMessage()+"\n");
          return;
        case 5:
          nodes.remove(node);
          report.appendText(node+" was removed from Cluster\n");
          break;
        case 6:
          if (nodes.contains(node)) return;
          nodes.add(node); // insert into PingNode
          report.appendText(node+" is added to Cluster\n");
          break;
        case 7:
          if (!nodes.contains(node)) return;
          nodes.remove(node); // remove from PingNode
          report.appendText(node+" is removed from Cluster\n");
          if (nodes.size() == 0) PingNode.setDisable(true);
          break;
        case 9:
          report.appendText(node+": "+e.getMessage()+" was forced to close.\n");
          return;
        case 10: // reply from online server
          if (!nodes.contains(node) && webHost.equals(e.getMessage())) {
            report.appendText(node+" is ONLINE\n");
            nodes.add(node);
          } else return;
          break;
        case 12:
          report.appendText(e.getMessage());
          return;
      }
      PingNode.setItems(FXCollections.observableArrayList(nodes));
    });
  }
  /**
  getService
  @return ODBService instance
  */
  public ODBService getService() {
    return odbService;
  }
  /**
  loadParm
  @param prop HashMap of ODBService properies
  @param config JODB config file
  @param uList UserList instance
  @param odbController ODBTab instance
  @param jfxController JFXController instance
  */
  public void loadParm(HashMap<String, String> prop, String config,
                       UserList uList, ODBTab ODBController,
                       JFXController jfxController) {
    this.prop = prop;
    this.uList = uList;
    this.config = config;
    this.ODBController = ODBController;
    this.jfxController = jfxController;
    logOn = prop.get("LOGGING").charAt(0) == '1';
    if (logOn) LogEnable.setText("LOG Disable");
    else LogEnable.setText("LOG Enable");
    webHost = prop.get("WEB_HOST/IP")+":"+prop.get("PORT");
    PingNode.setButtonCell(new ListCell<>() {
      @Override // set prompt text if empty or 0 item
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
          setText(PingNode.getPromptText());
        } else {
          setText(item);
        }
      }
    });
  }
  //
  public static boolean isOnline() {
    return registered;
  }
  //
  private UserList uList;
  private ODBTab ODBController;
  private ODBService odbService;
  private String config, webHost;
  private JFXController jfxController;
  private HashMap<String, String> prop;
  private ArrayList<String> nodes = new ArrayList<String>();
  private static boolean registered = false, mod = false, logOn;
}
