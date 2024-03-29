package ee.ut.cs.mc.scorpii.aws;

import android.util.Log;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceNetworkInterfaceSpecification;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class InstanceLauncher {

    public static final String NAME_TAG = "MHCM_android_";
    AmazonEC2Client amazonEC2Client;
    LaunchConfiguration config;

    private String TAG = InstanceLauncher.class.getName();


    public InstanceLauncher(InstanceController instanceController, LaunchConfiguration config) {
        this.amazonEC2Client = instanceController.getEc2Client();
        this.config = config;
    }

    public RunInstancesResult launch() {
        RunInstancesRequest runInstancesRequest =
                new RunInstancesRequest();

        runInstancesRequest
                .withImageId(config.imageId)
                .withInstanceType(config.instanceType)
                .withMinCount(1)
                .withMaxCount(1)
                .withKeyName(config.keyName)
                .withNetworkInterfaces(
                        new InstanceNetworkInterfaceSpecification()
                                .withDeviceIndex(0)
                                .withSubnetId("subnet-5da54504")
                                .withGroups(config.securityGroupId)
                                .withAssociatePublicIpAddress(true))
        ;
        runInstancesRequest.setPlacement(new Placement().withAvailabilityZone("us-east-1c"));
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


        Log.i(TAG, runInstancesResult.toString());
        return runInstancesResult;
    }
}
