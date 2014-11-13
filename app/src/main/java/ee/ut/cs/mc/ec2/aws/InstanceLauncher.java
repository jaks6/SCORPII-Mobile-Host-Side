package ee.ut.cs.mc.ec2.aws;

import android.util.Log;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;

public class InstanceLauncher {
	
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


		 
		 
		 Log.i(TAG , runInstancesResult.toString());
		 return runInstancesResult;
	}
}
