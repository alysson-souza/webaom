package epox.webaom;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Stack;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RuleMenu extends DefaultHandler {
    /** The currently active menu item being parsed. */
    private JMenuItem currentItem = null;
    /** Stack of nested submenus for hierarchical menu building. */
    private final Stack<JMenu> menuStack;
    /** Target text area for inserting rule text. */
    private final JTextArea textArea;

    public RuleMenu(JTextArea targetTextArea) {
        textArea = targetTextArea;
        menuStack = new Stack<>();
    }

    public JPopupMenu getMenu() {
        return menuStack.peek().getPopupMenu();
    }

    @Override
    public void startElement(String uri, String localName, String qName, final Attributes attributes)
            throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        switch (qName) {
            case "elem" -> {
                currentItem = new JMenuItem(attributes.getValue("title"));
                String valueAttr = attributes.getValue("value");
                InsertTextListener listener = new InsertTextListener(textArea, valueAttr);
                currentItem.addActionListener(listener);
                menuStack.peek().add(currentItem);
            }
            case "item" -> menuStack.push(new JMenu(attributes.getValue("title")));
            case "menu" -> menuStack.add(new JMenu());
            default -> {}
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);
        if (qName.equals("item")) {
            JMenu submenu = menuStack.pop();
            menuStack.peek().add(submenu);
        }
        currentItem = null;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        super.characters(ch, start, length);
        String tooltipText = new String(ch, start, length).trim();
        if (currentItem != null && !tooltipText.isEmpty()) {
            currentItem.setToolTipText(tooltipText);
        }
    }

    /** Listener that inserts rule text at the caret position in the target text area. */
    private class InsertTextListener implements ActionListener {
        private final JTextArea targetTextArea;
        private final String insertText;

        InsertTextListener(JTextArea textArea, String textToInsert) {
            targetTextArea = textArea;
            insertText = textToInsert;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            targetTextArea.insert(insertText, targetTextArea.getCaretPosition());
        }
        /*
         * public void mouseEntered(MouseEvent e) {
         * super.mouseEntered(e);
         * m_pb.setString(m_item.getToolTipText());
         * }
         * public void mouseExited(MouseEvent e) {
         * super.mouseExited(e);
         * m_pb.setString("");
         * }
         */
    }
    /*
     * protected final JProgressBar m_pb=new JProgressBar();
     * protected JTextArea m_tf = new JTextArea();
     * private static Font MYF = new Font("Tahoma", Font.PLAIN, 11);
     * public static void main (String [] args) {
     * final RuleMenu handler = new RuleMenu();
     * SAXParserFactory factory = SAXParserFactory.newInstance();
     * try {
     * SAXParser saxParser = factory.newSAXParser();
     * saxParser.parse( new File("C:\\rule-helper.xml"), handler );
     *
     * final JPopupMenu x = handler.getMenu();
     * x.setFont(MYF);
     *
     * JFrame f = new JFrame("test");
     * f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
     * handler.m_tf.setFont(new Font("Tahoma", Font.PLAIN, 18));
     * handler.m_tf.setMargin(new Insets(4,4,4,4));
     * handler.m_tf.addMouseListener(new MouseAdapter(){
     *
     * @Override
     * public void mouseClicked(MouseEvent e) {
     * super.mouseClicked(e);
     * if(e.getButton()==MouseEvent.BUTTON3)
     * x.show(handler.m_tf, e.getX(),e.getY());
     * }
     * });
     * f.getContentPane().setLayout(new BorderLayout());
     * f.getContentPane().add(handler.m_tf, BorderLayout.CENTER);
     * f.getContentPane().add(handler.m_pb, BorderLayout.SOUTH);
     * handler.m_pb.setStringPainted(true);
     * f.setBounds(100,200, 640, 480);
     * f.setVisible(true);
     *
     * handler.m_tf.
     * setText("IF A(Naruto);G(zx) DO FAIL //Do not rename file if it is Naruto\nDO ADD '%eng (%ann) - %enr - %epn ' //Add the base, same for all files\nIF D(japanese);S(english) DO ADD '(SUB)' //Add (SUB) if the file is subbed in english\nIF D(japanese);S(NONE) DO ADD '(RAW)' //Add (RAW) if the file is not subbed.\nIF G(!unknown) DO ADD '[%grp]' //Add group name if it is not unknown\nDO ADD '(%CRC)' //Always add crc"
     * );
     *
     *
     * } catch (Throwable t) {
     * t.printStackTrace();
     * }
     * }
     */
}
