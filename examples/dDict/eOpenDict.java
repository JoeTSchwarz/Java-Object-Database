import java.io.*;
import java.util.*;
import java.net.URL;
import java.util.zip.*;
import joeapp.odb.*;

// @author Joe T. Schwarz (c)
public class eOpenDict {
  public eOpenDict(String uid, String pw, String host, int port) throws Exception {
    this.pw = pw;
    this.uid = uid;
    odbc = new ODBConnect(host, port, pw, uid);
    cset = new String[] { odbc.charsetNameOf("Tiếng Việt"),
                          odbc.charsetNameOf("日本語"),
                          odbc.charsetNameOf("中国人")
                        };
    if (!odbc.autoCommit(true)) {
      System.out.println("Unable to set autoCommit() to Server");
      System.exit(0);
    }
    odb = odbs[0];
    csName = cset[0];
    odbc.connect(odb, csName);
    odbc.notify(odb, true);

    odbc.connect(odbs[1], cset[1]);
    odbc.notify(odbs[1], true);

    odbc.connect(odbs[2], cset[2]);
    odbc.notify(odbs[2], true);

    odbc.notify(odb, true);
    sort(odbc.getKeys(odb));
  }
  // mode
  // 0: Vietnamese ODB
  // 1: Japanese ODB
  // 2: Chinese ODB
  public void setODB(int mode) {
    odb = odbs[mode];
    csName = cset[mode];
  }
  // fail-safe
  public boolean switchNode(ODBEvent e) {
    try {
      int type = e.getEventType();
      odbc.disconnect(); // disconnect this
      odbc = e.onEvent(pw, uid, odbc.getDBList());
      activeNode = e.getActiveNode();
      return odbc != null;
    } catch (Exception ex) { }
    return false;
  }
  public void register(ODBEventListening odbe) {
    odbc.register(odbe);
  }
  /**
  getActiveNode
  @return String, the running node (Host:Port)
  */
  public String getActiveNode() {
    return activeNode;
  }
  public void getKeys( ) {  
    sort(odbc.getKeys(odb));
  }
  public String getUserID() {
    return odbc.getID();
  }
  public void sendMsg(String msg) {  
    odbc.sendMsg(odbc.getID()+": "+msg);
  }  
  public String search(String key) {
    try {
      for (String k:words) if (k.equals(key)) return (String) odbc.read(odb, key);
    } catch (Exception ex) { }
    return null;
  }
  public boolean lock(String key) {
    try {
      return odbc.lock(odb, key);
    } catch (Exception ex) { }
    return false;
  }
  public boolean isLocked(String word) {
    try {
      return odbc.isLocked(odb, word);
    } catch (Exception ex) { }
    return false;
  }
  public boolean unlock(String key) {
    try {
      return odbc.unlock(odb, key);
    } catch (Exception ex) { }
    return false;
  }
  public boolean update(String key, String meaning) {
    try {
      return odbc.xUpdate(odb, key, meaning);
    } catch (Exception ex) { }
    return false;
  }
  public boolean delete(String key) {
    try {
      return odbc.xDelete(odb, key);
    } catch (Exception ex) { }
    return false;
  }
  public void add(String key, String meaning) {
    try {
      odbc.add(odb, key, meaning);
      done = true;
    } catch (Exception ex) {
      done = false;
    }
  }
  public String read(String key) {
    done = true;
    try {
      Object obj = odbc.read(odb, key);
      if (obj instanceof String) return (String) obj;
      return new String((byte[])obj, csName);
    } catch (Exception ex) { }
    done = false;
    return null;
  }
  public void save() {
    try {
      odbc.commit(odb);
    } catch (Exception ex) { }
  }
  public void exit() {
    try {
      odbc.disconnect();
    } catch (Exception ex) { }
  }
  public boolean isDone() {
    return done;
  }
  //
  public String odb;
  public List<String> words;
  //
  private void sort(List<Object> lst) {
    words = new ArrayList<>();
    if (lst != null &&lst.size() > 0) {
      for (Object obj : lst) words.add((String)obj);
      Collections.sort(words);
    }
  }
  private boolean done;
  private ODBConnect odbc;
  private String pw, uid, activeNode, csName, cset[];
  private String[] odbs = { "vdDict", "jdDict", "cdDict" };
}
