import java.io.*;
import java.util.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.text.*;
import java.text.NumberFormat;
//
import joeapp.odb.*;
// @author Joe T. Schwarz (C)
/**
Modelling with SWINGLoader Package
*/
public class ODBTab {
  /**
  ODBTab Controller
  @param map HashMap of J-Swing elements
  */
  public ODBTab(HashMap<String, Object> map) {
    this.map = map;
    JFrame frame = (JFrame)map.get("frame");
    JTextArea report = (JTextArea) map.get("area3");
    report.setText("Report Area.\n");
    report.setEditable(false);
    //
    ((JButton) map.get("fclose")).addActionListener(e -> {
      String dbName = JOptions.input(frame, "ForcedClose ODB (local node only)");
      if (dbName != null && dbName.length() > 0) try {
        if (odbService.forcedClose(dbName)) report.append(dbName+" is forced to close.\n");
        else report.append("Unknown:"+dbName+".\n");
      } catch (Exception ex) {
        report.append("ForcedClose "+dbName+":"+ex.toString()+".\n");
      }
    });
    ((JButton) map.get("frollb")).addActionListener(e -> {
      String dbName = JOptions.input(frame, "ForcedRollback ODB (local node only)");
      if (dbName != null && dbName.length() > 0) {
        List<String> list = odbService.lockedKeyList(dbName);
        if (list.size() > 0) try {
          if (odbService.forcedRollback(dbName)) report.append(dbName+" is forced to rollback.\n");
          else report.append("Unable to rollback "+dbName+".\n");
        } catch (Exception ex) {
          report.append("ForcedRollback "+dbName+":"+ex.toString()+".\n");
        }
        else report.append("No key of "+dbName+" is locked, or unknown "+dbName+".\n");
      }
    });
    ((JButton) map.get("frollK")).addActionListener(e -> {
      String dbName =  JOptions.input(frame, "ForcedRollbackKey. DBName:");
      if (dbName != null && dbName.length() > 0) {
        List<String> list = odbService.lockedKeyList(dbName);
        if (list.size() > 0) {
          String key = JOptions.choice(frame, "Choose a Key:", list);
          if (key != null && key.length() > 0) try {
            if (odbService.forcedRollbackKey(dbName, key))
                 report.append(key+" of "+dbName+" is rollbacked.\n");
            else report.append("Unable to rollback "+key+" of "+dbName+"\n");
          } catch (Exception ex) {
            report.append("ForcedRollbackKey:"+key+" of "+dbName+":"+ex.toString()+".\n");
          }
        } else report.append("No key of "+dbName+" is locked, or unknown "+dbName+".\n");
      }
    });
    ((JButton) map.get("ffreeK")).addActionListener(e -> {
      String dbName = JOptions.input(frame, "ForcedFreeKey. DBName:");
      if (dbName != null && dbName.length() > 0) {
        List<String> list = odbService.lockedKeyList(dbName);
        if (list.size() > 0) {
          String key = JOptions.choice(frame, "Choose a Key:", list);
          if (key != null && key.length() > 0) try {
            if (odbService.forcedFreeKey(dbName, key))
                 report.append(key+" of "+dbName+" is freed.\n");
            else report.append("Unable to free "+key+" of "+dbName+"\n");
          } catch (Exception ex) {
            report.append("ForcedFreeKey:"+key+" of "+dbName+":"+ex.toString()+".\n");
          }
        } else report.append("No key of "+dbName+" is locked, or unknown "+dbName+".\n");
      }
    });
    ((JButton) map.get("ffree")).addActionListener(e -> {
      String dbName = JOptions.input(frame, "ForcedFreeKeys of ODB  (local node only)");
      if (dbName != null && dbName.length() > 0) {
        List<String> list = odbService.lockedKeyList(dbName);
        if (list.size() > 0) try {
          if (odbService.forcedFreeKeys(dbName))
               report.append("All keys of "+dbName+" are freed.\n");
          else report.append("Unable to free all keys of "+".\n");
        } catch (Exception ex) {
          report.append("ForcedFreeKeys "+dbName+":"+ex.toString()+".\n");
        }
        else report.append("No key of "+dbName+" is locked, or unknown "+dbName+".\n");
      }
    });
    ((JButton) map.get("addC")).addActionListener(e -> {
      String node = JOptionPane.showInputDialog(frame, "Add Node (Host:Port or IP:Port)");
      if (node != null && node.indexOf(":") > 0) {
        odbService.addNode(node.toLowerCase());
        report.append(node+" is added to Cluster.\n");
      }
    });
    ((JButton) map.get("remC")).addActionListener(e -> {
      String node = JOptionPane.showInputDialog(frame, "Remove Node (Host:Port or IP:Port)");
      if (node != null && node.indexOf(":") > 0) {
        if (webHost.equalsIgnoreCase(node)) {
          report.append("Can't remove itself ("+node+").\n");
          return;
        }
        odbService.removeNode(node.toLowerCase());
        report.append(node+" is removed from cluster.\n");
      }
    });
    ((JButton) map.get("killC")).addActionListener(e -> {
      String id = JOptions.input(frame, "Client ID (* for all):");
      if (id == null || id.length() == 0) return;
      boolean ok = odbService.removeClient(id);
      String msg = id.charAt(0) == '*'? "All Clients are ":"Client "+id+" is ";
      report.append(msg+(ok? "removed.":"unknown")+"\n");
    });
  }
  /**
  setService
  @param odbService ODBService
  */
  public void setService(ODBService odbService) {
    this.odbService = odbService;
  }
  /**
  @param webHost String, HostName:Port of this server
  */
  public void setNode(String webHost) {
    this.webHost = webHost;
  }
  //
  private String webHost;
  private ODBService odbService;
  private HashMap<String, Object> map;
}

