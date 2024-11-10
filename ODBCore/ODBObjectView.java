package joeapp.odb;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.math.*;
import java.nio.charset.*;
import java.nio.file.Files;
import java.lang.reflect.Method;
/**
Serialized Object Viewer without knowing/having its binary byte-codes class
<br>Note:
<br>- Supported are: String, ArrayList, BigInteger, BigDecimal, Integer/Long/Double/Float/Short and all primitives. 
<br>- All other Java APIs (e.g. HashMap, Stack, etc.) are NOT supported.
<br>- Array of Java APIs are NOT supported (except String)
<br>- Arrays of more than 3 dimensions are NOT supported.
@author Joe T. Schwarz
*/
public class ODBObjectView {
  /**
  Constructor.
  */
  public ODBObjectView( ) { }
  /**
  view the content of a serialized object
  @param bb  byte array of serialized object 
  @exception Exception thrown by JAVA (e.g. IOException, etc.)
  */
  public void view(byte[] bb) throws Exception {
    // Java Serialized Object Signature
    byte[] ID = { (byte)0xAC, (byte)0xED, x00, (byte)0x05, TC_OBJECT, TC_CLASSDESC };  
    for (p = 0; p < ID.length; ++p) 
    if (bb[p] != ID[p]) throw new Exception("Byte array is not a serialized object");
    this.bb = bb;
    vRef.clear();
    fNames.clear(); 
    vFields.clear();
    tFields.clear();
    ref = nFields = 0;
    view(false);
  }
  /**
  setCharset
  @param charset String, Charset Name, default: StandardCharsets.US_ASCII
  */
  public void setCharset(String charset) {
    this.charset = charset;
  }
  /**
  getSerialID 
  @return long SerialID of this object
  */
  public long getSerialID() {
    return serID;
  }
  /**
  getSize returns the number of fields
  @return int number of fields
  */
  public int getSize() {
    return nFields;
  }
  /**
  getClassName
  @return String Object class name
  */
  public String getClassName() {
    return clsName;
  }
  /**
  getFieldNames
  @return ArrayList of String containing all field names
  */
  public ArrayList<String> getFieldNames() {
    return new ArrayList<>(fNames);
  }
  /**
  getFieldType returns the field type 
  @param fName String, field name
  @return String I for Integer, D for Double, S for Short, etc. NULL if unknown
  */
  public String getFieldType(String fName) {
    return tFields.get(fName);
  }
  /**
  getFieldValue returns the content of the field with the given field name
  @param fName String, field name
  @return Object must be cast to I, D, String, etc.
  */
  public Object getFieldValue(String fName) {
    return vFields.get(fName);
  }
  /**
  isArray()
  @param fName String, field name
  @return boolean if fName is an array, false if unknown or not an array
  */
  public boolean isArray(String fName) {
    String type = tFields.get(fName);
    return (type != null && type.indexOf("[") >= 0);
  }
  /**
  viewVar: view a variable corresponding to a field
  @param vName String, variable Name (* for any field that passes the comp with pat)
  @param comp  String, comparator (EQ, LE, LT, GE, GT)
  @param pat   String, compared pattern or value (BigInteger, int, double, etc.)
  @return boolean true if vName is found and meets the comparator with the given pattern or value
  */
  @SuppressWarnings("unchecked")
  public boolean viewVar(String vName, String comp, String pat) throws Exception {
    comp = comp.toUpperCase();
    for (String fn : fNames) if (fn.equals(vName) || "*".equals(vName)) {
      String type = getFieldType(fn);
      Object obj = getFieldValue(fn);
      if (type.indexOf("[") < 0) {
        if (!"*".equals(vName)) { // all fields ?
          if ("String".equals(type)) return "EQ".equals(comp) && isFound((String)obj, pat);
          else if (type.endsWith("List")) { // List or ArrayList
            for (Object o : (List) obj) {
              if (o instanceof String) {
                if ("EQ".equals(comp) && isFound((String)o, pat)) return true;
              } else if (compValue(o, comp, pat)) return true;
            }
          } else if (!"Object".equals(type)) return compValue(obj, comp, pat);
          return false;
        } else { // all fields
          if ("String".equals(type)) {
            if ("EQ".equals(comp) && isFound((String)obj, pat)) return true;
          } else if (type.endsWith("List")) { // List or ArrayList
            for (Object o : (List) obj) {
              if (o instanceof String) {
                if ("EQ".equals(comp) && isFound((String)o, pat)) return true;
              } else if (compValue(o, comp, pat)) return true;
            }
          } else if (!"Object".equals(type)) {
            if (compValue(obj, comp, pat)) return true;
          }
        }
      } else {
        int a = type.indexOf("][");
        if (type.indexOf("int") >= 0) {
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              int[][] aa = (int[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  if (compValue(aa[i][j], comp, pat)) return true;
            } else {
              int[][][] aa = (int[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                    if (compValue(aa[i][j][l], comp, pat)) return true;
            }
          } else {
            int[] aa = (int[]) obj;
            for (int i = 0; i < aa.length; ++i) 
              if (compValue(aa[i], comp, pat)) return true;
          }
        } else if (type.startsWith("Integer")) {
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              Integer[][] aa = (Integer[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  if (compValue(aa[i][j], comp, pat)) return true;
            } else {
              Integer[][][] aa = (Integer[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                    if (compValue(aa[i][j][l], comp, pat)) return true;
            }
          } else {
            Integer[] aa = (Integer[]) obj;
            for (int i = 0; i < aa.length; ++i) 
              if (compValue(aa[i], comp, pat)) return true;
          }
        } else if (type.indexOf("long") >= 0) {
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              long[][] aa = (long[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  if (compValue(aa[i][j], comp, pat)) return true;
            } else {
              long[][][] aa = (long[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                    if (compValue(aa[i][j][l], comp, pat)) return true;
            }
          } else {
            long[] aa = (long[]) obj;
            for (int i = 0; i < aa.length; ++i)
              if (compValue(aa[i], comp, pat)) return true;
          }
        } else if (type.startsWith("Long")) {
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              Long[][] aa = (Long[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  if (compValue(aa[i][j], comp, pat)) return true;
            } else {
              Long[][][] aa = (Long[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                    if (compValue(aa[i][j][l], comp, pat)) return true;
            }
          } else {
            Long[] aa = (Long[]) obj;
            for (int i = 0; i < aa.length; ++i)
              if (compValue(aa[i], comp, pat)) return true;
          }
        } else if (type.indexOf("short") >= 0) {
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              short[][] aa = (short[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  if (compValue(aa[i][j], comp, pat)) return true;
            } else {
              short[][][] aa = (short[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                    if (compValue(aa[i][j][l], comp, pat)) return true;
            }
          } else {
            short[] aa = (short[]) obj;
            for (int i = 0; i < aa.length; ++i)
              if (compValue(aa[i], comp, pat)) return true;
          }
        } else if (type.indexOf("dou") >= 0) {
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              double[][] aa = (double[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                   if (compValue(aa[i][j], comp, pat)) return true;
            } else {
              double[][][] aa = (double[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                    if (compValue(aa[i][j][l], comp, pat)) return true;
            }
          } else {
            double[] aa = (double[]) obj;
            for (int i = 0; i < aa.length; ++i)
              if (compValue(aa[i], comp, pat)) return true;
          }
        } else if (type.startsWith("Double")) {
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              Double[][] aa = (Double[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  if (compValue(aa[i][j], comp, pat)) return true;
            } else {
              Double[][][] aa = (Double[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                    if (compValue(aa[i][j][l], comp, pat)) return true;
            }
          } else {
            Double[] aa = (Double[]) obj;
            for (int i = 0; i < aa.length; ++i)
              if (compValue(aa[i], comp, pat)) return true;
          }
        } else if (type.indexOf("flo") >= 0) {
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              float[][] aa = (float[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  if (compValue(aa[i][j], comp, pat)) return true;
            } else {
              float[][][] aa = (float[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                    if (compValue(aa[i][j][l], comp, pat)) return true;
            }
          } else {
            float[] aa = (float[]) obj;
            for (int i = 0; i < aa.length; ++i)
              if (compValue(aa[i], comp, pat)) return true;
          }
        } else if (type.startsWith("Float")) {
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              Float[][] aa = (Float[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  if (compValue(aa[i][j], comp, pat)) return true;
            } else {
              Float[][][] aa = (Float[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                    if (compValue(aa[i][j], comp, pat)) return true;
            }
          } else {
            Float[] aa = (Float[]) obj;
            for (int i = 0; i < aa.length; ++i)
              if (compValue(aa[i], comp, pat)) return true;
          }
        } else if (type.indexOf("String") >= 0) {
          if (!"EQ".equals(comp)) return false;
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              String[][] aa = (String[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  if (isFound(aa[i][j], pat)) return true;
            } else {
              String[][][] aa = (String[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                    if (isFound(aa[i][j][l], pat)) return true;
            }
          } else {
            String[] aa = (String[]) obj;
            for (int i = 0; i < aa.length; ++i) 
              if (isFound(aa[i], pat)) return true;
          }
        } else if (type.startsWith("BigI")) {
           if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              BigInteger[][] aa = (BigInteger[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  if (compValue(aa[i][j], comp, pat)) return true;
            } else {
              BigInteger[][][] aa = (BigInteger[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                    if (compValue(aa[i][j][l], comp, pat)) return true;
            }
          } else {
            BigInteger[] aa = (BigInteger[]) obj;
            for (int i = 0; i < aa.length; ++i)
              if (compValue(aa[i], comp, pat)) return true;
          }
        } else if (type.startsWith("BigD")) {
           if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              BigDecimal[][] aa = (BigDecimal[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  if (compValue(aa[i][j], comp, pat)) return true;
            } else {
              BigDecimal[][][] aa = (BigDecimal[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                    if (compValue(aa[i][j][l], comp, pat)) return true;
            }
          } else {
            BigDecimal[] aa = (BigDecimal[]) obj;
            for (int i = 0; i < aa.length; ++i)
              if (compValue(aa[i], comp, pat)) return true;
          }
        } 
      }        
    }
    return false;
  }
  //----------------------------------------------------------------------------------
  // private constructor
  private ODBObjectView(byte[] bb, int q, int ref) throws Exception {
    this.p = q;
    this.bb = bb;
    this.ref = ref;
    view(true);
  }
  //
  private int getIndex() {
    return p;
  }
  //
  private void view(boolean embedded) throws Exception {
    clsName = getString();
    if (clsName.indexOf("$") > 0) { // inner classes
      while ((p+1) < bb.length) if (bb[p] == TC_ENDBLOCKDATA) {
        if (bb[p+1] == TC_NULL) {
          p += 2;
          break;
        } else if (bb[p+1] == TC_CLASSDESC) {
          p += 2;
          clsName = getString();
        } else ++p;
      } else ++p;
    }
    serID = getLong();
    if (bb[p] < (byte)0x02 || bb[p] > (byte)0x03)
      throw new Exception("Object has NO serializable field");
    ++p; // ignore SERIALIZABLE byte
    nFields = ((int)(bb[p++]&0xFF)<<8)|(int)(bb[p++]&0xFF);
    if (nFields == 0) return;
    for (int i = 0; i < nFields && p < bb.length; ++i) {
      String type = ""+(char)bb[p++];
      String fName = getString(); // Field name
      fNames.add(fName);
      //
      if (bb[p] == TC_ENDBLOCKDATA && bb[p+1] == TC_NULL) break;
      if (bb[p] == TC_STRING) {
        ++p; // ignore TC_STRING
        String tmp = getString().replace(";", "");
        int e = tmp.lastIndexOf("/");
        if (e > 0) {
          int f = tmp.lastIndexOf("[");
          type = (f >= 0? tmp.substring(0, f+1):"")+tmp.substring(e+1, tmp.length());
        } else if (tmp.charAt(0) == '[') {
          if (tmp.lastIndexOf("[") > 2) throw new Exception(tmp+" has more than 3 dimensions");
          type = tmp; // array
        } else type = "Object"; // customer Object
      } else if (bb[p] == TC_REFERENCE && bb[p+1] == x00 && bb[p+2] == TC_ENUM) {
        p += 5; // C_REFERENCE+x00+TC_ENUM+2:ENUM
        type = "String";
      }
      tFields.put(fName, type);
    }
    //
    delta = 0;
    while ((p+1) < bb.length) {
      if (bb[p] == TC_ENDBLOCKDATA && bb[p+1] == TC_NULL) {
        p += 2;
        if (p < bb.length) {
          getValues( );
          if (embedded) return;
        }
      } else ++p;
    }
  }
  //
  @SuppressWarnings("unchecked")
  private void getValues( ) throws Exception {
    int mx = fNames.size();
    for (int i = 0; i < mx && p < bb.length; ++i) {
      String n = fNames.get(i);
      String t = tFields.get(n);
      if (t.indexOf("[") < 0) {
        switch (t) {
          case "I": // int
            setValue(n, getInt(), "int");
            break;
          case "D": // double
            setValue(n, getDouble(), "double");
            break;
          case "J": // long
            setValue(n, getLong(), "long");
            break;
          case "F": // float
            setValue(n, getFloat(), "float");
            break;
          case "S": // short
            setValue(n, getShort(), "short");
            break;
          case "String":
            if (bb[p] == TC_STRING) ++p;
            setValue(n, getString(), "String");
            break;
          case "B": // byte
            setValue(n, bb[p++], "byte");
            break;
          case "Z": // boolean
            setValue(n, bb[p++] == (byte)0x01, "boolean");
            break;
          case "C": // char
            setValue(n, getChar(), "char");
            break;
          default:
            if (bb[p] == TC_OBJECT && bb[p+1] == TC_CLASSDESC) { // nested Object
              if (t.endsWith("Integer") || t.endsWith("Decimal") || t.endsWith("List") || 
                  t.endsWith("Double") || t.endsWith("Float") || t.endsWith("Long") ||
                  t.endsWith("Short")) {
                loadAPI(t);
                if (!t.endsWith("List")) setValue(n, object, t);
                else setValue(n, object, t.substring(t.lastIndexOf(".")+1)+"<"+listType+">");
              } else {
                int oRef = ++ref;
                ODBObjectView ov = new ODBObjectView(bb, p+2, ref);
                Map<Integer, Object> map = getVRef();
                if (map.size() > 0) map.forEach(vRef::putIfAbsent);
                tFields.put(n, "Object");
                vRef.put(oRef, ov);
                vFields.put(n, ov);
                p = getIndex();
                ref = getRef();
              }
            } else if (t.endsWith("Integer") || t.endsWith("Decimal") || t.endsWith("List") || 
                       t.endsWith("Double") || t.endsWith("Float") || t.endsWith("Long") ||
                       t.endsWith("Short")) {
              loadAPI(t);
              if (!t.endsWith("List")) setValue(n, object, t);
              else setValue(n, object, t.substring(t.lastIndexOf(".")+1)+"<"+listType+">");
            }
          }
      } else {
        found = false;
        p = getArrayType(n);
        switch (t) {
        case "[I":
            int I[] = new int[le];
            for (int a = 0; a < le; ++a) {
              I[a] = getInt();
              vRef.put(++ref, I[a]);
            }
            tFields.replace(n, "int["+le+"]");
            vFields.put(n, I);
            break;
        case "[[I":
            int II[][] = new int[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) {
                II[a][b] = getInt();
                vRef.put(++ref, II[a][b]);
              }
              // 10 = TC_ARRAY + TC_REFERENCE + x00 + 3:baseWireHandle + 4:#Of elements
              p += 10;
            }
            p -= 10; // roll back
            tFields.replace(n, "int["+dim1+"]["+le+"]");
            vFields.put(n, II);
            break;
        case "[[[I":
            int III[][][] = new int[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) {
                  III[a][b][c] = getInt();
                  vRef.put(++ref, III[a][b][c]);
                }
                p += 10;
              }
              p += 10;
            }
            p -= 20;
            tFields.replace(n, "int["+dim1+"]["+dim2+"]["+le+"]");
            vFields.put(n, III);
            break;
        case "[Integer":
            one_Dim(n, "Integer["+le+"]", "getInt", new Integer[le]); 
            break;
        case "[[Integer":
            two_Dim(n, "Integer["+dim1+"]["+le+"]", "getInt", new Integer[dim1][le]); 
            break;
        case "[[[Integer":
            three_Dim(n, "Integer["+dim1+"]["+dim2+"]["+le+"]", "getInt", new Integer[dim1][dim2][le]); 
            break;
        case "[J":
            long J[] = new long[le];
            for (int a = 0; a < le; ++a) {
              J[a] = getLong();
              vRef.put(++ref, J[a]);
            }
            tFields.replace(n, "long["+le+"]");
            vFields.put(n, J);
            break;
        case "[[J":
            long JJ[][] = new long[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) {
                JJ[a][b] = getLong();
                vRef.put(++ref, JJ[a][b]);
              }
              p += 10;
            }
            p -= 10;
            tFields.replace(n, "long["+dim1+"]["+le+"]");
            vFields.put(n, JJ);
            break;
        case "[[[J":
            long JJJ[][][] = new long[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) {
                  JJJ[a][b][c] = getLong();
                  vRef.put(++ref, JJJ[a][b][c]);
                }
                p += 10;
              }
              p += 10;
            }
            p -= 20;
            tFields.replace(n, "long["+dim1+"]["+dim2+"]["+le+"]");
            vFields.put(n, JJJ);
            break;
        case "[Long":
            one_Dim(n, "Long["+le+"]", "getLong", new Long[le]); 
            break;
        case "[[Long":
            two_Dim(n, "Long["+dim1+"]["+le+"]", "getLong", new Long[dim1][le]); 
            break;
        case "[[[Long":
            three_Dim(n, "Long["+dim1+"]["+dim2+"]["+le+"]", "getLong", new Long[dim1][dim2][le]); 
            break;
        case "[D":
            double D[] = new double[le];
            for (int a = 0; a < le; ++a) {
              D[a] = getDouble();
              vRef.put(++ref, D[a]);
            }
            tFields.replace(n, "double["+le+"]");
            vFields.put(n, D);
            break;
        case "[[D":
            double DD[][] = new double[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) {
                DD[a][b] = getDouble();
                vRef.put(++ref, DD[a][b]);
              }
              p += 10;
            }
            p -= 10;
            tFields.replace(n, "double["+dim1+"]["+le+"]");
            vFields.put(n, DD);
            break;
        case "[[[D":
            double DDD[][][] = new double[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) {
                  DDD[a][b][c] = getDouble();
                  vRef.put(++ref, DDD[a][b][c]);
                }
                p += 10;
              }
              p += 10;
            }
            p -= 20;
            tFields.replace(n, "double["+dim1+"]["+dim2+"]["+le+"]");
            vFields.put(n, DDD);
            break;
        case "[Double":
            one_Dim(n, "Double["+le+"]", "getDouble", new Double[le]); 
            break;
        case "[[Double":
            two_Dim(n, "Double["+dim1+"]["+le+"]", "getDouble", new Double[dim1][le]); 
            break;
        case "[[[Double":
            three_Dim(n, "Double["+dim1+"]["+dim2+"]["+le+"]", "getDouble", new Double[dim1][dim2][le]); 
            break;
        case "[String":
            vRef.put(++ref, le); 
            String T[] = new String[le];
            for (int a = 0; a < le; ++a) {
              T[a] = getReference();
            }
            tFields.replace(n, "String["+le+"]");
            vFields.put(n, T);
            break;
        case "[[String":
            vRef.put(++ref, dim1);
            vRef.put(++ref, le);
            String TT[][] = new String[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) {
                TT[a][b] = getReference();
              }
            }
            tFields.replace(n, "String["+dim1+"]["+le+"]");
            vFields.put(n, TT);
            break;
        case "[[[String":
            vRef.put(++ref, dim1);
            vRef.put(++ref, dim2);
            vRef.put(++ref, le);
            String TTT[][][] = new String[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) {
                  TTT[a][b][c] = getReference();
                }
              }
              p += 10;
            }
            p -= 10;
            tFields.replace(n, "String["+dim1+"]["+dim2+"]["+le+"]");
            vFields.put(n, TTT);
            break;
        case "[S":
            short S[] = new short[le];
            for (int a = 0; a < le; ++a) {
              S[a] = getShort();
              vRef.put(++ref, S[a]);
            }
            tFields.replace(n, "short["+le+"]");
            vFields.put(n, S);
            break;
        case "[[S":
            short SS[][] = new short[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) {
                SS[a][b] = getShort();
                vRef.put(++ref, SS[a][b]);
              }
              p += 10;
            }
            p -= 10;
            tFields.replace(n, "short["+dim1+"]["+le+"]");
            vFields.put(n, SS);
            break;
        case "[[[S":
            short SSS[][][] = new short[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) {
                  SSS[a][b][c] = getShort();
                  vRef.put(++ref, SSS[a][b][c]);
                }
                p += 10;
              }
              p += 10;
            }
            p -= 20;
            tFields.replace(n, "short["+dim1+"]["+dim2+"]["+le+"]");
            vFields.put(n, SSS);
            break;
        case "[Short":
            one_Dim(n, "Short["+le+"]", "getShort", new Short[le]); 
            break;
        case "[[Short":
            two_Dim(n, "Short["+dim1+"]["+le+"]", "getShort", new Short[dim1][le]); 
            break;
        case "[[[Short":
            three_Dim(n, "Short["+dim1+"]["+dim2+"]["+le+"]", "getShort", new Short[dim1][dim2][le]); 
            break;
        case "[F":
            float F[] = new float[le];
            for (int a = 0; a < le; ++a){
              F[a] = getFloat();
              vRef.put(++ref, F[a]);
            }
            tFields.replace(n, "float["+le+"]");
            vFields.put(n, F);
            break;
          case "[[F":
            float FF[][] = new float[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) {
                FF[a][b] = getFloat();
                vRef.put(++ref, FF[a][b]);
              }
              p += 10;
            }
            p -= 10;
            tFields.replace(n, "float["+dim1+"]["+le+"]");
            vFields.put(n, FF);
            break;
        case "[[[F":
            float FFF[][][] = new float[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) {
                  FFF[a][b][c] = getFloat();
                  vRef.put(++ref, FFF[a][b][c]);
                }
                p += 10;
              }
              p += 10;
            }
            p -= 20;
            tFields.replace(n, "float["+dim1+"]["+dim2+"]["+le+"]");
            vFields.put(n, FFF);
            break;
        case "[Float":
            one_Dim(n, "Float["+le+"]", "getFloat", new Float[le]); 
            break;
        case "[[Float":
            two_Dim(n, "Float["+dim1+"]["+le+"]", "getFloat", new Float[dim1][le]); 
            break;
        case "[[[Float":
            three_Dim(n, "Float["+dim1+"]["+dim2+"]["+le+"]", "getFloat", new Float[dim1][dim2][le]); 
            break;
        case "[B":
            byte B[] = new byte[le];
            for (int a = 0; a < le; ++a) {
              B[a] = bb[p++];
              vRef.put(++ref, B[a]);
            }
            tFields.replace(n, "byte["+le+"]");
            vFields.put(n, B);
            break;
        case "[[B":
            byte BB[][] = new byte[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) {
                BB[a][b] = bb[p++];
                vRef.put(++ref, BB[a][b]);
              }
              p += 10;
            }
            p -= 10;
            tFields.replace(n, "byte["+dim1+"]["+le+"]");
            vFields.put(n, BB);
            break;
        case "[[[B":
            byte BBB[][][] = new byte[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) {
                  BBB[a][b][c] = bb[p++];
                  vRef.put(++ref, BBB[a][b][c]);
                }
                p += 10;
              }
              p += 10;
            }
            p -= 20;
            tFields.replace(n, "byte["+dim1+"]["+dim2+"]["+le+"]");
            vFields.put(n, BBB);
            break;
        case "[C":
            char C[] = new char[le];
            for (int a = 0; a < le; ++a) {
              C[a] = getChar();
              vRef.put(++ref, C[a]);
            }
            tFields.replace(n, "char["+le+"]");
            vFields.put(n, C);
            break;
        case "[[C":
            char CC[][] = new char[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) {
                CC[a][b] = getChar();
                vRef.put(++ref, CC[a][b]);
              }
              p += 10;
            }
            p -= 10;
            tFields.replace(n, "char["+dim1+"]["+le+"]");
            vFields.put(n, CC);
            break;
        case "[[[C":
            char CCC[][][] = new char[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) {
                  CCC[a][b][c] = getChar();
                  vRef.put(++ref, CCC[a][b][c]);
                }
                p += 10;
              }
              p += 10;
            }
            p -= 20;
            tFields.replace(n, "char["+dim1+"]["+dim2+"]["+le+"]");
            vFields.put(n, CCC);
            break;
        case "[Character":
            one_Dim(n, "Character["+le+"]", "getChar", new Character[le]); 
            break;
        case "[[Character":
            two_Dim(n, "Charactert["+dim1+"]["+le+"]", "getChar", new Character[dim1][le]); 
            break;
        case "[[[Character":
            three_Dim(n, "Character["+dim1+"]["+dim2+"]["+le+"]", "getChar", new Character[dim1][dim2][le]); 
            break;
        case "[Z":
            boolean Z[] = new boolean[le];
            for (int a = 0; a < le; ++a) {
              Z[a] = (int)bb[p++] == 1;
              vRef.put(++ref, Z[a]);
            }
            tFields.replace(n, "boolean["+le+"]");
            vFields.put(n, Z);
            break;
        case"[[Z":
            boolean ZZ[][] = new boolean[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) {
                ZZ[a][b] = (int)bb[p++] == 1;
                vRef.put(++ref, ZZ[a][b]);
              }
              p += 10;
            }
            p -= 10;
            tFields.replace(n, "boolean["+dim1+"]["+le+"]");
            vFields.put(n, ZZ);
           break;
        case "[[[Z":
            boolean ZZZ[][][] = new boolean[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) {
                  ZZZ[a][b][c] = (int)bb[p++] == 1;
                  vRef.put(++ref, ZZZ[a][b][c]);
                }
                p = +10;
              }
              p += 10;
            }
            p -= 20;
            tFields.replace(n, "boolean["+dim1+"]["+dim2+"]["+le+"]");
            vFields.put(n, ZZZ);
            break;
        case "[BigInteger":
            oneDim(n, "BigInteger["+le+"]", "bigInteger", new BigInteger[le]); 
            break;
        case "[[BigInteger":
            twoDim(n, "BigInteger["+dim1+"]["+le+"]", "bigInteger", new BigInteger[dim1][le]); 
            break;
        case "[[[BigInteger":
             threeDim(n, "BigInteger["+dim1+"]["+dim2+"]["+le+"]", "bigInteger", new BigInteger[dim1][dim2][le]); 
            break;
        case "[BigDecimal":
            oneDim(n, "BigDecimal["+le+"]", "bigDecimal", new BigDecimal[le]);
            break;
        case "[[BigDecimal":
            twoDim(n, "BigDecimal["+dim1+"]["+le+"]", "bigDecimal", new BigDecimal[dim1][le]);
            break;
        case "[[[BigDecimal":
            threeDim(n, "BigDecimal["+dim1+"]["+dim2+"]["+le+"]", "bigDecimal", new BigDecimal[dim1][dim2][le]);
            break;
        default: // unsupported array
          if (bb[p] == TC_OBJECT && bb[p+1] == TC_CLASSDESC) {
            ODBObjectView ov = new ODBObjectView(bb, p+2, ref);
            p = getIndex();
          }
          int l = t.indexOf("[L");
          String nt = l >= 0? t.substring(l+2):t;
          if (t.startsWith("[L")) nt += "["+le+"]";
          else if (t.startsWith("[[L")) nt += "["+dim1+"]["+le+"]";
          else if (t.startsWith("[[[L")) nt += "["+dim1+"]["+dim2+"]["+le+"]";
          else nt += "....too many dimensions";          
          tFields.replace(n, nt);
          vFields.put(n, "Unsupported");
        }              
      }            
    }
  }
  //
  private Object execMethod(String mName) throws Exception {
    Method method = ODBObjectView.class.getDeclaredMethod(mName, new Class[] {});
    method.setAccessible(true);
    return method.invoke(this, new Object[] {});
  }  
  //
  private void oneDim(String vName, String vType, String mName, Object obj[]) 
               throws Exception {
    for (int a = 0; a < obj.length; ++a) {
      obj[a] = execMethod(mName);
      vRef.put(++ref, obj[a]);
    }
    tFields.replace(vName, vType);
    vFields.put(vName, obj);
  }
  //
  private void one_Dim(String vName, String vType, String mName, Object obj[]) 
               throws Exception {
    for (int a = 0; a < obj.length; ++a) {
      obj[a] = execMethod(mName);
      vRef.put(++ref, obj[a]);
      if (p < bb.length) {
        if (bb[p] == TC_OBJECT) p += 6;
        // 16: 6: 7571007Exxxx + 4: xxxxxxxx + 6:7371007Exxxx
        else if (bb[p] == TC_ARRAY) p += 16;
      }
    }
    tFields.replace(vName, vType);
    vFields.put(vName, obj);
  }
  //
  private void twoDim(String vName, String vType, String mName,  Object obj[][]) 
               throws Exception {
    for (int a = 0; a < obj.length; ++a)      
      for (int b = 0; b < obj[a].length; ++b) {
        obj[a][b] = execMethod(mName);
        vRef.put(++ref, obj[a][b]);
      }
    tFields.replace(vName, vType);
    vFields.put(vName, obj);
  }
  //
  private void two_Dim(String vName, String vType, String mName,  Object obj[][]) 
               throws Exception {
    for (int a = 0; a < obj.length; ++a) {     
      for (int b = 0; b < obj[a].length; ++b) {
        obj[a][b] = execMethod(mName);
        vRef.put(++ref, obj[a][b]);
        if (p < bb.length) {
          if (bb[p] == TC_OBJECT) p += 6;
          // 16: 6: 7571007Exxxx + 4: #of_Elemets + 6:7371007Exxxx
          else if (bb[p] == TC_ARRAY) p += 16;
        }
      }
    }
    tFields.replace(vName, vType);
    vFields.put(vName, obj);
  }
  //
  private void threeDim(String vName, String vType, String mName, Object obj[][][]) 
               throws Exception {
    for (int a = 0; a < obj.length; ++a) 
      for (int b = 0; b < obj[a].length; ++b)  
        for (int c = 0; c < obj[a][b].length; ++c) {
          obj[a][b][c] = execMethod(mName);
          vRef.put(++ref, obj[a][b][c]);
        }
    tFields.replace(vName, vType);
    vFields.put(vName, obj);
  }
  //
  private void three_Dim(String vName, String vType, String mName, Object obj[][][]) 
               throws Exception {
    for (int a = 0; a < obj.length; ++a) {
      for (int b = 0; b < obj[a].length; ++b) {
        for (int c = 0; c < obj[a][b].length; ++c) {
          obj[a][b][c] = execMethod(mName);
          vRef.put(++ref, obj[a][b][c]);
          if (p < bb.length) {
            if (bb[p] == TC_OBJECT) p += 6;
            // 16: 6: 7571007Exxxx + 4: xxxxxxxx + 6:7371007Exxxx
            else if (bb[p] == TC_ARRAY) p += 16;
          }
        }
        if (p < bb.length) {
          if (bb[p] == TC_OBJECT) p += 6;
          // 16: 6: 7571007Exxxx + 4: xxxxxxxx + 6:7371007Exxxx
          else if (bb[p] == TC_ARRAY) p += 16;
        }
      }
    }
    tFields.replace(vName, vType);
    vFields.put(vName, obj);
  }
  //
  private HashMap<String, String> getTFields() {
    return new HashMap<>(tFields);
  }
  //
  private HashMap<String, Object> getVFields() {
    return new HashMap<>(vFields);
  }
  //
  private HashMap<Integer, Object> getVRef() {
    return new HashMap<>(vRef);
  }
  //
  private Object getObject() {
    return object;
  }
  //
  private String getArrayType() {
    return listType;
  }
  //
  private int getRef() {
    return ref;
  }
  //
  @SuppressWarnings("unchecked")
  private void loadAPI(String type) throws Exception {
    if (type.startsWith("BigD")) { // BigDecimal
      object = bigDecimal();
    } else if (type.startsWith("BigI")) { // BigInteger
      object = bigInteger();
    } else if (type.startsWith("In")) { // Integer
      object = getInt();
    } else if (type.startsWith("Do")) { // Double
      skip();
      object = getDouble();
    } else if (type.startsWith("Lo")) { // Long
      object = getLong();
    } else if (type.startsWith("Fl")) { // Float
      object = getFloat();
    } else if (type.startsWith("Sh")) { // Short
      object = getShort();
    } else if (type.endsWith("List")) { // ArrayList or List
      skip();
      int cap = getInt();
      if (bb[p] == TC_BLOCKDATA && bb[p+1] == (byte)0x04) {
        p += 2;
        cap = getInt();
      }
      boolean b = false;
      listType = "String";
      if (bb[p] == TC_OBJECT && bb[p+1] == TC_CLASSDESC) {
        p += 2;
        String s = getString(); // get Object type
        if (s.startsWith("java")) listType = s.substring(s.lastIndexOf(".")+1);
        while ((p+1) < bb.length) if (bb[p] == TC_ENDBLOCKDATA && bb[p+1] == TC_NULL) {
          b = true;
          p += 2;
          break;
        } else ++p; 
      }
      ArrayList list = new ArrayList(cap);
      while (p < bb.length) {
        byte by = bb[p];
        if (by == TC_STRING) {
          ++p; // String
          list.add(getString());
        } else if (by == TC_OBJECT || b) {
          if (by == TC_OBJECT && bb[p+1] == TC_REFERENCE) p += 6;
          switch (listType) {
          case "Integer":
            list.add(getInt());
            break;
          case "Double":
            list.add(getDouble());
            break;
          case "Long":
            list.add(getLong());
            break;
          case "Float":
            list.add(getFloat());
            break;
         case "Short":
            list.add(getShort());
            break;
          case "Byte":
            list.add(bb[p++]);
            break;
          case "Character":
            list.add(getChar());
            break;
          case "Boolean":
            list.add(bb[p++] == (byte)0x01);
            break;
          default:
            throw new Exception(String.format("ArrayList<%s> is unsupported.\n", listType));
          }
          b = false;
        } else if (bb[p] == TC_ENDBLOCKDATA) { // end Data block
          ++p;
          break;
        } else throw new Exception(String.format("Unknown code format: %02X\n", by));
      }
      object = list;
    } else throw new Exception("API:"+type+" is not supported.");
  }
  //
  private BigDecimal bigDecimal() {  
    int scale = 0, sign = 1;
    while (p < bb.length) {
      if (!found && bb[p] == (byte)'s' && bb[p+1] == (byte)'c' &&
           bb[p+2] == (byte)'a' && bb[p+3] == (byte)'l' && bb[p+4] == (byte)'e') {
        while (p < bb.length) if (bb[p] == TC_ENDBLOCKDATA) {
          if (bb[p+1] == TC_REFERENCE && bb[p+3] == TC_ENUM) { // with Ref
            p += 6; break; // scale value
          } else if (bb[p+1] == TC_NULL) {
            p += 2; break; // scale value
          } else ++p;
        } else ++p;
        found = true;
        scale = getInt();
      }
      if (found && bb[p] != TC_ENDBLOCKDATA) {
        if (bb[p] == TC_OBJECT && bb[p+1] == TC_CLASSDESC) {
          p += 2;
          while (p < bb.length) if (bb[p] == xFF) {
            return new BigDecimal(bigInteger(), scale);
          } else ++p;
        }
        if (bb[p] == xFF) return new BigDecimal(bigInteger(), scale);
        if (bb[p] == TC_OBJECT && bb[p+1] == TC_REFERENCE) {
          p += 6;
          scale = getInt();
          if (bb[p] == TC_OBJECT && bb[p+1] == TC_REFERENCE) p += 6;
          return new BigDecimal(bigInteger(), scale);
        }
      }
      ++p;
    }
    return null;
  }
  //
  private BigInteger bigInteger() {
    int sign = 1;
    while (p < bb.length) if (bb[p] == xFF) {
      p += 16;
      if (bb[p] == TC_ARRAY) sign = -1;
      else {
        sign = getInt();
        if (bb[p] == TC_ARRAY && bb[p+1] == TC_REFERENCE) {
          p += 6;
          break;
        } else if (bb[p] == TC_ARRAY && bb[p+1] == TC_CLASSDESC) {
          p += 2;
          int l = ((int)(bb[p++]&0xFF)<<8)|(int)(bb[p++]&0xFF);            
          p += l+11;// 11 = 8:serID, 1:serializable, 2:#Fields
          if (bb[p] == TC_ENDBLOCKDATA && bb[p+1] == TC_NULL) p += 2; 
        }
      }
      break;
    } else ++p;
    int l = getInt();
    byte[] val = new byte[l];
    System.arraycopy(bb, p, val, 0, l);
    p += l;
    return new BigInteger(sign, val);
  }
  //
  private String getReference() {
    if (bb[p] == TC_STRING) {
      ++p; // TC_STRING
      String s = getString();
      vRef.put(++ref, s);
      if (p < bb.length && bb[p] == TC_ARRAY && bb[p+1] == TC_REFERENCE) {
        if (nFields == 1) {
          p += 4; // TC_ARRAY+TC_REFERENCE+x00+TC_ENUM
          delta = ((int)(bb[p++]&0xFF)<<8)|(int)(bb[p++]&0xFF);
          p += 4; // dimension
        } else p += 10;
      }
      return s;
    }
    if (bb[p] == TC_REFERENCE) {
      p += 3; // TC_REFERENCE + x00 + TC_ENUM
      int x = ((int)(bb[p++]&0xFF)<<8)|(int)(bb[p++]&0xFF);
      if (nFields == 1) x -= delta;
      return (String)vRef.get(x);
    }
    return null;
  }
  //
  private void setValue(String oN, Object oV, String oT) {
    tFields.replace(oN, oT);
    vFields.put(oN, oV);
    vRef.put(++ref, oV);
  }
  //
  private void skip() {
    while ((p+1) < bb.length) if (bb[p] == TC_ENDBLOCKDATA) {
      if (bb[p+1] == TC_NULL) { 
        p += 2;
        return;
      }
      ++p;
    } else ++p;
  }
  //
  private String getString() {
    int l = ((int)(bb[p++]&0xFF)<<8)|(int)(bb[p++]&0xFF);
    try {
      String s =  new String(bb, p, l, charset);
      p += l;
      return s;
    } catch (Exception ex) { }
      String s =  new String(bb, p, l);
      p += l;
      return s;
  }
  //
  private int getInt() {
    return ((int)(bb[p++]&0xFF)<<24)|((int)(bb[p++]&0xFF)<<16)|
           ((int)(bb[p++]&0xFF)<<8)|((int)(bb[p++]&0xFF));
  }
  //
  private double getDouble() {
    double d = ByteBuffer.wrap(bb, p, 8).getDouble();
    p += 8;
    return d;
  }
  //
  private float getFloat() {
    float f = ByteBuffer.wrap(bb, p, 4).getFloat();
    p += 4;
    return f;
  }
  //
  private char getChar() {
    char c = ByteBuffer.wrap(bb, p, 2).getChar();
    p += 2;
    return c;
  }
  //
  private short getShort() {
    return (short)(((int)(bb[p++]&0xFF)<<8)|(int)(bb[p++]&0xFF));
  }
  //
  private long getLong() {
    return ((long)(bb[p++]&0xFF)<<56)|((long)(bb[p++]&0xFF)<<48)|
           ((long)(bb[p++]&0xFF)<<40)|((long)(bb[p++]&0xFF)<<32)|
           ((long)(bb[p++]&0xFF)<<24)|((long)(bb[p++]&0xFF)<<16)|
           ((long)(bb[p++]&0xFF)<<8)|((long)(bb[p++]&0xFF));
  }
  //
  private int getArrayType(String name) {
    int q = p;
    if (bb[p] == TC_ARRAY && bb[p+1] == TC_CLASSDESC) {
      q = p+2;
      int le = ((int)(bb[q++]&0xFF)<<8)|(int)(bb[q++]&0xFF);
      p = q+le;
      if (bb[p] != x00) {
        while ((p-1) < bb.length)
        if (bb[p] == TC_ENDBLOCKDATA && bb[p+1] == TC_NULL) break;
        else ++p;
      }    
      return getDimensions();
    }
    dim1 = -1; dim2 = -1; le = -1;
    if (bb[p] == TC_ARRAY && bb[p+1] == TC_REFERENCE) {
      p += 6; // TC_OBJECT+TC_REFERENCE+x00+TC_ENUM+2:ENUM
      while ((p+1) < bb.length) {
        if (dim1 < 0) dim1 = getInt();
        else if (dim2 < 0) dim2 = getInt();
        else le = getInt();
        if (bb[p] != TC_ARRAY && bb[p+1] != TC_REFERENCE) {
          if (le < 0) {
            le = dim1;
            dim1 = dim2;
            dim2 = -1;
          }
          return p;
        } else if (bb[p] == TC_ARRAY && bb[p+1] == TC_REFERENCE) {
          p += 6; // TC_OBJECT+TC_REFERENCE+x00+TC_ENUM+2:ENUM
        } else ++p;
      }
    }
    return q;
  }
  //
  private int getDimensions() {
    int q = p;
    dim1 = -1; dim2 = -1; le = -1;
    for (; (p+1) < bb.length; ++p) {
      if (bb[p] == TC_ENDBLOCKDATA && bb[p+1] == TC_NULL) {
        p += 2;
        if (bb[p+4] == TC_ARRAY && bb[p+5] == TC_REFERENCE) {
          while ((p+5) < bb.length) {
            if (dim1 < 0) dim1 = getInt();
            else if (dim2 < 0) dim2 = getInt();
            else le = getInt();
            if (bb[p] != TC_ARRAY && bb[p+1] != TC_REFERENCE) {
              if (le < 0) {
                le = dim1;
                dim1 = dim2;
                dim2 = -1;
              }
              return p;
            } else if (bb[p] == TC_ARRAY && bb[p+1] == TC_REFERENCE) {
              p += 6; // TC_OBJECT+TC_REFERENCE+x00+TC_ENUM+2:ENUM
            } else ++p;
          }
          return q;
        } else if (bb[p+4] != TC_ARRAY || bb[p+5] != TC_CLASSDESC ) {
          le = getInt();
          // is Object?
          if (bb[p] == TC_OBJECT) {
            if (bb[p+1] == TC_REFERENCE) {
              p += 6; // TC_OBJECT+TC_REFERENCE+x00+TC_ENUM+2:ENUM
            } else if (bb[p+1] == TC_CLASSDESC) {
              q = p; p += 2;
              int l = ((int)(bb[p++]&0xFF)<<8)|(int)(bb[p++]&0xFF);
              if ((new String(bb, p, l)).indexOf("Big") >= 0) p = q;
              else { // not BigInteger nor BigDecimal
                while ((p+2) < bb.length) if (bb[p] == TC_ENDBLOCKDATA && bb[p+1] == TC_NULL) {
                  p += 2;
                  break;
                } else ++p; 
              }              
            }
          }
          return p;          
        }
        if (dim1 < 0) dim1 = getInt();
        else dim2 = getInt();
      }        
    }
    return q;
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
  // Serializeation protocol
  final byte x00 = (byte)0x00;
  final byte xFF = (byte)0xFF;
  final byte TC_NULL = (byte)0x70;
  final byte TC_REFERENCE = (byte)0x71;
  final byte TC_CLASSDESC = (byte)0x72;
  final byte TC_OBJECT = (byte)0x73;
  final byte TC_STRING = (byte)0x74;
  final byte TC_ARRAY = (byte)0x75;
  final byte TC_CLASS = (byte)0x76;
  final byte TC_BLOCKDATA = (byte)0x77;
  final byte TC_ENDBLOCKDATA = (byte)0x78;
  final byte TC_RESET = (byte)0x79;
  final byte TC_BLOCKDATALONG = (byte)0x7A;
  final byte TC_EXCEPTION = (byte)0x7B;
  final byte TC_LONGSTRING = (byte) 0x7C;
  final byte TC_PROXYCLASSDESC = (byte) 0x7D;
  final byte TC_ENUM = (byte) 0x7E;
  
  final byte SC_WRITE_METHOD = 0x01; //if SC_SERIALIZABLE
  final byte SC_BLOCK_DATA = 0x08;   //if SC_EXTERNALIZABLE
  final byte SC_SERIALIZABLE = 0x02;
  final byte SC_EXTERNALIZABLE = 0x04;
  final byte SC_ENUM = 0x10;
  //
  private byte[] bb;
  private long serID;
  private Object object;
  private boolean found = false;
  private String clsName, objName, listType;
  private int nFields, p, dim1, dim2, le, ref, delta;
  //
  private String charset = "UTF-8";
  private ArrayList<String> fNames = new ArrayList<>(); 
  private HashMap<Integer, Object> vRef = new HashMap<>();
  private HashMap<String, Object> vFields = new HashMap<>();
  private HashMap<String, String> tFields = new HashMap<>();
}
