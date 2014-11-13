package ee.ut.cs.mc.ec2.aws;

import android.os.AsyncTask;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RunInstancesResult;

import ee.ut.cs.mc.ec2.MainActivity;

public class RunInstanceTask extends AsyncTask<InstanceLauncher, Void,RunInstancesResult> {
	MainActivity activity;
	
	public RunInstanceTask(MainActivity activity) {
		this.activity = activity;
	}
	
	@Override
	protected RunInstancesResult doInBackground(InstanceLauncher... arg0) {
		InstanceLauncher launcher = arg0[0];
		
		RunInstancesResult result = launcher.launch();
		return result;
	}

	@Override
	protected void onPostExecute(RunInstancesResult result) {
		
		if (result.getReservation().getInstances().size() != 1){
			
			activity.showInUi("Launched instances count was not 1, something is wrong.");
		} else {
			Instance i = result.getReservation().getInstances().get(0);
			activity.showInUi(String.format(
					"Launched instance, " +
					"\n id = '%s'." +
					"\n AMI ID = '%s'", i.getInstanceId(), i.getImageId()));
		}
	}

	@Override
	protected void onPreExecute() {
		activity.showInUi("Starting 'instance launch task'");
	}

	

}
