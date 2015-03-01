package ee.ut.cs.mc.scorpii;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.amazonaws.services.ec2.model.Instance;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ScorpiiService extends Service {

    private static final String TAG = ScorpiiService.class.getName();
    private static final long THING_REQUEST_TIMEOUT = 5;
    private static final long SD_FROM_URL_TIMEOUT = 5;
    private static final long CLOUD_TIMEOUT = 360;
    private int devicesToSimulate;
    private boolean useCloud;

    CloudServiceMediator cloudService;
    WebServiceMediator webService;
    private ExecutorService pool;

    public ScorpiiService() {
        this.cloudService = new CloudServiceMediator();
        this.webService = new WebServiceMediator();
    }

    /**
     * Handle requests to start or do certain tasks.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.i(TAG, "onStart");

        int action = intent.getIntExtra(Utils.INTENT_KEY_ACTION, Utils.INTENT_ACTION_DEFAULT);

        if (action == Utils.INTENT_ACTION_DEFAULT){

        } else if (action == Utils.INTENT_ACTION_START_FLOW) {
            //!TODO Check if cloud mode
            devicesToSimulate = intent.getIntExtra(Utils.INTENT_KEY_NO_OF_DEVICES, 0);
            useCloud = intent.getBooleanExtra(Utils.INTENT_KEY_USE_CLOUD,false);

            doFlow();
        }

        return Service.START_STICKY;
    }

    public void doFlow() {

        Thread t = new Thread(){
            @Override
            public void run() {
                Log.i(TAG, "STARTING FLOW IN 6 SECONDS");
                Utils.sleep(6000);
                Log.i(TAG, "Starting flow");

                ArrayList<ServiceDescriptor> descriptors;
                // restart IoT thing emulator server.
                boolean restartSuccessful = webService.restartThingSim();
                if (restartSuccessful) {
                    // Get set of responses
                    Log.i(TAG, "~~~~Starting communication with IoT things.");
                    ArrayList<ThingResponse> responses = communicateWithThings(devicesToSimulate);

                    if (useCloud) {
                        descriptors = offloadUrlsAndParseSdsLocally(responses, true);
                    } else {
                        // Extract SD-s from responses, use backups servers to obtain SD-s where necessary
                        Log.i(TAG, "~~~~Starting extraction of SD-s from responses.");
                        descriptors = getServiceDescriptorsFromResponses(responses);
                        parseDescriptors(descriptors, Utils.PARSE_ARGUMENT);
                        descriptors = filterMatchingSDs(descriptors);
                    }
                    Log.i(TAG, "~~~~Flow finished");
                } else {
                    Log.e(TAG, "-!-!- Could not restart thing emulator");
                }

                stopSelf();
            }
        };
        t.start();
    }


    private ArrayList<ServiceDescriptor> delegateToCloud(JSONArray responses) {
        ArrayList<ServiceDescriptor> result;
        cloudService.launchInstance();
        Instance i = null;
        //wait til scp tasks are done (bpel uploading etc)
        while (!cloudService.scpCompletedFlag) {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        i = cloudService.getInstanceController().getInstance();
        String instanceUrl = String.format("http://%s:8080/MhcmHandler/", i.getPublicIpAddress());
        Log.i(TAG, instanceUrl);
        //do a get to see if webserver is up
        webService.getTilServerResponds(instanceUrl);
        result = webService.sendUrlsToCloud(responses, instanceUrl);

        return result;
    }

    /**
     * Goes through the provided descriptors and parses each one to see
     * if it contains the wanted output definition. Changes the argument
     * arraylist. This executes on
     * a separate thread pool.
     *
     * @param descriptors
     */
    private void parseDescriptors(ArrayList<ServiceDescriptor> descriptors, final String definition) {
        int responsesLength = descriptors.size();
        pool = Executors.newFixedThreadPool(50);
        Future futures[] = new Future[responsesLength];

        for (int i = 0; i < responsesLength; i++) {
            final ServiceDescriptor descriptor = descriptors.get(i);
            futures[i] = pool.submit(new Runnable() {
                @Override
                public void run() {
                    descriptor.setContainsDefinition(definition);
                }
            });
            //Gather results (waiting for threads to finish)
        }
        for (Future f : futures) {
            try {
                // get() is a blocking call here, which timeouts according to the argument
                f.get(Utils.TIMEOUT_PARSE, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                Log.e(TAG, String.format("Timeout!\n%d", e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * split the ThingResponses into two: based on the fact
     * whether they have actual service descriptor content or just an url.
     * The Url ones are sent to a cloud service, where they are filtered and parsed.
     * The rest of the ones are processed locally.
     *
     * @param responses
     * @param useCloud
     * @return
     */
    private ArrayList<ServiceDescriptor> offloadUrlsAndParseSdsLocally(
            ArrayList<ThingResponse> responses, boolean useCloud) {

        final ArrayList<ServiceDescriptor> descriptors = new ArrayList<ServiceDescriptor>();
        final JSONArray responsesToDelegate = new JSONArray();

        //Separate url references from actual SD-s
        for (ThingResponse response : responses) {
            if (response.getUrl() != null) {
                responsesToDelegate.put(response.getUrl());
            } else {
                //extract SD from response locally
                descriptors.add(response.getServiceDescriptor(null));
            }
        }

        //Parse actual SD-s locally, and delegate url ones to cloud for parsing
        pool = Executors.newFixedThreadPool(5);
        Future cloudResult = pool.submit(new Callable() {
            @Override
            public ArrayList<ServiceDescriptor> call() throws Exception {
                return delegateToCloud(responsesToDelegate);
            }
        });

        Future localResult = pool.submit(new Callable() {
            @Override
            public ArrayList<ServiceDescriptor> call() throws Exception {
                //parse locally and return
                parseDescriptors(descriptors, Utils.PARSE_ARGUMENT);
                return filterMatchingSDs(descriptors);
            }
        });

        ArrayList<ServiceDescriptor> cloudResults = null;
        ArrayList<ServiceDescriptor> localResults = null;
        try {
            cloudResults = (ArrayList<ServiceDescriptor>) cloudResult.get(CLOUD_TIMEOUT, TimeUnit.SECONDS);
            localResults = (ArrayList<ServiceDescriptor>) localResult.get(THING_REQUEST_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        cloudResults.addAll(localResults);
        return cloudResults;
    }
    /**
     * Go through an array of ThingResponses and obtain a service descriptor (SD) from each one.
     * The ThingResponse might contain the SD itself, or an URL referencing to a SD. In the latter
     * case, this URL is used to fetch it from the provided URL.
     *
     * @param responses - an array of ThingResponses
     * @return a list of service descriptors
     */
    private ArrayList<ServiceDescriptor> getServiceDescriptorsFromResponses(final ArrayList<ThingResponse> responses) {
        int responsesLength = responses.size();
        ArrayList<ServiceDescriptor> descriptors = new ArrayList<ServiceDescriptor>();
        pool = Executors.newFixedThreadPool(50);
        Future futures[] = new Future[responsesLength];


        for (int i = 0; i < responsesLength; i++) {
            final ThingResponse response = responses.get(i);
            futures[i] = pool.submit(new Callable() {
                @Override
                public ServiceDescriptor call() throws Exception {
                    return response.getServiceDescriptor(webService);
                }
            });
            //Gather results (waiting for threads to finish)
        }
        for (Future f : futures) {
            try {
                // get() is a blocking call here, which timeouts according to the argument
                ServiceDescriptor sd = (ServiceDescriptor) f.get(
                        SD_FROM_URL_TIMEOUT, TimeUnit.SECONDS);
                descriptors.add(sd);
            } catch (TimeoutException e) {
                Log.e(TAG, String.format("Timeout!\n%d", e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return descriptors;
    }


    /** Simulate connecting to a given number of
     * smart objects, and getting responses from them.
     * In our prototype, the smart object is emulated as
     * a server on a PC. */
    public ArrayList<ThingResponse> communicateWithThings(int devices) {
        ArrayList<ThingResponse> responses = new ArrayList<ThingResponse>(devices);
        Future futures[] = new Future[devices];

        //Execute communication with multiple devices concurrently using a threadpool
        pool = Executors.newFixedThreadPool(50);
        for (int i = 0; i < devices; i++) {
            futures[i] = pool.submit(new Callable() {
                @Override
                public ThingResponse call() throws Exception {
                    return communicateWithThing();
                }
            });
        }

        //Gather results (waiting for threads to finish)
        for (Future f : futures) {
            try {
                // get() is a blocking call here, which timeouts according to the argument
                ThingResponse response = (ThingResponse) f.get(
                        THING_REQUEST_TIMEOUT, TimeUnit.SECONDS);
                responses.add(response);
            } catch (TimeoutException e) {
                Log.e(TAG, String.format("Timeout!\n%d", e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return responses;
    }

    public ArrayList<ServiceDescriptor> filterMatchingSDs(ArrayList<ServiceDescriptor> descriptors) {
        ArrayList<ServiceDescriptor> result = new ArrayList<ServiceDescriptor>();
        for (ServiceDescriptor sd : descriptors) {
            if (sd.containsDefinition()) {
                result.add(sd);
            }
        }
        return result;
    }


    /** Connect to a single smart object and get response from it.
     * @return response provided by the smart object, either an URL or a service descriptor.
     */
    private ThingResponse communicateWithThing() {
        return webService.simulateCommunicationWithThing();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return null;
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Log.i(TAG, "Trim memory");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.i(TAG, "Memory low");
    }

}
