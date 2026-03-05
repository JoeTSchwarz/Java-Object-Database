import java.io.*;
import java.util.*;
//
import javafx.fxml.FXML;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.application.Platform;
import javafx.scene.control.Alert.AlertType;
//
import joeapp.odb.*;
// @author Joe T. Schwarz (C)
/**
Controller for JfxODBServer
*/
public class JFXController implements Initializable {
  @FXML private TabPane tabPane;
  @FXML private ODBTab ODBController;
  @FXML private UserTab UserController;
  @FXML private ServerTab ServerController;
  @FXML private Tab userTab, serverTab, odbTab;
  //
  @FXML public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
    model = tabPane.getSelectionModel();
    jfx = new JFXOptions(tabPane, this.getClass().getResource(JFXODBServer.css).toExternalForm());
  }
  //
  @FXML void event(javafx.event.Event ev) {
    if (!userTab.isSelected() && !odbTab.isSelected()) return;
    if (!ServerTab.isOnline()) {
      model.select(0);
      return;
    }
    // return ret[0]: userID, ret[1]: password
    String[] aut = jfx.login("Superuser Authentication");
    if (aut == null || aut[0].length() == 0 || aut[1].length() == 0) {
      model.select(0);
      return;
    }
    suID = aut[0]; suPW = aut[1];
     if (!uList.isSuperuser(suPW, suID)) {
      jfx.warning("You must be Superuser to open this Tab");
      model.select(0);
      return;
    }
    if (model.getSelectedIndex() == 1) UserController.superuser(aut[0], aut[1]);
  }
  /**
  setParm - load JODB server properties
  @param config String, JODB config file name
  */
  public void setParm(String config) throws Exception {
    ServerController.loadParm(config, ODBController, this);
  }
  /**
  setParm - load JODB server properties
  @param odbService ODBService instance
  */
  public void setParm(ODBService odbService) throws Exception {
    uList = odbService.getUserList();
    UserController.loadParm(odbService);
  }
  //
  protected static JFXOptions jfx;
  protected static String suPW, suID;
  protected ODBService odbService;
  //
  private UserList uList;
  private SingleSelectionModel<Tab> model;
}
