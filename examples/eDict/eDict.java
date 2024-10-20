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
      ex.printStackTrace();
      System.exit(0);
    }
    This = this;
    dict.register(this);
    style = "style1.css";
    stage.setTitle("Personal eVocabularyBook (c)");
    java.net.URL url = getClass().getResource(style);
    if (url != null) css = url.toExternalForm();

    Group group = new Group( ); 
    Scene scene = new Scene(group, 480, 565, Color.ANTIQUEWHITE);
    //Scene scene = new Scene(group, 480, 565, Color.CORNSILK);
    if (css != null) scene.getStylesheets().add(css);
    ArrayList<String> keys = new ArrayList<String>();

    Text txt = new Text("Personal eVocabularyBook (c)");
    txt.setFill(Color.NAVY);
    txt.setBlendMode(BlendMode.COLOR_BURN);
    txt.setFont(Font.font(java.awt.Font.DIALOG_INPUT, FontWeight.THIN, 23));
    Reflection refl = new Reflection();
    refl.setFraction(1.0);
    txt.setEffect(refl);
    HBox htx = new HBox();
    // insets(top-right-bottom-left)
    htx.setPadding(new Insets(5, 100, 20, 50));
    htx.getChildren().addAll(txt);  
  
    dict.getKeys();
    dic = FXCollections.observableArrayList(dict.words);
    
    lv = new ListView<String>(dic);   
    lv.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    lv.setOnMouseClicked(a-> chkItem( ));
    lv.setStyle("-fx-text-fill: black; -fx-font-family: Veranda;");
    StackPane left = new StackPane();
    left.setPrefWidth(120);
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

    HBox hta = new HBox();
    // insets(top-right-bottom-left)
    hta.setPadding(new Insets(10, 10, 20, 25));
    hta.setSpacing(10);
    hta.getChildren().addAll(left, txtArea);   
    // MUCH better than ComboBox for RESELECTIONS
    // Image sel = new Image(getClass().getResourceAsStream("select.jpg"));
    // MenuButton combo = new MenuButton("eCHOICES", new ImageView(sel));
    MenuButton combo = new MenuButton("eCHOICES");
    MenuItem ad = new MenuItem("AddWord");
    ad.setOnAction(e -> addWord( ));
    MenuItem up = new MenuItem("Update");
    up.setOnAction(e -> update( ));
    MenuItem de = new MenuItem("Delete");
    de.setOnAction(e -> delete( ));
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
    combo.getItems().addAll(ad, up, de, lo, is, un, re);
    combo.setPrefHeight(40);
    combo.setStyle(bShape);

    Image find = new Image(getClass().getResourceAsStream("find.png"));
    Button search = new Button("SEARCH", new ImageView(find));
    search.setOnAction(a -> searchIt());
    search.setStyle(bShape);

    Image sv = new Image(getClass().getResourceAsStream("save.png"));
    Button save = new Button("SAVE", new ImageView(sv));
    save.setOnAction(a -> saveX());
    save.setPrefWidth(100);
    save.setStyle(bShape);

    hbox = new HBox(); 
    hbox.setStyle("-fx-background-color: #0000ff");
    // insets(top-right-bottom-left)
    hbox.setSpacing(25);
    hbox.setPadding(new Insets(17, 40, 20, 55));
    hbox.getChildren().addAll(search, combo, save);         

    BorderPane bp = new BorderPane();
    bp.setTop(htx);
    bp.setCenter(hta);
    bp.setBottom(hbox);

    group.getChildren( ).add(bp);
    stage.setOnCloseRequest(e -> exit());
    stage.setResizable(false);
    stage.setScene(scene);
    stage.show();
  }
  // implement the ODBEvent-----------------------------------------------------
  // only check for:
  // 0: node is down
  // 2: node is ready.
  // 4: forcedFreeKey, forcedRollabck, etc.
  // 6: addNode (handled by type 2)
  // 7: removeNode (ODBService)
  // 9: forcedClose
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
          else alert.setTitle("SEVERE PROBLEM @"+host_port+". "+dict.dict+" was closed.");
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
      } else if (type == 9 && msg.equals(dict.dict)) { // forcedClose
        refresh(msg+" on "+node+" is forced to close by Superuser.");
      }
    });
  }
  //
  private void refresh(String msg) {
    dict.getKeys();
    if (dict.words != null && dict.words.size() > 0) {
      dic.setAll(dict.words);
      txtArea.setText(msg+"\nKeyList AutoUpdate.\n");
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
    edict = new eParms();
    eDialog ed = new eDialog(edict, "AddWord", true);
    if (edict.word != null && edict.word.length() > 0 && edict.mean.length() > 0) {
      txtArea.setText(edict.word+":\n"+edict.mean);
      dict.keep(edict.word, edict.mean);
      dict.getKeys();
      if (dict.words != null && dict.words.size() > 0) dic.setAll(dict.words);
    }
  }
  private void update( ) {
    edict = new eParms();
    edict.word = getWord("Update");
    if (edict.word != null) {
      if (dict.isLocked(edict.word) && !dict.unlock(edict.word)) {
        txtArea.setText(edict.word+" is UNKNOWN or LOCKED");
        return;
      }
      edict.mean = dict.getMeaning(edict.word);
      eDialog ed = new eDialog(edict, "Update", false);
      if (edict.mean != null && edict.word.length() > 0 && edict.mean.length() > 0) {
        if (dict.update(edict.word, edict.mean)) {
          dict.getKeys();
          if (dict.words != null && dict.words.size() > 0) dic.setAll(dict.words);
          txtArea.setText(edict.word+" is updated.\n"+edict.mean);
        } else txtArea.setText("Unanble to update "+edict.word);
      }
    }
  }
  private void delete( ) {
    String word = getWord("Delete");
    if (word != null) {
      if (dict.isLocked(word) && !dict.unlock(word)) {
        txtArea.setText(word+" is UNKNOWN or LOCKED");
        return;
      }
      if (dict.delete(word)) {
        dic.remove(word);
        txtArea.setText(word+" is deleted.\n");
        dict.getKeys();
        if (dict.words != null && dict.words.size() > 0) dic.setAll(dict.words);
      } else txtArea.setText(word+" is locked or does not exist !");
    }
  }
  // LowerCase ONLY
  private String getWord(String hdr) {
    TextInputDialog dialog = new TextInputDialog( );
    dialog.setTitle("Joe's eDict");
    dialog.setHeaderText(hdr);
    dialog.setContentText(hdr+" Word");
    Optional<String> inp = dialog.showAndWait();
    if (inp.isPresent()) {
      String s = inp.get().trim().toLowerCase();
      if (s.length() > 0) return s;
    }
    return null;
  }
  private void searchIt() {
    String word = getWord("Search");
    if (word != null) {
      String res = dict.search(word);
      if (res != null) {
        txtArea.setText(word+":\n"+res);
      } else txtArea.setText(word+" does not exist !");
    }
  }
  private void saveX() {
    dict.save();
  }
  public void chkItem( ) {
    try {
      String word = lv.getSelectionModel().getSelectedItem().toLowerCase();
      txtArea.setText(dict.getMeaning(word));
    } catch (Exception ex) { }
  }
  private void exit() {
    dict.exit();
    Platform.exit();
    System.exit(0);
  }
  private byte[] rb;
  private HBox hbox;
  private eDict This;
  private eParms edict;
  private eOpenDict dict;
  private TextField trans;
  private TextArea txtArea;
  private ListView<String> lv;
  private ArrayList<String> keys;
  private String css, style, host_port;
  private ObservableList<String> dic, cob;
  // Google's Root or this Root http://www.oxfordlearnersdictionaries.com...
  private String ggl = "http://ssl.gstatic.com/dictionary/static/sounds/de/0/";
  private String url = "http://www.oxfordlearnersdictionaries.com/media/english/us_pron/";
  private String bShape = "-fx-background-color: linear-gradient(#f2f2f2, #d6d6d6),"+
        "linear-gradient(#fcfcfc 0%, #d9d9d9 50%, #d6d6d6 100%),"+
        "linear-gradient(#dddddd 0%, #f6f6f6 50%);\n"+
        "-fx-background-radius: 8,7,6;\n"+
        "-fx-background-insets: 0,1,2;\n"+
        "-fx-font-size: 14;\n"+
        "-fx-text-fill: blue;\n"+
        "-fx-font-weight: bold;\n"+
        "-fx-effect: dropshadow(three-pass-box , "+
        "rgba(0,0,0,0.6) , 5, 0.0 , 0 , 1 );\n";
}
