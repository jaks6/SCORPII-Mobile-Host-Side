package ee.ut.cs.mc.scorpii;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by jaks on 20/02/15.
 */
public class ServiceDescriptor implements Serializable {
    private String content;
    private boolean containsDefinition;

    /**
     * Constructor from HTTP response string
     *
     * @param xmlString
     */
    public ServiceDescriptor(String xmlString) {
        this.content = xmlString;
    }


    public Document toDocument() throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(content)));
    }

    public boolean containsDefinition() {
        return containsDefinition;
    }


    public ServiceDescriptor setContainsDefinition(String definition) {
        boolean exists = false;
        try {
            exists = XmlUtils.doesOutputExist(this.toDocument(), definition);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        this.containsDefinition = exists;
        return this;
    }
}
