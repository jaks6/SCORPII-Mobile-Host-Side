package ee.ut.cs.mc.ec2.aws;

import android.os.AsyncTask;

import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;

/**
 * Created by Jakob on 14.11.2014.
 */
public class TerminateInstanceTask extends AsyncTask<Client, Void, Void> {
    @Override
    protected Void doInBackground(Client... args) {
        Client awsClient = args[0];
        TerminateInstancesResult result =
                awsClient.getEc2Client().terminateInstances(new TerminateInstancesRequest()
                        .withInstanceIds(awsClient.getInstance().getInstanceId()));
        return null;

    }
}
