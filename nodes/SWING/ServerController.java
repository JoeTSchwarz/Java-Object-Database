import java.io.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.text.*;
import java.text.NumberFormat;
//
import joeapp.odb.*;
// 
/**
MVC-Modelling with SWINGLoader Package
@author Joe T. Schwarz (C)
*/
public class ServerController {
  /**
  Predefined Constructor for Controller
  @param map HashMap with String as keys (defined in the model) and Object as values (J Components)
  @param parms String array for additional parameters from main().
  */
  @SuppressWarnings("unchecked")
  public ServerController(HashMap<String, Object> map, String[] parms) {
    this.map = map;
    try {
      prop = ODBParser.odbProperty(parms[0]);
      uFile = prop.get("USERLIST");
      uList = new UserList(uFile);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(0);
    }
    // start Sysmon
    JFrame frame = (JFrame)map.get("frame");
    tabbed = (JTabbedPane) map.get("Tabbed");
    //
    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    serverTab = new ServerTab(map, prop, uList, this, parms[0]);
    userTab = new UserTab(map, uList, uFile);
    odbTab = new ODBTab(map);
    odbTab.setNode(serverTab.getNode());
    // customized JPanel SysMonSWING from _tab4_.txt
    tabbed.addChangeListener(e -> {
      int idx = tabbed.getSelectedIndex();
      if (idx == 0) return; // ServerTab
      if (!serverTab.isOnline()) {
        tabbed.setSelectedIndex(0);
        return;
      }
      String[] aut = JOptions.login(frame, "SuperUser Authentication");
      if (aut[0].length() == 0 || aut[1].length() == 0) {
        tabbed.setSelectedIndex(0);
        return;
      }
      if (!uList.isSuperuser(aut[1], aut[0])) {
        JOptionPane.showMessageDialog(frame, "You must be Superuser to open this Tab", 
                                             "ALERT", JOptionPane.ERROR_MESSAGE);
        tabbed.setSelectedIndex(0);
      }
    });
  }
  /**
  setService
  @param odbService ODBService
  */
  public void setService(ODBService odbService) {
    this.odbService = odbService;
    odbTab.setService(odbService);
  }
  private HashMap<String, String> prop;
  private HashMap<String, Object> map;
  private ODBService odbService;
  private ServerTab serverTab;
  private JTabbedPane tabbed;
  private UserTab userTab;
  private UserList uList;
  private ODBTab odbTab;
  private String uFile;
}

