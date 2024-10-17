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
      StringBuilder sb = new StringBuilder(inp);
      // your own encrypting algorithm
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
    if (inp != null) try {
      StringBuilder sb = new StringBuilder(inp);
      // your own decrypting algorithm
      return sb.toString();
    } catch (Exception ex) { }
    throw new Exception("Input String:"+inp+" is too small.");
  }
}
