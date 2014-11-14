package ee.ut.cs.mc.ec2.aws;

import android.util.Log;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.util.JodaTime;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class InstanceLauncher {

    public static final String NAME_TAG = "MHCM_android_";
    AmazonEC2Client amazonEC2Client;
	LaunchConfiguration config;
	
	private String TAG = InstanceLauncher.class.getName();
	
	
	public InstanceLauncher(Client client, LaunchConfiguration config) {
		this.amazonEC2Client = client.getAmazonEC2Client();
		this.config = config;
	}

	public RunInstancesResult launch(){
		RunInstancesRequest runInstancesRequest = 
				new RunInstancesRequest();

		runInstancesRequest
			.withImageId(config.imageId)
			.withInstanceType(config.instanceType)
			.withMinCount(1)
			.withMaxCount(1)
			.withKeyName(config.keyName)
			.withSecurityGroupIds(config.securityGroupId);
		
		 RunInstancesResult runInstancesResult = 
				  amazonEC2Client.runInstances(runInstancesRequest);

        // TAG EC2 INSTANCES
        List<Instance> instances = runInstancesResult.getReservation().getInstances();
        String timeStamp = new SimpleDateFormat("MMdd.HHmm").format(Calendar.getInstance().getTime());
        for (Instance instance : instances) {
            CreateTagsRequest createTagsRequest =
                    new CreateTagsRequest()
                        .withResources(instance.getInstanceId()) //
                        .withTags(new Tag("Name", NAME_TAG + timeStamp));
            amazonEC2Client.createTags(createTagsRequest);
        }
		 
		 
		 Log.i(TAG , runInstancesResult.toString());
		 return runInstancesResult;
	}
}
