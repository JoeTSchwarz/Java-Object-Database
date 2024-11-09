import joeapp.odb.*;
// @author Joe T. Schwarz (c)
public class CreateOODBMember {
  public static void main(String... args) {
    try {
      ODBConnect dbcon = new ODBConnect("localhost", 9999, "tester", "test");
      System.out.println("Connected to localhost:9999");
      dbcon.connect(dbName); // create if needed
      dbcon.autoCommit(true);
      String[] cities = { "San Diego", "New York", "Tampa", "New Orlean",
                          "San Francisco", "New Haven", "Santa Clara",
                          "Eureka", "Sacramento", "Houston", "Dallas",
                          "Santa Barbara", "Phoenix", "Los Angeles", "Seattle",
                          "New Haven", "New Mexico", "Scottdale", "Miami" };
      java.util.Random ran = new java.util.Random();
      for (int a, n, i = 0; i < 1000; ++i) try {
        while ((n = Math.abs(ran.nextInt(100000))) < 10000);
        while ((a = Math.abs(ran.nextInt(60))) < 20) ;
        if (!dbcon.isExisted(dbName, "Joe"+n)) {
          dbcon.add(dbName, "Joe"+n, new Member("Joe"+n, a, 
                    cities[ran.nextInt(cities.length)], n*2.0));
        }
        while ((n = Math.abs(ran.nextInt(100000))) < 10000);
        while ((a = Math.abs(ran.nextInt(60))) < 20) ;
        if (!dbcon.isExisted(dbName, "Bob"+n))
          dbcon.add(dbName, "Bob"+n, new Member("Bob"+n, a,
                    cities[ran.nextInt(cities.length)], n*2.0));
        while ((n = Math.abs(ran.nextInt(100000))) < 10000);
        while ((a = Math.abs(ran.nextInt(60))) < 20) ;
        if (!dbcon.isExisted(dbName, "Billy"+n))
          dbcon.add(dbName, "Billy"+n, new Member("Billy"+n, a,
                    cities[ran.nextInt(cities.length)], n*2.0));
        while ((n = Math.abs(ran.nextInt(100000))) < 10000);
        while ((a = Math.abs(ran.nextInt(60))) < 20) ;
        if (!dbcon.isExisted(dbName, "Teddy"+n))
          dbcon.add(dbName, "Teddy"+n, new Member("Teddy"+n, a,
                    cities[ran.nextInt(cities.length)], n*2.0));
        while ((n = Math.abs(ran.nextInt(100000))) < 10000);
        while ((a = Math.abs(ran.nextInt(60))) < 20) ;
        if (!dbcon.isExisted(dbName, "Joey"+n))
          dbcon.add(dbName, "Joey"+n, new Member("Joey"+n, a,
                    cities[ran.nextInt(cities.length)], n*2.0));
        while ((n = Math.abs(ran.nextInt(100000))) < 10000);
        while ((a = Math.abs(ran.nextInt(60))) < 20) ;
        if (!dbcon.isExisted(dbName, "Bobby"+n))
          dbcon.add(dbName, "Bobby"+n, new Member("Bobby"+n, a,
                    cities[ran.nextInt(cities.length)], n*2.0));
        while ((n = Math.abs(ran.nextInt(100000))) < 10000);
        while ((a = Math.abs(ran.nextInt(60))) < 20) ;
        if (!dbcon.isExisted(dbName, "Willy"+n))
          dbcon.add(dbName,"Willy"+n, new Member("Willy"+n, a,
                    cities[ran.nextInt(cities.length)], n*2.0));
        while ((n = Math.abs(ran.nextInt(100000))) < 10000);
        while ((a = Math.abs(ran.nextInt(60))) < 20) ;
        if (!dbcon.isExisted(dbName, "Ted"+n))
          dbcon.add(dbName, "Ted"+n, new Member("Ted"+n, a,
                  cities[ran.nextInt(cities.length)], n*2.0));
        while ((n = Math.abs(ran.nextInt(100000))) < 10000);
        while ((a = Math.abs(ran.nextInt(60))) < 20) ;
        if (!dbcon.isExisted(dbName, "Frank"+n))
          dbcon.add(dbName, "Frank"+n, new Member("Frank"+n, a,
                    cities[ran.nextInt(cities.length)], n*2.0));
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      dbcon.save(dbName);
      dbcon.close(dbName);
      dbcon.disconnect();
      java.util.concurrent.TimeUnit.SECONDS.sleep(1);
      dbcon = new ODBConnect("localhost", 9999, "tester", "test");
      System.out.println("Reconnected to localhost:9999");
      dbcon.connect(dbName); // create if needed
      System.out.println("Number of Members:"+dbcon.getKeys(dbName).size());
      dbcon.close(dbName);
      dbcon.disconnect();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    System.exit(0);
  }
  private static String dbName = "Members";
}

