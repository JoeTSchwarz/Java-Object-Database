package joeapp.odb;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.nio.charset.Charset;
import java.nio.channels.FileLock;
/**
Nano Database. Instantiated and maintained by ODBManager
<br>ODB consists of 2 areas: key area and record area
<br>- Key area has 3 fields: KeyLength, record length, key itself
<br>- Data area contains the records which can be serialized object or String
<br>- Data must be committed before closed or saved. Otherwise data will be lost.
@author Joe T. Schwarz (c)
*/
public class NanoDB {
  /**
  constructor
  @param fName  String, file name
  @exception Exception thrown by JAVA
  */
  public NanoDB(String fName) {
    this.fName = fName;
    cs = Charset.forName("UTF-8");
  }
  /**
  constructor
  @param fName  String, file name
  @param limit  int, max. size to be cached
  @exception Exception thrown by JAVA
  */
  public NanoDB(String fName, int limit) {
    this.fName = fName;
    this.limit = limit;
    cs = Charset.forName("UTF-8");
  }
  /**
  constructor
  @param fName  String, file name
  @param limit  int, max. size to be cached
  @param charsetName String, character set name (e.g. "UTF-8");
  @exception Exception thrown by JAVA
  */
  public NanoDB(String fName, String charsetName, int limit) {
    this.fName = fName;
    this.limit = limit;
    cs = Charset.forName(charsetName);
  }
  /**
  getKeys() returns an ArrayList of all ODB keys
  @return ArrayList of strings (as keys) or an empty arraylist if NanoDB must be created
  */
  public ArrayList<String> getKeys() {
    if (keysList.size() == 0) return new ArrayList<String>();
    return new ArrayList<>(keysList);
  }
  /**
  isExisted. Excl. deleted key
  @param key String key
  @return boolean true if querried key exists (deleted keys won't count)
  */
  public boolean isExisted(String key) {
    return keysList.contains(key) && !oCache.containsKey(key);
  }
  /**
  isKeyDeleted
  @param key String key
  @return boolean true if querried key is deleted
  */
  public boolean isKeyDeleted(String key) {
    return oCache.containsKey(key) && !keysList.contains(key);
  }
  /**
  readObject
  @param key String
  @return byte array which can be an array of (non)serialialized object
  @exception Exception thrown by JAVA
  */
  // check for existed key is done by ODBManager
  public byte[] readObject(String key) throws Exception {
    if (cache.containsKey(key)) return cache.get(key);
    byte[] buf = new byte[sizes.get(key)];
    raf.seek(pointers.get(key));
    raf.read(buf);
    return buf;
  }
  /**
  addObject
  @param key String
  @param buf byte array of a (non)serialized object in byte array
  */
  // check for existed key is done by ODBManager
  public void addObject(String key, byte[] buf) {
    oCache.put(key, new byte[] {});
    cache.put(key, buf);
    keysList.add(key);
  }
  /**
  deleteObject
  @param key String
  @return boolean true if success
  */
  public boolean deleteObject(String key) {
    if (!oCache.containsKey(key)) try {
      if (cache.containsKey(key)) oCache.put(key, cache.remove(key));
      else {
        byte[] buf = new byte[sizes.get(key)];
        raf.seek(pointers.get(key));
        raf.read(buf);
        oCache.put(key, buf);
      }
      keysList.remove(key);
      return true;
    } catch (Exception e) { }
    return false;
  }
  /**
  updateObject
  @param key String
  @param buf byte array of a (non)serialized object in byte array
  @return boolean true if success
  */
  public boolean updateObject(String key, byte[] buf) {
    if (!oCache.containsKey(key)) try {
      if (cache.containsKey(key)) oCache.put(key, cache.get(key));
      else {
        byte[] bb = new byte[sizes.get(key)];
        raf.seek(pointers.get(key));
        raf.read(bb);
        oCache.put(key, bb);
      }
      cache.put(key, buf);
      return true;
    } catch (Exception ex) { }
    return false;
  }
  /**
  commit transaction of the given key. Locked key will be remove
  @param key String
  @return boolean true if successful
  */
  public boolean commit(String key) {
    if (oCache.remove(key) == null) return false;
    committed = true;
    return true;
  }
  /**
  commit all transaction on this ODB
  @param keys String list of committing keys
  */
  public void commit(List<String> keys) {
    int i = oCache.size();
    for (String key : keys) oCache.remove(key);
    if (i > oCache.size()) committed = true;
  }
  /** 
  rollback() rollbacks the LAST modified/added action
  @param key String, key of object to be rollbacked
  @return boolean true if rollback is successful, false: unknown key, no rollback
  */
  public boolean rollback(String key) {
    if (oCache.size() == 0) return false;
    byte[] bb = oCache.remove(key);
    if (bb == null) return false;
    if (bb.length == 0) { // add
      keysList.remove(key);
      cache.remove(key);
      return false;
    }
    // delete or update
    if (!keysList.contains(key)) keysList.add(key);
    cache.put(key, bb);
    return true;
  }
  /** 
  rollback() rollbacks all the LAST modified/added actions
  @param kLst String arraylist of keys of object to be rollbacked
  */
  public void rollback(List<String> kLst) {
    if (oCache.size() == 0) return;
    for (String key : kLst) rollback(key);
  }
  /**
  open NanoDB
  <br>If the specified fName from Constructor does not exist, it will be created with the fName.
  */
  public void open() throws Exception {
    if (raf != null) throw new Exception(fName+" is opened.");
    cache.clear();
    oCache.clear();
    keysList = Collections.synchronizedList(new ArrayList<String>(512));
    pointers = new ConcurrentHashMap<>(512);
    sizes = new ConcurrentHashMap<>(512);
    // load keysList, pointers and sizes
    existed = (new File(fName)).exists();
    raf = new RandomAccessFile(fName, "rw");
    fLocked = raf.getChannel().lock();
    if (!existed) return; // new NanoDB
    // cached only if size < 2MB (2097152)
    cached = raf.length() < limit;
    //
    long pt  = (long)raf.readInt();
    byte[] all = new byte[(int)(pt - 4)];
    raf.read(all); // get the KeysList block
    // keys block-format: keyLength + dataLength + key = 2+4+n bytes
    for (int kl, dl, d, i = 0; i < all.length; i += (6+kl)) {
      // compute key Length and Data leng
      kl = (((int)all[i]   & 0xFF) * 0x100) | ((int)all[i+1] & 0xFF);
      dl = (((int)all[i+2] & 0xFF) * 0x1000000)|(((int)all[i+3] & 0xFF) * 0x10000)|
           (((int)all[i+4] & 0xFF) * 0x100) | ((int)all[i+5] & 0xFF);
      // cache keysList and pointers and sizes
      String key = new String(all, i+6, kl, cs);
      if (cached) { // cached
        byte[] bb = new byte[dl];
        raf.read(bb); // read
        cache.put(key, bb);
      }
      pointers.put(key, pt);
      sizes.put(key, dl);
      keysList.add(key);
      pt += dl;
    }
  }
  /**
  close and save ODB
  <br>If not autoCommit, all changes must be committed before close.
  <br>Otherwise all changes will be lost (empty NanoDB file if it must be created)
  */
  public void close() {
    try {
      save();
      fLocked.release();
      raf.close();
    } catch (Exception ex) { }
    keysList.clear();
    pointers.clear();
    sizes.clear();
    cache.clear();
    raf = null;
  }
  /**
  save NanoDB without close -see close()
  @exception Exception thrown by JAVA
  */
  public void save() throws Exception {    
    if (committed) {
      if (committed && oCache.size() > 0) { // recover the UNcommitted
        List<String> keys = new ArrayList<>(oCache.keySet());
        for (String key:keys) { // recover
          byte[] bb = oCache.remove(key);
          if (bb.length > 0) { // It's update
            if (cache.containsKey(key)) cache.replace(key, bb);
            else { // It's delete key
              keysList.add(key);
              cache.put(key, bb);
            }
          } else { // It's add
            cache.remove(key);
            keysList.remove(key);
          }
        }
      }
      if (cache.size() > 0) {
        ConcurrentHashMap<String, Long> pts = new ConcurrentHashMap<>(pointers.size());
        ConcurrentHashMap<String, Integer> szs = new ConcurrentHashMap<>(sizes.size());
        String tmp = String.format("%s_tmp", fName);
        if (!existed || cached) {
          tmp = fName;
          szs = sizes;
          pts = pointers;
          fLocked.release(); // release raf
          raf.close(); // data in cache: so delete
          if (cached) (new File(fName)).delete();
        }
        ByteArrayOutputStream bao = new ByteArrayOutputStream(65536);
        RandomAccessFile rTmp = new RandomAccessFile(tmp, "rw");
        FileLock fL = rTmp.getChannel().lock();
        // the key block
        for (String key : keysList) {
          int kl = key.length();
          int dl = cache.containsKey(key)? cache.get(key).length:sizes.get(key);
          bao.write(new byte[] { (byte)(kl / 0x100),
                                 (byte) kl,
                                 (byte)(dl / 0x1000000),
                                 (byte)(dl / 0x10000),
                                 (byte)(dl / 0x100),
                                 (byte) dl
                               }
                   );
          bao.write(key.getBytes(cs));
        }
        bao.flush();
        long pt = 4+bao.size();
        rTmp.write(new byte[] {(byte)((int)pt / 0x1000000),
                               (byte)((int)pt / 0x10000),
                               (byte)((int)pt / 0x100),
                               (byte) (int)pt
                              }
                  );
        rTmp.write(bao.toByteArray());
        bao.close();
        // now the data block
        for (String k : keysList) {
          byte[] bb = cache.get(k);
          if (bb == null) {
            bb = new byte[sizes.get(k)];           
            raf.seek(pointers.get(k));
            raf.read(bb);
          }
          // save data
          rTmp.write(bb, 0, bb.length);
          szs.put(k, bb.length);
          pts.put(k, pt);
        }
        fL.release();
        rTmp.close();
        if (existed && !cached) {
          fLocked.release();
          raf.close();
          File fi = new File(fName);
          fi.delete(); // delete the old
          TimeUnit.MICROSECONDS.sleep(20);
          (new File(tmp)).renameTo(fi);
          pointers = pts;
          sizes = szs;
        } 
        raf = new RandomAccessFile(fName, "rw");
        fLocked = raf.getChannel().lock();
      }
    }
    committed = false;
    existed = true;
    oCache.clear();
  }
  //---------------------------------------------------------------------------------------
  private ConcurrentHashMap<String, byte[]> oCache =  new ConcurrentHashMap<>(256);
  private ConcurrentHashMap<String, byte[]> cache = new ConcurrentHashMap<>(256);
  private volatile boolean committed = false, existed = false, cached = false;
  private ConcurrentHashMap<String, Integer> sizes;
  private ConcurrentHashMap<String, Long> pointers;
  private List<String> keysList;
  private RandomAccessFile raf;
  private int limit = 0x200000;
  private FileLock fLocked;
  private String fName;
  private Charset cs;
}
