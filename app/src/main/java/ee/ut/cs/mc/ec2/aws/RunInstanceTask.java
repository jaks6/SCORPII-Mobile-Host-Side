package ee.ut.cs.mc.ec2.aws;

import android.os.AsyncTask;

import com.amazonaws.services.ec2.model.DescribeAddressesRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceAttributeRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesResult;

import ee.ut.cs.mc.ec2.MainActivity;

public class RunInstanceTask extends AsyncTask<InstanceLauncher, Void,Instance> {
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
            activity.showInUi("Launched instances count was not 1, something is wrong.");
            return null;
        } else {
            String instanceIP = null;
            instance = result.getReservation().getInstances().get(0);

            //Wait until the public IP is known
            while (instanceIP == null){
                DescribeInstancesResult describeResult = launcher.amazonEC2Client.describeInstances(
                        new DescribeInstancesRequest()
                                .withInstanceIds(instance.getInstanceId()));

                for (Reservation r : describeResult.getReservations()) {
                    instance = r.getInstances().get(0);
                    instanceIP = instance.getPublicIpAddress();
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    activity.showInUi("Thread sleep interrupted exception:" + e);
                }
            }
        }
		return instance;
	}

	@Override
	protected void onPostExecute(Instance result) {
		if (result == null){
			activity.showInUi("Instance launch failed.");
		} else {
//            Instance i = result.getReservation().getInstances().get(0);
            String publicIP = result.getPublicIpAddress();
			activity.showInUi(String.format(
					"Launched instance, " +
                    "\n ip = '%s'"+
					"\n id = '%s'." +
					"\n AMI ID = '%s'", publicIP, result.getInstanceId(), result.getImageId()));

            activity.onInstanceUpdate(result);
            activity.doSCP(publicIP);
		}
	}

	@Override
	protected void onPreExecute() {
		activity.showInUi("Starting 'instance launch task'");
	}

	

}
