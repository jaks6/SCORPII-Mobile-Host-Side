package ee.ut.cs.mc.scorpii;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ScorpiiService extends Service {

    private static final String TAG = ScorpiiService.class.getName();
    private static final long THING_REQUEST_TIMEOUT = 5;
    private static final long SD_FROM_URL_TIMEOUT = 5;
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
        Log.i(TAG, "Starting flow");
        Thread t = new Thread(){
            @Override
            public void run() {
                // restart IoT thing emulator server.
                boolean restartSuccessful = webService.restartThingSim();
                if (restartSuccessful) {
                    // Get set of responses
                    Log.i(TAG, "~~~~Starting communication with IoT things.");
                    ArrayList<ThingResponse> responses = communicateWithThings(devicesToSimulate);

                    if (useCloud) {
                        ArrayList<ServiceDescriptor> descriptors = delegateToCloud(responses);
                    } else {
                        // Extract SD-s from responses, use backups servers to obtain SD-s where necessary
                        Log.i(TAG, "~~~~Starting extraction of SD-s from responses.");
                        ArrayList<ServiceDescriptor> descriptors = getServiceDescriptorsFromResponses(responses);

                        parseDescriptors(descriptors, Utils.PARSE_ARGUMENT);
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

    private ArrayList<ServiceDescriptor> delegateToCloud(ArrayList<ThingResponse> responses) {
        cloudService.launchInstance();
        return null;
    }

    /**
     * Goes through the provided descriptors and parses each one to see
     * if it contains the wanted output definition
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
