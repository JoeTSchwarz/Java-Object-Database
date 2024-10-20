import joeapp.odb.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.nio.file.*;
// @author Joe T. Schwarz (c)
//The ThePeople in SWING_MVC View
public class CreatePeopleODB {
  public CreatePeopleODB( ) throws Exception {
    ODBConnect con = new ODBConnect("localhost", 9999, "erika", "joe");
    con.autoCommit(true);
    con.connect(dbName);
    List<String> lines = Files.readAllLines((new File("people.txt")).toPath());
    int i, len = lines.size();
    //
    for (i = 0; i < len; i += 6) {
      char c = lines.get(i).charAt(0);
      if (c == '+') break;
      if (c == '-') ++i;
      if (!con.isExisted(dbName, lines.get(i))) {
        System.out.println("1.Create:"+lines.get(i));
        con.add(dbName, lines.get(i), new People(lines.get(i), 
                                       Integer.parseInt(lines.get(i+1)),
                                       lines.get(i+2),
                                       lines.get(i+3),
                                       Double.parseDouble(lines.get(i+4)),
                                       lines.get(i+5)));
        }
    }
    con.commit(dbName);
    con.close(dbName);
    con.disconnect();
    //
    con = new ODBConnect("localhost", 8888, "erika", "joe");
    con.autoCommit(true);
    con.connect(dbName);
    for (++i; i < len; i += 6) {
      if (lines.get(i).charAt(0) == '-') ++i;
      if (!con.isExisted(dbName, lines.get(i))) {
        System.out.println("2. Create:"+lines.get(i));
        con.add(dbName, lines.get(i), new People(lines.get(i), 
                                       Integer.parseInt(lines.get(i+1)),
                                       lines.get(i+2),
                                       lines.get(i+3),
                                       Double.parseDouble(lines.get(i+4)),
                                       lines.get(i+5)));
      }
    }
    con.commit(dbName);
    con.close(dbName);
    con.disconnect();
    System.out.println("Done");
    System.exit(0);
  }
  private String dbName = "people";
  //
  public static void main(String... a) throws Exception {
    new CreatePeopleODB( );
  }
}

