package joeapp.odb;
//
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import java.lang.reflect.*;
//
import java.nio.*;
import  java.nio.file.*;
import java.nio.channels.*;
import java.util.concurrent.*;
/**
 XML parser
 @author Joe T. Schwarz(C)
*/
public class ODBParser {
  /**
  split faster than String.split(regEx). Example: split(myString, myPattern)
  @param str  String, String to be splitted
  @param pat  String, pattern
  @return String array
  */
  public static String[] split(String str, String pat) {
    int le = pat.length();
    List<String> lst = new ArrayList<String>();
    if (le == 0) for (int a = 0, mx = str.length(); a < mx; ++a) lst.add(""+str.charAt(a));
    else for (int a = 0, b = 0, mx = str.length(); a < mx; a = b + le) {
      b = str.indexOf(pat, a);
      if (b < 0) {
        lst.add(str.substring(a));
        break;
      }
      lst.add(str.substring(a, b));
    }
    return lst.toArray(new String[lst.size()]);
  }
  /**
  JODB Property parser.
  @param config String, Config file name
  @return HashMap of Properties with key = name, value = property. NULL if config is invalid
  @exception Exception thrown by invalid ConfigFile.
  */
  public static HashMap<String, String> odbProperty(String config) throws Exception {
    String X = (new String(Files.readAllBytes((new File(config)).toPath()))).
               replace("< ", "<").replace("> ", ">").replace(" />", "/>").replace("\r","");
    // remove all comments
    for (int e, b; ;) {
      b = X.indexOf("<!");
      if (b < 0) break;
      e = X.indexOf("!>", b);
      if (e < 0) throw new Exception("Invalid ConfigFile:"+config);
      X = X.replace(X.substring(b, e+2), "");
    }
    // clean up empty lines
    String[] lines = X.split("\n");
    ArrayList<String> al = new ArrayList<>();
    for (String l : lines) if (l.trim().length() > 0) al.add(l.trim());
    if (al.size() == 0) throw new Exception("Invalid ConfigFile:"+config);
    // build a HashMap as ODB properties
    StringBuilder sb = new StringBuilder();
    HashMap<String, String> hm = new HashMap<>();
    for (int p, b, e, i = 0, siz = al.size(); i < siz; ++i) {
      String s = al.get(i); // line by line
      b = s.indexOf(">"); p = s.indexOf("<");
      if (b < p || s.indexOf("<<") >= 0 || s.indexOf(">>") >= 0) 
        throw new Exception("Invalid ConfigFile:"+config);
      // Mapping key/value
      if (p >= 0) {
        b = s.indexOf(">", ++p);
        if (b > 0) {
          sb.setLength(0); // reset sb
          String key = s.substring(p, b++);
          e = s.indexOf("/>", b);
          // continue in next line?
          while (e < 0 && ++i < siz) {
            sb.append(s.substring(b).trim());
            e = s.indexOf("/>");
            s = al.get(i);
            b = 0;
          }
          if (e < b || i >= siz) throw new Exception("Invalid ConfigFile:"+config);
          sb.append(s.substring(b, e).trim());
          hm.put(key, sb.toString());
        }
      }
    }
    return hm;
  }
}