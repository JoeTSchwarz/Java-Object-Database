import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.application.*;
import javafx.collections.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.text.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.geometry.*;
import javafx.event.*;
//
import java.util.*;
//
import joeapp.odb.*;
/**
 The Tabs
 @author Joe T. Schwarz (C)
*/
public class ODBTab implements Initializable {
  @FXML private Button fClose, fRollback, fRollbackKey, fFreeKeys,
                       fFreeKey, addCluster, removeCluster, removeClient;
  @FXML private TextArea report;
  //
  @FXML public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
    report.appendText("Report Area\n");
  }
  @FXML private void fClose() {
    String dbName = JFXController.jfx.input("ForcedClose ODB (local node only)");
    if (dbName != null && dbName.length() > 0) try {
      if (odbService.forcedClose(dbName)) report.appendText(dbName+" is forced to close.\n");
      else report.appendText("Unknown:"+dbName+".\n");
    } catch (Exception ex) {
      report.appendText("ForcedClose "+dbName+":"+ex.toString()+".\n");
    }
  }
  @FXML private void fRollback() {
    String dbName = JFXController.jfx.input("ForcedRollback ODB (local node only)");
    if (dbName != null && dbName.length() > 0) {
      List<String> list = odbService.lockedKeyList(dbName);
      if (list.size() > 0) try {
        if (odbService.forcedRollback(dbName)) report.appendText(dbName+" is forced to rollback.\n");
        else report.appendText("Unable to rollback "+dbName+".\n");
      } catch (Exception ex) {
        report.appendText("ForcedRollback "+dbName+":"+ex.toString()+".\n");
      }
      else report.appendText("No key of "+dbName+" is locked, or unknown "+dbName+".\n");
    }
  }
  @FXML private void fRollbackKey() {
    String dbName = JFXController.jfx.input("ForcedRollbackKey. DBName:");
    if (dbName != null && dbName.length() > 0) {
      List<String> list = odbService.lockedKeyList(dbName);
      if (list.size() > 0) {
        String key = JFXController.jfx.choice("ForcedRollnackKey", "Choose a key", list);
        if (key != null && key.length() > 0) try {
          if (odbService.forcedRollbackKey(dbName, key)) report.appendText(key+" of "+dbName+" is rollbacked.\n");
          else report.appendText("Unable to rollback "+key+" of "+dbName+"\n");
        } catch (Exception ex) {
          report.appendText("ForcedRollbackKey:"+key+" of "+dbName+":"+ex.toString()+".\n");
        }
      } else report.appendText("No key of "+dbName+" is locked, or unknown "+dbName+".\n");
    }
  }
  @FXML private void fFreeKeys() {
    String dbName = JFXController.jfx.input("ForcedFreeKeys of ODB (local node only)");
    if (dbName != null && dbName.length() > 0) {
      List<String> list = odbService.lockedKeyList(dbName);
      if (list.size() > 0) try {
          if (odbService.forcedFreeKeys(dbName)) report.appendText("All keys of "+dbName+" are freed.\n");
          else report.appendText("Unable to free all keys of "+dbName+".\n");
      } catch (Exception ex) {
        report.appendText("ForcedFreeKeys "+dbName+":"+ex.toString()+".\n");
      }
      else report.appendText("No key of "+dbName+" is locked, or unknown "+dbName+".\n");
    }
  }
  @FXML private void fFreeKey() {
    String dbName = JFXController.jfx.input("ForcedFreeKey. DBName:");
    if (dbName != null && dbName.length() > 0) {
      List<String> list = odbService.lockedKeyList(dbName);
      if (list.size() > 0) {
        String key = JFXController.jfx.choice("ForcedFreeKey","Choose a key", list);
        if (key != null && key.length() > 0) try {
          if (odbService.forcedFreeKey(dbName, key)) report.appendText(key+" of "+dbName+" is freed.\n");
          else report.appendText("Unable to free "+key+" of "+dbName+"\n");
        } catch (Exception ex) {
          report.appendText("ForcedFreeKey:"+key+" of "+dbName+":"+ex.toString()+".\n");
        }
      } else report.appendText("No key of "+dbName+" is locked, or unknown "+dbName+".\n");
    }
  }
  @FXML private void aCluster() {
    String node = JFXController.jfx.input("Add Node (Host:Port or IP:Port)");
    if (node != null && node.length() > 0 && node.indexOf(":") > 0) {
      odbService.addNode(node.toLowerCase());
      report.appendText(node+" is added to Cluster.\n");
    }
  }
  @FXML private void rCluster() {
    String node = JFXController.jfx.input("Remove Node (Host:Port or IP:Port)");
    if (node != null && node.length() > 0 && node.indexOf(":") > 0) {
      if (webHost.equalsIgnoreCase(node)) {
        report.appendText("Can't remove itself ("+node+").\n");
        return;
      }
      odbService.removeNode(node.toLowerCase());
      report.appendText(node+" is removed from Cluster.\n");
    }
  }
  @FXML private void rClient() {
    String id = JFXController.jfx.input("Client ID (* for all):");
    if (id == null || id.length() == 0) return;
    boolean ok = odbService.removeClient(id);
    String msg = id.charAt(0) == '*'? "All Clients are ":"Client "+id+" is ";
    report.appendText(msg+(ok? "removed.":"unknown")+"\n");
  }
  /**
  setParm
  @param odbService ODBService instance
  */
  public void setParm(ODBService odbService) {
    this.odbService = odbService;
  }
  /**
  setNode
  @param webHost String, HostName:Port of this server
  */
  public void setNode(String webHost) {
    this.webHost = webHost;
  }
  //
  private String webHost;
  private ODBService odbService;
}
