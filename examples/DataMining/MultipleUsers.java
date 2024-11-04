import java.io.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.util.concurrent.*;
//
import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
//
import joeapp.odb.*;
// @author Joe T. Schwarz (c)
public class MultipleUsers extends JFrame {
  private JTextArea ta;
  private JCheckBox cb;
  private JComboBox<String> combo, eTime, loop;

  public MultipleUsers( ) throws Exception {
    setTitle("MultipleUsers (as Threads) with OODBmix (local & remote)");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    // access the object
    // get the keys
    ODBConnect dbcon = new ODBConnect("localhost", 8888, "tester", "test");
    dbcon.connect(dbName);
    ArrayList<Object> objs = dbcon.getKeys(dbName);
    keys = new String[objs.size()];
    dbcon.disconnect( );
    //
    JPanel lp = new JPanel();
    ta = new JTextArea(22, 55);
    ta.setWrapStyleWord(true);
    ta.setLineWrap(true);
    ta.addMouseListener(new CopyAndPaste(ta));   
    ta.append("Following Objects are stored in OODBmix:\n\n");
    for (int i = 0, sz = keys.length; i < sz; ++i) {
      keys[i] = (String) objs.get(i);
      ta.append("Member_"+i+":"+keys[i]+"\n");
    }
    JScrollPane jsp = new JScrollPane(ta); 
    lp.setBorder(BorderFactory.createTitledBorder("ConcurrencyReport"));
    jsp.setAutoscrolls(true);
    lp.add(jsp);

    JLabel lab = new JLabel("Number of Users");
    combo = new JComboBox<String>(("5!10!20!30!40!50").split("!"));
    JLabel labt = new JLabel("DelayTime (miliSec)");
    eTime = new JComboBox<String>(("0!1!2!3").split("!"));
    JLabel labL = new JLabel("Looping");
    loop = new JComboBox<String>(("5!10!20!50!100").split("!"));
    JLabel labC = new JLabel("Before Update?");
    cb = new JCheckBox();
    cb.setSelected(false);
    GO = new JButton(("START"));
    GO.addActionListener(a->start());
    JPanel pa = new JPanel();
    pa.add(lab); pa.add(combo);
    pa.add(labt); pa.add(eTime);
    pa.add(labC); pa.add(cb);
    pa.add(labL); pa.add(loop);
    pa.add(GO);
    
    add("Center", lp);
    add("South", pa);
    setMinimumSize(new Dimension(700, 500));
    setLocation(100, 100);
    setResizable(true);
    setVisible(true);
  }
  private class DBtest extends Thread {
    public DBtest(int id, int cnt, int tTime) {
      this.id = id;
      this.cnt = cnt;
      this.tTime = tTime;
      ta.append("\nThread_"+id+" is running.");
    }
    private int id, cnt, tTime;
    public synchronized void run() {
      ODBConnect dbcon = null;
      int idx = ran.nextInt(2);
      try {
        dbcon = new ODBConnect("localhost", port[idx], "tester", "test");
        dbcon.connect(dbName); // openDB
      } catch (Exception ex) {
        ta.append("\nUnable to connect to "+port[idx]+". Reason:"+ex.toString());
        return;
      }
      double time = 0;
      ta.append("\nThread_"+id+"->Port:"+port[idx]+"<>ODB_ID:"+dbcon.getID()+"\nOpen: "+dbName);
      idx = cnt; // save counter
      
      while (cnt > 0 && !stopped) {
        int k = (int)Math.abs(ran.nextInt(keys.length-1));
        String key = keys[k];
        boolean bool = false;
        try {
          long beg = System.nanoTime();
          if (dbcon.lock(dbName, key)) {
            Object obj = dbcon.read(dbName, key);
            if (bef && tTime > 0) java.util.concurrent.TimeUnit.MILLISECONDS.sleep(tTime);
            if (obj instanceof Member) dbcon.update(dbName, key, (Member)obj);
            else if (obj instanceof Animal) dbcon.update(dbName, key, (Animal)obj);
            else dbcon.update(dbName, key, obj);
            // commit and free key after Thinking time
            if (!bef && tTime > 0) java.util.concurrent.TimeUnit.MILLISECONDS.sleep(tTime);
            if (dbcon.commit(dbName, key)) {
              time += (System.nanoTime()-beg);
              ta.append("\nUser:"+id+" locked '"+key+"', updated, commited and committed.");
            } else {
              dbcon.unlock(dbName, key);
              time += (System.nanoTime()-beg);
              ta.append("\nUser:"+id+" locked '"+key+"', updated but commited failed.");
            }
          } else {
            dbcon.unlock(dbName, key);
            time += (System.nanoTime()-beg);
            ta.append("\n\n>>>User:"+id+" locked '"+key+"' but failed");
          }
        } catch (Exception e) {
          dbcon.unlock(dbName, key);
          e.printStackTrace();
          ta.append("\n\n>>>User_"+id+" got UPDATE exception:"+e.toString());
        }
        ta.setCaretPosition(ta.getDocument().getLength());
        --cnt;
      }
      try {
        dbcon.save(dbName);
        dbcon.close(dbName);
        dbcon.disconnect();
      } catch (Exception e) { }
      String sec = String.format(" %4.3f miliSec.", (time /= (1000000*idx)));
      ta.append("\nUser_"+id+" quitted and disconnects"+
                "\nAverage Read/Lock/Update/Commit Time:"+sec+" by "+(idx-cnt)+" Transactions");
      System.out.println("User_"+id+" quitted and disconnects"+
                "\nAverage Read/Lock/Update/Commit Time:"+sec+" by "+(idx-cnt)+" Transactions");
      ta.setCaretPosition(ta.getDocument().getLength());
    }
  }
  private void start() {
    try {
      panicShutdown();
      bef = cb.isSelected();
      int tTime = Integer.parseInt((String)eTime.getSelectedItem());
      String tmp = (String) combo.getSelectedItem();
      int n = Integer.parseInt(tmp);
      int cnt = Integer.parseInt((String)loop.getSelectedItem());
      int no = n;
      for (n = 0; n < no; ++n) {
        ta.append("\nUser_"+n+" is started.");
        (new DBtest(n, cnt, tTime)).start();
        java.util.concurrent.TimeUnit.SECONDS.sleep(2);
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this, e.toString(), "Info", JOptionPane.PLAIN_MESSAGE);
    }
  }
  /**
  hook the panicShutdown watching dog.
  */
  private void panicShutdown(){
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        stopped = true;
      }
    });
  }
  public static void main(String... a) throws Exception {
    UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
    new MultipleUsers();
  }
  JButton GO;
  String dbName = "OODBmix";
  private String[] keys;
  private int[] port = {8888, 9999 };
  private volatile boolean stopped = false, bef = false;
  private java.util.Random ran = new java.util.Random();
}

