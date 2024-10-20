import java.io.*;
import java.net.URL;
import java.util.*;

import java.util.jar.*;
import java.awt.event.*;
import java.util.zip.ZipEntry;
//
import javafx.application.Application;
import javafx.application.Platform;

import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;

import javafx.scene.*;
import javafx.scene.text.*;
import javafx.scene.paint.Color;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
// @author Joe T. Schwarz (c)
public class eDialog {
  public eDialog(eParms edict, String title, final boolean enable) {
    BorderPane mom = new BorderPane();
    Dialog<Integer> dia = new Dialog<Integer>();

    dia.setTitle(title+" Dialog");
    dia.setHeaderText("Joe's Customized "+title+" Dialog");
    ButtonType okType = new ButtonType("OK", ButtonData.OK_DONE);
    dia.getDialogPane().getButtonTypes().add(okType);

    Label lx = new Label("English Word");
    final TextArea tw = new TextArea();
    tw.setText(edict.word);
    tw.setPrefWidth(200);
    tw.setWrapText(true);
    tw.setPrefRowCount(2);
    tw.setPrefColumnCount(20);
    tw.setScrollTop(Double.MAX_VALUE);
    tw.setStyle(style);
    VBox tVb = new VBox();
    // insets(top-right-bottom-left)
    tVb.setSpacing(5);
    tVb.setPadding(new Insets(10, 55, 5, 60));
    tVb.getChildren().addAll(lx, tw);  

    Label ly = new Label("Meaning in Vietnamese");
    final TextArea ta = new TextArea();
    ta.setText(edict.mean);
    ta.setPrefWidth(400);
    ta.setWrapText(true);
    ta.setPrefRowCount(20);
    ta.setPrefColumnCount(42);
    ta.setScrollTop(Double.MAX_VALUE);
    // gainsboro background
    ta.setStyle(style);
    VBox bVb = new VBox(5);
    bVb.getChildren().addAll(ly, ta);

    mom.setTop(tVb);
    mom.setCenter(bVb);
    // blanchedalmond Background
    mom.setStyle("-fx-background-color: #ffebcd;");

    Node OK = dia.getDialogPane().lookupButton(okType);
    Platform.runLater(() -> {
      if (enable) tw.requestFocus();
      else {
        tw.setEditable(false);
        ta.requestFocus();
      }
    });
    dia.getDialogPane().setContent(mom);

    dia.setResultConverter(but -> {
      String w = tw.getText();
      String m = ta.getText();
      if (w != null && m != null) {
        edict.word = w.trim().toLowerCase();
        edict.mean = m.trim().toLowerCase();
        return 1;
      }
      return 0;
    });
    Optional<Integer> rep = dia.showAndWait();
  }
  private String style = "-fx-text-fill: black;-fx-background-color: #dcdcdc;"+
             "-fx-font-family: Veranda;";
}
