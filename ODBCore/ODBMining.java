package joeapp.odb;
//
import java.io.*;
import java.net.*;
import java.util.*;
import java.math.*;
import java.nio.file.*;
import java.util.stream.*;
import java.lang.reflect.*;
/**
@author Joe T. Schwarz (C)
ODBMining for things like Searching, Selecting, etc.
*/
public class ODBMining extends ODBConnect {
    /**
    Contructor - connect to JODB server
    <br>Note:
    <br>- Serialized POJO object as key must have an implemented equals-method
    <br>- Leading or trailing spaces are consisdered as part of a string
    <br>- By select with comparator (EQ/LT/LE/GT/GE) EQ is the only choice if comp. value is a string
    @param dbHost String, Host of JODB Server
    @param port   int, port number
    @param pw String, User's password
    @param uID String, User's ID
    @exception  Exception for IO, NIO, etc.
    */
    public ODBMining(String dbHost, int port, String pw, String uID) throws Exception {
      super(dbHost, port, pw, uID);
    }
    /**
    getClass(dbName, "macaw"); 
    @param dbName  String, Database Name
    @param key     String, key of object
    @return Class  the class of object with key
    @exception Exception if dbName is null or Exception from JODB Server
    */
    public Class<?> getClass(String dbName, Object key) throws Exception {
      return read(dbName, key).getClass();
    }
    /**
    getField(cls, fdName); 
    @param obj    Object that contains fName
    @param fName  String, Field Name
    @return Object content of fName
    @exception Exception if dbName or clsName is null or Exception from JODB Server
    */
    public Object getField(Object obj, String fName) throws Exception {
      Field[] fd = obj.getClass().getDeclaredFields();
      for (Field f : fd) if (f.getName().equals(fName)) {
        f.setAccessible(true); // set accessible
        return f.get(obj);
      }
      return null;
    }
    /**
    getField(dbName, objName, fdName) -see ODBObjectView for restrictions
    @param dbName String, OODB name
    @param key String, access key to Object
    @param fName  String, Field Name
    @return Object content of fName (must be cast to double, float, String, etc.)
    @exception Exception if dbName or clsName is null or Exception from JODB Server
    */
    public Object getField(String dbName, String key, String fName) throws Exception {
      return getField(read(dbName, key), fName);
    }
    /**
    allClassNames(dbName); 
    @param dbName  String, Database Name
    @return ArrayList of all class names known in odb
    @exception Exception if dbName is null or Exception from JODB Server
    */
    public ArrayList<String> allClassNames(String dbName) throws Exception {
      send(dbName, 32);
      return ios.readList();
    }
    /**
    allClasses(dbName); 
    @param dbName String, Database Name
    @return ArrayList of all classes known in odb
    @exception Exception if dbName is null or Exception from JODB Server
    */
    public ArrayList<Class<?>> allClasses(String dbName) throws Exception {
      send(dbName, 32);
      ArrayList<String> nLst = ios.readList();
      ArrayList<Class<?>> lst = new ArrayList<>(nLst.size());
      for (String n : nLst) lst.add(Class.forName(n));
      return lst;
    }
    /**
    allObjects(dbName, "Animal"); 
    @param dbName  String, Database Name
    @param clsName String, Class or Object name of object
    @return arrayList of all objects with the given clsName
    @exception Exception if dbName or clsName is null or Exception from JODB Server
    */
    public ArrayList<Object> allObjects(String dbName, String clsName) throws Exception {
      send(dbName, 33, clsName);
      return ios.readObjList();
    }
    /**
    allObjects(dbName, class)
    @param dbName  String, Database Name
    @param cls Class, for example People.class
    @return arrayList of all objects with the given dbName
    @exception Exception if dbName or clsName is null or Exception from JODB Server
    */
    public ArrayList<Object> allObjects(String dbName, Class<?> cls) throws Exception {
      send(dbName, 33, cls.getSimpleName());
      return ios.readObjList(); 
    }
    /**
    Execute a method of a serialized Object.
    @param obj    executable serialized Object
    @param method String, invoking method name
    @param pTypes ClassArray, list of ParmTypes (e.g. String.class, etc.)
    @param parms  ObjectArray, list of parameters (e.g. "HelloWorld", etc.)
    @return Object returned value (null for void)
    @exception Exception from HOST or JAVA
    */
    public static Object exec(Object obj, String method,
                              Class<?> pTypes[],Object[] parms) throws Exception {
      Class<?> cls = obj.getClass();
      return cls.getMethod(method, pTypes).invoke(obj, parms);
    }
    /**
    SelectAll return all serialized objects that match the given string pattern
    <br>Pattern with wildcard * for a string or ? for 1 letter can be used (e.g. J*y or Jo?y)
    <br>Example:  selectAll(dbName, "Joe") or selectAll(dbName, "J?e")
    @param dbName  String, Database Name
    @param pat     String, the selecting pattern
    @return arrayList of all objects with the given pattern
    @exception Exception if dbName or pattern is null or Exception from JODB Server
    */
    public ArrayList<Object> selectAll(String dbName, String pat) throws Exception {
      send(dbName, 34, "*", pat);
      return ios.readObjList();     
    }
    /**
    select all objects with the vName that matches the pattern.
    <br>Note:
    <br>- varName is case-sensitive and must match one of the serialized Object's field names
    <br>- Pattern with wildcard * for a string or ? for 1 letter can be used (e.g. J*y or Jo?y)
    <br>Example: selectAll(dbName, vName, "Joe") or selectAll(dbName, vName, "J*e")
    @param dbName  String, Database Name
    @param vName   String, variable Name (non array variable)
    @param pat     String, the selecting pattern
    @return arrayList of all objects with the given pattern
    @exception Exception if dbName or or vName or pattern is null or Exception from JODB Server
    */
    public ArrayList<Object> selectAll(String dbName, String vName, String pat) throws Exception {
      send(dbName, 34, vName, pat);
      return ios.readObjList();     
    }
    /**
    SQL-like select
    <br>A SQL-like statement is for example:
    <br>"select varName_1 eq value_1 and varName_2 gt value_2"
    <br>"select varName_1 eq value_1 or  varName_2 gt value_2"
    <br>Conjunctor (and, or), Comparator (lt, le, eq, gt, ge) can be in upper or lower case.
    <br>Exception is arisen if Conjunctor or Comparator is invalid.
    <br>Rules: 
    <br>Comparison: Var_1 Comparator Var_2
    <br>Conjunction: Comparison_1 and/or comparison_2 and/or ....
    <br>value can contain wildcard *, ? if it is a string (otherwise: exception)
    <br>Note:
    <br>- varName is case-sensitive and must match one of the serialized Object's field names
    <br>- Float or double must be led by 0 if it is less than 1. Exp.: 0.123
    <br>- Negative value is preceded by a hyphen (MINUS sign). Positive sign (+) can be omitted.
    <br>- Comparator of VarName of string instance can only be EQ. Otherwise the returned list has NO0 element.
    <br>- value of primitive/BigInteger/BigDecimal must be nummeric. Otherwise the returned list has NO element.
    <br>- value of String instance must be a String (e.g. for an 1 = ""+1). Otherwise the returned list has NO element.
    @param dbName  String, Database Name
    @param sql     sql-like statement. Example: name eq Joe and income gt 10000
    @return ArrayList of all Objects that match the criteria of sql
    @exception Exception Exception from JODB Server
    */
    // 0      1     2  3     4   5     6  7     8  9     10 11    ...
    // select var_1 eq val_1 and var_2 gt val_2 or var_3 le val_3 ...
    public ArrayList<Object> SQL(String dbName, String sql) throws Exception {
      String tmp[] = sql.trim().split("[ ]+"); // ignore space and discard ""
      if (tmp.length < 4 || (tmp.length % 4) > 0 || !"select".equals(tmp[0]))
        throw new Exception("Invalid SQL expression:"+sql);
      for (int i = 4; i < tmp.length; i += 4) {
        tmp[i] = tmp[i].toUpperCase();
        if (!"AND".equals(tmp[i]) && !"OR".equals(tmp[i]))
          throw new Exception(tmp[i]+" is invalid in SQL expression:"+sql);
      }
      for (int i = 2; i < tmp.length; i += 4) {
        tmp[i] = tmp[i].toUpperCase();
        if (!"LT".equals(tmp[i]) && !"LE".equals(tmp[i]) && !"EQ".equals(tmp[i]) && !"GE".equals(tmp[i]) &&
            !"GT".equals(tmp[i])) throw new Exception(tmp[i]+" is invalid in SQL expression:"+sql);
      }
      //
      StringBuilder sb = new StringBuilder(tmp[0]);
      for (int i = 1; i < tmp.length; ++i) sb.append(" "+tmp[i]);
      send(dbName, 35, sb.toString());
      return ios.readObjList(); 
    }      
    /**
    select all objects that match the criterion of value
    @param dbName String, Database name
    @param comp   String, LT: Less, LE: LessEqual, EQ: Equal, GE: GreaterEqual, GT: Greater
    @param val    double, comparing value
    @return arrayList of all objects that match the criterion.
    @exception Exception if dbName is null or Exception from JODB Server
    */
    public ArrayList<Object> selectAll(String dbName, String comp, double val) throws Exception {
      return selectAll(dbName, "*", comp,  val);
    }
    /**
    Select all objects that match the criterion of value
    <br>Note: varName is case-sensitive and must match one of the serialized Object's field names
    @param dbName String, Database name
    @param vName  String, Variable name (non array variable)
    @param comp   String, LT: Less, LE: LessEqual, EQ: Equal, GE: GreaterEqual, GT: Greater
    @param val    double, comparing value
    @return arrayList of all objects that match the criterion.
    @exception Exception if dbName is null or Exception from JODB Server
    */
    public ArrayList<Object> selectAll(String dbName, String vName,
                                       String comp, double val) throws Exception {
      send(dbName, 36, vName, onComparator(comp)+(char)0x00+val);
      return ios.readObjList(); 
    }
    /**
    select all objects that match the criterion of value
    @param dbName String, Database name
    @param comp   String, LT: Less, LE: LessEqual, EQ: Equal, GE: GreaterEqual, GT: Greater
    @param val    float, comparing value
    @return arrayList of all objects that match the criterion.
    @exception Exception if dbName is null or Exception from JODB Server
    */
    public ArrayList<Object> selectAll(String dbName, String comp, float val) throws Exception {
      return selectAll(dbName, "*", comp,  val);
    }
    /**
    Select all objects that match the criterion of value
    <br>Note: varName is case-sensitive and must match one of the POJO's field names
    @param dbName String, Database name
    @param vName  String, Variable name (non array variable)
    @param comp   String, LT: Less, LE: LessEqual, EQ: Equal, GE: GreaterEqual, GT: Greater
    @param val    float, comparing value
    @return arrayList of all objects that match the criterion.
    @exception Exception if dbName is null or Exception from JODB Server
    */
    public ArrayList<Object> selectAll(String dbName, String vName,
                                       String comp, float val) throws Exception {
      send(dbName, 36, vName, onComparator(comp)+(char)0x00+val);
      return ios.readObjList(); 
    }
    /**
    select all objects that match the criterion of value
    @param dbName String, Database name
    @param comp   String, LT: Less, LE: LessEqual, EQ: Equal, GE: GreaterEqual, GT: Greater
    @param val    BigDecimal, comparing value
    @return arraylits of all objects that match the criterion.
    @exception Exception if dbName is null or Exception from JODB Server
    */
    public ArrayList<Object> selectAll(String dbName, String comp, BigDecimal val) throws Exception {      
    return selectAll(dbName, "*", comp,  val);    }
    /**
    Select all objects that match the criterion of value
    <br>Note: varName is case-sensitive and must match one of the serialized Object's field names
    @param dbName String, Database name
    @param vName  String, Variable name (non array variable)
    @param comp   String, LT: Less, LE: LessEqual, EQ: Equal, GE: GreaterEqual, GT: Greater
    @param val    BigDecimal, comparing value
    @return arraylist of all objects that match the criterion.
    @exception Exception if dbName is null or Exception from JODB Server
    */
    public ArrayList<Object> selectAll(String dbName, String vName,
                                       String comp, BigDecimal val) throws Exception {
      send(dbName, 36, vName, onComparator(comp)+(char)0x00+val.toString());
      return ios.readObjList(); 
    }
    /**
    select all objects that match the criterion of value
    @param dbName String, Database name
    @param comp   String, LT: Less, LE: LessEqual, EQ: Equal, GE: GreaterEqual, GT: Greater
    @param val    BigInteger, comparing value
    @return arraylits of all objects that match the criterion.
    @exception Exception if dbName is null or Exception from JODB Server
    */
    public ArrayList<Object> selectAll(String dbName, String comp, BigInteger val) throws Exception {
      return selectAll(dbName, "*", comp,  val);
    }
    /**
    Select all objects that match the criterion of value
    <br>Note: varName is case-sensitive and must match one of the serialized Object's field names
    @param dbName String, Database name
    @param vName  String, Variable name (non array variable)
    @param comp   String, LT: Less, LE: LessEqual, EQ: Equal, GE: GreaterEqual, GT: Greater
    @param val    BigInteger, comparing value
    @return arraylist of all objects that match the criterion.
    @exception Exception if dbName is null or Exception from JODB Server
    */
    public ArrayList<Object> selectAll(String dbName, String vName,
                                       String comp, BigInteger val) throws Exception {
      send(dbName, 36, vName, onComparator(comp)+(char)0x00+val.toString());
      return ios.readObjList(); 
    }
    /**
    select all objects that match the criterion of value
    @param dbName String, Database name
    @param comp   String, LT: Less, LE: LessEqual, EQ: Equal, GE: GreaterEqual, GT: Greater
    @param val    BigInteger, comparing value
    @return arraylits of all objects that match the criterion.
    @exception Exception if dbName is null or Exception from JODB Server
    */
    public ArrayList<Object> selectAll(String dbName, String comp, long val) throws Exception {
      return selectAll(dbName, "*", comp,  val);
    }
    /**
    Select all objects that match the criterion of value
    <br>Note: varName is case-sensitive and must match one of the serialized Object's field names
    @param dbName String, Database name
    @param vName  String, Variable name (non array variable)
    @param comp   String, LT: Less, LE: LessEqual, EQ: Equal, GE: GreaterEqual, GT: Greater
    @param val    long, comparing value
    @return arraylist of all objects that match the criterion.
    @exception Exception if dbName is null or Exception from JODB Server
    */
    public ArrayList<Object> selectAll(String dbName, String vName,
                                       String comp, long val) throws Exception {
      send(dbName, 36, vName, onComparator(comp)+(char)0x00+val);
      return ios.readObjList(); 
    }
    /**
    select all objects that match the criterion of value
    @param dbName String, Database name
    @param comp   String, LT: Less, LE: LessEqual, EQ: Equal, GE: GreaterEqual, GT: Greater
    @param val    int, comparing value
    @return arraylist of all objects that match the criterion.
    @exception Exception if dbName is null or Exception from JODB Server
    */
    public ArrayList<Object> selectAll(String dbName, String comp, int val) throws Exception {
      return selectAll(dbName, "*", comp,  val);
    }
    /**
    Select all objects that match the criterion of value
    <br>Note: varName is case-sensitive and must match one of the serialized Object's field names
    @param dbName String, Database name
    @param vName  String, Variable name (non array variable)
    @param comp   String, LT: Less, LE: LessEqual, EQ: Equal, GE: GreaterEqual, GT: Greater
    @param val    int, comparing value
    @return arraylist all objects that match the criterion.
    @exception Exception if dbName is null or Exception from JODB Server
    */
    public ArrayList<Object> selectAll(String dbName, String vName,
                                       String comp, int val) throws Exception {
      send(dbName, 36, vName, onComparator(comp)+(char)0x00+val);
      return ios.readObjList(); 
    }
    /**
    select all objects that match the criterion of value
    @param dbName String, Database name
    @param comp   String, LT: Less, LE: LessEqual, EQ: Equal, GE: GreaterEqual, GT: Greater
    @param val    short, comparing value
    @return arraylist of all objects that match the criterion.
    @exception Exception if dbName is null or Exception from JODB Server
    */
    public ArrayList<Object> selectAll(String dbName, String comp, short val) throws Exception {
      return selectAll(dbName, "*", comp,  val);
    }
    /**
    Select all objects that match the criterion of value
    <br>Note: varName is case-sensitive and must match one of the serialized Object's field names
    @param dbName String, Database name
    @param vName  String, Variable name (non array variable)
    @param comp   String, LT: Less, LE: LessEqual, EQ: Equal, GE: GreaterEqual, GT: Greater
    @param val    short, comparing value
    @return arraylist of all objects that match the criterion.
    @exception Exception if dbName is null or Exception from JODB Server
    */
    public ArrayList<Object> selectAll(String dbName, String vName,
                                       String comp, short val) throws Exception {
      send(dbName, 36, vName, onComparator(comp)+(char)0x00+val);
      return ios.readObjList(); 
    }
    /**
    getFieldNames. Example: getFieldNames("People").
    <br>Precondition: the requested byte code class must exist on the requester's site
    @param clsName String, serialized Object class name
    @return String arrayList of all fields of this class
    @exception Exception if dbName or pattern is null or Exception from JODB Server
    */
    public ArrayList<String> getFieldNames(String clsName) throws Exception {
      Class<?> cls = Class.forName(clsName);
      Field[] fd = cls.getDeclaredFields();
      ArrayList<String> lst = new ArrayList<String>(fd.length);
      for (Field f: fd) lst.add(f.getName());
      return lst;
    }
    /**
    getFieldNames of an object from a key. Example: getFieldNames(dbName, "Joe")
    <br>Precondition: the requested byte code class must exist on the requester's site
    @param dbName  String, Database Name
    @param keyName String, the key name
    @return ArrayList of all field names of this class
    @exception Exception if dbName or pattern is null or Exception from JODB Server
    */
    public ArrayList<String> getFieldNames(String dbName, String keyName) throws Exception {
      ArrayList<String> lst = new ArrayList<String>();
      Field[] fds = read(dbName, keyName).getClass().getDeclaredFields();
      for (Field f: fds) lst.add(f.getName());
      return lst;
    }
    /**
    getFieldNames. Example: getFieldNames(People); 
    @param cls     serialized Object class
    @return AarrayList of all field names
    @exception Exception if dbName or pattern is null or Exception from JODB Server
    */
    public ArrayList<String> getFieldNames(Class<?> cls) throws Exception {
      Field[] fd = cls.getDeclaredFields();
      ArrayList<String> lst = new ArrayList<String>(fd.length);
      for (Field f: fd) lst.add(f.getName());
      return lst;
    }
    /**
    getFieldNames. Example: getFieldNames(myObj); 
    @param obj     serailized Object
    @return ArrayList of all field names
    @exception Exception if dbName or pattern is null or Exception from JODB Server
    */
    public ArrayList<String> getFieldNames(Object obj) throws Exception {
      return getFieldNames(obj.getClass());
    }
    /**
    getClassName of an object from a key. Example: getClassName(dbName, "Joe")
    <br>Precondition: the requested byte code class must exist on the requester's site
    @param dbName  String, Database Name
    @param keyName String, the key name
    @return String the class name
    @exception Exception if dbName or pattern is null or Exception from JODB Server
    */
    public String getClassName(String dbName, String keyName) throws Exception {
      send(dbName, 37, keyName);
      return ios.readMsg(); 
    }
    /**
    allObjects(dbName); 
    @param dbName  String, Database Name
    @return arrayList of all objects with the given dbName
    @exception Exception if dbName or clsName is null or Exception from JODB Server
    */
    public ArrayList<Object> allObjects(String dbName) throws Exception {
      send(dbName, 38);
      return ios.readObjList(); 
    }
    //
    private String onComparator(String comp) throws Exception {
      String com = comp.trim().toUpperCase();
      if ("LT".equals(com) || "LE".equals(com) || "EQ".equals(com) ||
          "GE".equals(com) || "GT".equals(com)) return com;
      throw new Exception("Invalid comparator: "+comp);
    }
}

