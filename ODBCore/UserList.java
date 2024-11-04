package joeapp.odb;

import java.io.*;
import java.util.*;
import java.util.zip.*;
/**
UserList offers the following 3 features:
<br>1. AutoCreate a predefined GZIP file "userlist" if this file does not exist
<br>2. Login validation for login
<br>3. Maintain the userList with 3options: add, delete and update
<br>
<br>Access Privilege: Read only (0), Read/Write (1), Read/Write/Delete (2), SuperUser (3) + 2-privilege
<br>SuperUser has all rights Read/Write/Delete and access to the UserList.
<br>Entry format: password:userID@privilege
<br>Password and userID must not contain a colon (:) or plus sign (+) or an @
<br>
<br>Default ID is system and default PW is admin (both in lower case). This default is used:
<br>- to create a new userlist with different users,
<br>- and has no access to the JODB created later
@author Joe T. Schwarz (C)
*/
public class UserList {
  /**
  Contructor.  Abs. path to the "userlist" file or to be created (e.g. C:/MyODB/odb/userlist).
  @param path  String, the pathName. The fileName "userlist" will be automatically added.
  @exception Exception thrown by JAVA (IOs, etc.)
  */
  public UserList(String path) throws Exception {
    File fi = new File(path);
    if (fi.isDirectory()) {
      if (path.endsWith("/") || path.endsWith(File.separator)) path = path+"userlist";
      else path = path+"/userlist";
    } else {
      if (!path.endsWith("userlist")) throw new Exception(path+" must end with \"userlist\".");
    }
    this.uFile = path;
    uList = new ArrayList<String>();
    if (!fi.exists()) {
      uList.add("system:admin@3");
      save( );
    } else {
      int n;
      GZIPInputStream gin = new GZIPInputStream(new FileInputStream(uFile));
      String[] lst = (new String(gin.readAllBytes())).split("\n");
      for (String s : lst) uList.add(EnDecrypt.decrypt(s));
      gin.close();
    }
  }
  /**
  isVald - valid if inp does not contain a colon (:) or an at (@)
  @param inp String
  @return boolean true if inp does not contain the mentioned letters
  */
  public boolean isValid(String inp) {
    return (inp != null && inp.indexOf(":") < 0 && inp.indexOf("@") < 0 && inp.length() >= 3);
  }
  /**
  isVald - valid if pw or uid does not contain a colon (:) or an at (@)
  @param pw  String, User Password
  @param uid String, User Login ID
  @return boolean true if pw AND uid do not contain the mentioned letters
  */
  public boolean isValid(String pw, String uid) {
    return (pw != null && pw.indexOf(":") < 0 && pw.indexOf("@") < 0 && pw.length() >= 3 && 
            uid != null && uid.indexOf(":") < 0 && uid.indexOf("@") >= 0 && uid.length() >= 3);
  }
  /**
  isExisted - uID exists?
  @param uid String, User Login ID
  @return boolean true if uid exits in the userlist
  */
  public boolean isExisted(String uid) {
   if (uid != null) {
      for (String u : uList) {
        if (uid.equals(u.substring(u.indexOf(":")+1, u.indexOf("@")))) return true;
      }
    }
    return false;
  }
  /**
  isUser
  @param pw  String, User Password
  @param uid String, User Login ID
  @return boolean false if user is unknown
  */
  public boolean isUser(String pw, String uid) {
    if (pw != null && uid != null) {
      String pwui = pw+":"+uid+"@";
      for (String u : uList) if (u.indexOf(pwui) == 0) return true;
    }
    return false;
  }
  /**
  isSuperuser
  @param pw  String, User Password
  @param uid String, User Login ID
  @return boolean false if user is unknown
  */
  public boolean isSuperuser(String pw, String uid) {
    if (pw != null && uid != null) {
      String pwid = pw+":"+uid+"@";
      for (String u : uList) {
        if (u.indexOf(pwid) == 0) return u.endsWith("@3");
      }
    }
    return false;
  }
  /**
  getUserPrivilege()
  @param pw  String, User Password
  @param uid String, User Login ID
  @return int 0: readOnly, 1: ReadWrite, 2: RradWriteDelete, 3: SuperUser. -1 if user is unknown
  */
  public int getPrivilege(String pw, String uid) {
    if (isValid(pw, uid) && isUser(pw, uid)) {
      String pwid = pw+":"+uid+"@";
      for (String u : uList) if (u.indexOf(pwid) == 0) return u.charAt(u.length()-1) & 0x0F;
    }
    return -1;
  }
  /**
  add new User
  @param pw  String, User Password
  @param uid String, User Login ID
  @param right int, 0: only Read, 1: ReadWrite, 2: ReadWriteDelete, 3: SuperUser
  @return boolean true: user was added
  */
  public boolean addUser(String pw, String uid, int right) {
    if (isValid(pw, uid) && !isExisted(uid)) {
      if (right > 2) right = 3;
      else if (right < 0) right = 0;
      uList.add(pw+":"+uid+"@"+right);
      return true;
    }
    return false;
  }
  /**
  update User Password (used by users to change their PW)
  @param uid String, User Login ID
  @param pwOLD String, User old Password
  @param pwNEW String, User new Password
  @return boolean false if invalid uid
  */
  public boolean changePassword(String uid, String pwOLD, String pwNEW) {
    if (isValid(pwOLD, uid)  && isUser(pwOLD, uid) && pwNEW != null){
      String pwid = pwOLD+":"+uid+"@";
      for (String u : uList) if (u.indexOf(pwid) == 0) {
        uList.set(uList.indexOf(u), pwNEW+":"+uid+u.substring(u.indexOf("@")));
        return true;
      }
    }
    return false;
  }
  /**
  upgrade User's Privilege (used by Superuser)
  @param suPW String, Superuser ID
  @param suID String, Superuser PW
  @param uid String, User Login ID
  @param newPriv  int, privilege. 0: Read, 1: ReadWrite, 2: ReadWriteDelete, 3: SuperUser
  @return boolean false if invalid pw and/or uid
  */
  public boolean upgradePrivilege(String suPW, String suID, String uid, int newPriv) {
    if (isExisted(uid) && isSuperuser(suPW, suID)) {
      if (newPriv > 2) newPriv = 3;
      else if (newPriv < 0) newPriv = 0;
      String id = ":"+uid+"@";
      for (String u : uList) if (u.indexOf(id) > 0) {
        uList.set(uList.indexOf(u), u.substring(0, u.indexOf("@")+1)+newPriv);
        return true;
      }
    }
    return false;
  }
  /**
  delete User
  @param uid String, User Login ID
  @return boolean false: unknown uid/pw
  */
  public boolean deleteUser(String uid) {
    if (isExisted(uid)) {
      String id = ":"+uid+"@";
      for (String u : uList) if (u.indexOf(id) > 0) {
        uList.remove(u);
        return true;
      }
    }
    return false;
  }
  /**
  getUserList a list of all Users. Only accessible by SuperUser
  @param pw String, Superuser Password
  @param uid String, Superuser ID
  @return ArrayList contains all users (uID@priv) or null if not SuperUser
  <br>where [privilege] = 0 Read only, 1 ReadWrite, 2: ReadWriteDelete, 3: SuperUser
  */
  public ArrayList<String> getUserList(String pw, String uid) {
    if (isSuperuser(pw, uid)) {
      ArrayList<String> list = new ArrayList<>();
      for (String u:uList) list.add(u.substring(u.indexOf(":")+1));
      return list;
    }
    return null;
  }
  /**
  recover Userlist. recover a Userlist with the default Superuser PW:system, ID:admin
  @param path String, abs. path to the "userlist" file (e.g. C:/MyODB/odb).
  @exception Exception thrown by JAVA
  */
  public void recover(String path) throws Exception {
    uFile = path.replace(File.separator, "/")+"/userlist";
    if (!(new File(uFile)).exists()) throw new Exception("No \"userlist\" is found in "+path);
    uList = new ArrayList<String>();
    uList.add("system:admin@3");
    try {
      int n;
      GZIPInputStream gin = new GZIPInputStream(new FileInputStream(uFile));
      String[] lst = (new String(gin.readAllBytes())).split("\n");
      for (String s : lst) {
        n = s.indexOf("@"); // remove the old admin
        String user = EnDecrypt.decrypt(s.substring(0, n));
        if (!user.equals("system:admin")) uList.add(user + s.substring(n));
      }
      gin.close();
    } catch (Exception ex) { }
    save( );
  }
  /**
  reset User Password
  @param uID String, User Login ID
  @return String created Password (null if uID is unknown)
  */
  public String resetPassword(String uID) {
    String uid = ":"+uID+"@";
    for (String u : uList) if (u.indexOf(uid) > 0) {
      Random ran = new Random();
      String old = u.substring(0,u.indexOf(":"));
      String pw = String.format("%06X", ran.nextInt(1000000));
      uList.set(uList.indexOf(u), u.replace(old, pw));
      return pw;
    }
    return null;
  }
  /**
  Save userlist in the directory of given path (see Constructor)
  @exception Exception thrown by JAVA (IO, etc.)
  */
  public void save( ) throws Exception {
    GZIPOutputStream gzip = new GZIPOutputStream(new FileOutputStream(uFile, false));
    ODBIOStream ios = new ODBIOStream();
    for (String s : uList) ios.writeString(EnDecrypt.encrypt(s)+"\n");
    gzip.write(ios.toByteArray());
    gzip.flush( );
    gzip.close( );
  }
  //
  private String uFile;
  private ArrayList<String> uList;
}
