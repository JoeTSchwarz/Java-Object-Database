package joeapp.odb;
//
import java.net.*;
import java.util.*;
import java.io.OutputStream;
import java.util.concurrent.*;
/**
 ODB global parameters
 @author Joe T. Schwarz
*/
public class ODBParms {
  /**
  Constructor. Global parameters of ODB
  */
  public ODBParms( ) {
    nodeList = Collections.synchronizedList(new ArrayList<>(128));
  }
  /**
  @param msg, String -message to be logged
  */
  public void logging(String msg) {
    synchronized(logger) {
      try {
        logger.write((msg+System.lineSeparator()).getBytes());
        logger.flush();
      } catch (Exception ex) {
        System.err.println("Unable to log: "+msg);
      }
    }
  }
  //
  public int limit;
  public ODBManager odMgr;
  public ODBBroadcaster BC;
  public OutputStream logger;
  public volatile boolean log;
  public List<String> nodeList;
  public ODBEventListener listener;
  public String db_path, broadcaster, primary, userlist, webHostName;
}
