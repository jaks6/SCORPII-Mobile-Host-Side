package ee.ut.cs.mc.scorpii;

/**
 * Created by jaks on 20/02/15.
 */
public class ServiceDescriptor {
    private String content;

    /**
     * Constructor from HTTP response string
     *
     * @param xmlString
     */
    public ServiceDescriptor(String xmlString) {
        this.content = xmlString;
    }


}
