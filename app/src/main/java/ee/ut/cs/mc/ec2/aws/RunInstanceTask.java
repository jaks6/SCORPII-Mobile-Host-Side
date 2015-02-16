package ee.ut.cs.mc.ec2.aws;

import android.os.AsyncTask;

import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesResult;

import ee.ut.cs.mc.ec2.MainActivity;
import ee.ut.cs.mc.ec2.Utils;

public class RunInstanceTask extends AsyncTask<InstanceLauncher, String,Instance> {
	MainActivity activity;

	public RunInstanceTask(MainActivity activity) {
		this.activity = activity;
	}
	
	@Override
	protected Instance doInBackground(InstanceLauncher... arg0) {
        Instance instance;

        InstanceLauncher launcher = arg0[0];
        RunInstancesResult result = launcher.launch();

        if (result.getReservation().getInstances().size() != 1) {
            return null;
        } else {
            String instanceIP = null;
            String instanceState = null;
            instance = result.getReservation().getInstances().get(0);

            //Wait until the public IP is known
            while (instanceIP == null || !instanceState.equals("running")){
                publishProgress(instanceState);
                DescribeInstancesResult describeResult = launcher.amazonEC2Client.describeInstances(
                        new DescribeInstancesRequest()
                                .withInstanceIds(instance.getInstanceId()));

                for (Reservation r : describeResult.getReservations()) {
                    instance = r.getInstances().get(0);
                    instanceState = instance.getState().getName();
                    instanceIP = instance.getPublicIpAddress();
                }
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
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
			activity.appendToUiConsole("Instance launch failed.");
		} else {
//            Instance i = result.getReservation().getInstances().get(0);
            String publicIP = result.getPublicIpAddress();
			activity.appendToUiConsole(String.format(
                    "Launched instance, " +
                            "\n ip = '%s'" +
                            "\n id = '%s'." +
                            "\n AMI ID = '%s'", publicIP, result.getInstanceId(), result.getImageId()));

            activity.onInstanceUpdate(result, Utils.INSTANCE_RUNNING);
		}
	}

	@Override
	protected void onPreExecute() {
		activity.appendToUiConsole("Starting 'instance launch task'");
	}

	

}
