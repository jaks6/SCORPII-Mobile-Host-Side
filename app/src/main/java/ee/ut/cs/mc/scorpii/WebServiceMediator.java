package ee.ut.cs.mc.scorpii;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by jaks on 20/02/15.
 */
public class WebServiceMediator {

    static final String IOT_THING_SERVER_URL = "https://dl.dropboxusercontent.com/u/16030070/temp/hello.xml";
    static final String TAG = WebServiceMediator.class.getName();


    public String getFromURL(String url){
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        String responseString = null;
        try {
            response = httpclient.execute(new HttpGet(url));
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                responseString = out.toString();
                out.close();
            } else{
                //Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (ClientProtocolException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return responseString;
    }

    /**This method simulates asking an IoT thing for a service descriptor.
     * In this prototype, the IoT thing is acted by an Apache tomcat webapp.
     * @return ThingResponse - a service descriptor or a url
     */
    public ThingResponse simulateCommunicationWithThing() {
                ServiceDescriptor sd = new ServiceDescriptor(
                        getFromURL(IOT_THING_SERVER_URL));
        return new ThingResponse().setServiceDescriptor(sd);
    }

}
