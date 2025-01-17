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
import java.nio.charset.Charset;
/**
Object Database Worker
@author Joe T. Schwarz (c)
*/
public class ODBWorker implements Runnable {
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
    ArrayList<String> notify = new ArrayList<>();
    // loop until JODB server exits
    while (parms.loop) {
      try {
        ios.read(soc); // read the content from soc to ios
        ois = new ODBInputStream(ios.toByteArray());
        //
        int cmd = ois.read(); // Access Command
        String dbName = ois.readToken( );
        if (dbName.charAt(0) == '+') {
          int le = dbName.indexOf("|")+1;
          uID = dbName.substring(0, le);
          dbName = dbName.substring(le);
        }
        String charset = odMgr.getCharset(dbName);
        if (charset != null) {
          oov.setCharset(charset);
          ios.setCharset(charset);
        }
        ios.preset(); // a MUST for formated WRITE/READ
        // check for the request
        switch (cmd) {
        case 0: // ODBConnect
          key = authenticate(dbName);
          if (key == null) {
            odMgr.uIDList.add(uID);
            odMgr.workers.add(this);
            ios.writeMsg(priv+"/"+uID+"/"+parms.broadcaster);
            if (uID.charAt(0) != '+') parms.logging(uID+" is connected."); // abs. Path
            ios.writeBool(true);
            break;
          }
          // illegal user
          ios.writeBool(false);
          ios.writeErr(key);
          ios.write(soc);
          exit(); // quit
          return;
        case 1: // ODBCluster's connect
          priv = 2;
          odMgr.workers.add(this);
          break;
        case 2: // disconnect ODBConnect
          try {
            if (priv > 0) odMgr.removeClient(uID);
            else odMgr.close(uID);
            // free all bound dbAgents
            odMgr.unbindAgent("+"+uID+"|");
            ios.write(soc);
          } catch (Exception ex) { }
          odMgr.uIDList.remove(uID);
          odMgr.workers.remove(this);
          if (uID.charAt(0) != '+') parms.logging(uid+" disconnected.");
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
          if (uID.charAt(0) != '+')
            parms.logging("Object with key: "+key+" added to "+dbName+" by "+uID);
          if (notify.contains(dbName)) 
            parms.BC.broadcast(11, uID+"|"+dbName, Arrays.asList(key+" is added by "+uID));
          break;
        case 7: // unlock(dbName) all keys
          ios.writeBool(odMgr.unlock(uID, dbName));
          if (uID.charAt(0) != '+')
            parms.logging("Unlocked all keys of "+dbName+" by "+uID);
          break;
        case 8: // update(dbName, key, obj)
          key = ois.readToken( ); // key
          if (odMgr.update(uID, dbName, key, ois.readBytes())) {
            ios.writeBool(true);
            if (uID.charAt(0) != '+')
              parms.logging("Object with key: "+key+" of "+dbName+" updated by "+uID);
            if (notify.contains(dbName)) 
              parms.BC.broadcast(11, uID+"|"+dbName, Arrays.asList(key+" is updated by "+uID));
          } else {
            ios.writeBool(false);
            if (uID.charAt(0) != '+')
              parms.logging(uID+" updated "+dbName+" failed: "+key+" is unknown or unlocked");
          }
          break;
        case 9: // delete(dbName, key)
          key = ois.readToken( ); // key
          if (odMgr.delete(uID, dbName, key)) {
            ios.writeBool(true);
            if (uID.charAt(0) != '+') 
              parms.logging("Object with key: "+key+" of "+dbName+" deleted by "+uID);
            if (notify.contains(dbName))
              parms.BC.broadcast(11, uID+"|"+dbName, Arrays.asList(key+" is deleted by "+uID));
          } else {
            ios.writeBool(false);
            if (uID.charAt(0) != '+')
              parms.logging(uID+" deleted "+dbName+" failed: "+key+" is unknown or unlocked");
          }
          break;
        case 10: // read(dbName, key). Regardless of locked or unlocked
          key = ois.readToken( );
          ios.writeObj(odMgr.read(uID, dbName, key));
          if (uID.charAt(0) != '+')
            parms.logging("Object with key: "+key+" of "+dbName+" read by "+uID);
          break;
        case 11: // isExisted(dbName, key)
          ios.writeBool(odMgr.isExisted(uID, dbName, ois.readToken( )));
          break;
        case 12: // isLocked(dbName, key)
          key = ois.readToken( );
          ios.writeBool(odMgr.isLocked(uID, dbName, key));
          break;
        case 13: // lock(dbName, key)
          key = ois.readToken( );
          ios.writeBool(odMgr.lock(uID, dbName, key));
          if (uID.charAt(0) != '+')
            parms.logging("Key: "+key+" of "+dbName+" locked by "+uID);
          break;
        case 14: // unlock(dbName, key)
          key = ois.readToken( );
          ios.writeBool(odMgr.unlock(uID, dbName, key));
          if (uID.charAt(0) != '+')
            parms.logging("Key: "+key+" of "+dbName+" unlocked by "+uID);
          break;
        case 15: // rollback(dbName) or rollback(dbName, key)
          if (odMgr.isAutoCommit(uID)) {
            ios.writeErr(uID+" sets autoCommit: NO rollback.");
            break;
          }
          if (ois.remainderLength() > 0) {
            key = ois.readToken( ); // key
            if (odMgr.isLocked(uID, dbName, key)) {
              ios.writeBool(odMgr.restoreKey(uID, dbName, key, false));
              if (uID.charAt(0) != '+')
                parms.logging("Key: "+key+" of "+dbName+" rollbacked by "+uID);
            } else ios.writeErr(key+" must be locked before rollback.");
          } else {
            ios.writeBool(odMgr.restoreKeys(uID, dbName, false));
            if (uID.charAt(0) != '+')
              parms.logging("All keys of "+dbName+" rollbacked by "+uID);
          }
          break;
        case 16: // ODBCluster - connect(dbName, charset) - connect only if ODB exists
          if ((new File(parms.db_path+dbName)).exists()) odMgr.connect(uID, dbName, ois.readToken());
          break;
        case 17: // ODBConnect - connect(dbName, charset)
          boolean existed = (new File(parms.db_path+dbName)).exists();
          if (priv < 1 && !existed) {
            ios.writeErr(dbName+" does not exist and "+uid+" lacks of RW-Privilege.");
            if (uID.charAt(0) != '+')
              parms.logging(uID+" tried to connect "+dbName+", but failed. Reason: privilege:"+priv);
            break;
          }
          if (priv > 0 || existed) {
            String cs = ois.readToken();
            odMgr.connect(uID, dbName, cs); // setup dbAgents on cluster
            if (!parms.remList.contains(parms.webHostName)) odMgr.bindAgent("+"+uID+"|"+dbName, cs);
            if (uID.charAt(0) != '+') parms.logging(dbName+" (charset:"+cs+") connected with "+uID);
          } else ios.writeErr(dbName+" does not exist. Or "+uid+" lacks of RW-Privilege.");
          break;
        case 18: // close(dbName)
          odMgr.close(uID, dbName);
          if (uID.charAt(0) != '+') parms.logging(dbName+" closed by "+uID);
          break;
        case 19: // save(dbName)
          odMgr.save(uID, dbName);
          if (uID.charAt(0) != '+') parms.logging(dbName+" saved by"+uID);
          break;
        case 20: // xDelete
          key = ois.readToken( ); // key
          if (odMgr.lock(uID, dbName, key) || uID.equals(odMgr.lockedBy(dbName, key))) {
            if (odMgr.delete(uID, dbName, key)) {
              if (odMgr.isAutoCommit(uID) || odMgr.restoreKey(uID, dbName, key, true)) {
                ios.writeBool(true);
                if (uID.charAt(0) != '+')
                  parms.logging("xDeleted key: "+key+" of "+dbName+" by "+uID);
                if (notify.contains(dbName)) 
                  parms.BC.broadcast(11, uID+"|"+dbName, Arrays.asList(key+" is deleted by "+uID));
              } else ios.writeErr("xDelete/commit "+dbName+" @"+key+" failed.");
              break;
            }
            ios.writeErr("xDelete "+dbName+" @"+key+" failed.");
            if (uID.charAt(0) != '+')
              parms.logging("xDeleted key: "+key+" of "+dbName+" by "+uID+", but failed.");
          } else ios.writeErr("xDelete: Cannot lock Key "+key);
          odMgr.unlock(uID, dbName, key);
          ios.writeBool(false);
          break;
        case 21: // xUpdate
          key = ois.readToken( ); // key
          if (odMgr.lock(uID, dbName, key) || uID.equals(odMgr.lockedBy(dbName, key))) {
             if (odMgr.update(uID, dbName, key, ois.readBytes())) {
               if (odMgr.isAutoCommit(uID) || odMgr.restoreKey(uID, dbName, key, true)) {
                 ios.writeBool(true);
                 if (uID.charAt(0) != '+')
                   parms.logging("xUpdated key: "+key+" of "+dbName+" by "+uID);
                  if (notify.contains(dbName)) 
                    parms.BC.broadcast(11, uID+"|"+dbName, Arrays.asList(key+" is updated by "+uID));
               } else ios.writeErr("xUpdate/commit "+dbName+" @"+key+" failed.");
               break;
             }
             ios.writeErr("xUpdate "+dbName+" @"+key+" failed.");
             if (uID.charAt(0) != '+') 
               parms.logging("xUpdated key: "+key+" of "+dbName+" by "+uID+", but failed.");
          } else ios.writeErr("xUpdate: Cannot lock Key "+key);
          odMgr.unlock(uID, dbName, key);
          ios.writeBool(false);
          break;
        case 22: // commit
          if (odMgr.isAutoCommit(uID)) break;
          //
          key = ois.readToken( );
          if (odMgr.isLocked(uID, dbName, key)) {
            ios.writeBool(odMgr.restoreKey(uID, dbName, key, true));
            if (uID.charAt(0) != '+') 
              parms.logging("Key: "+key+" of "+dbName+" committed by "+uID);
          } else {
            ios.writeErr(key+" must be locked before COMMIT.");
            if (uID.charAt(0) != '+')
              parms.logging("Key: "+key+" of "+dbName+" by "+uID+", but failed.");
          }
          break;
        case 23: // commit all transaction
          ios.writeBool(odMgr.restoreKeys(uID, dbName, true));
          if (uID.charAt(0) != '+')
            parms.logging("All keys "+dbName+" committed by "+uID);
          break;
        case 24: // lockedBy(dbName, key)
          String id = odMgr.lockedBy(dbName, ois.readToken( ));
          if (id == null) ios.writeMsg("?");
          else ios.writeMsg(id);
          break;
        case 25:  // ODBCluster - closeAgents & disconnect
          odMgr.removeAgent(ois.readToken());
          odMgr.workers.remove(this);
          exit();
          return;
        case 26: // set autoCommit ODBConnect
        case 27: // reset autoCommit ODBConnect
          odMgr.autoCommit(uID, cmd == 26);
          if (uID.charAt(0) != '+') 
            parms.logging("autoCommit("+(cmd == 26?"true":"false")+") set by "+uID);
          ios.writeBool(cmd == 26);
          break;
        case 28: // isKeyFree
          ios.writeBool(odMgr.isKeyFree(uID, dbName, ois.readToken( )));
          break;
        case 29: // add(dbName, key, obj, node)
          //String[] keys = ois.readToken( ).split("+");
          String[] keys = ODBParser.split(ois.readToken(), "+");
          if (odMgr.isExisted(uID, dbName, keys[0])) {
            ios.writeErr(keys[0]+" exists.");
            break;
          }
          ODBCluster odbc = odMgr.getAgent(keys[1]);
          if (odbc == null) {
            ios.writeErr("Unknown "+keys[1]);
            if (uID.charAt(0) != '+')
              parms.logging("Unknown node "+keys[1]+". Add failed.");
          } else {
            odbc.add("+"+uID+"|"+dbName, keys[0], ois.readBytes());
            if (uID.charAt(0) != '+') parms.logging("Add Object to node "+keys[1]);
          }
          break;
        case 30: // keysOwner(dbName) - ODBCluster
          ios.writeList(odMgr.lockedKeyList(uID, dbName));
          break;
        case 31: // changePassword(oldPW, newPW) Client Change Password
          UserList uList = new UserList(parms.userlist);
          ios.writeBool(uList.changePassword(uid,
                        EnDecrypt.decrypt(dbName),
                        EnDecrypt.decrypt(ois.readToken( ))));
          uList.save();
          if (uID.charAt(0) != '+') parms.logging("Password changed by "+uID);
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
          key = ois.readToken(); // vName
          ArrayList<byte[]> sLst = new ArrayList<>();
          String pat = charset != null? new String(ois.readBytes(), charset):new String(ois.readBytes());
          kLst = odMgr.getKeys(uID, dbName);
          for (String k : kLst) {
            byte[] obj = odMgr.read(uID, dbName, k);
            if (oov.viewVar(obj, key, "EQ", pat)) sLst.add(obj);             
          }
          ios.writeObjList(sLst);
          if (uID.charAt(0) != '+')
            parms.logging("select("+dbName+","+key+","+pat+") by "+uID);
          break;
        case 35: // SQL(String dbName, String sql)
          /*
           SQL-like select
            0      1    2  3     4   5     6  7     8  9     10 11    ...
           select var_1 eq val_1 and var_2 gt val_2 or var_3 le val_3 ...
          */
          kLst = odMgr.getKeys(uID, dbName);
          String sql = ois.readToken();
          String tmp[] = ODBParser.split(sql, " "); // faster
          ArrayList<byte[]> qLst = new ArrayList<>();
          //String tmp[] = sql.trim().split("[ ]+"); // ignore space and discard ""
          try {
            for (String k : kLst) {
              byte[] bb = odMgr.read(uID, dbName, k);
              boolean OK = oov.viewVar(bb, tmp[1], tmp[2], tmp[3]);
              for (int i = 4; i < tmp.length; i += 4) { // evaluation from left to right
               if ("or".equalsIgnoreCase(tmp[i])) {
                  if (!OK) OK = oov.viewVar(null, tmp[i+1], tmp[i+2], tmp[i+3]);
                } else { // and
                  OK = OK && oov.viewVar(null, tmp[i+1], tmp[i+2], tmp[i+3]);
                }
              }
              if (OK) qLst.add(bb);
            }
          } catch (Exception es) {
            qLst.clear();
          }
          ios.writeObjList(qLst);
          if (uID.charAt(0) != '+')
            parms.logging("sql("+dbName+","+sql+") by "+uID);
          break;
        case 36: // selectAll(vName, "GT", 123456) or selectAll(vName, "GT", 123.456)
          key = ois.readToken();
          kLst = odMgr.getKeys(uID, dbName);
          ArrayList<byte[]> aLst = new ArrayList<>();
          String sa = charset != null? new String(ois.readBytes(), charset):new String(ois.readBytes());
          //String[] val = sa.split(""+(char)0x00);
          String[] val = ODBParser.split(sa, ""+(char)0x00);
          for (String k : kLst) {
            byte[] ba = odMgr.read(uID, dbName, k);
            if (oov.viewVar(ba, key, val[0], val[1])) aLst.add(ba);
          }
          if (uID.charAt(0) != '+') 
            parms.logging("select("+dbName+","+key+" "+val[0]+" "+val[1]+") by "+uID);
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
        case 39: // notify(dbName, enabled) add/delete/update notify
          boolean enabled = ois.readToken().charAt(0) == '1';
          if (enabled) { // enable
            if (!notify.contains(dbName)) notify.add(dbName);
          } else notify.remove(dbName); // disable
          if (uID.charAt(0) != '+') 
            parms.logging("notify("+(enabled?"true":"false")+") set by "+uID);
          break;
        case 40:
          parms.BC.broadcast(12, parms.webHostName, Arrays.asList(dbName));
          break;
        //----------------------------------SuperUser---------------------------
        case 96: // restoreKey(dbName, key, mode)
          key = ois.readToken( ); // key
          ios.writeBool(odMgr.restoreKey(uID, dbName, key, (new String(ois.readBytes())).equals("true")));
          break;
        case 97: // joinNode - Cluster (invoked via ODBCluster)
          odMgr.joinNode(dbName);
          parms.BC.broadcast(10, dbName, Arrays.asList(ois.readToken( )));
          break;
        case 98: // removeClient - Cluster
          odMgr.removeClient(uID);
          break;
        case 99: // ping()
          soc.write(ByteBuffer.wrap((""+System.currentTimeMillis()).getBytes()));
          exit();
          return;
        default:
          ios.writeErr("Unknown Request:"+cmd);
          if (uID.charAt(0) != '+') parms.logging("Unknown Request:"+cmd);
        }
        // send back the result
        ios.write(soc);
      } catch (Exception e) {
        //e.printStackTrace();
        try {
          if (soc == null) {
            kLst = odMgr.userDBList(uID);
            if(kLst != null && kLst.size() > 0) {
              for (String dbN:kLst) odMgr.unlock(uID, dbN);
            }
            if (uID.charAt(0) != '+') parms.logging("ODBServer was down or shutdown by Superuser.");
            ios.writeErr("ODBServer was down or shutdown by Superuser...");
          } else ios.writeErr(e.toString());
          // send back the Exception message
          ios.write(soc);
        } catch (Exception ex) { }
      }
    }
  }
  //
  public void exit() {
    if (soc != null) try {
      soc.shutdownInput();
      soc.shutdownOutput();
      soc.close();
      soc = null;
    } catch (Exception e) { }
  }
  // User authentication for ODBConnect
  private String authenticate(String encrypt) throws Exception {
    if (uID != null) return null; // already authenticated
    if (!parms.userlist.endsWith("userlist")) return "Missing \"userlist\".";
    //
    UserList uList = new UserList(parms.userlist);
    String u = EnDecrypt.decrypt(encrypt.substring(0, encrypt.indexOf("@")));
    if ("system:admin".equals(u)) return "\"admin\" is not allowed here.";
    int idx = u.indexOf(":");
    String pw  = u.substring(0, idx);
    uid = u.substring(idx+1);
    if (uList.isUser(pw, uid)) {
      priv = uList.getPrivilege(pw, uid);
      uID = uid+"@"+String.format("%X", System.nanoTime());
      parms.logging("User "+uid+" successfully signs in. Assigned ID: "+uID);
      return null;
    }
    parms.logging("User "+uid+" is unknown.");
    return "\""+uid+"\" is unknown.";
  }
  // -------------------------------------------------------------------------------
  private SocketChannel soc;
  private String uID, uid;
  private ODBParms parms;
  private int priv = 0;
}
