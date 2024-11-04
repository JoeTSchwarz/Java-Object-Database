public class Member implements java.io.Serializable {
  private static final long serialVersionUID = 1234567890L;
  private int age;
  private double salary;
  private String name, address;
  public Member(String name, int age, String address, double salary) {
    this.age = age;
    this.name = name;
    this.address = address;
    this.salary = salary;
  }
  public String toString() {
    return "Name:\t"+name+"\nAge:    \t"+age+"\nAddr.:  \t"+address+"\nSalary:\t"+salary;
  }
  public void print() {
    System.out.println("Name:\t"+name+
                       "\nAge:    \t"+age+
                       "\nAddr.:  \t"+address+
                       "\nSalary:\t"+salary);
  }
}
