package ee.ut.cs.mc.scorpii;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Created by Jakob on 27.02.2015.
 */
public class XmlUtils {

    /**
     * Simple dumb parser , based on the example:
     * http://www.sensorml.com/sensorML-2.0/examples/helloWorld.html
     *
     * @param doc
     * @param definition - the string to look for
     */
    public static boolean doesOutputExist(Document doc, String definition) {
        doc.getDocumentElement().normalize();

        Element outputsNode = (Element) doc.getElementsByTagName("sml:outputs").item(0);
        Element outputListNode = (Element) outputsNode.getElementsByTagName("sml:OutputList").item(0);
        NodeList outputNodeList = outputListNode.getElementsByTagName("sml:output");

        for (int i = 0; i < outputNodeList.getLength(); i++) {
            Node output = outputNodeList.item(i);
            if (output instanceof Element) {
                Element outputElem = (Element) output;
                Node qty = outputElem.getElementsByTagName("swe:Quantity").item(0);
                if (qty != null) {
                    if (qty.hasAttributes()) {
                        String value = qty.getAttributes().getNamedItem("definition").getNodeValue();
                        if (value != null && value.equals(definition)) return true;
                    }
                }
            }
        }
        return false;
    }

}