package ee.ut.cs.mc.ec2.aws;

import java.io.IOException;
import java.io.InputStream;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;

public class Client{

	private static final String EC2_AWS_CREDENTIALS_PROPERTIES = "/assets/AwsCredentials.properties";
	String endPoint;
	AWSCredentials credentials;
	AmazonEC2Client amazonEC2Client;
	
	public Client(String endPoint) throws IOException{
		InputStream is = getClass().getResourceAsStream(EC2_AWS_CREDENTIALS_PROPERTIES);
		credentials = new PropertiesCredentials(is);
		
		amazonEC2Client = new AmazonEC2Client(credentials);
		
//		By default, the service endpoint is ec2.us-east-1.amazonaws.com. 
		amazonEC2Client.setEndpoint(endPoint);
	}

	public AmazonEC2Client getAmazonEC2Client() {
		return amazonEC2Client;
	}

}
