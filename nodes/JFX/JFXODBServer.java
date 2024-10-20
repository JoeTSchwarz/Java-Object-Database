import java.io.*;
import java.util.*;
//
import javafx.application.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.stage.Stage;
import javafx.scene.*;
//
import joeapp.odb.*;
// @author Joe T. Schwarz (C)
/**
JODB server in JFX
@author Joe T. Schwarz (C)
*/
public class JFXODBServer extends Application {
  /**
  init implement
  */
  public void init() {
    Application.Parameters params = getParameters();
    java.util.List<String> parm = params.getRaw();
    if (parm.size() < 1) {
      System.out.println("Missing Config File.");
      System.exit(0);
    }
    config = parm.get(0);
    if (parm.size() > 1) css = parm.get(1);
    else css = "resources/joe.css";
    try {
      prop = ODBParser.odbProperty(config);
      uList = new UserList(prop.get("USERLIST"));
    } catch (Exception ex) {
      if (prop == null) System.out.println("can't parse "+config);
      else System.err.println("Unable to access UserList.");
      System.exit(0);
    }   
  }
  /**
  start implement
  @param stage Stage
  */
  public void start(Stage stage) throws Exception {
    stage.setTitle("JavaFX -MVC- ODBServer (c)");
    stage.getIcons().add(new Image(getClass().getResourceAsStream("icons/bee.gif")));
    FXMLLoader fxml = new FXMLLoader(getClass().getResource("resources/jfxServer.fxml"));
    stage.setScene(new Scene(fxml.load()));
    // do NOTHING on clicking at X-button
    // in case that ODBServer isn't ready
    stage.setOnCloseRequest(e -> {
      if (server == null && server.getODBService() == null) {
        Platform.exit();
        System.exit(0);
      } else e.consume();
    });
    stage.show();
    server = (JFXController)fxml.getController();
    server.setParm(prop, config, uList);
  }
  //
  private String config;
  private UserList uList;
  public static String css;
  private JFXController server;
  private HashMap<String, String> prop;
}

