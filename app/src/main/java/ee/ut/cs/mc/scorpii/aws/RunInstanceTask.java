package ee.ut.cs.mc.scorpii.aws;

import android.os.AsyncTask;
import android.util.Log;

import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesResult;

import ee.ut.cs.mc.scorpii.CloudServiceMediator;
import ee.ut.cs.mc.scorpii.Utils;

public class RunInstanceTask extends AsyncTask<InstanceLauncher, String,Instance> {
    private static final String TAG = RunInstanceTask.class.getName();
    private final CloudServiceMediator mediator;

    public RunInstanceTask(CloudServiceMediator mediator) {
        this.mediator = mediator;
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
                    Thread.sleep(2000);
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
//        Log.i(TAG, String.format("Waiting for instance to get ready;" +
//                "\n instance state = %s", progress[0]));
    }


    @Override
	protected void onPostExecute(Instance result) {
        Log.i(TAG, "*** Instance is running");
		if (result == null){
            Log.i(TAG, "Instance launch failed.");
        } else {
//            Instance i = result.getReservation().getInstances().get(0);
            String publicIP = result.getPublicIpAddress();
            Log.i(TAG, String.format(
                    "Launched instance, " +
                            "\n ip = '%s'" +
                            "\n id = '%s'." +
                            "\n AMI ID = '%s'", publicIP, result.getInstanceId(), result.getImageId()));

            mediator.onInstanceUpdate(result, Utils.INSTANCE_RUNNING);
        }
	}

	@Override
	protected void onPreExecute() {
        Log.i(TAG, "Starting 'instance launch task'");
    }

	

}
