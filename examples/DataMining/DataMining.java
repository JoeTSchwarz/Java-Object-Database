import java.io.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
//
import joeapp.odb.*;
// @author Joe T. Schwarz (c)
public class DataMining extends JFrame {
  public DataMining(String[] parms) throws Exception {
    // access the object
    JPanel lp = new JPanel();
    ta = new JTextArea(22, 55);
    ta.setWrapStyleWord(true);
    ta.setLineWrap(true);
    ta.addMouseListener(new CopyAndPaste(ta));
    //
    dbName = parms.length > 1? parms[1]:"members";
    setTitle("ODBMining with "+dbName);
    
    int port = parms.length > 0? Integer.parseInt(parms[0]):9999;
    mining = new ODBMining("localhost", port, "erika", "joe");
    mining.connect(dbName); // create if needed
    ArrayList<Object> members = mining.getKeys(dbName);
    for (int i = 0, sz = members.size(); i < sz; ++i)
    ta.append("Member_"+i+":"+(String)members.get(i)+"\n");

    JScrollPane jsp = new JScrollPane(ta); 
    lp.setBorder(BorderFactory.createTitledBorder("members-Report"));
    jsp.setAutoscrolls(true);
    lp.add(jsp);
    JLabel lab = new JLabel("Action");
    combo = new JComboBox<String>(("Member!selectAll(pattern)!selectAll(variable,"+
                                    "pattern)!selectAll(fieldName, mode, value)!"+
                                    "AccessTime").split("!"));
    combo.addActionListener(a->access());
    JPanel pa = new JPanel();
    pa.add(lab); pa.add(combo);
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent w){
        exit();
      }
    });
    add("Center", lp);
    add("South", pa);
    setMinimumSize(new Dimension(700, 500));
    setLocation(100, 100);
    setResizable(true);
    setVisible(true);
  }
  private void access() {
    try {
      int sel = combo.getSelectedIndex();
      String val, var, name;
      ArrayList<Object> obj;
      String d;
      long beg;
      OUT: switch (sel) {
      case 0: val = JOptionPane.showInputDialog(this, "Member:");
          if (val == null) return; else val = val.trim();
          ta.append("\nMember:\n"+mining.read(dbName, val));
          break;
      case 1: val = JOptionPane.showInputDialog(this, "Matching Pattern:");
          if (val == null) return; else val = val.trim();
          beg = System.currentTimeMillis();
          obj = mining.selectAll(dbName, val);
          d = String.format("%.6f",(double)(System.currentTimeMillis() - beg)/(sec*obj.size()));
          if (obj != null && obj.size() > 0) {
            int i = 1;
            for (Object o : obj) ta.append("\n"+(i++)+".\n"+((Member)o).toString());
            ta.append("\n\nselectAll(members, "+val+"): "+
            obj.size()+" objects are found.\nAccessTime/Object (Seconds): "+d);
          } else ta.append("\nselectAll(members, "+val+"): NOT FOUND!");
          break;
      case 2: var = JOptionPane.showInputDialog(this, "VariableName:");
          if (var == null) return; else var = var.trim();
          val = JOptionPane.showInputDialog(this, "Matching Pattern:");
          if (val == null) return; else val = val.trim();
          beg = System.currentTimeMillis();
          obj = mining.selectAll(dbName, var, val);
          d = String.format("%.6f",(double)(System.currentTimeMillis() - beg)/(sec*obj.size()));
          if (obj != null && obj.size() > 0) {
            int i = 1;
            for (Object o : obj) ta.append("\n"+(i++)+".\n"+((Member)o).toString());
            ta.append("\n\nselectAll(members, "+var+", "+val+
                      "): "+obj.size()+" objects are found.\nAccessTime/Object (Seconds): "+d);
          } else ta.append("\nselectAll(members, "+var+", "+val+"): NO object is found.");
          break;
      case 3: name = JOptionPane.showInputDialog(this, "VariableName:");
          if (name == null) return; else name = name.trim();
          var = JOptionPane.showInputDialog(this, "Mode (LT/LE/EQ/GE/GT):");
          if (var == null) return; else var = var.toUpperCase().trim();
          val = JOptionPane.showInputDialog(this, "Value:");
          if (val == null) return; else val = val.trim();
          int m = 0; // 0: int, 1:double
          for (int i = 0, l = val.length(); i < l; ++i) {
            if (val.charAt(i) != '.' && val.charAt(i) < '0' && val.charAt(i) > '9') {
              ta.append("\nInvalid:"+val);
                break OUT;
            } else if (val.charAt(i) == '.') {
              if (m == 1) { 
                ta.append("\nInvalid:"+val);
                break OUT;
              }
              m = 1;
            }
          }
          if (m == 0) {
            beg = System.currentTimeMillis();
            obj = mining.selectAll(dbName, name, var, Integer.parseInt(val));
            d = String.format("%.6f",(double)(System.currentTimeMillis() - beg)/(sec*obj.size()));
          } else {
            beg = System.currentTimeMillis();
            obj = mining.selectAll(dbName, name, var, Double.parseDouble(val));
            d = String.format("%.6f",(double)(System.currentTimeMillis() - beg)/(sec*obj.size()));
          }
          if (obj != null && obj.size() > 0) {
            int i = 1;
            for (Object o : obj) ta.append("\n"+(i++)+".\n"+((Member)o).toString());
            ta.append("\n\nselectAll(members ,"+name+", \""+var+"\", "+val+
            "): "+obj.size()+" objects are found.\nAccessTime/Object (Seconds): "+d);
          }  else ta.append("\n\nselectAll(members ,"+name+", \""+var+"\", "+val+"). No Object found");
          break;
      case 4: beg = System.currentTimeMillis();
          obj = mining.allObjects(dbName);
          d = String.format("%.6f",(double)(System.currentTimeMillis() - beg)/(sec*obj.size()));
          if (obj != null && obj.size() > 0) {
            ta.append("\n\nAvaraged AccessTime for "+obj.size()+" objects (Seconds):  "+d);
          } else ta.append("\n\nNO Object found. Avaraged AccessTime");
          break;
      }
      ta.setCaretPosition(ta.getDocument().getLength());
    } catch (Exception e) {
      //e.printStackTrace();
      JOptionPane.showMessageDialog(this, e.toString(), "Info", JOptionPane.PLAIN_MESSAGE);
    }
  }
  private void exit() {
    try {
      mining.disconnect();
    } catch (Exception e) { }
    System.exit(0);
  }
  private JTextArea ta;
  private int sec = 1000;
  private ODBMining mining;
  private JComboBox<String> combo;
  private String dbName;

  public static void main(String... args) throws Exception {
    UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
    new DataMining(args);
  }
}

