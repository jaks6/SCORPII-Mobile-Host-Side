package ee.ut.cs.mc.ec2;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.amazonaws.services.ec2.model.Instance;
import com.example.ec2.R;

import java.io.IOException;

import ee.ut.cs.mc.ec2.aws.InstanceController;
import ee.ut.cs.mc.ec2.aws.LaunchConfiguration;
import ee.ut.cs.mc.ec2.aws.OnAwsUpdate;
import ee.ut.cs.mc.ec2.scp.ScpManager;
import ee.ut.cs.mc.ec2.scp.SshThread;

public class MainActivity extends Activity implements OnAwsUpdate {

    private static final String TAG = MainActivity.class.getSimpleName();

    //For SSH-ing into EC2 instance
    private static final String SHELL_USER = "ubuntu";
    private static final String KEY_FILE = "jakobmass.pem";
    private static final int PORT = 22;

    //FILES
    private static final String SHELL_SCRIPT = "setup.sh";
    private static final String BPEL = "bpel.zip";

    TextView consoleTextView;
    InstanceController ec2InstanceController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        consoleTextView = (TextView) findViewById(R.id.textview_console);
    }

    private void connectSSH() {
        SshThread sshThread = new SshThread(this);
        sshThread.start();
    }

    public void doSCP(View v) {
        Log.v(TAG, "Started doSCP method");
        new SCPTask().execute();

    }

    public void launchInstance() {
        try {
            ec2InstanceController = new InstanceController(this);
            //By default, the service endpoint is ec2.us-east-1.amazonaws.com.
            ec2InstanceController.setEndPoint("ec2.us-east-1.amazonaws.com");
            ec2InstanceController.launchInstance(new LaunchConfiguration(
                            "t1.micro",          //instance type
                            "ami-98aa1cf0",      // machine image
                            "jakob.mass",        // key
                            "sg-001c416a"        //security group
                    ),this);
        } catch (IOException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    public void showError(String msg) {
        consoleTextView.setText(msg);
    }
    public void showInUi(String msg) {
        consoleTextView.setText(msg);
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

    public void startInstance(View view) {
        launchInstance();
    }
    public void terminateInstance(View view) {
        terminateInstance();
    }

    private void terminateInstance() {
        ec2InstanceController.terminateInstance();
        showInUi("Shutting down last instance.");
    }

    @Override
    public void onInstanceUpdate(Instance i) {
        if (ec2InstanceController !=null) ec2InstanceController.setInstance(i);
    }

    public class SCPTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            if (ec2InstanceController != null){
                ScpManager scp = new ScpManager();
                scp.configureSession(SHELL_USER, ec2InstanceController.getInstance().getPublicIpAddress(), PORT, getAssets(),KEY_FILE);
                scp.sendFile(getAssets(), SHELL_SCRIPT);
                scp.sendFile(getAssets(), BPEL);
                scp.sendCommand("sudo bash setup.sh");

            } else {
                showInUi("ERROR - tried doing SCP with instance null!");
            }
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d("MyAsyncTask", "Finished");
        }
    }
}
