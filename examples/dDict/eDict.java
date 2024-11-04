import java.util.*;
import java.net.URL;
import java.io.*;
// JFX
import javafx.application.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.text.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.effect.*;
import javafx.scene.image.*;
import javafx.scene.media.*;
import javafx.geometry.*;
import javafx.event.*;
import javafx.beans.*;
import javafx.beans.value.*;
import javafx.collections.*;
import javafx.concurrent.*;
//
import joeapp.odb.*;
// @author Joe T. Schwarz (c)
public class eDict extends Application implements ODBEventListening {
  public void stop( ) {
    exit();
  }
  public void start(Stage stage) {
    Application.Parameters params = getParameters();
    java.util.List<String> pl = params.getRaw();
    if (pl.size() != 4) {
      System.out.println("userID Password WebServerName WebServerPort");
      System.exit(0);
    }
    try {
      int port = Integer.parseInt(pl.get(3));
      dict = new eOpenDict(pl.get(0),pl.get(1), pl.get(2), port);
      host_port = pl.get(2)+":"+port;
      dict.getKeys();
    } catch (Exception ex) {
      System.out.println("OpenDict Exception:"+ex.toString());
      System.exit(0);
    }
    dict.register(this);
    lab = "Meaning in Vietnamese";
    style = "resources/style1.css";
    stage.setTitle("Personal eVocabularyBook (c)");
    java.net.URL url = getClass().getResource(style);
    if (url != null) css = url.toExternalForm();

    VBox vbox = new VBox(20); 
    vbox.setAlignment(Pos.CENTER);
    Scene scene = new Scene(vbox, 500, 560, Color.ANTIQUEWHITE);
    if (css != null) scene.getStylesheets().add(css);

    Text txt = new Text("Personal eVocabularyBook (c)");
    txt.setFill(Color.NAVY);
    txt.setBlendMode(BlendMode.COLOR_BURN);
    txt.setFont(Font.font(java.awt.Font.DIALOG_INPUT, FontWeight.THIN, 23));
    Reflection refl = new Reflection();
    refl.setFraction(1.0);
    txt.setEffect(refl);
  
    dict.getKeys();
    dic = FXCollections.observableArrayList(dict.words);
    lv = new ListView<String>(dic);   
    lv.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    lv.setOnMouseClicked(a-> chkItem( ));
    lv.setStyle("-fx-text-fill: black; -fx-font-family: Veranda;");
    StackPane left = new StackPane();
    left.setPrefWidth(130);
    left.getChildren().add(lv);

    txtArea = new TextArea();
    txtArea.textProperty().addListener(c->{
      txtArea.setScrollTop(Double.MAX_VALUE);
    });
    txtArea.setWrapText(true);
    txtArea.setEditable(false);
    txtArea.setPrefRowCount(20);
    txtArea.setPrefColumnCount(42);
    txtArea.setScrollTop(Double.MAX_VALUE);
    txtArea.setPrefSize(300, 200);

    HBox hta = new HBox(10);
    hta.setAlignment(Pos.CENTER);
    hta.getChildren().addAll(left, txtArea);   

    MenuButton menu = new MenuButton("eCHOICES");
    MenuItem ad = new MenuItem("AddWord");
    ad.setOnAction(e -> addWord( ));
    MenuItem up = new MenuItem("Update");
    up.setOnAction(e -> update( ));
    MenuItem de = new MenuItem("Delete");
    de.setOnAction(e -> delete( ));
    MenuItem msg = new MenuItem("SendMessage");
    msg.setOnAction(e -> {
      dict.sendMsg(getWord("Message to JODB "+host_port));
    });
    MenuItem lo = new MenuItem("Lock");
    lo.setOnAction(e -> {
      String word = getWord("Lock");
      if (word != null) {
        if (dict.lock(word)) {
          txtArea.setText(word+" is locked.\n");
        } else txtArea.setText(word+" is locked or it does not exist !");
      }
    });
    MenuItem is = new MenuItem("isLocked");
    is.setOnAction(e -> {
      String word = getWord("isLocked");
      if (word != null) {
        if (dict.isLocked(word)) {
          txtArea.setText(word+" is locked.\n");
        } else txtArea.setText(word+" is unlocked!");
      }
    });
    MenuItem un = new MenuItem("Unlock");
    un.setOnAction(e -> {
      String word = getWord("Unlock");
      if (word != null) {
        if (dict.unlock(word)) {
          txtArea.setText(word+" is unlocked.\n");
        } else txtArea.setText(word+" is locked by the other or it does not exist !");
      }
    });
    MenuItem re = new MenuItem("Refresh");
    re.setOnAction(e -> {
      dict.getKeys();
      if (dict.words != null && dict.words.size() > 0) dic.setAll(dict.words);
    });
    //
    menu.getItems().addAll(ad, up, de, lo, is, un, re, msg);
    menu.setPrefHeight(40);
    menu.setStyle(bShape);

    ComboBox<String> combo = new ComboBox<>();
    combo.getItems().addAll(lang);
    combo.setPrefHeight(40);
    combo.setPromptText("Your Language");
    combo.setValue("Vietnamese");
    combo.setOnAction(ev -> {
      int idx = combo.getSelectionModel().getSelectedIndex();
      dict.setODB(idx);
      if (idx == 0) lab = "Meaning in Vietnamese";
      else if (idx == 1) lab = "Meaning in Japanese";
      else lab = "Meaning in Chinese";
      txtArea.clear();
      dic.clear();
      dict.getKeys();
      if (dict.words != null && dict.words.size() > 0) dic.setAll(dict.words);
    });
    
    Image find = new Image(getClass().getResourceAsStream("icons/find.png"));
    Button search = new Button("SEARCH", new ImageView(find));
    search.setOnAction(a -> searchIt());
    search.setStyle(bShape);

    Image sv = new Image(getClass().getResourceAsStream("icons/save.png"));
    Button save = new Button("SAVE", new ImageView(sv));
    save.setOnAction(a -> { dict.save(); });
    save.setPrefWidth(100);
    save.setStyle(bShape);

    HBox hbox = new HBox(); 
    hbox.setSpacing(10);
    hbox.setAlignment(Pos.CENTER);
    hbox.getChildren().addAll(search, combo, menu, save);

    vbox.getChildren().addAll(txt, hta, hbox);

    stage.setOnCloseRequest(e -> exit());
    stage.setResizable(false);
    stage.setScene(scene);
    stage.show();
    This = this;
  }
  // implement the ODBEvent-----------------------------------------------------
  // only check for:
  // 0: node is down
  // 2: node is ready.
  // 4: forcedFreeKey, forcedRollabck, etc.
  // 6: addNode (handled by type 2)
  // 7: removeNode (ODBService)
  // 9: forcedClose
  // 10: node joins cluster
  // 11: user add/delete/update (only if ODBConnect.notify(dbName) is set)
  public void odbEvent(ODBEvent event) {
    Platform.runLater(() -> {
      String node = event.getActiveNode();
      int type = event.getEventType();
      String msg = event.getMessage();
      if (type == 0) {
        if (host_port.equals(node)) {
          Alert alert = new Alert(AlertType.CONFIRMATION);
          addCSS(alert);
          if (type == 0) alert.setTitle("SEVERE PROBLEM @"+host_port);
          else alert.setTitle("SEVERE PROBLEM @"+host_port+". "+dict.odb+" was closed.");
          alert.setHeaderText("FAILSAFE: Switch to the NEXT Server Node, OK?");
          alert.setContentText("Your Choice");           
          Optional<ButtonType> result = alert.showAndWait();
          if (result.get() != ButtonType.OK) exit();
          if (!dict.switchNode(event)) {
            alert = new Alert(AlertType.ERROR);
            addCSS(alert);
            alert.setTitle("ALERT");
            alert.setHeaderText("This is an EMERGENCY Message");
            alert.setContentText("Unable to start any backup Server!");           
            alert.showAndWait();
            exit();
          }
          // must be This
          dict.register(This);
          host_port = dict.getActiveNode();
          if (type != 0) txtArea.setText("On "+node+": "+msg+" was forced to close.\n");
          txtArea.appendText("Switch to "+host_port+"\nKeyList AutoRefresh.\n");
        }
        refresh(node+" is down.");
      } else if (type == 2) {
        refresh(msg+" is online.");
      } else if (type == 4) { // forcedFreeKey/, etc.
        txtArea.setText("On local "+node+": "+msg+"\n");
      } else if (type == 7 && !host_port.equals(node)) { // addNode or removeNode
        refresh(node+" is removed from cluster.");
      } else if (type == 9 && msg.equals(dict.odb)) { // forcedClose
        refresh(msg+" on "+node+" is forced to close by Superuser.");
      } else if (type == 10) { // addNode/joinNode
        refresh(msg+" joins Cluster.");
      } else if (type == 11) { // user add/delete/update
        int p = node.indexOf("|"); // uID|dbName
        if (p < 0) return;
        if (!dict.getUserID().equals(node.substring(0, p)) && dict.odb.equals(node.substring(p+1))) {
          refresh(msg);
        }
      }
    });
  }
  //
  private void refresh(String msg) {
    dict.getKeys();
    if (dict.words.size() > 0) {
      dic.setAll(dict.words);
      txtArea.setText(msg+"\nKeyList AutoUpdate.\n");
    } else {
      dic.clear();
      txtArea.setText(msg+"\nKeyList is empty.\n");
    }
  }
  //
  private void addCSS(Dialog<?> dialog) {
    DialogPane diaPane = dialog.getDialogPane();
    java.net.URL url = this.getClass().getResource(style);
    if (url != null) {
      diaPane.getStylesheets().add(url.toExternalForm());
      diaPane.getStyleClass().add("popup");
    }
  }
  // ---------------------------------------------------------------------------
  private void addWord( ) {
    String[] s = eDialog.dialog("AddWord", lab, null, null, true);
    if (s == null) return;
    txtArea.setText(s[0]+":\n"+s[1]);
    dict.add(s[0], s[1]);
    dict.getKeys();
    dic.setAll(dict.words);
  }
  private void update( ) {
    String key = getWord("Update word");
    if (key == null) return;
    String m = dict.read(key);
    if (m == null) {
      txtArea.setText(key+" is UNKNOWN or LOCKED");
      return;
    }
    if (dict.isLocked(key)) {
      txtArea.setText(key+" is LOCKED");
      return;
    }
    String[] s = eDialog.dialog("Update", lab, key, m, false);
    if (s == null) return;
    if (dict.update(key, s[1])) {
      dict.getKeys();
      dic.setAll(dict.words);
      txtArea.setText(key+" is updated.\n"+s[1]);
    } else txtArea.setText("Unable to update "+key);
  }
  private void delete( ) {
    String key = getWord("Delete word");
    if (key != null) {
      if (dict.isLocked(key)) {
        txtArea.setText(key+" is LOCKED");
        return;
      }
      if (dict.delete(key)) {
        txtArea.setText(key+" is deleted.\n");
        dict.getKeys();
        dic.setAll(dict.words);
      } else txtArea.setText(key+" is locked or does not exist !");
    }
  }
  // LowerCase ONLY
  private String getWord(String hdr) {
    TextInputDialog dialog = new TextInputDialog( );
    dialog.setTitle("Joe's eDict");
    dialog.setHeaderText(hdr);
    dialog.setContentText(hdr);
    Optional<String> inp = dialog.showAndWait();
    if (inp.isPresent()) {
      String s = inp.get().trim();
      if (s.length() > 0) return s;
    }
    return null;
  }
  private void searchIt() {
    String word = getWord("Search");
    if (word != null) {
      String m = dict.search(word);
      if (m != null) txtArea.setText(word+":\n"+m);
      else txtArea.setText(word+" does not exist !");
    }
  }
  public void chkItem( ) {
    try {
      String word = lv.getSelectionModel().getSelectedItem();
      String m = dict.read(word);
      if (m == null) return;
      txtArea.setText(m);
    } catch (Exception ex) { }
  }
  private void exit() {
    dict.save();
    dict.exit();
    Platform.exit();
    System.exit(0);
  }
  //
  private eDict This;
  private eOpenDict dict;
  private TextField trans;
  private TextArea txtArea;
  private ListView<String> lv;
  private ObservableList<String> dic;
  private String css, style, host_port, lab;
  private String[] lang = { "Vietnamese", "Japanese", "Chinese" };
  private String bShape = "-fx-background-color: linear-gradient(#f2f2f2, #d6d6d6),"+
        "linear-gradient(#fcfcfc 0%, #d9d9d9 50%, #d6d6d6 100%),"+
        "linear-gradient(#dddddd 0%, #f6f6f6 50%);\n"+
        "-fx-background-radius: 8,7,6;\n"+
        "-fx-background-insets: 0,1,2;\n"+
        "-fx-font-size: 14;\n"+
        "-fx-font-weight: bold;\n"+
        "-fx-effect: dropshadow(three-pass-box , "+
        "rgba(0,0,0,0.6) , 5, 0.0 , 0 , 1 );\n";
}
