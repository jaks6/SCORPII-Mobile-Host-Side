package ee.ut.cs.mc.scorpii.aws;

import android.os.AsyncTask;

import com.amazonaws.services.ec2.model.Instance;

import ee.ut.cs.mc.scorpii.MainActivity;

public class ConnectToInstanceTask extends AsyncTask<Void, String,Instance> {
    private final InstanceConnector connector;
    MainActivity activity;

	public ConnectToInstanceTask(MainActivity activity, InstanceConnector instanceConnector) {
        connector = instanceConnector;
		this.activity = activity;
	}

	@Override
	protected Instance doInBackground(Void... arg0) {
        Instance instance = null;
        try {
            instance = connector.launch();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (instance == null || instance.getState().getName().equals("terminated")) {
            activity.showError("Could not connect to instance.");
        }
		return instance;
	}
    @Override
    protected void onProgressUpdate(String... progress) {
        if(progress[0]==null) progress[0] = "null";
        activity.appendToUiConsole(String.format("Waiting for instance to get ready;" +
                "\n instance state = %s", progress[0]));
    }

    @Override
	protected void onPostExecute(Instance result) {
		if (result == null){
			activity.showError("Instance connection failed.");
		} else {
//            Instance i = result.getReservation().getInstances().get(0);
            String publicIP = result.getPublicIpAddress();
			activity.appendToUiConsole(String.format(
                    "Restored connection to instance, " +
                            "\n ip = '%s'" +
                            "\n id = '%s'." +
                            "\n AMI ID = '%s'", publicIP, result.getInstanceId(), result.getImageId()));

//            activity.onInstanceUpdate(result, Utils.INSTANCE_RUNNING);
		}
	}

	@Override
	protected void onPreExecute() {
        activity.appendToUiConsole("Connecting to existing instance with id: " + connector.getInstanceId());
	}

	

}
