package ee.ut.cs.mc.ec2;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;

import com.amazonaws.services.ec2.model.Instance;
import com.example.ec2.R;

import java.io.IOException;
import java.util.Locale;

import ee.ut.cs.mc.ec2.aws.InstanceController;
import ee.ut.cs.mc.ec2.aws.LaunchConfiguration;
import ee.ut.cs.mc.ec2.aws.OnAwsUpdate;
import ee.ut.cs.mc.ec2.scp.ScpManager;

public class MainActivity extends Activity implements OnAwsUpdate {

    private static final String TAG = MainActivity.class.getSimpleName();
    //EC2 AMIs
    private static final String AMI_ODE_SNAPSHOT = "ami-905c0bf8";
    private static final String AMI_UBUNTU = "ami-98aa1cf0";

    //For SSH-ing into EC2 instance
    private static final String SHELL_USER = "ubuntu";
    private static final String KEY_FILE = "jakobmass.pem";
    private static final int PORT = 22;

    //FILES
    private static final String BPEL_FILENAME = "bpel.zip";
    private static final String BPEL_FOLDERNAME = "HelloWorld";
    private static final String EC2_INSTANCE_SETTINGS = "Ec2PrefsFile";

    // MIRRORS
    private static final String APACHEODE_MIRROR_URL = "http://mirror.symnds.com/software/Apache/ode/apache-ode-war-1.3.6.zip";


    TextView consoleTextView;
    ScrollView scrollView;
    InstanceController ec2InstanceController;
    CheckBox useSnapshotAMIcheckBox;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        useSnapshotAMIcheckBox = (CheckBox) findViewById(R.id.chckBox_use_snapshot);
        consoleTextView = (TextView) findViewById(R.id.textview_console);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        ec2InstanceController = createInstanceControllerIfDoesntExist();
    }

    /** Main MHCM Use case is initiated from this method */
    public void doSCP(View v) {
        Log.v(TAG, "Started doSCP method");
        new SCPTask().execute();
    }

    public void launchInstance() {
        Log.i(TAG, "*** Launch Instance method");
        String machine_AMI;
        machine_AMI = useSnapshotAMIcheckBox.isChecked() ? AMI_ODE_SNAPSHOT : AMI_UBUNTU;

        try {
            ec2InstanceController = createInstanceControllerIfDoesntExist();
            ec2InstanceController.launchInstance(new LaunchConfiguration(
                            "m3.large",          //instance type
                            machine_AMI,        // machine image
                            "jakob.mass",        // key
                            "sg-001c416a"        //security group
                    ),this);
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    public void showError(String msg) {
        consoleTextView.setText(msg);
    }
    public void appendToUiConsole(String msg) {
        consoleTextView.append("\n" +msg);
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    public void startInstance(View view) {
        launchInstance();
    }
    public void terminateInstance(View view) {
        terminateInstance();
    }

    private void terminateInstance() {
        ec2InstanceController.terminateInstance();
        appendToUiConsole("Shutting down last instance.");
    }

    public void onInstanceUpdate(Instance i, int stateCode) {
        if (ec2InstanceController != null) ec2InstanceController.setInstance(i);
        if (stateCode==Utils.INSTANCE_RUNNING){
            if (useSnapshotAMIcheckBox.isChecked()){
                new SCPTaskSnapshot().execute();
            } else {
                new SCPTask().execute();
            }
        }
    }
    /** Checks if we have already launched an instance and if so,
     * tries to get that instances description */
    private void reconnectToInstanceIfExists() {
        Log.i(TAG, "reconnectToInstanceIfExists method");
        SharedPreferences settings = getSharedPreferences(EC2_INSTANCE_SETTINGS, 0);

        boolean instanceLaunched = settings.getBoolean("InstanceLaunched",false);
        String instanceId = settings.getString("InstanceIdentifier", "");
        if (instanceLaunched){
            ec2InstanceController = createInstanceControllerIfDoesntExist();
            ec2InstanceController.connectToInstance(instanceId, this);
        }
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        SharedPreferences settings = getSharedPreferences(EC2_INSTANCE_SETTINGS, 0);
        SharedPreferences.Editor editor = settings.edit();
        boolean instanceLaunched = false;
        instanceLaunched =
                (ec2InstanceController != null && ec2InstanceController.getInstance() != null);

        editor.putBoolean("InstanceLaunched", instanceLaunched);
        if (instanceLaunched) {
            editor.putString("InstanceIdentifier", ec2InstanceController.getInstance().getInstanceId());
        }
        editor.commit();

    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        reconnectToInstanceIfExists();
    }


    private InstanceController createInstanceControllerIfDoesntExist() {
        if (ec2InstanceController == null){
            try {
                InstanceController controller = new InstanceController(this);
                controller.setEndPoint("ec2.us-east-1.amazonaws.com");
                return controller;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ec2InstanceController;
    }

    public class SCPTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            Log.i(TAG, "***SCP TASK STARTED");
            if (ec2InstanceController != null){
                try {
                    ScpManager scp = new ScpManager();
                    scp.configureSession(SHELL_USER, ec2InstanceController.getInstance()
                            .getPublicIpAddress(), PORT, getAssets(),KEY_FILE);

                    scp.sendFileFromRawResources(MainActivity.this, R.raw.setup);
                    scp.sendFileFromAssets(getAssets(), BPEL_FILENAME);

                    String command = String.format(Locale.getDefault(),
                                    "sudo bash setup '%s' '%s' '%s'",
                                    APACHEODE_MIRROR_URL,
                                    BPEL_FILENAME,
                                    BPEL_FOLDERNAME);
                    scp.sendCommand(command);
                } catch (IOException e) {
                    showError(e.getMessage());
                }
            } else {
                appendToUiConsole("ERROR - tried doing SCP with instance null!");
            }
            return "";
        }
        @Override
        protected void onPostExecute(String result) {
            Log.d("*** SCP Task", "Finished");
        }
    }

    public class SCPTaskSnapshot extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            Log.i(TAG, "***SCPTaskSnapshot STARTED");
            if (ec2InstanceController != null){
                try {
                    ScpManager scp = new ScpManager();
                    scp.configureSession(SHELL_USER, ec2InstanceController.getInstance()
                            .getPublicIpAddress(), PORT, getAssets(),KEY_FILE);

                    scp.sendFileFromAssets(getAssets(), BPEL_FILENAME);

                    String command1 = String.format(
                            "sudo unzip -j %s -d %s",
                            BPEL_FILENAME,
                            "/var/lib/tomcat7/webapps/ode/WEB-INF/processes/"+BPEL_FOLDERNAME);
                    String command2 = ("sudo chown -R tomcat7 /var/lib/tomcat7/webapps/ode/WEB-INF/processes/"+BPEL_FOLDERNAME);
                    String command3 = ("sudo rm -f /var/lib/tomcat7/webapps/ode/WEB-INF/processes/*.deployed");
                    scp.sendCommand(String.format("%s && %s && %s", command1,command2,command3));
                } catch (IOException e) {
                    showError(e.getMessage());
                }
            } else {
                appendToUiConsole("ERROR - tried doing SCP with instance null!");
            }
            return "";
        }
        @Override
        protected void onPostExecute(String result) {
            Log.d("*** SCPTaskSnapshot", "Finished");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
