import java.util.*;
//
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.fxml.Initializable;
//
import joeapp.odb.*;
/**
 UserTab
 @author Joe T. Schwarz (C)
*/
public class UserTab implements Initializable {
  @FXML private Button Add, Change, Upgrade, Delete, Reset, Userlist, Recover, Save;
  @FXML private Label labPW, labID, labP;
  @FXML private TextField userID, priv;
  @FXML private PasswordField userPW;
  @FXML private TextArea report;
  /**
  initialize
  @param location URL instance
  @param resources ResourceBundle instance
  */
  @FXML public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
    report.appendText("Report Area\n");
    reset();
  }
  @FXML private void add() {
    if (mod) {
      mod = false;
      enable(Add, true);
      Add.setText("OK");
      JFXController.jfx.focus(userID);
      priv.setEditable(true);
      userID.setEditable(true);
      userPW.setEditable(true);
      labP.setTextFill(Color.RED);
      labID.setTextFill(Color.RED);
      labPW.setTextFill(Color.RED);
      Add.setStyle("-fx-text-fill: red;");
      report.setText("Fill the Fields with RED label then \n"+
                     "Click OK afer completing the required inputs\n"+
                     "To CANCEL OK with empty userID field\n"+
                     "No semicolon (:) or at (@) in PW and ID\n"+
                     "Min. 3 letters for uID and min. 4 for Password\n");
      return;
    }
    String uID = userID.getText();
    if (uID != null && uID.trim().length() >= 3) {
      report.clear();
      String uPW = userPW.getText();
      if (uPW == null|| uPW.trim().length() < 4) {
        report.appendText("Invalid UserPW. NO add.\n");
      } else if (uList.isValid(uPW, uID) && !uList.isExisted(uID)) {
        int pri = Integer.parseInt( priv.getText()); 
        if (uList.addUser(uPW, uID, pri)) {
          report.appendText("User "+uID+" is added.\n");
        } else report.appendText("Unable to add User "+uID+"\n");
      } else {
        if (uList.isExisted(uID)) report.appendText(uID+" existed already.\n");
        else report.appendText("Password or userID contains invalid charater : or @\n");
      }
    }
    Add.setText("ADD");
    Add.setStyle("-fx-text-fill: blue;");
    reset();
  }
  @FXML private void change() {
    if (mod) {
      mod = false;
      enable(Change, true);
      Change.setText("OK");
      JFXController.jfx.focus(userID);
      userID.setEditable(true);
      userPW.setEditable(true);
      labPW.setText("New Password");
      //
      labID.setTextFill(Color.RED);
      labPW.setTextFill(Color.RED);
      Change.setStyle("-fx-text-fill: red;");
      report.setText("Fill the Fields with RED label then \n"+
                     "Click OK afer completing the required inputs\n"+
                     "No semicolon (:) or at (@) in PW\n"+
                     "To CANCEL OK with empty userID field\n");
      return;
    }
    String uID = userID.getText();
    if (uID != null && uID.trim().length() >= 3) {
      report.clear();
      String uPW = userPW.getText(); 
      String oPW = JFXController.jfx.password("Old Password");
      if (uPW == null || uPW.trim().length() < 4 || oPW == null || oPW.trim().length() < 4) {
        report.appendText("Invalid Inputs\n");
      } else if (uList.isValid(uPW, uID) && uList.isValid(oPW)) {
        if (uList.changePassword(uID, oPW, uPW)) {
          report.appendText("Password of user "+uID+" is updated.\n");
        } else report.appendText("Unable to upgrade User "+uID+"\n");
      } else report.appendText("Password contains invalid charater : or + or @\n");
    }
    Change.setText("CHANGE PW");
    Change.setStyle("-fx-text-fill: blue;");
    labPW.setText("User Password");
    reset();
  }
  @FXML private void upgrade() {
    if (mod) {
      mod = false;
      enable(Upgrade, true);
      Upgrade.setText("OK");
      JFXController.jfx.focus(userID);
      userID.setEditable(true);
      priv.setEditable(true);
      labP.setTextFill(Color.RED);
      labID.setTextFill(Color.RED);
      Upgrade.setStyle("-fx-text-fill: red;");
      report.setText("Fill the Fields with RED label then \n"+
                     "Click OK afer completing the required inputs\n"+
                     "To CANCEL OK with empty userID field\n");
      return;
    }
    String uID = userID.getText();
    if (uID != null && uID.trim().length() >= 3 && uList.isValid(uID)) {
      report.clear();
      int pri = Integer.parseInt(priv.getText()); 
      if (uList.upgradePrivilege(suPW, suID, uID, pri)) {
        report.appendText("Privilege of user "+uID+" is upgraded.\n");
      } else report.appendText("Unable to upgrade User "+uID+"\n");
    }
    Upgrade.setText("UPGRADE");
    Upgrade.setStyle("-fx-text-fill: blue;");
    reset();
  }
  @FXML private void delete() {
    if (mod) {
      mod = false;
      enable(Delete, true);
      Delete.setText("OK");
      userID.requestFocus();
      userID.setEditable(true);
      labID.setTextFill(Color.RED);
      Delete.setStyle("-fx-text-fill: red;");
      report.setText("Fill the Field with RED label then \n"+
                     "Click OK afer completing the required inputs\n"+
                     "To CANCEL OK with empty userID field\n");
      return;
    }
    String uID = userID.getText();
    if (uID != null && uID.trim().length() >= 3 && uList.isValid(uID)) {
      report.clear();
      if (uList.deleteUser(uID)) {
        report.appendText("User "+uID+" is deleted.\n");
      } else report.appendText("Unable to delete User "+uID+"\n");
    }
    Delete.setText("DELETE");
    Delete.setStyle("-fx-text-fill: blue;");
    reset();
  }
  @FXML private void resetPW() {
    if (mod) {
      mod = false;
      enable(Reset, true);
      Reset.setText("OK");
      JFXController.jfx.focus(userID);
      userID.setEditable(true);
      labID.setTextFill(Color.RED);
      Reset.setStyle("-fx-text-fill: red;");
      report.setText("Fill the Field with RED label then \n"+
                     "Click OK afer completing the required inputs\n"+
                     "To CANCEL OK with empty userID field\n");
      return;
    }
    String uID = userID.getText();
    if (uID != null && uID.trim().length() >= 3 && uList.isValid(uID)) {
      report.clear();
      String pw = uList.resetPassword(uID);
      report.appendText("Password of user "+uID+" is now: "+pw+"\n");
    }
    Reset.setText("RESET PW");
    Reset.setStyle("-fx-text-fill: blue;");
    reset();
  }
  @FXML private void userlist() {
    ArrayList<String> lst = uList.getUserList(suPW, suID);  
    if (lst.size() > 0) {
      int i = 1;
      report.setText("UserList (UserID@Privilege):\n");
      for (String u : lst) {
        report.appendText(i+". "+u+"\n");
        ++i;
      }
    } else report.appendText("UserList is empty\n");
  }
  @FXML private void recover() {
    if (JFXController.jfx.confirm("Recover userlist")) try {
      uList.recover(prop.get("USERLIST"));
      report.setText("Recover completed.");
    } catch (Exception ex) {
      report.appendText("Unable to recover:"+prop.get("ODB_PATH")+
                        "/userlist.\nReason:\n"+ex.toString());
    }
  }
  @FXML private void save() {
    try {
      uList.save();
      report.setText("UserList is saved.");
    } catch (Exception ex) {
      report.setText("Unable to save UserList. Reason:"+ex.toString());
    }
  }
  //
  private void enable(Button but, boolean on) {
    Add.setDisable(on);
    Change.setDisable(on);
    Upgrade.setDisable(on);
    Delete.setDisable(on);
    Reset.setDisable(on);
    Userlist.setDisable(on);
    Recover.setDisable(on);
    Save.setDisable(on);
    if (but != null) but.setDisable(!on);
  }
  //
  public void loadParm(HashMap<String, String> prop, UserList uList) {
    this.uList = uList;
    this.prop = prop;
  }
  //
  public void superuser(String suID, String suPW) {
    this.suPW = suPW;
    this.suID = suID;
  }
  //
  private void reset() {
    mod = true;
    userID.setText("");
    userPW.setText("");
    enable(null, false);
    priv.setEditable(false);
    userID.setEditable(false);
    userPW.setEditable(false);
    labP.setTextFill(Color.WHITE);
    labID.setTextFill(Color.WHITE);
    labPW.setTextFill(Color.WHITE);
    //
    priv.setPromptText("0");
    priv.setOnKeyTyped(e -> {
      String c = priv.getText();
      int l = priv.getText().length();
      if(l == 0 || l > 1 || c.charAt(0) < '0' || c.charAt(0) > '3') {
        priv.clear();
        e.consume();
      }
    });
  }
  private HashMap<String, String> prop;
  private String suPW, suID;
  private UserList uList;
  private boolean mod;
}
