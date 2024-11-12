package joeapp.odb;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Alert.*;
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
import java.util.*;
import java.io.File;
//
import joeapp.odb.*;
/**
@author Joe T. Schwarz (C)
JFXOptions is the customized pop-up JavaFX dialog for input with/without customized CSS file.
<br>Note: if the JFX app is customized by a CSS file and this file contains a part for the JFXOptions
<br>pop-ups then the JFX components must be referenced and preceded by .css (in lower case)
<br>Example:
<br>.css .button {
<br>    -fx-background-color:
<br>        linear-gradient(#f0ff35, #a9ff00),
<br>        radial-gradient(center 50% -40%, radius 200%, #b8ee36 45%, #80c800 50%);
<br>    -fx-background-radius: 6, 5;
<br>    -fx-background-insets: 0, 1;
<br>    -fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.4) , 5, 0.0 , 0 , 1 );
<br>    -fx-text-fill: #395306;
<br>    -fx-font-size: 9pt;
<br>}
<br>.css .button:hover {
<br>  -fx-background-color: bisque;
<br>}
*/
public class JFXOptions {
  /**
  Constructor without css
  @param node main Node where the Dialog is relatively positioned
  */
  public JFXOptions(Node node) {
    this.node = node;
  }
  /**
  Constructor with CSS
  @param node main Node where the Dialog is relatively positioned
  @param css String, css file name of format: URL.toExternalForm()
  */
  public JFXOptions(Node node, String css) {
    this.node = node;
    this.css = css;
  }
  /**
  info
  @param info String, information text
  @param sel String, selection text
  @return boolean true if YES is selected
  */
  public boolean info(String info, String sel) {
    Dialog<ButtonType> dia = new Dialog<>();
    dia.setContentText(sel);
    DialogPane dp = addCSS(info, dia);
    ButtonType OK = new ButtonType("YES");
    ButtonType NO = new ButtonType("NO");
    dp.getButtonTypes().addAll(OK, NO);      
    Optional<ButtonType> but = dia.showAndWait();
    return but.get() == OK;
  }
  /**
  input
  <br>Click OK to terminate the input process, X-CLOSE to escape.
  @param header String
  @return input String or null if X-CLOSE
  */
  public String input(String header) {
    Dialog<ButtonType> dia = new Dialog<ButtonType>();
    // Set up dialog pane
    VBox box = new VBox();
    box.setPadding(new Insets(5, 5, 0, 5));
    TextField txt = new TextField( );
    txt.setPrefWidth(250);
    box.getChildren().add(txt);
    //
    DialogPane diaPane = addCSS(header, dia);   
    diaPane.getButtonTypes().addAll(ButtonType.OK);
    diaPane.setContent(box);
    // 
    Platform.runLater(() -> txt.requestFocus());
    Optional<ButtonType> B = dia.showAndWait();
    try {
      ButtonType but = B.get();
      if (but == ButtonType.OK) return txt.getText().trim();
    } catch (Exception ex) { }
    return null;
  }
  /**
  choice from a List of String
  <br>Click OK to terminate the input process, X-CLOSE to escape.
  @param header String
  @param pText String, prompt Text
  @param list List of String
  @return String from Combobox or null if X-CLOSE
  */
  public String choice(String header, String pText, List<String> list) {
    Dialog<ButtonType> dia = new Dialog<ButtonType>();
    // Set up dialog pane
    VBox box = new VBox();
    box.setPadding(new Insets(5, 5, 0, 5));
    ComboBox<String> combo = new ComboBox<>( );
    combo.setItems(FXCollections.observableArrayList(list));
    combo.setPromptText(pText);
    //combo.setValue(list.get(0));
    combo.setPrefWidth(250);
    box.getChildren().add(combo);
    //
    DialogPane diaPane = addCSS(header, dia);   
    diaPane.getButtonTypes().addAll(ButtonType.OK);
    diaPane.setContent(box);
    // 
    Platform.runLater(() -> combo.requestFocus());
    Optional<ButtonType> B = dia.showAndWait();
    try {
      ButtonType but = B.get();
      if (but == ButtonType.OK) return combo.getValue();
    } catch (Exception ex) { }
    return null;
  }
  /**
  password
  <br>Click OK to terminate the input process, X-CLOSE to escape.
  @param header String
  @return Password String or null if X-CLOSE
  */
  public String password(String header) {
    Dialog<ButtonType> dia = new Dialog<ButtonType>();
    //
    VBox box = new VBox(3);
    // Insets(top, right, bottom, left)
    box.setPadding(new Insets(5, 5, 0, 5));
    // Set up dialog pane
    DialogPane diaPane = addCSS(header, dia);     
    diaPane.getButtonTypes().addAll(ButtonType.OK);
    diaPane.setContent(box);
    // 
    PasswordField pw = new PasswordField();
    TextField txt = new TextField( );
    txt.setPrefWidth(250);
    pw.setPrefWidth(250);
    CheckBox cb = new CheckBox("Show");
    cb.setOnAction(e -> {
      if (cb.isSelected()) {
        box.getChildren().remove(pw);
        box.getChildren().add(txt);
        txt.setText(pw.getText());
        txt.requestFocus();
      } else {
        box.getChildren().remove(txt);
        box.getChildren().add(pw);
        pw.setText(txt.getText());
        pw.requestFocus();
      }
    });
    Platform.runLater(() -> pw.requestFocus());
    box.getChildren().addAll(cb, pw);
    Optional<ButtonType> B = dia.showAndWait();
    try {
      ButtonType but = B.get();
      if (but == ButtonType.OK) {
        if (cb.isSelected()) return txt.getText().trim();
        return pw.getText().trim();
      }
    } catch (Exception ex) { }
    return null;
  }
  // return ret[0]: userID, ret[1]: password
  public String[] login(String header) {
    Dialog<ButtonType> dia = new Dialog<ButtonType>();
    //
    VBox box = new VBox(3);
    // Insets(top, right, bottom, left)
    box.setPadding(new Insets(5, 5, 0, 5));
    // Set up dialog pane
    DialogPane diaPane = addCSS(header, dia);   
    diaPane.getButtonTypes().addAll(ButtonType.OK);
    diaPane.setContent(box);
    // 
    TextField id = new TextField( );
    id.setPrefWidth(250);
    //
    PasswordField pw = new PasswordField();
    TextField txt = new TextField( );
    txt.setPrefWidth(250);
    pw.setPrefWidth(250);
    //
    CheckBox cb = new CheckBox("Show");
    cb.setOnAction(e -> {
      if (cb.isSelected()) {
        box.getChildren().remove(pw);
        box.getChildren().add(txt);
        txt.setText(pw.getText());
        txt.requestFocus();
      } else {
        box.getChildren().remove(txt);
        box.getChildren().add(pw);
        pw.setText(txt.getText());
        pw.requestFocus();
      }
    });
    Platform.runLater(() -> id.requestFocus());
    box.getChildren().addAll(id, cb, pw);
    Optional<ButtonType> B = dia.showAndWait();
    String[] ret = { "", "" };
    try {
      ButtonType but = B.get();
      if (but == ButtonType.OK) {
        ret[0] = id.getText().trim();
        if (cb.isSelected()) ret[1] = txt.getText().trim();
        else ret[1] = pw.getText().trim();
      }
    } catch (Exception ex) { }
    return ret;
  }
  /**
  multipleInputs with Labels. The number of input fields is equal the number of the given lab array
  <br>Click OK to terminate the input process, X-CLOSE to escape.
  @param lab String array for the input labels
  @param header String
  @return Array of input Strings, null if closed
  */
  public String[] multipleInputs(String[] lab, String header) {
    return multipleInputs(lab, new Object[lab.length], header);
  }
  /**
  multipleInputs with Labels and Defaults. The number of input fields is equal the number of the given lab array
  <br>Click OK to terminate the input process, X-CLOSE to escape.
  @param lab String array for the input labels
  @param obj default object array (element as a string or a string array)
  @param header String
  @return Array of input Strings, null if closed
  */
  public String[] multipleInputs(String[] lab, Object[] obj, String header) {
    Dialog<ButtonType> dia = new Dialog<ButtonType>();
    // set the min. width for this dialog
    ((Stage)dia.getDialogPane().getScene().getWindow()).setMinWidth(250);
    // Grid of components
    GridPane grid = new GridPane();
    grid.setHgap(5);
    grid.setVgap(2);
    // Set up dialog pane
    DialogPane diaPane = addCSS(header, dia);     
    diaPane.getButtonTypes().addAll(ButtonType.OK);
    diaPane.setContent(grid);
    // load labels and defaults
    boolean focus = false;
    for (int i = 0; i < lab.length; ++i) {
      Label label = new Label(lab[i]);
      grid.add(label, 0, i);
      grid.setHalignment(label, HPos.RIGHT);
      if (obj[i] instanceof String[]) {
        String[] sa = (String[])obj[i];
        ComboBox<String> combo = new ComboBox<>( );
        combo.getItems().addAll(Arrays.asList(sa));
        grid.setHalignment(combo, HPos.LEFT);
        combo.setValue(sa[0]);
        grid.add(combo, 1, i);
      } else { // String or null
        TextField txt = new TextField( );
        txt.setPrefWidth(250);
        if (obj[i] != null) txt.setText((String)obj[i]);
        grid.setHalignment(txt, HPos.LEFT);
        grid.add(txt, 1, i);
        if (!focus) {
          Platform.runLater(() -> txt.requestFocus());
          focus = true;
        }
      }
    }
    Optional<ButtonType> B = dia.showAndWait();
    try {
      ButtonType but = B.get();
      if (but == ButtonType.OK) {
        ObservableList<Node> nodes = grid.getChildren();
        List<String> list = new ArrayList<>( );
        for (Node n:nodes) {
          if (n instanceof ComboBox) list.add((String)((ComboBox)n).getValue());            
          else if (n instanceof TextField) list.add(((TextField)n).getText().trim());
        }
        return list.toArray(new String[list.size()]);
      }
    } catch (Exception ex) { }
    return null;
  }
  /**
  dynamicInputs. Click OK to terminate the input process,. X-CLOSE to escape, ENTER for the next input.
  @param header String
  @return List of input Strings, null if closed
  */
  public List<String> dynamicInputs(String header) {
    Dialog<ButtonType> dia = new Dialog<ButtonType>();
    // set the min. width for this dialog
    ((Stage)dia.getDialogPane().getScene().getWindow()).setMinWidth(250);
    // Spacing 1, Insets(top, right, bottom, left)
    VBox box = new VBox(2);
    box.setPadding(new Insets(5, 5, 0, 5));
    // Set up dialog pane
    DialogPane diaPane = addCSS(header, dia);   
    diaPane.getButtonTypes().addAll(ButtonType.OK);
    diaPane.setContent(box);
    // Create the first field to start with
    TextField txt = new TextField( );
    txt.setPrefWidth(250);
    txt.setOnAction((e) -> {
      // check for cleared TextFields
      List<Node> lst = new ArrayList<>();
      ObservableList<Node> nodes = box.getChildren();
      for (Node n:nodes) if (((TextField)n).getText().trim().length() == 0) lst.add(n);
      if (lst.size() > 0) { // check for all deleted fields
        if (nodes.get(nodes.size()-1) == lst.get(0)) return;
        for (Node n:lst) box.getChildren().remove(n);
      }
      TextField tf = new TextField( );
      tf.setPrefWidth(250);
      box.getChildren().add(tf);
      tf.setOnAction(txt.getOnAction());
      diaPane.getScene().getWindow().sizeToScene();
      tf.requestFocus();
    });
    box.getChildren().add(txt);
    Platform.runLater(() -> txt.requestFocus());
    Optional<ButtonType> B = dia.showAndWait();
    try {
      ButtonType but = B.get();
      if (but == ButtonType.OK) {
        ObservableList<Node> nodes = box.getChildren();
        List<String> list = new ArrayList<String>();
        for (Node n : nodes) {
          String s = ((TextField)n).getText().trim();
          if (s.length() > 0) list.add(s);
        }
        return list;
      }
    } catch (Exception ex) { }
    return null;
  }
  /**
  @param msg String
  */
  public void warning(String msg) {
    Alert alert = new Alert(AlertType.WARNING);
    alert.setContentText(msg);
    addCSS("WARNING", alert); 
    alert.showAndWait();
  }
  /**
  @param msg String
  @return boolean true if OK is selected
  */
  public boolean confirm(String msg) {
    Alert alert = new Alert(AlertType.CONFIRMATION);
    addCSS("CONFIRM", alert);
    alert.setContentText(msg);
    Optional<ButtonType> result = alert.showAndWait();
    if (result.get() == ButtonType.OK) return true;
    return false;
  }
  /**
  @param uList UserList
  @return boolean true if supperuser
  */
  public boolean isSuperuser(UserList uList) {
    String suID = input("Superuser ID");
    if (suID != null && suID.length() > 0) {
      String suPW = password("Superuser Password");
      if(suPW != null && suPW.length() > 0) return uList.isSuperuser(suPW, suID);
    }
    return false;
  }
  /**
  focus on an object
  @param obj Object
  */
  public void focus(Object obj) {
    Platform.runLater(() -> {
      if (obj instanceof TextField) ((TextField)obj).requestFocus();
      else if (obj instanceof PasswordField) ((PasswordField)obj).requestFocus();
    });
  }
  //
  private DialogPane addCSS(String header, Dialog<?> dialog) {
    DialogPane diaPane = dialog.getDialogPane();
    if (css != null) {
      diaPane.getStylesheets().add(css);
      diaPane.getStyleClass().add("css");
    }
    Bounds pos = node.localToScreen(node.getBoundsInLocal());
    dialog.setX(pos.getMinX() + pos.getWidth() / 2);
    dialog.setY(pos.getMinY() + pos.getHeight() / 2);
    dialog.setTitle("JFXOptions");
    dialog.setHeaderText(header);
    dialog.setResizable(true);
    return diaPane;
  }
  private String css;
  private Node node;
}