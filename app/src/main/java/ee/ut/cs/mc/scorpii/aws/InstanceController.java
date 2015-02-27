package ee.ut.cs.mc.scorpii.aws;

import android.content.Context;
import android.util.Log;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Instance;
import com.example.ec2.R;

import java.io.InputStream;

import ee.ut.cs.mc.scorpii.CloudServiceMediator;
import ee.ut.cs.mc.scorpii.MainActivity;

public class InstanceController {

	private static final String EC2_AWS_CREDENTIALS_PROPERTIES = "/assets/AwsCredentials.properties";
    private static final String TAG = InstanceController.class.getName();

    String endPoint;
	AWSCredentials credentials;
	AmazonEC2Client ec2Client;
    Instance instance;

    Context context;
    private CloudServiceMediator mediator;


    public InstanceController(Context context) throws Exception {
        this(context, null);
    }

    public InstanceController(Context context, CloudServiceMediator cloudServiceMediator) throws Exception {
        this.mediator = cloudServiceMediator;
        context = context.getApplicationContext();

        InputStream credentialsInputStream = getClass().getResourceAsStream(EC2_AWS_CREDENTIALS_PROPERTIES);

        credentials = new PropertiesCredentials(credentialsInputStream);
        if (credentials.getAWSAccessKeyId().isEmpty() ||
                credentials.getAWSSecretKey().isEmpty()){
            throw new Exception(context.getString(R.string.aws_credentials_missing_error));
        }
        else {
            ec2Client = new AmazonEC2Client(credentials);

        }

    }


    public void terminateInstance() {
        if (instance != null){
            TerminateInstanceTask instanceTask = new TerminateInstanceTask();
            instanceTask.execute(this);
        }
    }

    public void launchInstance(LaunchConfiguration conf) {
        InstanceLauncher launcher = new InstanceLauncher(this,conf);
        if (instance == null){
            RunInstanceTask instanceTask = new RunInstanceTask(mediator);
            instanceTask.execute(launcher);
        } else {
            Log.i(TAG, "Did not launch new instance because current instance was not null.");
        }
    }

    public void connectToInstance(String instanceId, MainActivity activity) {
        InstanceConnector connector = new InstanceConnector(this,instanceId);
        if (instance == null){
            ConnectToInstanceTask instanceTask = new ConnectToInstanceTask(activity,connector);
            instanceTask.execute();
        }
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
    public void setEndPoint(String endPoint) {this.endPoint = endPoint; }
}
