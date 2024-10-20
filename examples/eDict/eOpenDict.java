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
    odbc.connect(dict);
    if (!odbc.autoCommit(true)) {
      System.out.println("Unable to set autoCommit() to Server");
      System.exit(0);
    }
    sort(odbc.getKeys(dict));
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
    sort(odbc.getKeys(dict));
  }
  public String search(String key) {
    try {
      for (String k:words) if (k.equals(key)) return (String) odbc.read(dict, key);
    } catch (Exception ex) { }
    return null;
  }
  public boolean lock(String key) {
    try {
      return odbc.lock(dict, key);
    } catch (Exception ex) { }
    return false;
  }
  public boolean isLocked(String word) {
    try {
      return odbc.isLocked(dict, word);
    } catch (Exception ex) { }
    return false;
  }
  public boolean unlock(String key) {
    try {
      return odbc.unlock(dict, key);
    } catch (Exception ex) { }
    return false;
  }
  public boolean update(String key, String meaning) {
    try {
      return odbc.xUpdate(dict, key, meaning);
    } catch (Exception ex) { }
    return false;
  }
  public boolean delete(String key) {
    try {
      return odbc.xDelete(dict, key);
    } catch (Exception ex) { }
    return false;
  }
  public void keep(String key, String meaning) {
    try {
      odbc.add(dict, key, meaning);
      odbc.commit(dict, key);
      done = true;
    } catch (Exception ex) {
      done = false;
    }
  }
  public String getMeaning(String key) {
    done = true;
    try {
      return (String) odbc.read(dict, key);
    } catch (Exception ex) { }
    done = false;
    return "Can't read. Probably "+key+" is locked.";
  }
  public void save() {
    try {
      odbc.commit(dict);
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
  public String dict = "eDict";
  public ArrayList<String> words;
  //
  private void sort(List<Object> lst) {
    if (lst == null) return;
    words = new ArrayList<>(lst.size());
    for (Object obj : lst) words.add((String)obj);
    Collections.sort(words);
  }
  private boolean done;
  private ODBConnect odbc;
  private String pw, uid, activeNode;
}
