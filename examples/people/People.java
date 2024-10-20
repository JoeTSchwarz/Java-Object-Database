import java.io.*;
import java.awt.*;
import java.net.URL;
import javax.swing.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
// @author Joe T. Schwarz (c)
public class People implements java.io.Serializable {
  private static final long serialVersionUID = 1234L;
  public People(String name, int age, String address,
                String profession, double income, String url) {
    this.age = age;
    this.url = url;
    this.name = name;
    this.income = income;
    this.address = address;
    this.profession = profession;
  }
  private int age;
  private double income;
  private String name, url, address, profession;
  public String toString() {
    return "Name: "+name+", Age: "+age+", Addr.: "+address+", Income: "+income+
           ", Profession: "+profession;
  }
  public String[] getData() {
    return new String[] {""+age, address, profession, ""+income, url };
  }
  public void print() {
    System.out.println("Name:\t\t"+name+System.lineSeparator()+
                       "Age:\t\t"+age+System.lineSeparator()+
                       "Addr.:\t\t"+address+System.lineSeparator()+
                       "Income:\t\t"+income+System.lineSeparator()+
                       "Profession:\t"+profession+System.lineSeparator());
  }
  public void picture(JFrame jf) {
    (new Picture(jf)).setVisible(true);
  }
  class Picture extends JDialog {
    public Picture(JFrame jf) {
      setTitle(name);
      setDefaultCloseOperation(DISPOSE_ON_CLOSE);
      JButton but = getButton( );
      but.setContentAreaFilled(false);
      but.setBorderPainted(false);
      add(but, BorderLayout.CENTER);     
      setLocationRelativeTo(jf);
      pack();
    }
    //
    private JButton getButton( ) {
      try {
        int w, h;
        ByteArrayInputStream bis = null;
        if (url.indexOf("://") > 0) {
          byte[] buf = new byte[65536];
          ByteArrayOutputStream bao = new ByteArrayOutputStream();
          InputStream is = (new URL(url)).openStream();
          while ((w = is.read(buf)) != -1) bao.write(buf, 0, w);
          bis = new ByteArrayInputStream(bao.toByteArray());
        } else if ((new File(url)).exists()) {
          FileInputStream fis = new FileInputStream(url);
          byte[] buf = new byte[fis.available()];
          w = fis.read(buf);
          bis = new ByteArrayInputStream(buf, 0, w);
        } else { // something wrong with url
          JButton but = new JButton("NO IMAGE");
          return but;
        }
        BufferedImage img = ImageIO.read(bis);
        double ratio = 1d;
        w = img.getWidth();
        h = img.getHeight();
        // resizing the image ?
        if (h > 2500)  ratio = 0.15d;
        else if (h > 2000)  ratio = 0.2d;
        else if (h > 1500)  ratio = 0.25d;
        else if (h > 1000)  ratio = 0.3d;
        else if (h > 900)  ratio = 0.35d;
        else if (h > 800)  ratio = 0.4d;
        else if (h > 700)  ratio = 0.45d;
        else if (h > 600)  ratio = 0.5d;
        else if (h > 500)  ratio = 0.6d;
        //
        if (w > 3000) ratio = 0.1d;
        else if (w > 2500) ratio = 0.15d;
        else if (w > 2000) ratio = 0.2d;
        else if (w > 1500) ratio = 0.25d;
        else if (w > 1400) ratio = 0.30d;
        else if (w > 1200) ratio = 0.35d;
        else if (w > 1000) ratio = 0.4d;
        else if (w > 900) ratio = 0.5d;
        else if (w > 800) ratio = 0.6d;
        else if (w > 700) ratio = 0.7d;
        if (ratio < 1d) {
          // resizing the image
          w = (int)(w * ratio);
          h = (int)(h * ratio);
          Image image = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
          img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
          img.getGraphics().drawImage(image, 0, 0 , null);
        }
        JButton but = new JButton(new ImageIcon(img));
        return but;
      } catch (Exception ex) { }
      JButton but = new JButton("NO IMAGE");
      return but;
    }
  }
}
