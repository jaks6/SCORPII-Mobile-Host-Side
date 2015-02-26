package ee.ut.cs.mc.scorpii;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

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
    private int devicesToSimulate;
    private boolean useCloud;

    CloudServiceMediator cloudService;
    WebServiceMediator webService;
    private final ScorpiiServiceBinder binder = new ScorpiiServiceBinder(this);
    private ExecutorService pool;

    public ScorpiiService() {
        this.cloudService = new CloudServiceMediator();
        this.webService = new WebServiceMediator();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.i(TAG, "onStart");

        int action = intent.getIntExtra(Utils.INTENT_KEY_ACTION, Utils.INTENT_ACTION_DEFAULT);

        if (action == Utils.INTENT_ACTION_DEFAULT){

        } else if (action == Utils.INTENT_ACTION_START_FLOW){
            devicesToSimulate = intent.getIntExtra(Utils.INTENT_KEY_NO_OF_DEVICES, 0);
            useCloud = intent.getBooleanExtra(Utils.INTENT_KEY_USE_CLOUD,false);

            doFlow();
        }

        return Service.START_STICKY;
    }

    public void doFlow(){
        Thread t = new Thread(){
            @Override
            public void run() {
                ArrayList<ThingResponse> responses = communicateWithThings(devicesToSimulate);
                ArrayList<ServiceDescriptor> descriptors = getServiceDescriptorsFromResponses(responses);

                stopSelf();
            }
        };
        t.start();

    }

    private ArrayList<ServiceDescriptor> getServiceDescriptorsFromResponses(ArrayList<ThingResponse> responses) {
        ArrayList<ServiceDescriptor> descriptors = new ArrayList<ServiceDescriptor>();
        for (ThingResponse response : responses) {
            try {
                descriptors.add(response.getServiceDescriptor(webService));
            } catch (ContextException e) {
                e.printStackTrace();
            }
        }
        return descriptors;
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

    /** Simulate connecting to a given number of
     * smart objects, and getting responses from them.
     * In our prototype, the smart object is emulated as
     * a server on a PC. */
    public ArrayList<ThingResponse> communicateWithThings(int devices) {
        ArrayList<ThingResponse> responses = new ArrayList<ThingResponse>(devices);
        Future futures[] = new Future[devices];

        //Execute communication
        pool = Executors.newFixedThreadPool(50);
        for (int i = 0; i < devices; i++) {
            futures[i] = pool.submit(new Callable() {
                @Override
                public ThingResponse call() throws Exception {
                    return communicateWithThing();
                }
            });
        }

        //Gather results (wait for threads to finish)
        for (Future f : futures) {
            try {
                responses.add((ThingResponse) f.get(10, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }

        return responses;
    }

    /** Connect to a single smart object and get
     * response from it.
     * @return response provided by the smart object, either an URL or a service descriptor.
     */
    private ThingResponse communicateWithThing() {
        //Emulate
        return webService.simulateCommunicationWithThing();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return binder;
    }

    public class ScorpiiServiceBinder extends Binder {
        ScorpiiService service;
        public ScorpiiServiceBinder (ScorpiiService service) {
            this.service = service;
        }
        public ScorpiiService getService () {
            return service;
        }
    }


}
