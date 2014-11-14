package ee.ut.cs.mc.ec2.aws;

import java.io.IOException;
import java.io.InputStream;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;

import ee.ut.cs.mc.ec2.MainActivity;

public class Client{

	private static final String EC2_AWS_CREDENTIALS_PROPERTIES = "/assets/AwsCredentials.properties";
	String endPoint;
	AWSCredentials credentials;
	AmazonEC2Client amazonEC2Client;
    Instance instance;

	
	public Client(String endPoint) throws IOException{
		InputStream credentialsInputStream = getClass().getResourceAsStream(EC2_AWS_CREDENTIALS_PROPERTIES);
		credentials = new PropertiesCredentials(credentialsInputStream);

		amazonEC2Client = new AmazonEC2Client(credentials);
		
//		By default, the service endpoint is ec2.us-east-1.amazonaws.com. 
		amazonEC2Client.setEndpoint(endPoint);
	}

	public AmazonEC2Client getAmazonEC2Client() {
		return amazonEC2Client;
	}

    public Instance getInstance() {
        return instance;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    public void terminateInstance() {
        if (instance != null){
            TerminateInstanceTask instanceTask = new TerminateInstanceTask();
            instanceTask.execute(this);
        }
    }

    public void launchInstance(LaunchConfiguration conf, MainActivity activity) {
        InstanceLauncher launcher = new InstanceLauncher(this,conf);
        if (instance == null){
            RunInstanceTask instanceTask = new RunInstanceTask(activity);
            instanceTask.execute(launcher);
        }
    }
}
