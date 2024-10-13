package joeapp.odb;
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
/**
@author Joe T. Schwarz (C)
JFXOptions is the customized pop-up JavaFX dialog for input with/without CSS file.
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
  input
  <br>Click OK to terminate the input process, or X-CLOSE to escape.
  @param css customized CSS file name or null for JFX defaults
  @param header String
  @return input String or null if X-CLOSE
  */
  public static String input(String css, String header) {
    Dialog<ButtonType> dia = new Dialog<ButtonType>();
    dia.setTitle("JFXOptions");
    dia.setHeaderText(header);
    dia.setResizable(true);

    // Set up dialog pane
    VBox box = new VBox();
    box.setPadding(new Insets(5, 5, 0, 5));
    TextField txt = new TextField( );
    txt.setPrefWidth(250);
    box.getChildren().add(txt);
    //
    DialogPane diaPane = dia.getDialogPane();    
    diaPane.getButtonTypes().addAll(ButtonType.OK);
    diaPane.setContent(box);
    // 
    Platform.runLater(() -> txt.requestFocus());
    if (css != null && (new File(css)).exists()) {
      diaPane.getStylesheets().add(css);
      diaPane.getStyleClass().add("css");
    }
    Optional<ButtonType> B = dia.showAndWait();
    try {
      ButtonType but = B.get();
      if (but == ButtonType.OK) return txt.getText().trim();
    } catch (Exception ex) { }
    return null;
  }
  /**
  password
  <br>Click OK to terminate the input process, or X-CLOSE to escape.
  @param css customized CSS file name or null for JFX defaults
  @param header String
  @return Password String or null if X-CLOSE
  */
  public static String password(String css, String header) {
    Dialog<ButtonType> dia = new Dialog<ButtonType>();
    dia.setTitle("JFXOptions");
    dia.setHeaderText(header);
    dia.setResizable(true);

    VBox box = new VBox(3);
    // Insets(top, right, bottom, left)
    box.setPadding(new Insets(5, 5, 0, 5));
    // Set up dialog pane
    DialogPane diaPane = dia.getDialogPane();    
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
    if (css != null && (new File(css)).exists()) {
      diaPane.getStylesheets().add(css);
      diaPane.getStyleClass().add("css");
    }
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
  /**
  login
  @param css customized CSS file name or null for JFX defaults
  @param header String
  @return String array contains: string[0]: user ID, string[1]: Password, or null (X-CLOSE)
  */
  public static String[] login(String css, String header) {
    Dialog<ButtonType> dia = new Dialog<ButtonType>();
    dia.setTitle("JFXOptions");
    dia.setHeaderText(header);
    dia.setResizable(true);

    VBox box = new VBox(3);
    // Insets(top, right, bottom, left)
    box.setPadding(new Insets(5, 5, 0, 5));
    // Set up dialog pane
    DialogPane diaPane = dia.getDialogPane();    
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
    if (css != null && (new File(css)).exists()) {
      diaPane.getStylesheets().add(css);
      diaPane.getStyleClass().add("css");
    }
    Optional<ButtonType> B = dia.showAndWait();
    try {
      ButtonType but = B.get();
      if (but == ButtonType.OK) {
        String[] ret = new String[2];
        ret[0] = id.getText();
        if (cb.isSelected()) ret[1] = txt.getText().trim();
        else ret[1] = pw.getText().trim();
        return ret;
      }
    } catch (Exception ex) { }
    return null;
  }
  /**
  multipleInputs with Labels. The number of input fields is equal the number of the given lab array
  <br>Click OK to terminate the input process, X-CLOSE to escape.
  @param css customized CSS file name or null for JFX defaults
  @param lab String array for the input labels
  @param header String
  @return Array of input Strings, null if closed
  */
  public static String[] multipleInputs(String css, String[] lab, String header) {
    return multipleInputs(css, lab, new Object[lab.length], header);
  }
  /**
  multipleInputs with Labels and Defaults. The number of input fields is equal the number of the given lab array
  <br>Click OK to terminate the input process, X-CLOSE to escape.
  @param css customized CSS file name or null for JFX defaults
  @param lab String array for the input labels
  @param obj default object array (element as a string or a string array)
  @param header String
  @return Array of input Strings, null if closed
  */
  public static String[] multipleInputs(String css, String[] lab, Object[] obj, String header) {
    Dialog<ButtonType> dia = new Dialog<ButtonType>();
    //
    dia.setTitle("JFXOptions");
    dia.setHeaderText(header);
    dia.setResizable(true);
    // set the min. width for this dialog
    ((Stage)dia.getDialogPane().getScene().getWindow()).setMinWidth(250);
    // Grid of components
    GridPane grid = new GridPane();
    grid.setHgap(5);
    grid.setVgap(2);
    // Set up dialog pane
    DialogPane diaPane = dia.getDialogPane();    
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
    if (css != null && (new File(css)).exists()) {
      diaPane.getStylesheets().add(css);
      diaPane.getStyleClass().add("css");
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
  dynamicInputs. Click OK to terminate the input process, X-CLOSE to escape. ENTER for the next input.
  @param css customized CSS file name or null for JFX defaults
  @param header String
  @return List of input Strings, null if closed
  */
  public static List<String> dynamicInputs(String css, String header) {
    Dialog<ButtonType> dia = new Dialog<ButtonType>();
    //
    dia.setTitle("JFXOptions");
    dia.setHeaderText(header);
    dia.setResizable(true);
    // set the min. width for this dialog
    ((Stage)dia.getDialogPane().getScene().getWindow()).setMinWidth(250);
    // Spacing 1, Insets(top, right, bottom, left)
    VBox box = new VBox(2);
    box.setPadding(new Insets(5, 5, 0, 5));
    // Set up dialog pane
    DialogPane diaPane = dia.getDialogPane();    
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
    if (css != null && (new File(css)).exists()) {
      diaPane.getStylesheets().add(css);
      diaPane.getStyleClass().add("css");
    }
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
}