package ee.ut.cs.mc.scorpii;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.amazonaws.services.ec2.model.Instance;

import java.io.IOException;

import ee.ut.cs.mc.scorpii.aws.InstanceController;
import ee.ut.cs.mc.scorpii.aws.LaunchConfiguration;
import ee.ut.cs.mc.scorpii.aws.OnAwsUpdate;
import ee.ut.cs.mc.scorpii.scp.ScpManager;

/**
 * Created by jaks on 20/02/15.
 */
public class CloudServiceMediator implements OnAwsUpdate {
    public static final String TAG = CloudServiceMediator.class.getName();

    InstanceController instanceController;

    public boolean isScpCompletedFlag() {
        return scpCompletedFlag;
    }

    boolean scpCompletedFlag = false;
    Context ctx;

    public CloudServiceMediator() {
        this.ctx = App.getContext();
    }


    public void launchInstance() {
        Log.i(TAG, "*** Launch Instance method");
        try {
            instanceController = createInstanceControllerIfDoesntExist();
            instanceController.launchInstance(new LaunchConfiguration(
                    "t2.medium", //instance type
                    Utils.AMI_UBUNTU, // machine image
                    "jakob.mass", // key
                    "sg-fd262b98" //security group
            ));
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private InstanceController createInstanceControllerIfDoesntExist() {
        if (instanceController == null) {
            try {
                InstanceController controller = new InstanceController(App.getContext(), this);
                controller.setEndPoint("ec2.us-east-1.amazonaws.com");
                return controller;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return instanceController;
    }

    @Override
    public void onInstanceUpdate(Instance i, int stateCode) {
        if (instanceController != null) instanceController.setInstance(i);
        if (stateCode == Utils.INSTANCE_RUNNING) {
            new SCPTaskSnapshot().execute();
        }
    }


    public class SCPTaskSnapshot extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            Log.i(TAG, "***SCPTaskSnapshot STARTED");
            if (instanceController != null) {
                try {
                    ScpManager scp = new ScpManager();
                    scp.configureSession(Utils.SHELL_USER, instanceController.getInstance()
                            .getPublicIpAddress(), Utils.PORT, ctx.getAssets(), Utils.KEY_FILE);
                    scp.sendFileFromAssets(ctx.getAssets(), Utils.BPEL_FILENAME);

                    String command = combineShellCommand();
                    scp.sendCommand(command);

                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            } else {
                Log.e(TAG, "ERROR - tried doing SCP with instance null!");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            scpCompletedFlag = true;
            Log.d("*** SCPTaskSnapshot", "Finished");
        }
    }

    private String combineShellCommand() {
        String command1 = String.format(
                "sudo unzip -j %s -d %s",
                Utils.BPEL_FILENAME,
                "/var/lib/tomcat7/webapps/ode/WEB-INF/processes/" + Utils.BPEL_FOLDERNAME);
        String command2 = (
                "sudo chown -R tomcat7 /var/lib/tomcat7/webapps/ode/WEB-INF/processes/" + Utils.BPEL_FOLDERNAME);
        String command3 = (
                "sudo rm -f /var/lib/tomcat7/webapps/ode/WEB-INF/processes/*.deployed");
        return String.format("%s && %s && %s", command1, command2, command3);
    }

    public InstanceController getInstanceController() {
        return instanceController;
    }
}

