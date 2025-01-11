package joeapp.odb;
/**
Encrypt and Decrypt Algorithm
@author Joe T. Schwarz (c)
*/
public class EnDecrypt {
  /**
    proprietary decrypt algorithm
    @param inp String to be decrypted
    @return decrypted String (null if invalid inp)
    @exception Exception thrown if inp is NOT encrypted by encrypt()
  */
  public static String decrypt(String inp) throws Exception {
    if (inp != null) try {
      int mx = inp.length(), le = (mx/5)-1;
      StringBuilder sb = new StringBuilder( );
      int b = Integer.parseInt(inp.substring(0, 5)), a = ((b%le)+1)*5, j = 1; 
      int p = (int)inp.charAt(b%5), f = Integer.parseInt(inp.substring(a, a+5))/p;
      //
      sb.append((char)(b/f));
      for (int i = 5; i < a; i += 5, ++j) 
      sb.append((char)((Integer.parseInt(inp.substring(i, i+5))/f)-p-j));
      for (int i = a+5; i < mx; i += 5, ++j) 
      sb.append((char)((Integer.parseInt(inp.substring(i, i+5))/f)-p-j));
      return sb.toString();
    } catch (Exception ex) { }
    throw new Exception("Unknown or corrupted Encrypted String:"+inp);
  }
  /**
    proprietary encrypt algorithm
    @param inp String to be encrypted
    @return encrypted String 
    @exception Exception thrown if the inp is less/equal than 5 letters
  */
  public static String encrypt(String inp) throws Exception {
    if (inp != null && inp.length() >= 5) try {
      int le = inp.length(), f = 0;
      StringBuilder sb = new StringBuilder( );
      while (f < 3 || f > 256) f = (int)(Math.random()*2550);
      int b = ((int)inp.charAt(0))*f, a = (b%le)+1;
      //
      sb.append(String.format("%05d", b)); int p = (int)sb.charAt(b%5);
      for (int i = 1; i < a; ++i) sb.append(String.format("%05d",(p+i+(int)inp.charAt(i))*f));
      sb.append(String.format("%05d", f*p));
      for (int i = a; i < le; ++i) sb.append(String.format("%05d",(p+i+(int)inp.charAt(i))*f));
      return sb.toString();
    } catch (Exception ex) { }
    throw new Exception("Input String:"+inp+" is too small.");
  }
}