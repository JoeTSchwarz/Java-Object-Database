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
// Joe Nartca (c)
public class Concurrency extends JFrame {
  private JTextArea ta;
  private JComboBox<String> combo, eTime, loop, iItem;

  public Concurrency(int wt) throws Exception {
    this.wt = wt;
    setTitle("ConcurrencyTest with the ODB Stock (local & remote)");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    pool = Executors.newFixedThreadPool(256);
    // access the object
    // get the keys
    ODBConnect dbcon = new ODBConnect("192.168.0.70", 9999, "tester", "test");
    dbcon.connect("Stock");
    ArrayList<Object> items = dbcon.getKeys("Stock");
    //
    JPanel lp = new JPanel();
    ta = new JTextArea(22, 55);
    ta.setWrapStyleWord(true);
    ta.setLineWrap(true);
    ta.addMouseListener(new CopyAndPaste(ta));   
    ta.append("Following Items are stored in ODB Stock:\n\n");
    for (int i = 0, sz = items.size(); i < sz; ++i) {
      try {
        Items item = (Items)dbcon.read("Stock", (String)items.get(i));
        ta.append("Item_"+i+": "+item.name+" stock: "+item.counter+"\n");
      } catch (Exception ex) { }
    }
    dbcon.disconnect( );
    JScrollPane jsp = new JScrollPane(ta); 
    lp.setBorder(BorderFactory.createTitledBorder("ConcurrencyReport"));
    jsp.setAutoscrolls(true);
    lp.add(jsp);

    JLabel lab = new JLabel("Number of Users");
    combo = new JComboBox<String>(("5!10!20!30!40!Stop").split("!"));
    JLabel labt = new JLabel("ThinkingTime (miliSec)");
    eTime = new JComboBox<String>(("0!1!2!3").split("!"));
    JLabel labi = new JLabel("Item");
    iItem = new JComboBox<String>(("TunaCan!CocaBottle").split("!"));
    JLabel labL = new JLabel("Looping");
    loop = new JComboBox<String>(("5!10!20!50!100").split("!"));
    JButton GO = new JButton(("START"));
    GO.addActionListener(a->start());
    JPanel pa = new JPanel();
    pa.add(lab); pa.add(combo);
    pa.add(labt); pa.add(eTime);
    pa.add(labi); pa.add(iItem);
    pa.add(labL); pa.add(loop);
    pa.add(GO);
    
    add("Center", lp);
    add("South", pa);
    setMinimumSize(new Dimension(700, 500));
    setLocation(100, 100);
    setResizable(true);
    setVisible(true);
  }
  private class ConcurrencyTest implements Runnable {
    public ConcurrencyTest(int id, int port, String key) {
      this.id = id;
      this.key = key;
      this.port = port;
    }
    private int id, port;
    private String key;
    public void run() {
      ODBConnect dbcon = null;
      Items obj = null;
      int stock = 0;
      try {
        dbcon = new ODBConnect("192.168.0.70", port, "tester", "test");
        dbcon.connect("Stock"); // connect to Stock
        obj = (Items)dbcon.read("Stock", key);
        stock = obj.counter;
      } catch (IOException e) {
        e.printStackTrace();
      } catch (Exception ex) {
        ta.append("\nException:"+ex.toString());
        return;
      }
      int x = cnt;
      long beg, time = 0;
      boolean bool = false;
      ta.append("\n\nThread_"+id+"@Port:"+port+" starts, ODB_ID:"+dbcon.getID()+
                ". Open: Stock of "+key+"-Stock:"+stock);
      
      while (!stopped && x > 0) try {
        beg = System.nanoTime();
        if (dbcon.lock("Stock", key)) {
          try {
            obj =(Items) dbcon.read("Stock", key);
            ++obj.counter; // increment counter
            dbcon.update("Stock", key, obj);
            //dbcon.unlock("Stock", key); // will be lost
            if (!dbcon.commit("Stock", key))
              ta.append("\n>>>User_"+id+" got COMMIT failure.");
          } catch (Exception e) {
            ta.append("\n\n>>>User:"+id+" updates '"+key+"' but failed");
          }
          time += System.nanoTime()-beg;             
        } else {
          ta.append("\n\n>>>User:"+id+" locked '"+key+"' but failed");
        }
        --x;
        dbcon.unlock("Stock", key);
        ta.setCaretPosition(ta.getDocument().getLength());
        // Thinking time
        java.util.concurrent.TimeUnit.MILLISECONDS.sleep(sTime);
      } catch (Exception e) {
        ta.append("\n\n>>>User_"+id+" got LOCK exception:"+e.toString());
        ta.setCaretPosition(ta.getDocument().getLength());
      }
      Items item = null;
      try {
        item = (Items) dbcon.read("Stock", key);
        dbcon.commit("Stock");
        dbcon.save("Stock");
        dbcon.disconnect();
      } catch (Exception e) { }
      int sum = stock + cnt;
      double sec = (double)time/(1000000*cnt);
      ta.append("\n\nUser_"+id+" quitted and disconnects"+
                "\nAverage Read/Lock/Update/Unlock Time:"+sec+" milliSec."+
                "\nItem:"+item.name+" Stock:"+item.counter+" Expected:"+sum);
      //
      System.out.println("\nUser_"+id+" quitted and disconnects"+
                "\nAverage Read/Lock/Update/Unlock Time:"+sec+" milliSec."+
                "\nItem:"+item.name+" Stock:"+item.counter+" Expected:"+sum);
      ta.setCaretPosition(ta.getDocument().getLength());
    }
  }
  private void start() {
    try {
      String tmp = (String) combo.getSelectedItem();
      stopped = tmp.charAt(0) == 'S';
      if (!stopped) {
        int n = Integer.parseInt(tmp);
        sTime = Integer.parseInt((String)eTime.getSelectedItem());
        cnt = Integer.parseInt((String)loop.getSelectedItem());
        String key = (String)iItem.getSelectedItem();
        no = n;
        int port = 9999;
        for (n = 0; n < no; ++n) {
          ta.append("\nStart Thread_"+n);
          pool.submit(new ConcurrencyTest(n, port, key));
          java.util.concurrent.TimeUnit.MILLISECONDS.sleep(wt);
          if (port == 9999) port = 8888; else port = 9999;
        }
      } else {
        ta.append("\n\nConcurrencyTest terminated");
        ta.setCaretPosition(ta.getDocument().getLength());
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this, e.toString(), "Info", JOptionPane.PLAIN_MESSAGE);
    }
  }
  public static void main(String... a) throws Exception {
    UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
    int t = 200;
    if (a.length > 0) t = Integer.parseInt(a[0]);
    new Concurrency(t);
  }
  private ExecutorService pool;
  private volatile boolean stopped = false;
  private int no = 0, sTime, cnt, idx = 0, wt;
  private java.util.Random ran = new java.util.Random();
}

