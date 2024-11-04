//
import javafx.application.Platform;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.geometry.Insets;
// @author Joe T. Schwarz (c)
public class eDialog {
  public static String[] dialog(String title, String word, String meaning, boolean enable) {
    Dialog<ButtonType> dia = new Dialog<>();
    DialogPane dp = dia.getDialogPane();

    dia.setTitle(title+" Dialog");
    dia.setHeaderText("Joe's Customized "+title+" Dialog");
    dp.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    Label lx = new Label("English Word");
    TextArea tw = new TextArea();
    if (word != null) tw.setText(word);
    tw.setPrefWidth(200);
    tw.setWrapText(true);
    tw.setPrefRowCount(2);
    tw.setPrefColumnCount(20);
    tw.setScrollTop(Double.MAX_VALUE);
    tw.setStyle("-fx-text-fill: black;-fx-background-color: #dcdcdc; -fx-font-family: Veranda;");
    tw.setEditable(enable);
    VBox tVb = new VBox();
    // insets(top-right-bottom-left)
    tVb.setSpacing(5);
    tVb.setPadding(new Insets(10, 55, 5, 60));
    tVb.getChildren().addAll(lx, tw);  

    TextArea ta = new TextArea();
    if (meaning != null) ta.setText(meaning);
    ta.setPrefWidth(400);
    ta.setWrapText(true);
    ta.setPrefRowCount(20);
    ta.setPrefColumnCount(42);
    ta.setScrollTop(Double.MAX_VALUE);
    // gainsboro background
    ta.setStyle("-fx-text-fill: black;-fx-background-color: #dcdcdc; -fx-font-family: Veranda;");
    VBox vbox = new VBox(5);
    vbox.getChildren().addAll(tVb, new Label("Meaning in Vietnamese"), ta);

    dp.setContent(vbox);
    // blanchedalmond Background
    dp.setStyle("-fx-background-color: #ffebcd;");

    Platform.runLater(() -> {
      if (enable) tw.requestFocus();
      else ta.requestFocus();
    });
    if ((dia.showAndWait()).get() == ButtonType.OK) {
      String w = tw.getText().trim();
      String m = ta.getText().trim();
      if (m != null && w != null && m.length() > 0 && w.length() > 0) {
        return new String[] { w, m }; // word, meaning
      }
    }
    return null;
  }
}
