package ee.ut.cs.mc.scorpii;

/**
 * Created by jaks on 20/02/15.
 */
public class ServiceDescriptor {
    private String content;
    /** Constructor from HTTP response string */
    public ServiceDescriptor(String s) {
        this.content = s;
    }
}
