package ee.ut.cs.mc.scorpii.aws;

import android.os.AsyncTask;

import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;

/**
 * Created by Jakob on 14.11.2014.
 */
public class TerminateInstanceTask extends AsyncTask<InstanceController, Void, Void> {
    @Override
    protected Void doInBackground(InstanceController... args) {
        InstanceController awsInstanceController = args[0];
        TerminateInstancesResult result =
                awsInstanceController.getEc2Client().terminateInstances(new TerminateInstancesRequest()
                        .withInstanceIds(awsInstanceController.getInstance().getInstanceId()));
        awsInstanceController.setInstance( null);
        return null;

    }
}
