package ee.ut.cs.mc.scorpii;

import java.io.IOException;

/**
 * Created by jaks on 20/02/15.
 *
 * Class which is a wrapper for response messages
 * provided by nodes/IoT-'things' in proximity which
 * the mobile client interacts with.
 *
 * The contents can either be a service descriptor or
 * an URL to a server from which the service descriptor
 * can be obtained.
 */
public class ThingResponse {
    private String url;
    private ServiceDescriptor serviceDescriptor;

    public ThingResponse() {
    }

    public String getUrl() {
        return url;
    }

    /** Retrieve the containing service descriptor OR
     * fetch the descriptor from a remote server if
     * this response only contains an URL.
     * @return
     */
    public ServiceDescriptor getServiceDescriptor(WebServiceMediator webMediator) throws ContextException {
        if (this.serviceDescriptor != null) return serviceDescriptor;
        /** If no actual service descriptor is available, try to get it
         * from the URL (if provided) */
        else if (url != null) {
            try {
                return getServiceDescriptorViaUrl(url, webMediator);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private ServiceDescriptor getServiceDescriptorViaUrl(String url, WebServiceMediator webMediator) throws ContextException, IOException {
        if (webMediator!= null){
            String urlResponse = webMediator.getStringFromUrl(url);
            return new ServiceDescriptor(urlResponse);
        }
        else return null;
    }

    public ThingResponse setURL(String url){
        this.url = url;
        return this;
    }

    public ThingResponse setServiceDescriptor(ServiceDescriptor serviceDescriptor){
        this.serviceDescriptor = serviceDescriptor;
        return this;
    }

}
