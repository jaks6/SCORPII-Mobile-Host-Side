package ee.ut.cs.mc.ec2.aws;

import android.os.AsyncTask;

import com.amazonaws.services.ec2.model.Instance;

import ee.ut.cs.mc.ec2.MainActivity;

public class ConnectToInstanceTask extends AsyncTask<InstanceConnector, String,Instance> {
	MainActivity activity;

	public ConnectToInstanceTask(MainActivity activity) {
		this.activity = activity;
	}
	
	@Override
	protected Instance doInBackground(InstanceConnector... arg0) {
        Instance instance = null;

        InstanceConnector connector = arg0[0];
        try {
            activity.showInUi("Connecting to existing instance with id: " + connector.getInstanceId());
            instance = connector.launch();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (instance == null) {
            activity.showError("Could not connect to instance.");
        }
		return instance;
	}

    @Override
	protected void onPostExecute(Instance result) {
		if (result == null){
			activity.showError("Instance connection failed.");
		} else {
//            Instance i = result.getReservation().getInstances().get(0);
            String publicIP = result.getPublicIpAddress();
			activity.showInUi(String.format(
					"Restored connection to instance, " +
                    "\n ip = '%s'"+
					"\n id = '%s'." +
					"\n AMI ID = '%s'", publicIP, result.getInstanceId(), result.getImageId()));

            activity.onInstanceUpdate(result);
		}
	}

	@Override
	protected void onPreExecute() {
		activity.showInUi("Starting 'instance launch task'");
	}

	

}
