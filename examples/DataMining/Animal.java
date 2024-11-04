import java.io.*;
import java.awt.*;
import java.net.URL;
import javax.swing.*;
// @author Joe T. Schwarz (c)
public class Animal implements java.io.Serializable {
  private static final long serialVersionUID = 1234L;
  private ImageIcon img;
  private double weight;
  private String name, pic;
  public Animal(String name, String pic, double weight) {
    this.pic = pic;
    this.name = name;
    this.weight = weight;
  }
  public ImageIcon getImage( ) {
    if (pic.indexOf("://") < 0 && (new File(pic)).exists()) return new ImageIcon(pic);
    try {
      return new ImageIcon(new URL(pic));
    } catch (Exception e) { }
    return null;
  }
  public void setWeight(double w) {
    weight = w;
  }
  public String toString() {
    return "ZooAnimal:"+name+" has a weight "+weight+"Kg. Image@pic:"+pic;
  }
  public void print() {
    System.out.println("ZooAnimal:"+name+" has a weight "+weight);
  }
  public void print(OutputStream out) {
    try {
      out.write(("ZooAnimal:"+name+" has a weight "+weight).getBytes());
      out.flush();
    } catch (Exception e) {
      System.out.println("Troubles with OutputStream out");
    }
  }
  public void show(JFrame jf) {
    new Display(jf, getImage( ));
  }
  //
  private class Display extends JDialog {
    public Display(JFrame jf, ImageIcon icon) {
      super(jf, true);      
      setImage(jf, icon);
    }
    private void setImage(JFrame jf, ImageIcon img) { 
      setTitle("Animal:"+name);
      setDefaultCloseOperation(DISPOSE_ON_CLOSE);
      JButton but = new JButton(img);
      but.setContentAreaFilled(false);
      but.setBorderPainted(false);
      add(but, BorderLayout.CENTER);
      setSize(img.getIconWidth()+40, img.getIconHeight()+40);
      //
      setLocationRelativeTo(jf);
      setVisible(true);
    }
  }
}
