package joeapp.odb;
import java.io.*;
import java.nio.charset.*;
/**
 @author Joe T. Schwarz (C)
*/
// Used by ODBIOStream
public class ODBInputStream extends InputStream {
  /**
  Constructor
  @param buf byte array
  */
  public ODBInputStream(byte[] buf) {
    this.buf = buf;
  }
  /**
  Constructor
  @param ba byte array
  @param pos starting position
  @param len the length or number of bytes
  */
  public ODBInputStream(byte[] ba, int pos, int len) {
    buf = new byte[len]; // create the buffer
    System.arraycopy(ba, pos, buf, 0, len);
  }
  /**
  setCharset
  @param charset Charset, default: StandardCharsets.US_ASCII
  */
  public void setCharset(Charset charset) {
    this.charset = charset;
  }
  /**
  read a byte
  @return int the byte or -1 if EndOfByteArray (EOB)
  */
  public int read( ) {
    if (pos >= buf.length) return -1;
    return buf[pos++] & 0xFF;
  }
  /**
  readShort - read 2 continuous bytes and convert to an int
  @return int the int or -1 if EndOfByteArray (EOB)
  */
  public int readShort( ) {
    if ((pos+2) >= buf.length) return -1;
    return ((buf[pos++]&0xFF) << 8)|(buf[pos++]&0xFF);
  }
  /**
  readInt - read 4 continuous bytes and convert to an int
  @return int the int or -1 if EndOfByteArray (EOB)
  */
  public int readInt( ) {
    if ((pos+4) >= buf.length) return -1;
    return ((buf[pos++]&0xFF) << 24)|((buf[pos++]&0xFF) << 16)|
           ((buf[pos++]&0xFF) << 8)|(buf[pos++]&0xFF);
  }
  /**
  readString - read le continuous bytes and convert to a String
  @param le int, the number of bytes
  @return String (0 length if EOB)
  */
  public String readString(int le) {
    if ((pos+le) > buf.length) le = buf.length - pos;
    String s = new String(buf, pos, le, charset);
    pos += le;
    return s;
  }
  /**
  readToken - read n continuous bytes and convert to a String 
  @return String (0 length if EOF)
  */
  public String readToken( ) {
    int le =((buf[pos++]&0xFF)<<8)|buf[pos++]&0xFF;
    String s = new String(buf, pos, le, charset);
    pos += le;
    return s;
  }
  /**
  readBytes - read n bytes
  @param n int, the number of bytes
  @return byte array (0 length if EOB)
  */
  public byte[] readBytes(int n) {
    if ((pos+n) > buf.length) n = buf.length - pos;
    byte[] bb = new byte[n];
    System.arraycopy(buf, pos, bb, 0, n);
    pos += n;
    return bb;
  }
  /**
  readBytes - read the remainder (from current position to EOF)
  @return byte array (0 length if EOB)
  */
  public byte[] readBytes( ) {
    int le = buf.length - pos;
    byte[] bb = new byte[le];
    System.arraycopy(buf, pos, bb, 0, le);
    pos += le;
    return bb;
  }
  /**
  remainderLength
  @return int, the number of unread bytes (0 if EOB)
  */
  public int remainderLength() {
    return buf.length - pos;
  }
  /**
  read bb.length bytes and put into the given byte array.
  @param bb byte array
  @return the number of bytes (could be less than the bb-length in case of EOB)
  */
  public int read(byte[] bb) throws IOException {
    if (pos >= buf.length) return -1;
    //Note: for internal use only. No check
    int len = buf.length - pos;
    if (len > bb.length) len = bb.length;
    System.arraycopy(buf, pos, bb, 0, len);
    pos += len;
    return len;
  }
  /**
  read n bytes and put into the given byte array
  @param bb byte array
  @param off int, the starting offset
  @param le int, the number of bytes to be read
  @return the number of bytes (could be less than the bb-length in case of EOB)
  */
  public int read(byte[] bb, int off, int le) throws IOException {
    if (pos >= buf.length) return -1;
    //Note: for internal use only. No check
    if ((off+le) > bb.length) le = bb.length - off;    
    if ((buf.length - pos) < le) le = buf.length - pos;
    System.arraycopy(buf, pos, bb, off, le);
    pos += le;
    return le;
  }
  /**
  readAll the entire content of this Stream
  @return byte array
  */
  public byte[] readAll() {
    pos = buf.length;
    return buf;
  }
  /**
  available
  @return int, the available bytes
  */
  public int available() {
    return buf.length;
  }
  /**
  reset the stream to the first position
  */
  public void reset() {
    pos = 0;
  }
  //
  public void close() {
    buf = null;
    pos = 0;
  }
  //
  public boolean markSupported() {
    return false;
  }
  /**
  skip
  @param n long, skip n byte
  @return long the actual number of bytes is skipped
  */
  public long skip(long n) {
    long x = (long)buf.length - n;
    if (x < 0) {
      return 0;
    }
    if (x < n) {
      pos += x;
      return x;
    }
    pos += n;
    return n;
  }
  //
  private byte[] buf;
  private int pos = 0;
  private Charset charset = StandardCharsets.US_ASCII;
}
