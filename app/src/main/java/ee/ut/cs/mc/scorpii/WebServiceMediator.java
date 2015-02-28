package ee.ut.cs.mc.scorpii;

import android.util.Log;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by jaks on 20/02/15.
 */
public class WebServiceMediator {


    static final String TAG = WebServiceMediator.class.getName();


    public HttpResponse getFromURL(String url) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = null;

        String responseString = null;
        try {
            response = httpclient.execute(new HttpGet(url));
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                return response;
            } else {
                //Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (ClientProtocolException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        return response;
    }

    public String getStringFromUrl(String url) {
        String result = "";
        HttpResponse resp = getFromURL(url);
        try {
            result = readResponseToString(resp);
            if (resp.getEntity() != null) {
                Log.v(TAG, "Closing connection in getStringFromUrl");
                resp.getEntity().consumeContent();
            }
        } catch (IOException e) {
            result += e.getMessage();
        }

        return result;
    }

    /**
     * Checks HTTP response mime type and creates a ThingResponse accordingly. (Either
     * giving a value to the url or descriptor field.
     *
     * @param response - HttpResponse
     * @return ThingResponse
     * @throws IOException
     */
    private ThingResponse getThingResponseFromHttpResponse(HttpResponse response) throws IOException {
        ThingResponse thingResponse = null;
        String responseString = readResponseToString(response);

        boolean isXml = isXmlContentType(response);
        thingResponse = new ThingResponse();
        if (isXml) {
            thingResponse.setServiceDescriptor(new ServiceDescriptor(responseString));
        } else {
            thingResponse.setURL(responseString);
        }

        return thingResponse;

    }

    private String readResponseToString(HttpResponse response) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        response.getEntity().writeTo(out);
        String responseString = out.toString();
        out.close();
        return responseString;
    }

    private boolean isXmlContentType(HttpResponse response) {
        String re1 = "((?:[a-z][a-z]+))";    // application or text
        String re2 = "(\\/xml)";    // Unix Path 1
        Header contentType = response.getEntity().getContentType();
        return contentType.getValue().matches(re1 + re2);
    }

    /**This method simulates asking an IoT thing for a service descriptor.
     * In this prototype, the IoT thing is acted by an Apache tomcat webapp.
     * @return ThingResponse - a service descriptor or a url
     */
    public ThingResponse simulateCommunicationWithThing() {
        ThingResponse thingResponse = null;
        HttpResponse httpResponse = getFromURL(Utils.IOT_THING_SERVER_URL);
        try {
            thingResponse = getThingResponseFromHttpResponse(httpResponse);
            httpResponse.getEntity().consumeContent();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return thingResponse;
    }

    public boolean restartThingSim() {
        HttpResponse resp = getFromURL(Utils.IOT_THING_SERVER_URL + "?restart");
        return resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
    }

    public ArrayList<ServiceDescriptor> sendUrlsToCloud(JSONArray json, String server_address) {
        ArrayList<ServiceDescriptor> result = null;
        Object reply;
        URL url;
        HttpURLConnection urlConn;
        try {
            url = new URL(server_address);
            urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod("POST");
            urlConn.setRequestProperty("Content-Type", "application/json");
            urlConn.connect();

            DataOutputStream output;

            output = new DataOutputStream(urlConn.getOutputStream());
            /*Construct the POST data.*/
            String content = json.toString();

            /* Send the request data.*/
            output.writeBytes(content);
            output.flush();
            output.close();


            //receive reply
            ObjectInputStream objIn = new ObjectInputStream(urlConn.getInputStream());
            reply = objIn.readObject();
            objIn.close();

            result = (ArrayList<ServiceDescriptor>) reply;


        } catch (Exception ex) {
            // it is ok if we get an exception here
            // that means that there is no object being returned
            System.out.println("No Object Returned");
            if (!(ex instanceof EOFException))
                ex.printStackTrace();
            System.err.println("*");
        }
        return result;


    }


    public Object sendThingResponsesToCloud(ArrayList<ThingResponse> responses, String server_address) {
        Object reply = null;
        HttpURLConnection cox = null;
        try {
            URL url = new URL(server_address);
            cox = (HttpURLConnection) url.openConnection();
            cox.setDoOutput(true);
            cox.setDoInput(true);
            cox.setInstanceFollowRedirects(false);
            cox.setRequestMethod("POST");
            cox.setUseCaches(false);

            ObjectOutputStream oos = new ObjectOutputStream(cox.getOutputStream());
            oos.writeObject(responses);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //receive reply
        try {
            ObjectInputStream objIn = new ObjectInputStream(cox.getInputStream());
            reply = objIn.readObject();
            objIn.close();
        } catch (Exception ex) {
            // it is ok if we get an exception here
            // that means that there is no object being returned
            System.out.println("No Object Returned");
            if (!(ex instanceof EOFException))
                ex.printStackTrace();
            System.err.println("*");
        }
        return reply;


    }
}
