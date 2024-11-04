import joeapp.odb.*;
// @author Joe T. Schwarz (c)
public class OODBmix {
  public static void main(String... args) {
    try {
      ODBConnect dbcon = new ODBConnect("localhost", 8888, "erika", "joe");
      System.out.println("Connected to OODB-Server@8888");
      dbcon.connect("OODBmix"); // create if needed
      if (!dbcon.autoCommit(true)) {
        System.out.println("Unable to set autoCommit() to OODB-Server@8888");
        System.exit(0);
      }
      if (!dbcon.isExisted("OODBmix", "Joe")) {
        // Object Member
        dbcon.add("OODBmix", "Joe", new Member("Joe", 40, "San Diego", 1000000.0));
        dbcon.add("OODBmix", "Bob", new Member("Bobby", 45, "New York", 100000.0));
    
        // Object Animal
        dbcon.add("OODBmix", "bear", new Animal("bear", "https://encrypted-tbn3.gstatic.com/images?q=tbn:ANd9GcQtAZFuz4hyzoDAGqv-M88eHJxYX_jV7-TE1qJdzecL86gpOaMq8g", 500.0));
        dbcon.add("OODBmix", "fasan", new Animal("pheasant", "http://t1.gstatic.com/images?q=tbn:ANd9GcQ1skdj5XcpP55D56uLWEdw10zMEKuVaX_uNNR670QOEmw23ILisldQAQ", 3.0));
    
        // Object String (plain text)
        dbcon.add("OODBmix", "joe", "Joe Nartca, PhD nuclear Physicist");
        dbcon.add("OODBmix", "donald", "Donald Trump, President of the USA");
    
        // save
        dbcon.save("OODBmix");
      }
      dbcon.close("OODBmix");
      java.util.concurrent.TimeUnit.MILLISECONDS.sleep(200);
      dbcon.disconnect();
      dbcon = new ODBConnect("localhost", 9999, "erika", "joe");
      System.out.println("Connected to OODB-Server@9999");
      dbcon.connect("OODBmix"); // create if needed
      if (!dbcon.autoCommit(true)) {
        System.out.println("Unable to set autoCommit() to OODB-Server@9999");
        System.exit(0);
      }
      if (!dbcon.isExisted("OODBmix", "Bill")) {
        // Object Member
        dbcon.add("OODBmix", "Bill", new Member("Billy", 50, "Tampa", 500000.0));
        dbcon.add("OODBmix", "Ted", new Member("Teddy", 55, "New Orlean", 550000.0));
    
        // Object Animal
        dbcon.add("OODBmix", "peacock", new Animal("peacock", "https://encrypted-tbn1.gstatic.com/images?q=tbn:ANd9GcSrwZhKZzpcazX5KI_UiJkDx_EB22yrX4yzwldJbazcACFW8DIp", 5.0));
        dbcon.add("OODBmix", "macaw", new Animal("macaw", "http://i.telegraph.co.uk/multimedia/archive/01846/macaw_1846932c.jpg", 3.0));
    
        // Object String (plain text)
        dbcon.add("OODBmix", "vladimir", "Vladimir Putin, President of Russia");
        dbcon.add("OODBmix", "ping", "Xhit CheatPig, President of Cheatna");
    
        // save
        dbcon.save("OODBmix");
      } 
      dbcon.close("OODBmix");
      java.util.concurrent.TimeUnit.MILLISECONDS.sleep(200);
      dbcon.disconnect();
    } catch (Exception ex) {
      ex.printStackTrace();
      System.out.println("Troubles:"+ex.toString());
    }
    System.exit(0);
  }
}

