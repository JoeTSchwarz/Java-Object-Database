// Demo Example in Joe's SWING_MVC
import javax.swing.*;
import joeapp.mvc.SWINGLoader;
// @author Joe T. Schwarz (c)
//The ThePeople in SWING_MVC View
public class ThePeople {
  public ThePeople(String... argv) throws Exception {
    SWINGLoader ml = new SWINGLoader("resources/People.txt", 
                                     "PeopleController",
                                     argv);
    ((JFrame) ml.load()).setVisible(true);
  }
  public static void main(String... a) throws Exception {
    UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
    String[] argv = a;
    if (a.length == 0) argv = new String[] { "localhost:9999" };
    else argv[0] = "localhost:"+argv[0];
    //  
    new ThePeople(argv);
  }
}

