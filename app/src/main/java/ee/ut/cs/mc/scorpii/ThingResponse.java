package ee.ut.cs.mc.scorpii;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

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
    private ScorpiiService s;
    private ServiceDescriptor serviceDescriptor;

    public ThingResponse() {
    }

    public ThingResponse(ServiceDescriptor serviceDescriptor) {

    }

    public String getUrl() {
        return url;
    }

    /** Retrieve the containing service descriptor OR
     * fetch the descriptor from a remote server if
     * this response only contains an URL.
     * @return
     */
    public ServiceDescriptor getServiceDescriptor() throws ContextException {
        if (this.serviceDescriptor != null) return serviceDescriptor;
        /** If no actual service descriptor is available, try to get it
         * from the URL (if provided) */
        else if (url != null) {
            try {
                return getServiceDescriptorViaUrl(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private ServiceDescriptor getServiceDescriptorViaUrl(String url) throws ContextException, IOException {
        WebServiceMediator webMediator = getWebServiceMediator();
        if (webMediator!= null){
            String urlResponse =  webMediator.getFromURL(url);
            return new ServiceDescriptor(urlResponse);
        }
        else return null;
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            ScorpiiService.ScorpiiServiceBinder b = (ScorpiiService.ScorpiiServiceBinder) binder;
            s = b.getService();
        }
        public void onServiceDisconnected(ComponentName className) {
            s = null;
        }
    };

    private WebServiceMediator getWebServiceMediator() throws ContextException {
        Context context = App.getContext();
        if (context == null) throw new ContextException("context null!");
        else {
            Intent intent = new Intent(context,ScorpiiService.class);
            context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            return s.webService;
        }
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
