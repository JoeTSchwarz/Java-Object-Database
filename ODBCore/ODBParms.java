package joeapp.odb;
//
import java.net.*;
import java.util.*;
import java.nio.charset.*;
import java.io.OutputStream;
import java.util.concurrent.*;
/**
 ODB global parameters
 @author Joe T. Schwarz
*/
public class ODBParms {
  /**
  Constructor. Global parameters for JODB
  @param logger OutputStream, null if no logger
  @param db_path String, path to JODB files
  @param active boolean, true if active
  */
  public ODBParms(OutputStream logger, String db_path, boolean active) {
    this.db_path = db_path;
    this.logger = logger;
    this.active = active;
    log = logger != null;
  }
  //
  public ODBManager odMgr;
  public ODBBroadcaster BC;
  public OutputStream logger;
  public ExecutorService pool;
  public ODBEventListener listener;
  public volatile boolean active = true, log;
  public Charset charset = StandardCharsets.US_ASCII;
  public String db_path, broadcaster, primary, userlist, webHostName;
  public List<String> nodes = new ArrayList<>(), remList = new ArrayList<>();
}
