package ee.ut.cs.mc.ec2.aws;

import android.util.Log;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;

import java.util.List;

public class InstanceConnector {

    public static final String NAME_TAG = "MHCM_android_";
    AmazonEC2Client amazonEC2Client;



    private String instanceId;
	private String TAG = InstanceConnector.class.getName();


	public InstanceConnector(InstanceController instanceController, String instanceId) {
		this.amazonEC2Client = instanceController.getEc2Client();
		this.instanceId = instanceId;
	}

	public Instance launch() throws Exception {
        DescribeInstancesRequest describeRequest =
                new DescribeInstancesRequest().
                        withInstanceIds(instanceId);

        DescribeInstancesResult describeResult = amazonEC2Client.describeInstances(describeRequest);
        Log.i(TAG , describeResult.toString());


        List<Reservation> reservations = describeResult.getReservations();
        if (!reservations.isEmpty()){
            List<Instance> instances= reservations.get(0).getInstances();
            if (!instances.isEmpty()) {
                Instance instance = instances.get(0);
                return instance;
            } else {
                throw new Exception("Couldnt retrieve instance");
            }
        } else {
            throw new Exception("Couldnt retrieve instance");
        }
	}
    public String getInstanceId() {
        return instanceId;
    }
}
