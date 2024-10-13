package joeapp.odb;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.stream.*;
import java.util.concurrent.*;
//
import java.math.*;
import java.lang.reflect.*;
//
import java.nio.*;
import java.nio.channels.*;
/**
Object Database Worker
@author Joe T. Schwarz (c)
*/
public class ODBWorker extends Thread {
  /**
  contructor. Associated partner of Client app
  @param soc   SocketChannel
  @param parms ODBParms, odb Parameters
  */
  public ODBWorker(SocketChannel soc, ODBParms parms) {
    this.parms = parms;
    this.soc = soc;
  }
  //
  public void run() {
    try {
      soc.socket().setTcpNoDelay(true);
      soc.socket().setSendBufferSize(65536);
      soc.socket().setReceiveBufferSize(65536);
    } catch (Exception ex) { }
    //
    String key;
    ODBInputStream ois;
    ArrayList<String> kLst;
    ODBManager odMgr = parms.odMgr;
    ODBIOStream ios = new ODBIOStream( );
    ODBObjectView oov = new ODBObjectView();
    oov.setCharset(parms.charset);
    ios.setCharset(parms.charset);
    while (parms.active) {
      try {
        ios.read(soc); // write the content from soc to ios
        ois = new ODBInputStream(ios.toByteArray());
        ois.setCharset(parms.charset);
        //
        int cmd = ois.read(); // network Command
        String dbName = ois.readToken( );
        if (dbName.charAt(0) == '+') {
          int le = dbName.indexOf("|")+1;
          uID = dbName.substring(0, le);
          dbName = dbName.substring(le);
        }
        ios.preset(); // a MUST
        // check for the request
        switch (cmd) {
        case 0: // ODBConnect
          if (authenticate(dbName)) {
            odMgr.setID(uID);
            ios.writeBool(true);
            ios.writeMsg(priv+uID+"/"+parms.broadcaster);
            logging(uID+" is connected."); // abs. Path
            break;
          }
          // illegal user
          ios.writeBool(false);
          ios.writeErr("Unknown User "+uid+".");
          ios.write(soc);
          exit(); // quit
          return;
        case 1: // ODBCluster
          priv = 2;
          ios.writeBool(true);
          break;
        case 2: // disconnect ODBConnect
          try {
            if (priv > 0) odMgr.removeClient(uID);
            else odMgr.close(uID);
            // free all bound dbAgents
            odMgr.unbindAgent('+'+uID+'|');
            ios.write(soc);
          } catch (Exception ex) { }
          logging(uid+" disconnected.");
          exit();
          return;
        case 3: // getKeys(dbName)
          ios.writeList(odMgr.getKeys(uID, dbName));
          break;
        case 4: // getLocalKey
          ios.writeList(odMgr.getLocalKeys(uID, dbName));
          break;
        case 5: // getClusterKey
          ios.writeList(odMgr.getClusterKeys(uID, dbName, ois.readToken( )));
          break;
        case 6: // add(dbName, key, obj)
          key = ois.readToken( ); // key
          odMgr.add(uID, dbName, key, ois.readBytes());
          logging(uID+" added object with key: "+key+" to "+dbName+".");
          break;
        case 7: // unlock(dbName) all keys
          ios.writeBool(odMgr.unlock(uID, dbName));
          logging(uID+" unlocked all keys of "+dbName+".");
          break;
        case 8: // update(dbName, key, obj)
          key = ois.readToken( ); // key
          if (odMgr.update(uID, dbName, key, ois.readBytes())) {
            ios.writeBool(true);
            logging(uID+" updated object with key: "+key+" of "+dbName+".");
          } else {
            ios.writeBool(false);
            logging(uID+" updated "+dbName+" failed: "+key+" is unknown or unlocked");
          }
          break;
        case 9: // delete(dbName, key)
          key = ois.readToken( ); // key
          if (odMgr.delete(uID, dbName, key)) {
            ios.writeBool(true);
            logging(uID+" deleted object with key: "+key+" of "+dbName+".");
          } else {
            ios.writeBool(false);
            logging(uID+" deleted "+dbName+" failed: "+key+" is unknown or unlocked");
          }
          break;
        case 10: // read(dbName, key). Regardless of locked or unlocked
          ios.writeObj(odMgr.read(uID, dbName, ois.readToken( )));
          break;
        case 11: // isExisted(dbName, key)
          ios.writeBool(odMgr.isExisted(uID, dbName, ois.readToken( )));
          break;
        case 12: // isLocked(dbName, key)
          key = ois.readToken( );
          ios.writeBool(odMgr.isLocked(uID, dbName, key));
          logging(key+" of "+dbName+" is locked.");
          break;
        case 13: // lock(dbName, key)
          key = ois.readToken( );
          ios.writeBool(odMgr.lock(uID, dbName, key));
          logging(uID+" locked key: "+key+" of "+dbName+".");
          break;
        case 14: // unlock(dbName, key)
          key = ois.readToken( );
          ios.writeBool(odMgr.unlock(uID, dbName, key));
          logging(uID+" unlocked key: "+key+" of "+dbName+".");
          break;
        case 15: // rollback(dbName) or rollback(dbName, key)
          if (odMgr.isAutoCommit(uID)) {
            ios.writeErr(uID+" sets autoCommit: NO rollback.");
            break;
          }
          if (ois.remainderLength() > 0) {
            key = ois.readToken( ); // key
            if (odMgr.isLocked(uID, dbName, key)) {
              ios.writeBool(odMgr.odbRestoreKey(uID, dbName, key, false));
              logging(uID+" rollbacked key: "+key+" of "+dbName+".");
            } else ios.writeErr(key+" must be locked before rollback.");
          } else {
            ios.writeBool(odMgr.odbRestore(uID, dbName, false));
            logging(uID+" rollbacked all keys of "+dbName+".");
          }
          break;
        case 16: // ODBCluster - connect(dbName)
          if ((new File(parms.db_path+dbName)).exists()) odMgr.connect(uID, dbName);
          break;
        case 17: // ODBConnect - connect(dbName)
          boolean existed = (new File(parms.db_path+dbName)).exists();
          if (priv < 2 && !existed) {
            ios.writeErr(dbName+" does not exist and "+uid+" lacks of RW-Privilege.");
            logging(uID+" tried to connect "+dbName+", but failed due to lack of privileg.");
            break;
          }
          if (priv > 0 || existed) {
            odMgr.connect(uID, dbName); // setup dbAgents on cluster
            if (!parms.remList.contains(parms.webHostName)) odMgr.bindAgent('+'+uID+'|'+dbName);
            logging(uID+" is connected to "+dbName+".");
          } else ios.writeErr(dbName+" does not exist. Or "+uid+" lacks of RW-Privilege.");
          break;
        case 18: // close(dbName)
          odMgr.close(uID, dbName);
          logging(uid+" closed "+dbName+".");
          break;
        case 19: // save(dbName)
          odMgr.save(uID, dbName);
          logging(uid+" saved "+dbName+".");
          break;
        case 20: // xDelete
          key = ois.readToken( ); // key
          if (odMgr.lock(uID, dbName, key) || uID.equals(odMgr.lockedBy(dbName, key))) {
            if (odMgr.delete(uID, dbName, key)) {
              if (odMgr.isAutoCommit(uID) || odMgr.odbRestoreKey(uID, dbName, key, true)) {
                ios.writeBool(true);
                logging(uID+" xDeleted key: "+key+" of "+dbName+".");
              } else ios.writeErr("xDelete/commit "+dbName+" @"+key+" failed.");
              break;
            }
            ios.writeErr("xDelete "+dbName+" @"+key+" failed.");
            logging(uID+" xDeleted key: "+key+" of "+dbName+", but failed.");
          } else ios.writeErr("xDelete: Cannot lock Key "+key);
          ios.writeBool(false);
          break;
        case 21: // xUpdate
          key = ois.readToken( ); // key
          if (odMgr.lock(uID, dbName, key) || uID.equals(odMgr.lockedBy(dbName, key))) {
             if (odMgr.update(uID, dbName, key, ois.readBytes())) {
               if (odMgr.isAutoCommit(uID) || odMgr.odbRestoreKey(uID, dbName, key, true)) {
                 ios.writeBool(true);
                 logging(uID+" xUpdated key: "+key+" of "+dbName+".");
               } else ios.writeErr("xUpdate/commit "+dbName+" @"+key+" failed.");
               break;
             }
             ios.writeErr("xUpdate "+dbName+" @"+key+" failed.");
             logging(uID+" xUpdated key: "+key+" of "+dbName+", but failed.");
          } else ios.writeErr("xUpdatte: Cannot lock Key "+key);
          ios.writeBool(false);
          break;
        case 22: // commit
          if (odMgr.isAutoCommit(uID)) {
            ios.writeErr(uID+" sets autoCommit. NO need to commit.");
            break;
          }
          key = ois.readToken( );
          if (odMgr.isLocked(uID, dbName, key)) {
            ios.writeBool(odMgr.odbRestoreKey(uID, dbName, key, true));
            logging(uID+" committed key: "+key+" of "+dbName+".");
          } else {
            ios.writeErr(key+" must be locked before COMMIT.");
            logging(uID+" committed key: "+key+" of "+dbName+", but failed.");
          }
          break;
        case 23: // commit all transaction
          ios.writeBool(odMgr.odbRestore(uID, dbName, true));
          logging(uID+" committed all keys "+dbName+".");
          break;
        case 24: // lockedBy(dbName, key)
          String id = odMgr.lockedBy(dbName, ois.readToken( ));
          if (id == null) ios.writeMsg("?");
          else ios.writeMsg(id);
          break;
        case 25:  // ODBCluster - closeAgents & disconnect
          exit();
          return;
        case 26: // set autoCommit ODBConnect
        case 27: // reset autoCommit ODBConnect
          odMgr.autoCommit(uID, cmd == 26);
          logging(uID+" sets autoCommit().");
          ios.writeBool(cmd == 26);
          break;
        case 28: // isKeyFree
          ios.writeBool(odMgr.isKeyFree(uID, dbName, ois.readToken( )));
          break;
        case 29: // add(dbName, key, obj, node)
          String[] keys = ois.readToken( ).split("+");
          if (odMgr.isExisted(uID, dbName, keys[0])) {
            ios.writeErr(keys[0]+" exists.");
            break;
          }
          ODBCluster odbc = odMgr.getAgent(keys[1]);
          if (odbc == null) {
            ios.writeErr("Unknown "+keys[1]);
            logging("Unknown node "+keys[1]+". Add failed.");
          } else {
            odbc.add('+'+uID+'|'+dbName, keys[0], ois.readBytes());
            logging("Add Object to node "+keys[1]);
          }
          break;
        case 30: // keysOwner(dbName) - ODBCluster
          ios.writeList(odMgr.lockedKeyList(uID, dbName));
          break;
        case 31: // changePassword(oldPW, newPW) Client Change Password
          ios.writeBool((new UserList(parms.userlist)).changePassword(uid,
                        EnDecrypt.decrypt(dbName),
                        EnDecrypt.decrypt(ois.readToken( ))));
          break;
        //------------------------extended DataMining----------------------------
        case 32: // allClassNames(String dbName)
          kLst = odMgr.getKeys(uID, dbName);
          ArrayList<String> cLst = new ArrayList<>();
          for (String k : kLst) {
            oov.view(odMgr.read(uID, dbName, k));
            String cls = oov.getClassName();
            if (!cLst.contains(cls)) cLst.add(cls);
          }
          ios.writeList(cLst);
          break;
       case 33: // allObjects(String dbName, String clsName)
          key = ois.readToken( ); // class name
          kLst = odMgr.getKeys(uID, dbName);
          ArrayList<byte[]> bLst = new ArrayList<>();
          for (String k : kLst) {
            byte[] obj = odMgr.read(uID, dbName, k);
            oov.view(obj); // load the content for view
            if (oov.getClassName().equals(key)) bLst.add(obj);
          }
          ios.writeObjList(bLst);
          break; 
        case 34: // selectAll(String dbName, String vName, String pat)
          key = ois.readToken().trim(); // vName
          ArrayList<byte[]> sLst = new ArrayList<>();
          String pat = new String(ois.readBytes(), parms.charset).trim();
          kLst = odMgr.getKeys(uID, dbName);
          boolean ok = true;
          for (String k : kLst) {
            byte[] obj = odMgr.read(uID, dbName, k);
            oov.view(obj); // load the content for view
            List<String> fNames = oov.getFieldNames();
            if ("*".equals(key)) {               
              for (String name:fNames) {
                Object fv = oov.getFieldValue(name);
                if (fv instanceof String && isFound((String)fv, pat)) sLst.add(obj);
              }
            } else {
              if (!fNames.contains(key)) break;
              if (isFound((String)oov.getFieldValue(key), pat)) sLst.add(obj);
            }
          }
          ios.writeObjList(sLst);
          break;
        case 35: // SQL(String dbName, String sql)
          /*
           SQL-like select
            0      1    2  3     4   5     6  7     8  9     10 11    ...
           select var_1 eq val_1 and var_2 gt val_2 or var_3 le val_3 ...
          */
          kLst = odMgr.getKeys(uID, dbName);
          String sql = ois.readToken().trim();
          ArrayList<byte[]> qLst = new ArrayList<>();
          String tmp[] = sql.trim().split("[ ]+"); // ignore space and discard ""
          try {
            for (String k : kLst) {
              byte[] bb = odMgr.read(uID, dbName, k);
              oov.view(bb); // load Object for viewing
              boolean OK = onSQL(oov, tmp[1], tmp[2], tmp[3]);
              for (int i = 4; i < tmp.length; i += 4) { // evaluation from left to right
                boolean o1 = onSQL(oov, tmp[i+1], tmp[i+2], tmp[i+3]);
                if ("or".equalsIgnoreCase(tmp[i])) {
                  if (!OK) OK = o1;
                } else { // and
                  OK = OK && o1;
                }
              }
              if (OK) qLst.add(bb);
            }
          } catch (Exception es) {
            qLst.clear();
          }
          ios.writeObjList(qLst);
          break;
        case 36: // selectAll(vName, "GT", 123456) or selectAll(vName, "GT", 123.456)
          key = ois.readToken().trim();
          kLst = odMgr.getKeys(uID, dbName);
          ArrayList<byte[]> aLst = new ArrayList<>();
          String[] val = new String(ois.readBytes(), parms.charset).trim().split(""+(char)0x00);
          //
          for (String k : kLst) {
            byte[] ba = odMgr.read(uID, dbName, k);
            oov.view(ba); // load ba for viewing
            List<String> fNames = oov.getFieldNames();
            for (String fn : fNames) if (!oov.isArray(fn) && ("*".equals(key) || fn.equals(key))) {
              if (compValue(oov.getFieldValue(fn), val[0], val[1])) {
                aLst.add(ba);
                break;
              }
            }
          }
          ios.writeObjList(aLst);
          break;
        case 37: // getClassName(dbName, key)
          oov.view(odMgr.read(uID, dbName, ois.readToken()));
          ios.writeMsg(oov.getClassName());
          break;
        case 38: // allObjects(dbName)
          kLst = odMgr.getKeys(uID, dbName);
          ArrayList<byte[]> fLst = new ArrayList<>();
          for (String k:kLst) fLst.add(odMgr.read(uID, dbName, k));
          ios.writeObjList(fLst);
          break;
        //----------------------------------SuperUser---------------------------
        case 97: // joinNode - Cluster (invoked via ODBCluster)
          odMgr.joinNode(dbName);
          parms.BC.broadcast(10, dbName, parms.nodes);
          return;
        case 98: // removeClient - Cluster
          odMgr.removeClient(uID);
          logging("Client "+uID+" is removed.");
          break;
        case 99: // ping()
          soc.write(ByteBuffer.wrap((""+System.currentTimeMillis()).getBytes()));
          exit();
          return;
        default:
          ios.writeErr("Unknown Request:"+cmd);
        }
        // send back the result
        ios.write(soc);
      } catch (Exception e) {
        //e.printStackTrace();
        try {
          if (!parms.active) {
            kLst = odMgr.userDBList(uID);
            if(kLst != null && kLst.size() > 0) {
              for (String dbN:kLst) odMgr.unlock(uID, dbN);
            }
            logging("ODBServer was down or shutdown by Superuser.");
            ios.writeErr("ODBServer was down or shutdown by Superuser...");
          } else ios.writeErr(e.toString());
          // send back the Exception message
          ios.write(soc);
        } catch (Exception ex) { }
      }
    }
    exit();
  }
  //
  public void exit() {
    try {
      soc.shutdownInput();
      soc.shutdownOutput();
      soc.close();
    } catch (Exception e) { }
  }
  // only ONE * is allowed, but more ? are possible
  private boolean isFound(String vName, String pat) {
    // ??... or * are in pat
    int p = pat.indexOf("?");
    int q = pat.indexOf("*");
    if (p < 0 && q < 0) return vName.equals(pat);
    StringBuilder sb = new StringBuilder(vName);
    int ple = pat.length();
    while (p >= 0 && p < ple) {
      sb.replace(p, p+1,"?");
      p = pat.indexOf("?", p+1);
    }
    vName = sb.toString();
    if (q == 0) { // * or *abc
      if (ple == 1) return true;
      return vName.endsWith(pat.substring(q+1));
    }
    if (q > 0) { // abc* or ab*c
      String fro = pat.substring(0, q);
      if (q == (ple-1)) return vName.startsWith(fro);
      return vName.startsWith(fro) && vName.endsWith(pat.substring(q+1));
    }
    return vName.equals(pat);
  } 
  //  
  private boolean onSQL(ODBObjectView oov, String vName, String comp, String pat) {
    String cmp = comp.toUpperCase();
    List<String> fNames = oov.getFieldNames();
    for (String fn : fNames) if (!oov.isArray(fn) && fn.equals(vName)) {
      Object o = oov.getFieldValue(fn);
      if (o instanceof String) 
        return "EQ".equals(cmp)? isFound((String)o, pat):false;
      return compValue(o, cmp, pat);
    }
    return false;
  }
  // possible exception: nummeric malformat
  private boolean compValue(Object o, String cmp, String val) {
    double d = 0;
    if (o instanceof Double)      d = (Double)o;
    else if (o instanceof Long)   d = (double)(((Long)o).longValue());
    else if (o instanceof Integer)d = (double)(((Integer)o).intValue());
    else if (o instanceof Float)  d = (double)(((Float)o).floatValue());
    else if (o instanceof Short)  d = (double)(((Short)o).shortValue());
    else { // object is BigInteger or BigDecimal
      int r = 0;
      if (o instanceof BigDecimal) 
        r = ((BigDecimal)o).compareTo(new BigDecimal(new BigInteger(val)));
      else if (o instanceof BigInteger)
        r = ((BigInteger)o).compareTo(new BigInteger(val));
      else return false; // wrong object
      if ("LT".equals(cmp)) return r <  0;
      if ("LE".equals(cmp)) return r <= 0;
      if ("EQ".equals(cmp)) return r == 0;
      if ("GE".equals(cmp)) return r >= 0;
      return r > 0; // GT
    }
    double v = Double.parseDouble(val);
    if ("LT".equals(cmp)) return d <  v;
    if ("LE".equals(cmp)) return d <= v;
    if ("EQ".equals(cmp)) return d == v;
    if ("GE".equals(cmp)) return d >= v;
    return d > v; // GT
  }
  //
  private synchronized void logging(String msg) {
    if (parms.log) try {
      parms.logger.write((msg+System.lineSeparator()).getBytes());
      parms.logger.flush();
    } catch (Exception ex) {
      System.err.println("Unable to log \""+msg+"\"");
    }
  }
  // User authentication for ODBConnect
  private boolean authenticate(String encrypt) throws Exception {
    if (uID != null) return true; // already authenticated
    if (!parms.userlist.endsWith("userlist")) return false;
    //
    UserList uList = new UserList(parms.userlist);
    String u = EnDecrypt.decrypt(encrypt.substring(0, encrypt.indexOf("@")));
    if ("system:admin".equals(u)) return false; // no default admin/system for oodb
    int idx = u.indexOf(":");
    String pw  = u.substring(0, idx);
    uid = u.substring(idx+1);
    if (uList.isUser(pw, uid)) {
      priv = uList.getPrivilege(pw, uid);
      uID = uid+"@"+String.format("%X", System.nanoTime());
      logging("User "+uid+" successfully signs in. Assigned ID: "+uID);
      return true;
    }
    logging("User "+uid+" is unknown. No Connection.");
    return false;
  }
  // -------------------------------------------------------------------------------
  private SocketChannel soc;
  private String uID, uid;
  private ODBParms parms;
  private int priv = 0;
}
