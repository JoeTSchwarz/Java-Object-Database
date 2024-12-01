import java.io.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.text.*;
import java.text.NumberFormat;
//
import joeapp.odb.*;
// @author Joe T. Schwarz (C)
/**
MVC-Modelling with SWINGLoader Package
*/
public class UserTab {
  /**
  UserTab
  @param map HashMap with String as keys (defined in the model) and Object as values (J Components)
  @param uList UserList instance
  @param uFile UserList file name
  */
  public UserTab(HashMap<String, Object> map,  UserList uList, String uFile) {
    this.map = map;
    this.uList = uList;
    this.uFile = uFile;
    frame = (JFrame)map.get("frame");
    report = (JTextArea) map.get("area2");
    tPri = (JFormattedTextField) map.get("pri");
    tPri.setText("0");
    tPri.addKeyListener(new KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        char c = e.getKeyChar();
        if (c < '0' || c > '3') e.consume(); 
      }
    });
    tID = (JTextField) map.get("tuid");
    tPW = (JPasswordField) map.get("tpw");
    lab = (JLabel) map.get("lpw");
    //
    reset();
    report.setEditable(false);
    report.setText("Report Area\n");
    JButton aBut = (JButton) map.get("add");
    aBut.addActionListener(e -> {
      add(aBut);
    });
    JButton tBut = (JButton)map.get("change");
    tBut.addActionListener(e->{
      change(tBut);
    });
    JButton uBut = (JButton)map.get("upgrade");
    uBut.addActionListener(e->{
      upgrade(uBut);
    });
    JButton dBut = (JButton)map.get("delete");
    dBut.addActionListener(e -> {
      delete(dBut);
    });
    JButton rpwBut = (JButton) map.get("resetPW");
    rpwBut.addActionListener(e -> {
      resetPW(rpwBut);
    });
    JButton rBut = (JButton)map.get("recover");
    rBut.addActionListener(e -> {
      Object[] options = { "OK", "CANCEL" };
      if (JOptionPane.showOptionDialog(frame, "Are you sure?", "Warning",
          JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
          null, options, options[0]) == 0) {
        try {
          uList.recover(uFile);
        } catch (Exception ex) {
          report.append("Unable to recover:"+odbPath+"/userlist.\nReason:\n"+ex.toString());
        }
      }
    });
    ((JButton)map.get("uList")).addActionListener(e->{
      ArrayList<String> lst = uList.getUserList(suPW, suID);
      report.setText("");;
      if (lst.size() > 0) {
        report.append("UserList (UserID@Privilege):\n");
        int i = 1;
        for (String u : lst) {
          report.append(i+". "+u+"\n");
          ++i;
        }
      } else report.append("UserList is empty\n");
    });
    ((JButton) map.get("save")).addActionListener(e -> {
      try {
        uList.save();
        report.setText("UserList is saved.");
      } catch (Exception ex) {
        report.setText("Unable to save UserList. Reason:"+ex.toString());
      }
    });
  }
  /**
  authenticate
  @param pw String, password
  @param uid String, userID
  */
  public void authenticate(String pw, String uid) {
    suID = uid;
    suPW = pw;
  }
  /**
  add a new user
  @param but JButton instance
  */
  private void add(JButton but) {
    if (mod) {
      mod = false;
      but.setText("OK");
      tID.requestFocus();
      tID.setEditable(true);
      tPW.setEditable(true);
      tPri.setEditable(true);
      enable("add", false);
      but.setForeground(Color.red);
      ((JLabel) map.get("lpw")).setForeground(Color.red);
      ((JLabel) map.get("luid")).setForeground(Color.red);
      ((JLabel) map.get("lpri")).setForeground(Color.red);
      report.append("Fill the Fields with RED label then \n"+
                     "Click OK afer completing the required inputs\n"+
                     "To CANCEL OK with empty userID field\n"+
                     "No semicolon (:) or at (@) in PW and ID\n"+
                     "Min. 3 letters for uID and min. 4 for Password\n");
      return;
    }
    uID = tID.getText();
    if (uID != null && uID.trim().length() >= 3) {
      report.setText("");;
      uPW = new String(tPW.getPassword()); 
      if (uPW == null || uPW.trim().length() < 4) {
        report.append("Invalid Privilege or UserID or UserPW. NO add.\n");
      } else if (uList.isValid(uPW, uID) && !uList.isExisted(uID)) {
        int pri = Integer.parseInt(tPri.getText()); 
        if (uList.addUser(uPW, uID, pri)) {
          report.append("User "+uID+" is added.\n");
        } else error("Unable to add User "+uID+"\n");
      } else {
        if (uList.isExisted(uID)) report.append(uID+" existed already.\n");
        else report.append("Password or userID contains invalid charater : or @\n");
      }
    }
    but.setText("ADD");
    but.setForeground(Color.black);
    reset();
  }
  private void change(JButton but) {
    if (mod) {
      mod = false;
      but.setText("OK");
      tID.requestFocus();
      tID.setEditable(true);
      enable("change", false);
      but.setForeground(Color.red);
      ((JLabel) map.get("luid")).setForeground(Color.red);
      report.append("Fill the Fields with RED label then \n"+
                     "Click OK afer completing the required inputs\n"+
                     "No semicolon (:) or at (@) in PW\n"+
                     "To CANCEL OK with empty userID field\n");
      return;
    }
    uID = tID.getText();
    if (uID != null && uID.trim().length() >= 3) {
      report.setText("");
      // pw[0]: oldPW, pw[1]: newPW, pw[2]: confirmed PW
      String pw[] = JOptions.changePW(frame);
      if (pw != null) {
        if (pw[0].trim().length() < 4 || pw[1].trim().length() < 4 ||
            pw[2].trim().length() < 4 || !pw[2].equals(pw[1])) {
          report.append("Invalid or mismatched PWs\n");
          return;
        }
        if (uList.isValid(pw[1], uID) && uList.isValid(pw[0])) {
          if (uList.changePassword(uID, pw[0], pw[1])) {
            report.append("Password of user "+uID+" is updated.\n");
          } else report.append("Unable to upgrade User "+uID+"\n");
        } else report.append("Password contains invalid charater : or + or @\n");
      }
    }
    but.setText("CHANGE PW");
    but.setForeground(Color.black);
    lab.setText("User Password");
    reset();
  }
  private void upgrade(JButton but) {
    if (mod) {
      mod = false;
      but.setText("OK");
      tID.requestFocus();
      tID.setEditable(true);
      tPri.setEditable(true);
      enable("upgrade", false);
      but.setForeground(Color.red);
      ((JLabel) map.get("luid")).setForeground(Color.red);
      ((JLabel) map.get("lpri")).setForeground(Color.red);
      report.append("Fill the Fields with RED label then \n"+
                     "Click OK afer completing the required inputs\n"+
                     "To CANCEL OK with empty userID field\n");
      return;
    }
    uID = tID.getText();
    if (uID != null && uID.trim().length() >= 3 && uList.isValid(uID)) {
      report.setText("");;
      int pri = Integer.parseInt(tPri.getText()); 
      if (uList.upgradePrivilege(suPW, suID, uID, pri)) {
        report.append("Privilege of user "+uID+" is upgraded.\n");
      } else error("Unable to upgrade User "+uID+"\n");
    }
    but.setText("UPGRADE");
    but.setForeground(Color.black);
    reset();
  }
  private void resetPW(JButton but) {
    if (mod) {
      mod = false;
      but.setText("OK");
      tID.requestFocus();
      tID.setEditable(true);
      enable("resetPW", false);
      but.setForeground(Color.red);
      ((JLabel) map.get("luid")).setForeground(Color.red);
      report.append("Fill the Field with RED label then \n"+
                     "Click OK afer completing the required input\n"+
                     "To CANCEL OK with empty userID field\n");
      return;
    }
    uID = tID.getText();
    if (uID != null && uID.trim().length() >= 3 && uList.isValid(uID)) {
      report.setText("");;
      String pw = uList.resetPassword(uID);
      report.append("Password of user "+uID+" is now: "+pw+"\n");
    }
    but.setText("RESET PW");
    but.setForeground(Color.black);
    reset();
  }
  private void delete(JButton but) {
    if (mod) {
      mod = false;
      but.setText("OK");
      tID.requestFocus();
      tID.setEditable(true);
      enable("delete", false);
      but.setForeground(Color.red);
      ((JLabel) map.get("luid")).setForeground(Color.red);
      report.append("Fill the Field with RED label then \n"+
                     "Click OK afer completing the required input\n"+
                     "To CANCEL OK with empty userID field\n");
      return;
    }
    uID = tID.getText();
    if (uID != null && uID.trim().length() >= 3 && uList.isValid(uID)) {
      report.setText("");;
      if (uList.deleteUser(uID)) {
        report.append("User "+uID+" is deleted.\n");
      } else error("Unable to delete User "+uID+"\n");
    }
    but.setText("DELETE");
    but.setForeground(Color.black);
    reset();
  }
  private void enable(String but, boolean on) {
    ((JButton) map.get("add")).setEnabled(on);
    ((JButton) map.get("change")).setEnabled(on);
    ((JButton) map.get("upgrade")).setEnabled(on);
    ((JButton) map.get("delete")).setEnabled(on);
    ((JButton) map.get("resetPW")).setEnabled(on);
    ((JButton) map.get("uList")).setEnabled(on);
    ((JButton) map.get("recover")).setEnabled(on);
    ((JButton) map.get("save")).setEnabled(on);
    if (but != null) ((JButton) map.get(but)).setEnabled(!on);
  }
  private void reset() {
    pri = 0;
    mod = true;
    tID.setText("");
    tPW.setText("");
    tPri.setText("0");
    enable(null, true);
    tID.setEditable(false);
    tPW.setEditable(false);
    tPri.setEditable(false);
    ((JLabel) map.get("lpw")).setForeground(Color.black);
    ((JLabel) map.get("luid")).setForeground(Color.black);
    ((JLabel) map.get("lpri")).setForeground(Color.black);
  }
  private void error(String msg) {
    JOptionPane.showMessageDialog(frame, msg, "Error", JOptionPane.ERROR_MESSAGE);
  }
  private int pri;
  private JLabel lab;
  private JFrame frame;
  private UserList uList;
  private JTextField tID;
  private JTextArea report;
  private boolean mod = true;
  private JPasswordField tPW;
  private JFormattedTextField tPri;
  private HashMap<String, Object> map;
  private String uID, uPW, odbPath, suID, suPW, uFile;
}

