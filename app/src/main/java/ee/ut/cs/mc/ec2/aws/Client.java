package ee.ut.cs.mc.ec2.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Instance;

import java.io.IOException;
import java.io.InputStream;

import ee.ut.cs.mc.ec2.MainActivity;

public class Client{

	private static final String EC2_AWS_CREDENTIALS_PROPERTIES = "/assets/AwsCredentials.properties";
	String endPoint;
	AWSCredentials credentials;
	AmazonEC2Client ec2Client;
    Instance instance;

	
	public Client(String endPoint) throws IOException{
		InputStream credentialsInputStream = getClass().getResourceAsStream(EC2_AWS_CREDENTIALS_PROPERTIES);
		credentials = new PropertiesCredentials(credentialsInputStream);

		ec2Client = new AmazonEC2Client(credentials);
		
//		By default, the service endpoint is ec2.us-east-1.amazonaws.com. 
		ec2Client.setEndpoint(endPoint);
	}

	public AmazonEC2Client getEc2Client() {
		return ec2Client;
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
