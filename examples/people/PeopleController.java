import java.io.*;
import java.awt.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import java.nio.file.*;
import javax.swing.text.*;
import javax.swing.event.*;
// Joe's stuffs
import joeapp.odb.*;
import joeapp.mvc.SWING;
import joeapp.mvc.SWINGLoader;
// @author Joe T. Schwarz (c)
/**
ThePeople in SWING_MVC
*/
public class PeopleController {
  public PeopleController(HashMap<String, Object> map, String[] args) {
    frame = (JFrame) map.get("frame");
    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    dbLst = new ArrayList<String>();
    jta   = (JTextArea) map.get("area");
    JButton bc = (JButton) map.get("connect");
    bc.addActionListener(e -> {
      if (login == null) {
        login = JOptions.login(frame, "Login");
        if (login == null || login[0].length() == 0 || login[1].length() == 0) return;
        try {
          int p = args[0].indexOf(":");
          String host = args[0].substring(0,p);
          int port = Integer.parseInt(args[0].substring(p+1));
          bCon = new ODBMining(host, port, login[1], login[0]);
        } catch (Exception ex) {
          //ex.printStackTrace();
          error("Invalid:"+args[0]);
          System.exit(0);
        }
      }
      try {
        bCon.connect(dbName);
        jta.append(dbName+" is connected.\n");
        if (JOptions.question(frame, "OODB", "AutoCommit ?")) {
          if (bCon.autoCommit(true)) jta.append("Autocommit\n");
          else jta.append("Run without autoCommit\n");
        }
        connected = true;
        dbLst.add(dbName);
        bc.setEnabled(false);
      } catch (Exception ex) {
        login = null;
        ex.printStackTrace();
        jta.append("Cannot connect:"+dbName+". Reason:"+getErrorMessage(ex.toString())+"\n");
      }
      jta.setCaretPosition(jta.getText().length());
    });
    ((JButton) map.get("add")).addActionListener(e -> {
      if (!connected) {
        error("There's NO connection to any OODBServer.");
        return;
      }
      // People(String name, int age, String address, String position, double salary)
      String name = JOptions.input(frame, "People Name");  
      if (name.length() == 0) return;
      if (bCon.isExisted(dbName, name)) {
        error(name+" exists already.");
        return;
      }
      try {
        String[] inp = JOptions.multipleInputs(frame, 
                                    "Age (int)!Address!Profession!Income (double)!Image URL".split("!"),
                                    "Add "+name);
        if (inp == null || inp[0].length() == 0 || inp[1].length() == 0 || inp[2].length() == 0 ||
            inp[3].length() == 0 ||inp[4].length() == 0 || !imgExisted(inp[4])) return;
        bCon.add(dbName, name, new People(name, Integer.parseInt(inp[0]),
                                      inp[1], inp[2], Double.parseDouble(inp[3]), inp[4]));
        jta.append("People "+name+" is added to "+dbName+".\n");
      } catch (Exception ex) {
        ex.printStackTrace();
        jta.append(getErrorMessage(ex.toString())+"\n");
      }
      jta.setCaretPosition(jta.getText().length());
    });
    ((JButton) map.get("del")).addActionListener(e -> {
      if (!connected) {
        error("There's NO connection to any OODBServer.");
        return;
      }
      String name = JOptions.input(frame, "PeopleName");
      if (name.length() == 0) return;
      if (!bCon.isExisted(dbName, name)) {
        error(name+" is unknown.");
        return;
      }
      try {
        if (bCon.isLocked(dbName, name)) {
          bCon.delete(dbName, name);
          jta.append("Object "+name+" is deleted from "+dbName+".\n");
        } else {
          jta.append("Key:"+name+" must be locked before delete.\n");
        }
      } catch (Exception ex) {
        error(getErrorMessage(ex.toString()));
      }
      jta.setCaretPosition(jta.getText().length());
    });
    ((JButton) map.get("read")).addActionListener(e -> {
      if (!connected) {
        error("There's NO connection to any OODBServer.");
        return;
      }
      String name = JOptions.input(frame, "PeopleName");
      if (name.length() == 0) return;
      if (bCon.isExisted(dbName, name))
        try {
          long beg = System.currentTimeMillis();
          People peo = (People)bCon.read(dbName, name);
          double time = (double)(System.currentTimeMillis()-beg)/1000;
          jta.append("read("+dbName+","+name+").\nElapsed time:"+time+" sec.)\n"+peo.toString()+"\n");
          peo.print( );  // print out
          peo.picture(frame); // display him/her
        } catch (Exception ex) {
          jta.append(getErrorMessage(ex.toString())+"\n");
        }
      else jta.append(name+" is unknown.\n");
      jta.setCaretPosition(jta.getText().length());
    });
    ((JButton) map.get("update")).addActionListener(e -> {
      if (!connected) {
        error("There's NO connection to any OODBServer.");
        return;
      }
      String name = JOptions.input(frame, "People Name as Key");
      if (name.length() == 0) return;
      if (bCon.isExisted(dbName, name)) 
        try {
          if (bCon.isLocked(dbName, name)) {
            String[] inp = ((People)bCon.read(dbName, name)).getData();
            inp = JOptions.multipleInputs(frame, 
                                          "Age (int)!Address!Profession!Income (double)!Image URL".split("!"),
                                           inp, "Update "+name);
            if (inp == null || inp[0].length() == 0 || inp[1].length() == 0 || inp[2].length() == 0 ||
                inp[3].length() == 0 ||inp[4].length() == 0 || !imgExisted(inp[4])) return;
            long beg = System.currentTimeMillis();
            bCon.update(dbName, name, new People(name, Integer.parseInt(inp[0]),
                                                 inp[1], inp[2], Double.parseDouble(inp[3]), inp[4]));
            double time = (double)(System.currentTimeMillis()-beg)/1000;
            jta.append("Successfully updated Object "+name+" in "+dbName+
                       ". Elapsed time:"+time+" sec.)\n");
          } else {
            jta.append(name+" must be locked before update.\n");
          }
        } catch (Exception ex) {
          jta.append(getErrorMessage(ex.toString())+"\n");
        }
      else jta.append(name+" is unknown.\n");
      jta.setCaretPosition(jta.getText().length());
    });
    ((JButton) map.get("lock")).addActionListener(e -> {
      if (!connected) {
        error("There's NO connection to any OODBServer.");
        return;
      }
      String name = JOptions.input(frame, "PeopleName");
      if (name.length() == 0) return;
      if (bCon.isExisted(dbName, name)) {
        if (bCon.lock(dbName, name)) jta.append(name+" is locked.\n");
        else jta.append("Cannot lock "+name+"/"+dbName+". Probably it's locked by someone or unknown.\n");
      } else jta.append(name+" is unknown.\n");
      jta.setCaretPosition(jta.getText().length());
    });
    ((JButton) map.get("unlock")).addActionListener(e -> {
      if (!connected) {
        error("There's NO connection to any OODBServer.");
        return;
      }
      String name = JOptions.input(frame, "PeopleName");
      if (name.length() == 0) return;
      if (bCon.isExisted(dbName, name)) {
        if (bCon.unlock(dbName, name)) jta.append(name+" is unlocked.\n");
        else jta.append(name+"/"+dbName+" is unknown or locked by someone.\n");
      } else jta.append(name+" is unknown.\n");
      jta.setCaretPosition(jta.getText().length());
    });
    ((JButton) map.get("list")).addActionListener(e -> {
      if (!connected) {
        error("There's NO connection to any OODBServer.");
        return;
      }
      long beg = System.currentTimeMillis();
      ArrayList<Object> names = bCon.getKeys(dbName);
      //while (names.size() == 0) names = bCon.getKeys(dbName);
      double time = (double)(System.currentTimeMillis()-beg)/1000;
      if (names != null) {
        int z = names.size();
        jta.append(dbName+" contains: ("+z+" name"+(z > 1?"s":"")+". Elapsed time:"+time+" sec.)\n");
        for (Object k : names) jta.append("- "+(String)k+"\n");
      } else if (dbLst != null) jta.append(dbLst+" is empty\n");
      else jta.append("There ain't connection to ODBServer.\n");
      jta.setCaretPosition(jta.getText().length());
    });
    ((JButton) map.get("xaction")).addActionListener(e -> {
      if (!connected) {
        error("There's NO connection to any OODBServer.");
        return;
      }
      String name = JOptions.input(frame, "PeopleName");
      if (name.length() == 0) return;
      if (!bCon.isExisted(dbName, name)) {
        error(name+" is unknown.");
        return;
      }
      String action = JOptions.input(frame, "TransAction (update or delete)");
      if (action.length() == 0) return;
      try {
        String[] inp = ((People)bCon.read(dbName, name)).getData();
        People people = null;
        boolean upd = false;
        if (action.equalsIgnoreCase("update")) {
          inp = JOptions.multipleInputs(frame, 
                                       "Age (int)!Address!Profession!Income (double)!Image URL".split("!"),
                                       inp, "TransAction "+name);
          if (inp == null || inp[0].length() == 0 || inp[1].length() == 0 || inp[2].length() == 0 ||
              inp[3].length() == 0 || inp[4].length() == 0 || !imgExisted(inp[4])) return;
          upd = true;
          long beg = System.currentTimeMillis();
          people = new People(name, Integer.parseInt(inp[0]), inp[1], inp[2], Double.parseDouble(inp[3]), inp[4]);
        } else if (!action.equalsIgnoreCase("delete")) {
          error("Invalid action:"+action);
          return;
        }
        long beg = System.currentTimeMillis();
        boolean boo = upd? bCon.xUpdate(dbName, name, people):bCon.xDelete(dbName, name);
        double time = (double)(System.currentTimeMillis()-beg)/1000;
        if (boo)
             jta.append(action+" for "+name+" was successful. Elapsed time:"+time+" sec.\n");
        else jta.append(action+" for "+name+" was failed.\n");
      } catch (Exception ex) {
        jta.append(action+" for "+name+" was failed. Reason:"+getErrorMessage(ex.toString())+"\n");
      }
      jta.setCaretPosition(jta.getText().length());
    });
    ((JButton) map.get("select_1")).addActionListener(e -> {
      if (!connected) {
        error("There's NO connection to any OODBServer.");
        return;
      }
      // ArrayList<Object> selectAll(String dbName, String pat) 
      String sP = JOptions.input(frame, "Selecting Pattern");
      if (sP.length() == 0) return;
      try {
        long beg = System.currentTimeMillis();
        ArrayList<Object> aL = bCon.selectAll(dbName, sP);
        double time = (double)(System.currentTimeMillis()-beg)/1000;
        if (aL != null  && aL.size() > 0) {
          jta.append("SelectAll("+dbName+", "+sP+").\nElapsed time:"+time+" sec.\n");
          System.out.println("SelectAll("+dbName+", "+sP+")");
          int i = 1;
          for (Object o : aL) {
            jta.append("- "+((People)o).toString()+"\n");
            System.out.println(i+".");
            ((People)o).print();
            ++i;
          }
        } else jta.append("NO pattern:"+sP+" found.\n");
      } catch (Exception x) {
        jta.append("SelectAll:"+getErrorMessage(x.toString())+"\n");
      }
      jta.setCaretPosition(jta.getText().length());
    });
    ((JButton) map.get("select_2")).addActionListener(e -> {
      if (!connected) {
        error("There's NO connection to any OODBServer.");
        return;
      }
      // ArrayList<Object> selectAll(String dbName, String vName, String pat) 
      String vN = JOptions.input(frame, "Variable or FieldName");
      if (vN.length() == 0) return;
      String sP = JOptions.input(frame, "Selecting Pattern");
      if (sP.length() == 0) return;
      try {
        long beg = System.currentTimeMillis();
        ArrayList<Object> aL = bCon.selectAll(dbName, vN, sP);
        double time = (double)(System.currentTimeMillis()-beg)/1000;
        if (aL != null  && aL.size() > 0) {
          jta.append("SelectAll("+dbName+", "+vN+", "+sP+").\nElapsed time:"+time+" sec.\n");
          System.out.println("SelectAll("+dbName+", "+vN+", "+sP+")");
          int i = 1;
          for (Object o : aL) {
            jta.append("- "+((People)o).toString()+"\n");
            System.out.println(i+".");
            ((People)o).print();
            ++i;
          }
        } else jta.append("NO pattern:"+sP+" or FieldName:"+vN+" found.\n");
      } catch (Exception x) {
        jta.append(getErrorMessage(x.toString())+"\n");
      }
      jta.setCaretPosition(jta.getText().length());
    });
    ((JButton) map.get("select_3")).addActionListener(e -> {
      if (!connected) {
        error("There's NO connection to any OODBServer.");
        return;
      }
      //ArrayList<Object> selectAll(String dbName, String comp, double cVal)
      String vN = JOptions.input(frame, "Field/VariableName");
      if (vN.length() == 0) return;
      String cp = JOptions.input(frame, "Comparator (LT, LE, EQ, NE, GE, GT)");
      if (cp.length() == 0) return;
      String cV = JOptions.input(frame, "ComparingValue (if double: 1.0 for 1)");
      if (cV.length() == 0) return;
      try {
        cp = cp.toUpperCase();
        long beg = System.currentTimeMillis();
        ArrayList<Object> aL = (cV.indexOf(".") > 0) ?
                               bCon.selectAll(dbName, vN, cp, Double.parseDouble(cV)):
                               bCon.selectAll(dbName, vN, cp, Integer.parseInt(cV));
        double time = (double)(System.currentTimeMillis()-beg)/1000;
        if (aL != null  && aL.size() > 0) {
          jta.append("SelectAll("+dbName+", "+vN+", "+cp+", "+cV+").\nElapsed time:"+time+" sec.\n");
          System.out.println("SelectAll("+vN+", "+cp+", "+cV+")");
          int i = 1;
          for (Object o : aL) {
            jta.append("- "+((People)o).toString()+"\n");
            System.out.println(i+".");
            ((People)o).print();
            ++i;
          }
        } else jta.append("NOTHING is "+cp+" to "+cV+" found.\n");
      } catch (Exception x) {
        jta.append(getErrorMessage(x.toString())+"\n");
      }
      jta.setCaretPosition(jta.getText().length());
    });
    ((JButton) map.get("select_4")).addActionListener(e -> {
      if (!connected) {
        error("There's NO connection to any OODBServer.");
        return;
      }
      //ArrayList<Object> selectSQL(String dbName, sqlString)
      String sql = JOptions.input(frame, "SQL String");
      if (sql == null || sql.length() == 0) return;
      try {
        long beg = System.currentTimeMillis();
        ArrayList<Object> aL = bCon.SQL(dbName, sql);
        double time = (double)(System.currentTimeMillis()-beg)/1000;
        if (aL != null  && aL.size() > 0) {
          jta.append("SQL("+dbName+", "+sql+").\nElapsed time:"+time+" sec.\n");
          System.out.println("SQL("+dbName+", "+sql+")");
          int i = 1;
          for (Object o : aL) {
            jta.append("- "+((People)o).toString()+"\n");
            System.out.println(i+".");
            ((People)o).print();
            ++i;
          }
        } else jta.append("No match for '"+sql+"'.\n");
      } catch (Exception x) {
        jta.append(getErrorMessage(x.toString())+"\n");
      }
      jta.setCaretPosition(jta.getText().length());
    });
    ((JButton) map.get("FieldNames")).addActionListener(e -> {
      if (!connected) {
        error("There's NO connection to any OODBServer.");
        return;
      }
      /*
      try {
        long beg = System.currentTimeMillis();
        double d = (double)bCon.getField(dbName, "Joe", "income");
        double time = (double)(System.currentTimeMillis()-beg)/1000;
        jta.append("getField(\"Joe\", \"income\"):"+d+"\nElapsed time:"+time+" sec.\n");
      } catch (Exception x) {
        x.printStackTrace();
        jta.append(getErrorMessage(x.toString())+"\n");
      }
      jta.setCaretPosition(jta.getText().length());
      */
      //String oN = JOptions.input(frame, "ObjectName");
      String oN = JOptions.input(frame, "Key");
      if (oN.length() == 0) return;
      try {
        long beg = System.currentTimeMillis();
        ArrayList<String> vN = bCon.getFieldNames(dbName, oN);
        double time = (double)(System.currentTimeMillis()-beg)/1000;
        if (vN.size() > 0) {
          jta.append("getFieldNames("+oN+").\nElapsed time:"+time+" sec.\n");
          for (String n : vN) {
            jta.append("- "+n+"\n");
          }
        } else jta.append("NO "+oN+" is found.\n");
      } catch (Exception x) {
        x.printStackTrace();
        jta.append(getErrorMessage(x.toString())+"\n");
      }
      jta.setCaretPosition(jta.getText().length());
    });
    ((JButton) map.get("isExisted")).addActionListener(e -> {
      if (!connected) {
        error("There's NO connection to any OODBServer.");
        return;
      }
      String name = JOptions.input(frame, "PeopleName");
      if (name.length() == 0) return;
      if (bCon.isExisted(dbName, name)) jta.append(name+" is existed.\n");
      else jta.append(name+" is unknown.\n");
      jta.setCaretPosition(jta.getText().length());
    });
    ((JButton) map.get("isLocked")).addActionListener(e -> {
      if (!connected) {
        error("There's NO connection to any OODBServer.");
        return;
      }
      String name = JOptions.input(frame, "PeopleName");
      if (name.length() == 0) return;
      if (bCon.isExisted(dbName, name)) {
        if (bCon.isLocked(dbName, name)) jta.append(name+" is locked.\n");
        else jta.append(name+" is unknown or unlocked.\n");
      } else jta.append(name+" is unknown.\n");
      jta.setCaretPosition(jta.getText().length());
    });
    ((JButton) map.get("rollback")).addActionListener(e -> {
      if (!connected) {
        error("There's NO connection to any OODBServer.");
        return;
      }
      String name = JOptions.input(frame, "PeopleName");
      if (name.length() == 0) return;
      if (bCon.isLocked(dbName, name))
        try {
          if (bCon.rollback(dbName, name)) jta.append(name+" is rolled back.\n");
          else jta.append(name+" was committed. NO rollback\n");
        } catch (Exception ex) {
          jta.append(getErrorMessage(ex.toString())+"\n");
        }
      else jta.append(name+" must be locked before rollback.\n");
      jta.setCaretPosition(jta.getText().length());
    });
    ((JButton) map.get("commit")).addActionListener(e -> {
      if (!connected) {
        error("There's NO connection to any OODBServer.");
        return;
      }
      String name = JOptions.input(frame, "PeopleName");
      if (name.length() == 0) return;
      if (bCon.isLocked(dbName, name)) try {
        if (bCon.commit(dbName, name)) jta.append(name+" is committed.\n");
        else jta.append(name+"/"+dbName+" is unknown.\n");
      } catch (Exception ex) {
        jta.append(getErrorMessage(ex.toString())+"\n");
      }
      else jta.append(name+" must be locked before commit.\n");
      jta.setCaretPosition(jta.getText().length());
    });
    ((JButton) map.get("commitAll")).addActionListener(e -> {
      if (!connected) {
        error("There's NO connection to any OODBServer.");
        return;
      }
      try {
        bCon.commit(dbName);
      } catch (Exception ex) {
        jta.append(getErrorMessage(ex.toString())+"\n");
      }
      jta.append("All transactions on /"+dbName+" are committed.\n");
      jta.setCaretPosition(jta.getText().length());
    });
    ((JButton) map.get("exit")).addActionListener(e -> {
      if (connected) try {
        bCon.disconnect( );
      } catch (Exception x) { }
      System.exit(0);
    });
  }
  //
  private String getErrorMessage(String errMsg) {
    return errMsg.replace("java.lang.Exception: ","");
  }
  private void error(String msg) {
    JOptionPane.showMessageDialog(frame, msg, "Error", JOptionPane.ERROR_MESSAGE);
  }
  private boolean imgExisted(String url) {
    try {
      if (url.indexOf("://") > 0) {
        URL u = new URL(url);
      } else {
        File f = new File(url);
      }
      return true;
    } catch (Exception e) { }
    return false;
  }
  //
  private JFrame frame;
  private JTextArea jta;
  private ODBMining bCon;
  private boolean connected;
  private ArrayList<String> dbLst;
  private String login[], dbName = "people";
}

