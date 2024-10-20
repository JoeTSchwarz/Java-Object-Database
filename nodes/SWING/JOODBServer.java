import java.awt.*;
import javax.swing.*;

import joeapp.mvc.*;
/**
JOODBServer in MVC-SWING manner
@author Joe T. Schwarz (C)
*/
public class JOODBServer {
  /**
  Constructor
  @param config String, Config file
  */
  public JOODBServer(String config) throws Exception {
    Toolkit tKit = Toolkit.getDefaultToolkit();
    Image img = tKit.createImage(ClassLoader.getSystemResource("icons/oodb.jpg"));
    SWINGLoader ml = new SWINGLoader("resources/_joodbServer_.txt",
                                     "ServerController", 
                                     new String[] {config}
                                    );
    JFrame jframe = (JFrame) ml.load();
    jframe.getFrames()[0].setIconImage(img);
    jframe.setTitle("SWING ODBServer (C)");
    jframe.setVisible(true);
  }
  private static String[] parms;
  /**
  @param a Parameter String array
  @exception Exception thrown by JAVA
  */
  public static void main(String... a) throws Exception {
    UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
    new JOODBServer(a.length > 0? a[0]:"odbConfig_Node1.txt");
  }
}