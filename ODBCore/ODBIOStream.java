package joeapp.odb;
//
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.math.BigInteger;
//
import java.nio.*;
import java.nio.charset.*;
import java.nio.channels.*;
import java.util.concurrent.*;
/**
 @author Joe T. Schwarz (C)
*/
public class ODBIOStream {
  /**
  Constructor
  */
  public ODBIOStream( ) {
    buf = new byte[MAX];
  }
  /**
  setCharset
  @param csName String Charset name, default: "UTF-8"
  */
  public void setCharset(String csName) {
    this.csName = csName;
  }
  // unformated write (consecutive)
  /**
  write a byte
  @param b int, the last byte to be written
  @exception IOException thrown by JAVA
  */
  public void write(int b) throws IOException {
    if ((pos+1) >= MAX) enlarge(0);
    buf[pos++] = (byte)b;
  }
  /**
  writeShort
  @param i int, write the 2 last bytes of i (high order first)
  */
  public void writeShort(int i) {
    if ((pos+2) >= MAX) enlarge(0);
    buf[pos++] = (byte) (i >> 8);
    buf[pos++] = (byte)  i;
  }
  /**
  writeInt
  @param i int, write the 4 bytes of i (high order first)
  */
  public void writeInt(int i) {
    if ((pos+4) >= MAX) enlarge(0);
    buf[pos++] = (byte) (i >> 24);
    buf[pos++] = (byte) (i >> 16);
    buf[pos++] = (byte) (i >>  8);
    buf[pos++] = (byte)  i;
  }
  /**
  writeString
  @param s String
  @exception Exception thrown by JAVA
  */
  public void writeString(String s) throws Exception {
    //Note: for internal use only. No check
    int le = s.length();
    if ((pos+le) >= MAX) enlarge(le);
    System.arraycopy(s.getBytes(csName), 0, buf, pos, le);
    pos += le;
  }
  /**
  writeToken
  @param name String
  @exception Exception thrown by JAVA
  */
  public void writeToken(String name) throws Exception {
    int le = name.length();
    if ((pos+le) >= MAX) enlarge(le);
    buf[pos++] = (byte)(le>>8);
    buf[pos++] = (byte) le;
    // native arraycopy is faster than for-loop
    System.arraycopy(name.getBytes(csName), 0, buf, pos, le);
    pos += le;
  }  
  /**
  writeKey
  @param key Object of type String, long, BigInteger
  @exception IOException thrown by JAVA
  */
  public void writeKey(Object key) throws Exception {
    String k = null;
    // key Tag: 0x00 for String as key
    if (key instanceof String) {
      if (((String)key).charAt(0) > (char)0x02) k = (char)0x00+((String)key);
      else k = (String)key;
    } else
    // key Tag: 0x01 for long/Long as key
    if (key.getClass().getName().equals("java.lang.Long")) {
      k = (char)0x01+""+(long)key;
    } else
    // key Tag: 0x02 for BigInteger as key
    if (key.getClass().getName().equals("java.math.BigInteger")) {
      k = (char)0x02+((BigInteger)key).toString();
    }
    writeToken(k);
  }
  /**
  write
  @param bb byte array
  @exception IOException thrown by JAVA
  */
  public void write(byte[] bb) throws IOException {
    //Note: for internal use only. No check
    if ((pos+bb.length) >= MAX) enlarge(bb.length);
    System.arraycopy(bb, 0, buf, pos, bb.length);
    pos += bb.length;
  }
  /**
  write
  @param bb byte array
  @param off int, starting offset
  @param le int, the number of bytes
  @exception IOException thrown by JAVA
  */
  public void write(byte[] bb, int off, int le) throws IOException {
    //Note: for internal use only. No check
    if ((off+le) > bb.length) le = bb.length-off;
    if ((pos+le) >= MAX) enlarge(le);
    System.arraycopy(bb, off, buf, pos, le);
    pos += le;
  }
  /**
  write a serialized Object
  @param obj Object
  @exception IOException thrown by JAVA
  */
  public void write(Object obj) throws IOException {
    if (obj instanceof byte[]) { // write(byte[])
      if ((pos+((byte[]) obj).length) >= MAX) enlarge(((byte[]) obj).length);
      System.arraycopy((byte[]) obj, 0, buf, pos, ((byte[]) obj).length);
      pos += ((byte[]) obj).length;
      return;
    }
    if (obj instanceof String) { // write(String)
      byte[] bb = ((String)obj).getBytes(csName);
      if ((pos+bb.length) >= MAX) enlarge(bb.length);
      System.arraycopy(bb, 0, buf, pos, bb.length);
      pos += bb.length;
      return;
    }
    ByteArrayOutputStream bao = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bao);
    oos.writeObject(obj);
    oos.flush();
    //
    int le = bao.size();
    if ((pos+le) >= MAX) enlarge(le);
    System.arraycopy(bao.toByteArray(), 0, buf, pos, le);
    pos += le;
    oos.close();
  }
  /**
  write to SocketChannel
  @param soc SocketChannel
  @exception IOException thrown by JAVA
  */
  public void write(SocketChannel soc) throws IOException {
    if (pos < 1024) soc.write(ByteBuffer.wrap(buf, 0, pos));
    else { // too big GZIP the content before send
      ByteArrayOutputStream bao = new ByteArrayOutputStream(pos);
      GZIPOutputStream go = new GZIPOutputStream(bao, true);
      go.write(buf, 0, pos);
      go.flush();
      go.close();
      soc.write(ByteBuffer.wrap(bao.toByteArray()));
    }
    soc.socket().getOutputStream().flush();
  }
  /*
  Use for Receive (ODBConnect/ODBCluster) and send (ODBWorker).Formatted Input/Output 0-17+lengths, 
  formatted read/write from 0-17+lengths

  <br> The ODBIOStream Buffer Layout
  <br> --------------------------------------------------------------------------------------
  <br>  Byte Pos.  length-in-bytes  name   OBBIOStream
  <br>    0:             1          bool   contains bool x01 for true, x00 for false
  <br>    1-4:           4          Int    contains an Int return value
  <br>    5-6:           2          msg    contains msg length, mLen can be 0
  <br>    7-8:           2          err    contains err length, eLen can be 0
  <br>    9-12:          4          list   contains list length (not the size), lLen can be 0
  <br>    13-16:         4          obj    contains the byte array length,. bLen can be 0
  <br> ---------------- 17 bytes ------------------------------------------------------------
  <br>    17             mLen              start of msg String
  <br>    a = 17+mLen    eLen              start of err String
  <br>    b = a+eLen     lLen              start of list or array of all String items (see note)
  <br>    c = b+lLen     bLen              start of byte array
  <br>  len = c+bLen                       EOF (the content of Stream)
  <br> --- Total in bytes: len = 17+mLen+eLen+lLen+bLen -------------------------------------
  <br> If xLen = 0 item x is NULL and the next Item takes this position. Exp. a = 17 if
       mLen is 0 and eLen is not 0 then the err starting pos. is 17. The size is variable.
  <br> Note: each list/array element is started by 2 bytes indicating the length of the element
  <br> and the element itself in bytes.
  @param buf byte array has the length len and is the ODBReceiveStream content
  */
  /**
  writeBool
  @param b the boolean value
  */
  public void writeBool(boolean b) {
    buf[0] = b? (byte)0x01:(byte)0x00;
  }
  /**
  writeInt
  @param i int, write the 4 bytes of i (high order first)
  */
  public void writeInteger(int i) {
    buf[1] = (byte) (i >> 24);
    buf[2] = (byte) (i >> 16);
    buf[3] = (byte) (i >>  8);
    buf[4] = (byte)  i;
  }
  /**
  writeMsg
  @param msg the msg string
  */
  public void writeMsg(String msg) {
    int len = msg.length();
    if ((pos+len) >= MAX) enlarge(len);
    buf[5] = (byte) (len >> 8);
    buf[6] = (byte)  len;
    try {
      System.arraycopy(msg.getBytes(csName), 0, buf, 17, len);
    } catch (Exception ex) {
      System.arraycopy(msg.getBytes(), 0, buf, 17, len);
    }
    pos = 17+len;
  }
  /**
  writeErr
  @param err the err string
  */
  public void writeErr(String err) {
    int len = err.length();
    if ((pos+len) >= MAX) enlarge(len);
    buf[7] = (byte) (len >> 8);
    buf[8] = (byte)  len;
    System.arraycopy(err.getBytes(), 0, buf, pos, len);
    pos += len;
  }
  /**
  writeList
  @param list the String ArrayList
  */
  public void writeList(List<String> list) {
    int len = 0; 
    for (String S : list) len += (2+S.length());
    if ((pos+len) >= MAX) enlarge(len);
    // set length
    buf[9]  = (byte) (len >> 24);
    buf[10] = (byte) (len >> 16);
    buf[11] = (byte) (len >> 8);
    buf[12] = (byte)  len;
    for (String S : list) {
      int le = S.length();
      buf[pos++] = (byte)(le >> 8);
      buf[pos++] = (byte) le;
      try {
        System.arraycopy(S.getBytes(csName), 0, buf, pos, le);
      } catch (Exception ex) {
        System.arraycopy(S.getBytes(), 0, buf, pos, le);
      }
      pos += le;
    }
  }
  /**
  writeList
  @param list the String ArrayList
  */
  public void writeObjList(List<byte[]> list) {
    int len = 0;
    for (byte[] bb: list) len += (2+bb.length);
    if ((pos+len) >= MAX) enlarge(len);
    // set length
    buf[9]  = (byte) (len >> 24);
    buf[10] = (byte) (len >> 16);
    buf[11] = (byte) (len >> 8);
    buf[12] = (byte)  len;
    for (byte[] bb : list) {
      if ((pos+bb.length) >= MAX) enlarge(bb.length);
      buf[pos++] = (byte)(bb.length >> 8);
      buf[pos++] = (byte) bb.length;
      System.arraycopy(bb, 0, buf, pos, bb.length);
      pos += bb.length;
    }
  }
  /**
  writeObj
  @param bb byte array (of serialized object)
  */
  public void writeObj(byte[] bb) {
    if ((pos+bb.length) >= MAX) enlarge(bb.length);
    buf[13] = (byte) (bb.length >> 24);
    buf[14] = (byte) (bb.length >> 16);
    buf[15] = (byte) (bb.length >> 8);
    buf[16] = (byte)  bb.length;
    System.arraycopy(bb, 0, buf, pos, bb.length);
    pos += bb.length;
  }
  /**
  writePrimitives
  @param obj object that can be a String, an int, a double, a float,a long, a short, a char...
  @exception Exception thrown by JAVA
  */
  public void writePrimitive(Object obj) throws Exception {
    if (obj instanceof String) {
      int le = ((String)obj).length();
      if ((pos+le) >= MAX) enlarge(le);
      buf[13] = (byte) 0x00; // String
      buf[14] = (byte) (le >> 16);
      buf[15] = (byte) (le >> 8);
      buf[16] = (byte)  le;
      System.arraycopy(((String)obj).getBytes(csName), 0, buf, pos, le);
      pos += le;
    } else {
      if ((pos+8) >= MAX) enlarge(32);
      if (obj instanceof Integer) {
        buf[13] = (byte) 0x01; // int
        int I = (int) obj;
        buf[pos++] = (byte)(I >> 24);
        buf[pos++] = (byte)(I >> 16);
        buf[pos++] = (byte)(I >> 8);
        buf[pos++] = (byte)(I & 0xFF);
      } else if (obj instanceof Long) {
        buf[13] = (byte) 0x02; // long
        long I = (long) obj;
        buf[pos++] = (byte)(I >> 56);
        buf[pos++] = (byte)(I >> 48);
        buf[pos++] = (byte)(I >> 40);
        buf[pos++] = (byte)(I >> 32);
        buf[pos++] = (byte)(I >> 24);
        buf[pos++] = (byte)(I >> 16);
        buf[pos++] = (byte)(I >> 8);
        buf[pos++] = (byte)(I & 0xFF);
      } else if (obj instanceof Short) {
        buf[13] = (byte) 0x03; // short
        int I = (short) obj;
        buf[pos++] = (byte)(I >> 8);
        buf[pos++] = (byte)(I & 0xFF);
      } else if (obj instanceof Double) {
        buf[13] = (byte) 0x04; // double
        byte [] bb = ByteBuffer.allocate(8).putDouble((double)obj).array();
        System.arraycopy(bb, 0, buf, pos, 8);
        pos += 8;
      } else if (obj instanceof Float) {
        buf[13] = (byte) 0x05; // float
        byte[] bb = ByteBuffer.allocate(4).putFloat((float)obj).array();
        System.arraycopy(bb, 0, buf, pos, 4);
        pos += 4;
      } else if (obj instanceof Character) {
        buf[13] = (byte) 0x06; // char
        byte[] bb = ByteBuffer.allocate(2).putChar((char)obj).array();
        System.arraycopy(bb, 0, buf, pos, 2);
        pos += 2;
      } else if (obj instanceof Byte) {
        buf[13] = (byte) 0x07; // byte
        buf[pos++] = (byte)(obj);
      } else if (obj instanceof Boolean) {
        buf[13] = (byte) 0x08; // boolean
        buf[pos++] = (byte)((boolean)obj? 0x01:0x00);
      } else throw new IOException("Object is not a primitive!"); 
    }    
  }
  //-----------------fromatted Input read--------------------
  /**
  read the content from SocketChanneland load to buffer
  @param soc SocketChannel
  @exception Exception thrown by JAVA
  */
  public void read(SocketChannel soc) throws Exception {
    ByteBuffer bbuf = ByteBuffer.allocateDirect(65536);
    int le = 0; pos = 0;
    do {
      bbuf.clear();
      le = soc.read(bbuf);
      if ((pos+le) >= MAX) enlarge(le);
      ((ByteBuffer)bbuf.flip()).get(buf, pos, le);
      pos += le;
    } while (le >= 65536);
    if (buf[0] == (byte)0x1F && buf[1] == (byte)0x8B &&  buf[2] == (byte)0x08) {
      GZIPInputStream gi = new GZIPInputStream(new ODBInputStream(buf, 0, pos));
      pos = 0; // reset starting position
      byte bb[] = new byte[65536]; // 64KB
      for (le = gi.read(bb); le > 0; le = gi.read(bb)) {
        if ((pos+le) >= MAX) enlarge((le));
        System.arraycopy(bb, 0, buf, pos, le);
        pos += le;
      }
      gi.close();
    }
  }
  /**
  getSoc - load the content from SocketChannel to buffer
  @param soc SocketChannel
  @exception Exception thrown by JAVA or ODBWorker
  */
  public void getSoc(SocketChannel soc) throws Exception {
    read(soc);
    // check ODBWorker Exception
    int l = ((buf[7]&0xFF) << 8)|(buf[8]&0xFF);
    if (l > 0) throw new IOException(new String(buf,17+(((buf[5]&0xFF)<<8)|(buf[6]&0xFF)), l));
  }
  /**
  readObj
  @return byte array of serialized object or null
  */
  public byte[] readObj() {
    int len = ((buf[13]&0xFF) << 24)|((buf[14]&0xFF) << 16)|
              ((buf[15]&0xFF) << 8)|(buf[16]&0xFF);
    if (len == 0) return null;
    int p = 17+(((buf[5]&0xFF) << 8)|(buf[6]&0xFF))+(((buf[7]&0xFF) << 8)|(buf[8]&0xFF))+
               (((buf[9]&0xFF) << 24)|((buf[10]&0xFF) << 16)|((buf[11]&0xFF) << 8)|(buf[12]&0xFF));
    byte[] bb = new byte[len];
    System.arraycopy(buf, p, bb, 0, len);
    return bb;    
  }
  /**
  ReadPrimitive
  @return Object that can be a String, an int, a double, a float,a long, a short, a char...
  @exception IOException thrown by JAVA
  */
  public Object readPrimitive() throws IOException {
    int p = 17+(((buf[5]&0xFF) << 8)|(buf[6]&0xFF))+(((buf[7]&0xFF) << 8)|(buf[8]&0xFF))+
               (((buf[9]&0xFF) << 24)|((buf[10]&0xFF) << 16)|((buf[11]&0xFF) << 8)|(buf[12]&0xFF));
    if (buf[13] == (byte)0x00) { // String
      int le = (int)(buf[14] & 0xFF) << 16 |
               (int)(buf[15] & 0xFF) << 8 |
               (int)(buf[16] & 0xFF);
      return new String(buf, p, le, csName);
    } else if (buf[13] == (byte)0x01) { // int
      return ((int)(buf[p++] & 0xFF) << 24 |
              (int)(buf[p++] & 0xFF) << 16 |
              (int)(buf[p++] & 0xFF) << 8 |
              (int)(buf[p] & 0xFF)
             );
    } else if (buf[13] == (byte)0x02) { // long
     return ((long)(buf[p++] & 0xFF) << 56 |
             (long)(buf[p++] & 0xFF) << 48 |
             (long)(buf[p++] & 0xFF) << 40 |
             (long)(buf[p++] & 0xFF) << 32 |
             (long)(buf[p++] & 0xFF) << 24 |
             (long)(buf[p++] & 0xFF) << 16 |
             (long)(buf[p++] & 0xFF) << 8  |
             (long)(buf[p] & 0xFF)
            );
    } else if (buf[13] == (byte)0x03) { // short
      return ((int)(buf[p++] & 0xFF) << 8 |
              (int)(buf[p] & 0xFF)
             );
    } else if (buf[13] == (byte)0x04) { // double
      return ByteBuffer.wrap(buf, p, 8).getDouble();
    } else if (buf[13] == (byte)0x05) { // float
      return ByteBuffer.wrap(buf, p, 4).getFloat();
    } else if (buf[13] == (byte)0x06) { // char
      return ByteBuffer.wrap(buf, p, 2).getChar();
    } else if (buf[13] == (byte)0x07) { // byte
      return buf[p];
    } else if (buf[13] == (byte)0x08) {
      return (buf[p] == (byte)0x01);
    } else throw new IOException("Object is not a primitive!");    
  }
  /**
  readKeys. Object could be String (0x00), long value (0x02) or serializable Object (0x01)
  @return Object ArrayList
  @exception Exception thrown by JAVA
  */
  public ArrayList<Object> readKeys() throws Exception {
    ArrayList<Object> list = new ArrayList<>(); // create a List
    int len = ((buf[9]&0xFF) << 24)|((buf[10]&0xFF) << 16)|
              ((buf[11]&0xFF) << 8)|(buf[12]&0xFF);
    // 17 + length of ID + length of err
    int p = 17+(((buf[5]&0xFF) << 8)|+(buf[6]&0xFF))+(((buf[7]&0xFF) << 8)|(buf[8]&0xFF));
    for (int sum = 0, le = 0; sum < len; sum += (3+le)) {
      le = ((buf[p]&0xFF) << 8)+(buf[p+1]&0xFF)-1; // length
      p += 2; // position of Key-Tag
      // String as key
      if (buf[p] == (byte)0x00) {
        list.add(new String(buf, ++p, le, csName));
      }
      // long value as Key
      else if (buf[p] == (byte)0x01) {
        list.add(Long.parseLong(new String(buf, ++p, le)));
      }
      // BigInteger as key
      else { // if (buf[p] == (byte)0x02) {
        byte[] val = new byte[le];
        System.arraycopy(buf, ++p, val, 0, le);
        list.add(new BigInteger(new String(val)));
      } 
      p += le;
    }
    return list;
  }
  /**
  readObjList. Object could be String (0x00), long value (0x01) or BigDecimal (0x02)
  @return Object ArrayList
  @exception Exception thrown by JAVA
  */
  public ArrayList<Object> readObjList() throws Exception {
    ArrayList<Object> list = new ArrayList<>(); // create a List
    int len = (((buf[9]&0xFF) << 24)|((buf[10]&0xFF) << 16)|
              ((buf[11]&0xFF) << 8)|(buf[12]&0xFF));
    // 17 + length of ID + length of err
    int p = 17+(((buf[5]&0xFF) << 8)|(buf[6]&0xFF))+(((buf[7]&0xFF) << 8)|(buf[8]&0xFF));
    for (int i, l, sum = 0, le; sum < len; sum += (2+le)) {
      le = ((buf[p]&0xFF) << 8)+(buf[p+1]&0xFF); // length
      ObjectInputStream ois = new ObjectInputStream(new ODBInputStream(buf, p+2, le));
      list.add(ois.readObject());
      ois.close();
      p += (2+le);
    }
    return list;
  }
  /**
  readList
  @return String ArrayList
  @exception Exception thrown by JAVA
  */
  public ArrayList<String> readList() throws Exception {
    ArrayList<String> list = new ArrayList<>(); // create a List
    int len = (((buf[9]&0xFF) << 24)|((buf[10]&0xFF) << 16)|
              ((buf[11]&0xFF) << 8)|(buf[12]&0xFF));
    // 17 + length of ID + length of err
    int p = 17+(((buf[5]&0xFF) << 8)|(buf[6]&0xFF))+(((buf[7]&0xFF) << 8)|(buf[8]&0xFF));
    for (int sum = 0, le; sum < len; sum += (2+le)) {
      le = ((buf[p]&0xFF) << 8)+(buf[p+1]&0xFF);
      list.add(new String(buf, p+2, le, csName));
      p += (2+le);
    }
    return list;
  }
  /**
  readMsg
  @return String or null
  @exception Exception thrown by JAVA
  */
  public String readMsg() throws Exception {
    int len = ((buf[5]&0xFF) << 8)|(buf[6]&0xFF);
    if (len == 0) return null;
    return new String(buf, 17, len, csName);
  }
  /**
  readInt
  @return integer
  */
  public int readInteger( ) {
    return ((buf[1]&0xFF) << 24)|((buf[2]&0xFF) << 16)|
           ((buf[3]&0xFF) << 8)|(buf[4]&0xFF);
  }
  /**
  readBool
  @return boolean
  */
  public boolean readBool() {
    return buf[0] == (byte)0x01;
  }
  // ----------------------------------------------------------  
  /**
  toByteArray
  @return byte array of the content
  */
  public byte[] toByteArray() {
    byte[] bb = new byte[pos];
    System.arraycopy(buf, 0, bb, 0, pos);
    return bb;
  }
  /**
  size of the buffer
  @return int, the number of bytes (or content) of this Stream
  */
  public int size() {
    return pos;
  }
  /**
  reset buffer sets reading position at the beginning
  */
  public void reset() {
    pos = 0;
  }
  /**
  preset buffer cleans the buffer header and sets the reading position to the end of the buffer header
  */
  public void preset() {
    for (int i = 0; i < 24; ++i) buf[i] = (byte)0x00;
    pos = 17;
  }
  //-------------------------- private area---------------------
  private void enlarge(int le) {
    MAX += (le+1024);
    byte[] tmp = new byte[MAX];
    System.arraycopy(buf, 0, tmp, 0, pos);
    buf = tmp;
  }
  private byte[] buf;
  private int MAX=65536, pos=0;
  private String csName = "UTF-8";
}
