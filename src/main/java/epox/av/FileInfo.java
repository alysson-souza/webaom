/*
 * WebAOM - Web Anime-O-Matic
 * Copyright (C) 2005-2010 epoximator 2025 Alysson Souza
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <https://www.gnu.org/licenses/>.
 */

package epox.av;

import epox.util.StringUtilities;
import epox.webaom.data.AttributeMap;
import java.io.IOException;
import java.io.StringReader;
import java.util.Vector;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class FileInfo {
    public final Vector<AttributeMap> vid;
    public final Vector<AttributeMap> aud;
    public final Vector<AttributeMap> sub;
    public String m_xml = null;

    public FileInfo(String xml) throws IOException {
        vid = new Vector<>();
        aud = new Vector<>();
        sub = new Vector<>();
        byXml(xml);
    }

    private static void dump(Vector<AttributeMap> v) {
        for (AttributeMap attributeMap : v) {
            String[][] a = attributeMap.toArray();
            for (String[] strings : a) {
                System.out.println(strings[0] + ": " + strings[1]);
            }
        }
    }

    public void dump() {
        dump(vid);
        dump(aud);
        dump(sub);
    }

    public void byXml(String str) throws IOException {
        try {
            MyHandler myh = new MyHandler();
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            XMLReader xml = parser.getXMLReader();
            xml.setContentHandler(myh);
            xml.parse(new InputSource(new StringReader(str)));
            m_xml = str.replaceAll("[\r\n\t]", "").replaceAll(" {2}", " ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String convert(String schema, int type) {
        if (schema == null) {
            return null;
        }
        Vector<AttributeMap> v = null;
        switch (type) {
            case 0:
                v = vid;
                break;
            case 1:
                v = aud;
                break;
            case 2:
                v = sub;
                break;
            default:
                return null;
        }
        StringBuilder sb = new StringBuilder(schema.length() * v.size());
        for (int j = 0; j < v.size(); j++) {
            String s = StringUtilities.replaceCCCode(schema, v.get(j));
            if (j > 0) {
                sb.append('\n');
            }
            sb.append(s);
        }
        return sb.toString();
    }

    protected class MyHandler extends DefaultHandler {
        static final int DUR = 0;
        static final int VID = 1;
        static final int AUD = 2;
        static final int SUB = 3;
        int ctyp = -1;
        int tcnt = 1;
        String name = null;
        String id = null;
        AttributeMap m;

        @Override
        public void startElement(String namespace, String localname, String type, org.xml.sax.Attributes attributes)
                throws org.xml.sax.SAXException {
            name = type;
            id = attributes.getValue("id");

            if (ctyp > 0) {
                return;
            }

            switch (type) {
                case "vid" -> ctyp = VID;
                case "aud" -> ctyp = AUD;
                case "sub" -> ctyp = SUB;
                default -> ctyp = -1;
            }

            if (ctyp > 0) {
                m = new AttributeMap();
                String d = attributes.getValue("default");
                m.put("def", d != null && d.equals("1") ? "default" : "");
                m.put("num", tcnt++);
            }
        }

        @Override
        public void endElement(String namespace, String localname, String type) throws org.xml.sax.SAXException {
            if (type.equals("vid") && ctyp == VID) {
                vid.add(m);
                m = null;
                ctyp = -1;
                return;
            }
            if (type.equals("aud") && ctyp == AUD) {
                aud.add(m);
                m = null;
                ctyp = -1;
                return;
            }
            if (type.equals("sub") && ctyp == SUB) {
                sub.add(m);
                m = null;
                ctyp = -1;
            }
        }

        @Override
        public void characters(char[] ch, int start, int len) {
            String text = new String(ch, start, len).trim();
            if (text.isEmpty()) {
                return;
            }
            if (m != null) {
                m.put(name, text);
            }
        }
    }
}
