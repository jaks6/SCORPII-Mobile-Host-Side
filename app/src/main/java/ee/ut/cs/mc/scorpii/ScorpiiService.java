package ee.ut.cs.mc.scorpii;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class ScorpiiService extends Service {

    private int devicesToSimulate;
    private boolean useCloud;

    CloudServiceMediator cloudService;
    WebServiceMediator webService;
    private final ScorpiiServiceBinder binder = new ScorpiiServiceBinder(this);


    public ScorpiiService() {
        this.cloudService = new CloudServiceMediator();
        this.webService = new WebServiceMediator();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        devicesToSimulate = intent.getIntExtra(Utils.INTENT_KEY_NO_OF_DEVICES, 0);
        useCloud = intent.getBooleanExtra(Utils.INTENT_KEY_USE_CLOUD,false);

        return Service.START_STICKY;
    }

    public void doFlow(){

        ThingResponse[] responses = communicateWithThings(devicesToSimulate);
        ServiceDescriptor[] descriptors = getServiceDescriptorsFromResponses(responses);

    }

    private ServiceDescriptor[] getServiceDescriptorsFromResponses(ThingResponse[] responses) {
        return new ServiceDescriptor[0];
    }

    /** Simulate connecting to a given number of
     * smart objects, and getting responses from them.
     * In our prototype, the smart object is emulated as
     * a server on a PC. */
    public ThingResponse[] communicateWithThings(int devices){
        ThingResponse responses[] = new ThingResponse[devices];
        for (int i = 0; i < devices; i++) {
            responses[i] = communicateWithThing();
        }
        return responses;
    }

    /** Connect to a single smart object and get
     * response from it.
     * @return response provided by the smart object, either an URL or a service descriptor.
     */
    private ThingResponse communicateWithThing() {
        // !TODO
        //1. Connect to server
        //2. Get response
        return new ThingResponse();
    }

    @Override
    public IBinder onBind(Intent intent) {
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
