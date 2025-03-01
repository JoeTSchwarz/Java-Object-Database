package joeapp.odb;
//
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.text.*;
/**
Tools for Java Swing
@author Joe T. Schwarz (c)
*/
public class JOptions {
  /**
  question
  @param  frame   - JFrame the calling frame (owner) or NULL
  @param  tit     - string title
  @param  ask     - String, the question
  @return booean yes or no
  */
  public static boolean question(JFrame frame, String tit, String ask) {
    return JOptionPane.showConfirmDialog(frame, ask, tit, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
  }
  /**
  multipleInputs with Labels. The number of input fields is equal the number of the given lab array.
  @param  frame   - JFrame the calling frame (owner) or NULL
  @param  lab     - string array for Labels
  @param  tit     - String, Title
  @return String array of inputs (incl. empty strings)
  */
  public static String[] multipleInputs(JFrame frame, String[] lab, String tit) {
    return multipleInputs(frame, lab, null, tit);
  }
  /**
  multipleInputs with Labels and defaults. The number of input fields is equal the number of the given lab array.
  <br> defaults can be null.
  @param  frame   - JFrame the calling frame (owner) or NULL
  @param  lab     - string array for Labels
  @param  obj     - Default object array (object as a string or a string array
  @param  tit     - String, Title
  @return String array of inputs (incl. empty strings)
  */
  @SuppressWarnings({"unchecked", "deprecated"}) 
  public static String[] multipleInputs(JFrame frame, String[] lab, Object obj[], String tit) {
    GridBagLayout layout = new GridBagLayout();
    GridBagConstraints left = new GridBagConstraints();
    left.anchor = GridBagConstraints.EAST;
    GridBagConstraints right = new GridBagConstraints();
    right.anchor = GridBagConstraints.WEST;
    right.gridwidth = GridBagConstraints.REMAINDER;
    
    JPanel all = new JPanel(layout);
    String[] inp = new String[lab.length];
    JComponent jCom[] = new JComponent[lab.length];
    JDialog dia = (new JOptionPane(all)).createDialog(frame, tit);
    dia.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    //
    int idx = -1;
    setMenu(dia);
    for (int i = 0; i < lab.length; ++i) {
      JLabel jlab = new JLabel(lab[i]+" ");
      layout.setConstraints(jlab, left);
      all.add(jlab);
      //
      if (obj == null) {
        jCom[i] = new JTextField(24);
        jCom[i].addMouseListener(new CopyCutPaste((JTextField)jCom[i]));
        if (idx < 0) idx = i;
      } else {
        if (obj[i] instanceof String[]) {
          jCom[i] = new JComboBox<String>((String[])obj[i]);
        } else { // String or null
          jCom[i] = new JTextField(24);
          if (obj[i] != null) ((JTextField)jCom[i]).setText((String)obj[i]);
          jCom[i].addMouseListener(new CopyCutPaste((JTextField)jCom[i]));
          if (idx < 0) idx = i;
        }
      }
      layout.setConstraints(jCom[i], right);
      all.add(jCom[i]);
    }
    if (idx >= 0) dia.addComponentListener(listener(jCom[idx]));
    CloseWindow cs = new CloseWindow(dia);
    dia.addWindowListener(cs);
    dia.pack();
    dia.setVisible(true);
    if (!cs.closed) for (int i = 0; i < inp.length; ++i) {
      if (obj != null && obj[i] instanceof String[]) 
        inp[i] = (String)((JComboBox<String>)jCom[i]).getSelectedItem();
      else inp[i] = ((JTextField)jCom[i]).getText().trim(); // String or 0 length
    }
    dia.dispose();
    return inp;
  }
  /**
  dynamicInputs. Return a list of inputs (String) if OK or CLOSE is clicked.
  <br>Previous inputs can be modified at anytime.
  <br>Note: all leading and ending blanks are truncated. Input with 0 length is ignored.
  @param frame JFrame, null to display JOptions in the center of screen
  @param tit title String
  @return list of input string (without empty string)
  */
  public static java.util.List<String> dynamicInputs(JFrame frame, String tit) {
    return dynamicInputs(frame, tit, -1);
  }
  /**
  dynamicInputs. Return a list of inputs (String) if OK or CLOSE is clicked.
  <br>Previous inputs can be modified at anytime.
  <br>Note: all leading and ending blanks are truncated. Input with 0 length is ignored.
  @param frame JFrame, null to display JOptions in the center of screen
  @param tit title String
  @param msgType int, JOptionPane.MessageType (e.g. JOptionPane.INFORMATION_MESSAGE, JOptionPane.QUESTION_MESSAGE, etc.) 
  @return list of input string (without empty string)
  */
  public static java.util.List<String> dynamicInputs(JFrame frame, String tit, int msgType) {
    java.util.List<String> list = new java.util.ArrayList<>();
    JPanel pan = new JPanel(new GridLayout(1,0));
    JTextField tf = new JTextField(24);
    pan.add(tf);
    JDialog dia;
    if (msgType >= 0) {
      JOptionPane op = new JOptionPane(pan);      
      op.setMessageType(msgType);        
      dia = op.createDialog(frame, tit);
    } else dia = (new JOptionPane(pan)).createDialog(frame, tit);
    //
    setMenu(dia);
    dia.addComponentListener(listener(tf));
    // recursive until OK or CLOSE is clicked
    tf.addMouseListener(new CopyCutPaste(tf));
    tf.addActionListener(e->{
      Component com[] = pan.getComponents();
      int n = 1; // set counter
      for (Component C:com) {
        if (((JTextField)C).getText().trim().length() > 0) ++n;
        else pan.remove(C);
      }
      JTextField jt = new JTextField();
      jt.addActionListener(tf.getActionListeners()[0]);
      pan.setLayout(new GridLayout(n, 0));
      pan.add(jt);
      jt.requestFocusInWindow();
      pan.validate();
      dia.pack();
    });
    CloseWindow cs = new CloseWindow(dia);
    dia.addWindowListener(cs);
    dia.pack();
    dia.setVisible(true);
    // Load list
    list.clear();
    if (!cs.closed) {
      Component com[] = pan.getComponents();
      for (Component C:com) {
        String S = ((JTextField)C).getText().trim();
        if (S.length() > 0) list.add(S);
      }
    }
    dia.dispose();
    return list;
  }
  /**
  changePW 
  @param frame JFrame, null to display JOptions in the center of screen
  @return string array: old_PW, new_PW, confirmed_PW.
  */
  public static String[] changePW(JFrame frame) {
    JLabel jpw  = new JLabel("old PW  ");
    JPasswordField pw = new JPasswordField(24);
    pw.addMouseListener(new CopyCutPaste(pw));
    JTextField tf  = new JTextField(24);
      
    JLabel jnpw = new JLabel("new PW  ");
    JPasswordField npw = new JPasswordField(24);
    npw.addMouseListener(new CopyCutPaste(npw));
    JTextField ntf  = new JTextField(24);
      
    JLabel jcpw = new JLabel("PW again");
    JPasswordField cpw = new JPasswordField(24);
    cpw.addMouseListener(new CopyCutPaste(cpw));
    JTextField ctf = new JTextField(24);
      
    JCheckBox cb = new JCheckBox("Show");
    JLabel emp  = new JLabel("  ");
    // create a panel for npw/ntf and cb in 2 rows
    GridBagLayout layout = new GridBagLayout();
    JPanel pan = new JPanel(layout);
      
    GridBagConstraints left = new GridBagConstraints();
    left.anchor = GridBagConstraints.EAST;
    GridBagConstraints right = new GridBagConstraints();
    right.anchor = GridBagConstraints.WEST;
    right.gridwidth = GridBagConstraints.REMAINDER;
      
    layout.setConstraints(emp, left);
    pan.add(emp);
    layout.setConstraints(cb, right);
    pan.add(cb);
      
    layout.setConstraints(jpw, left);
    pan.add(jpw);
    layout.setConstraints(pw, right);
    pan.add(pw);
      
    layout.setConstraints(jnpw, left);
    pan.add(jnpw);
    layout.setConstraints(npw, right);
    pan.add(npw);
      
    layout.setConstraints(jcpw, left);
    pan.add(jcpw);
    layout.setConstraints(cpw, right);
    pan.add(cpw);
      
    // convert JOptionPane to JDialog
    JDialog dia = (new JOptionPane(pan)).createDialog(frame, "Change Password");
    dia.addComponentListener(listener(pw));
    // toggle between Show/hide
    cb.addItemListener(e->{
      if (cb.isSelected()) {
        if (tf.getText().length() == 0) tf.requestFocusInWindow();
        else ntf.requestFocusInWindow();
        tf.setText(new String(pw.getPassword()));
        pan.remove(pw);
        layout.setConstraints(jpw, left);
        pan.add(jpw);
        layout.setConstraints(tf, right);
        pan.add(tf);
          
        ntf.setText(new String(npw.getPassword()));
        pan.remove(npw);
        layout.setConstraints(jnpw, left);
        pan.add(jnpw);
        layout.setConstraints(ntf, right);
        pan.add(ntf);
          
        ctf.setText(new String(cpw.getPassword()));
        pan.remove(cpw);
        layout.setConstraints(jcpw, left);
        pan.add(jcpw);
        layout.setConstraints(ctf, right);
        pan.add(ctf);          
      } else {
        if (ntf.getText().length() == 0) pw.requestFocusInWindow();
        else npw.requestFocusInWindow();
        pw.setText(tf.getText());
        pan.remove(tf);
        layout.setConstraints(jpw, left);
        pan.add(jpw);
        layout.setConstraints(pw, right);
        pan.add(pw);
         
        npw.setText(ntf.getText());
        pan.remove(ntf);
        layout.setConstraints(jnpw, left);
        pan.add(jnpw);
        layout.setConstraints(npw, right);
        pan.add(npw);        
          
        cpw.setText(ctf.getText());
        pan.remove(ctf);
        layout.setConstraints(jcpw, left);
        pan.add(jcpw);
        layout.setConstraints(cpw, right);
        pan.add(cpw);
      }
      pan.validate();
    });
    CloseWindow cs = new CloseWindow(dia);
    dia.addWindowListener(cs);
    setMenu(dia);
    dia.pack();
    dia.setVisible(true);
    if (cs.closed) return null;
    dia.dispose();
    if (cb.isSelected()) return new String[] { tf.getText().trim(),
                                               ntf.getText().trim(),
                                               ctf.getText().trim()
                                             };
    return new String[] { (new String(pw.getPassword()).trim()),
                          (new String(npw.getPassword()).trim()),
                          (new String(cpw.getPassword()).trim())
                        };
  }
  /**
  password allows to obfuscate the input characters.
  @param frame JFrame, null to display JOptions in the center of screen
  @param tit title String
  @return password string
  */
  public static String password(JFrame frame, String tit) {
    JPasswordField pf = new JPasswordField(24);
    JCheckBox cb = new JCheckBox("Show");
    JTextField tf = new JTextField(24);
    // create a panel for pf/tf and cb in 2 rows
    JPanel pan = new JPanel(new java.awt.GridLayout(2,0));
    pan.add(cb); pan.add(pf);
    // convert JOptionPane to JDialog
    JDialog dia = (new JOptionPane(pan)).createDialog(frame, tit);
    dia.addComponentListener(listener(pf));
    // toggle between Show/hide
    cb.addItemListener(e->{
      if (cb.isSelected()) {
        tf.setText(new String(pf.getPassword()));
        tf.addMouseListener(new CopyCutPaste(tf));
        pan.remove(pf);
        pan.add(tf);
        tf.requestFocusInWindow();
      } else {
        pf.setText(tf.getText());
        pf.addMouseListener(new CopyCutPaste(pf));
        pan.remove(tf);
        pan.add(pf);
        pf.requestFocusInWindow();
      }
      pan.validate();
    });
    CloseWindow cs = new CloseWindow(dia);
    dia.addWindowListener(cs);
    setMenu(dia);
    dia.pack();
    dia.setVisible(true);
    if (cs.closed) return "";
    dia.dispose();
    if (cb.isSelected()) return tf.getText().trim();
    return new String(pf.getPassword()).trim();
  }
  /**
  iNummeric allows to input only numeric string
  @param frame JFrame, null to display JOptions in the center of screen
  @param lab String, label
  @param size field size
  @return string
  */
  public static String iNumeric(JFrame frame, String lab, int size) {
    JTextField tf = new JTextField(size);
    tf.addMouseListener(new CopyCutPaste(tf));
    tf.addKeyListener(new KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        char c = e.getKeyChar();
        if (c < '0' || c > '9') e.consume(); 
      }
    });
    JLabel lb = new JLabel(lab);
    // create a panel for tf
    JPanel pan = new JPanel();
    pan.add(lb); pan.add(tf);
    // convert JOptionPane to JDialog
    JDialog dia = (new JOptionPane(pan)).createDialog(frame, "JOptions.iNumeric");
    dia.addComponentListener(listener(tf));
    CloseWindow cs = new CloseWindow(dia);
    dia.addWindowListener(cs);
    setMenu(dia);
    dia.pack();
    dia.setVisible(true);
    if (cs.closed) return "";
    dia.dispose();
    return tf.getText().trim();
  }
  /**
  input allows to input a string
  @param frame JFrame, null to display JOptions in the center of screen
  @param lab String, label
  @return string
  */
  public static String input(JFrame frame, String lab) {
    JTextField tf = new JTextField(24);
    tf.addMouseListener(new CopyCutPaste(tf));
    JLabel lb = new JLabel(lab);
    // create a panel for tf
    JPanel pan = new JPanel();
    pan.add(lb); pan.add(tf);
    // convert JOptionPane to JDialog
    JDialog dia = (new JOptionPane(pan)).createDialog(frame, "JOptions.input");
    dia.addComponentListener(listener(tf));
    CloseWindow cs = new CloseWindow(dia);
    dia.addWindowListener(cs);
    setMenu(dia);
    dia.pack();
    dia.setVisible(true);
    if (cs.closed) return "";
    dia.dispose();
    return tf.getText().trim();
  }
  /**
  input allows to input a string
  @param frame JFrame, null to display JOptions in the center of screen
  @param list List of String
  @param lab String, label
  @return string, empty if canceled
  */
  public static String choice(JFrame frame, String lab, java.util.List<String> list) {
    final JComboBox<String> combo = new JComboBox<>(list.toArray(new String[list.size()]));
    JPanel pan = new JPanel();
    pan.add(new JLabel(lab)); pan.add(combo);
    // convert JOptionPane to JDialog
    JDialog dia = (new JOptionPane(pan)).createDialog(frame, "JOptions.choice");
    dia.addComponentListener(listener(combo));
    CloseWindow cs = new CloseWindow(dia);
    dia.addWindowListener(cs);
    setMenu(dia);
    dia.pack();
    dia.setVisible(true);
    if (cs.closed) return "";
    dia.dispose();
    return (String) combo.getSelectedItem();
  }
  /**
  login
  @param frame JFrame, null to display JOptions in the center of screen
  @param header String
  @return String array contains: string[0]: user ID, string[1]: Password, or null (X-CLOSE)
  */
  public static String[] login(JFrame frame, String header) {
    JTextField uID = new JTextField(24);
    uID.addMouseListener(new CopyCutPaste(uID));
    JPasswordField pf = new JPasswordField(24);
    pf.addMouseListener(new CopyCutPaste(pf));
    JCheckBox cb = new JCheckBox("Show");
    JTextField tf = new JTextField(24);
    JLabel jid = new JLabel("UserID  ");
    JLabel jpw = new JLabel("UserPW  ");
    JLabel emp = new JLabel("  ");
    // create a panel for pf/tf and cb in 2 rows
    GridBagLayout layout = new GridBagLayout();
    JPanel pan = new JPanel(layout);
    
    GridBagConstraints left = new GridBagConstraints();
    left.anchor = GridBagConstraints.EAST;
    GridBagConstraints right = new GridBagConstraints();
    right.anchor = GridBagConstraints.WEST;
    right.gridwidth = GridBagConstraints.REMAINDER;
    
    layout.setConstraints(jid, left);
    pan.add(jid);
    layout.setConstraints(uID, right);
    pan.add(uID);
    
    layout.setConstraints(emp, left);
    pan.add(emp);
    layout.setConstraints(cb, right);
    pan.add(cb);
    
    layout.setConstraints(jpw, left);
    pan.add(jpw);
    layout.setConstraints(pf, right);
    pan.add(pf);
    
    // convert JOptionPane to JDialog
    JDialog dia = (new JOptionPane(pan)).createDialog(frame, header);
    dia.addComponentListener(listener(pf));
    // toggle between Show/hide
    cb.addItemListener(e->{
      if (cb.isSelected()) {
        tf.setText(new String(pf.getPassword()));
        pan.remove(pf);
        layout.setConstraints(jpw, left);
        pan.add(jpw);
        layout.setConstraints(tf, right);
        pan.add(tf);
        tf.requestFocusInWindow();
      } else {
        pf.setText(tf.getText());
        pan.remove(tf);
        layout.setConstraints(jpw, left);
        pan.add(jpw);
        layout.setConstraints(pf, right);
        pan.add(pf);
        pf.requestFocusInWindow();
      }
      pan.validate();
    });
    dia.addComponentListener(listener(uID));
    CloseWindow cs = new CloseWindow(dia);
    dia.addWindowListener(cs);
    setMenu(dia);
    dia.pack();
    dia.setVisible(true);
    String[] ret = {"", ""};
    if (!cs.closed) {
      ret[0] = uID.getText().trim();
      if (cb.isSelected()) ret[1] = tf.getText().trim();
      else ret[1] = new String(pf.getPassword()).trim();
      dia.dispose();
    }
    // return ret[0]: userID, ret[1]: password
    return ret;
  }
  /**
  ComponentListener
  @param jcom JComponent
  @return ComponentListener
  */
  public static ComponentListener listener(final JComponent jcom) {
    ComponentListener l = new ComponentListener() {
      public void componentShown(ComponentEvent e) {
        jcom.requestFocusInWindow();
      }
      public void componentHidden(ComponentEvent e) {
        //jcom.setPreferredSize(new Dimension(0,0));
      }
      public void componentResized(ComponentEvent e) {}
      public void componentMoved(ComponentEvent e) {}
    };
    return l;
  }
  // private area---------------------------------------------------------------------------------
  private static void setMenu(JDialog dia) {
    char[] mne = { 'C', 'P' };
    String[] key = { "Copy", "Paste" };
    JTextField ta = new JTextField();
    Action[] act = {
      ta.getActionMap().get(DefaultEditorKit.copyAction),
      ta.getActionMap().get(DefaultEditorKit.pasteAction)
    };
    JMenuItem[] mItem = new JMenuItem[2];
    JMenuBar menuBar = new JMenuBar();
    JMenu menu = new JMenu("ActionMenu");
    for (int i = 0; i < 2; ++i) {
      mItem[i] = new JMenuItem(act[i]);
      mItem[i].setAccelerator(KeyStroke.getKeyStroke((int)mne[i], InputEvent.CTRL_DOWN_MASK));
      mItem[i].setMnemonic(mne[i]);
      mItem[i].setText(key[i]);
      menu.add(mItem[i]);
    }
    menuBar.add(menu);
    dia.setJMenuBar(menuBar);
  }
  //
  private static class CloseWindow extends WindowAdapter {
    public CloseWindow(JDialog dia) {
      this.dia = dia;
    }
    @Override
    public void windowClosing(WindowEvent w) {
      closed = true;
      dia.dispose();
    }
    public boolean closed = false;
    private JDialog dia;
  }
  private static class CopyCutPaste implements MouseListener {
    /**
     Constructor A pop-up menu "Copy/Cut/Paste" with the click of the right mouse-pad
     @param jcomp any Jcomponent. Example JTextArea, JtextField, JTextPane, etc.
    */
    public CopyCutPaste(JTextComponent jcomp) {
      this.jcomp = jcomp;
    }
    /**
    self-explained
    @param e MouseEvent
    */
    public void mousePressed(MouseEvent e){
      if (e.isPopupTrigger()) popup(e);
    }
    /**
    self-explained
    @param e MouseEvent
    */
    public void mouseReleased(MouseEvent e){
      if (e.isPopupTrigger())popup(e);
    }
   
    private void popup(MouseEvent e){
      JPopupMenu menu = new JPopupMenu();
      JMenuItem copy = new JMenuItem(jcomp.getActionMap().get(DefaultEditorKit.copyAction));
      copy.setAccelerator(KeyStroke.getKeyStroke((int)'C', InputEvent.CTRL_DOWN_MASK));
      copy.setText("Copy");
      menu.add(copy);
   
      JMenuItem cut = new JMenuItem(jcomp.getActionMap().get(DefaultEditorKit.cutAction));
      cut.setAccelerator(KeyStroke.getKeyStroke((int)'X', InputEvent.CTRL_DOWN_MASK));
      cut.setText("Cut");
      menu.add(cut);
   
      JMenuItem paste = new JMenuItem(jcomp.getActionMap().get(DefaultEditorKit.pasteAction));
      paste.setAccelerator(KeyStroke.getKeyStroke((int)'V', InputEvent.CTRL_DOWN_MASK));
      paste.setText("Paste");
      menu.add(paste);
      menu.show(e.getComponent(), e.getX(), e.getY());
    }
    // Implementation of MouseListener
    public void mouseClicked(MouseEvent me) {
      Highlighter hl = jcomp.getHighlighter();
      Highlighter.Highlight[] hls = hl.getHighlights();
      try { // remove the highlight at this click
        int caretPos = jcomp.getCaretPosition();
        int beg = Utilities.getWordStart((JTextComponent)jcomp, caretPos);
        int end = Utilities.getWordEnd((JTextComponent)jcomp, caretPos);
        String word = null;
        try {
          word = jcomp.getDocument().getText(beg, end - beg);
        } catch (Exception ex) { }
        if (word == null) return;
        for (int i = 0, b, e; i < hls.length; ++i) {
          b = hls[i].getStartOffset();
          e = hls[i].getEndOffset();
          if (beg <= b && e >= end ) {
            hl.removeHighlight(hls[i]);
            return;
          }
        }
      } catch (Exception ex) { }
    }  
    public void mouseEntered(MouseEvent me) { }  
    public void mouseExited(MouseEvent me) { }  
    private JTextComponent jcomp;
  } 
}