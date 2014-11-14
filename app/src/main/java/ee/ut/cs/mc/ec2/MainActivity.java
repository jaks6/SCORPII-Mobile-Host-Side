package ee.ut.cs.mc.ec2;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.amazonaws.services.ec2.model.Instance;
import com.example.ec2.R;

import java.io.IOException;
import java.io.InputStream;

import ee.ut.cs.mc.ec2.aws.Client;
import ee.ut.cs.mc.ec2.aws.LaunchConfiguration;
import ee.ut.cs.mc.ec2.aws.OnAwsUpdate;
import ee.ut.cs.mc.ec2.scp.ScpManager;
import ee.ut.cs.mc.ec2.scp.SshThread;

public class MainActivity extends Activity implements OnAwsUpdate {
    public static final String SHELL_USER = "ubuntu";
    public static final String KEY_FILE = "jakobmass.pem";
    public static final int PORT = 22;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String SHELL_SCRIPT = "/assets/setup.sh";
    TextView consoleTextView;
    Client ec2Client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        consoleTextView = (TextView) findViewById(R.id.textview_console);

        //launchInstance();

//        connectSSH();
//        doSCP();
    }

    private void connectSSH() {
        SshThread sshThread = new SshThread(this);
        sshThread.start();
    }

    public void doSCP(View v) {
        Log.v(TAG, "Started doSCP method");
        if (ec2Client != null){
            ScpManager scp = new ScpManager(this);
            scp.configureSession(SHELL_USER, ec2Client.getInstance().getPublicIpAddress(), PORT, KEY_FILE);
//            scp.transferFile(getClass().getResourceAsStream(R.raw.file));

            InputStream shellScriptInputStream = getClass().getResourceAsStream(SHELL_SCRIPT);
            scp.transferFile(shellScriptInputStream, "setup.sh");
        } else {
            showInUi("ERROR - tried doing SCP with instance null!");
        }
    }

    public void launchInstance() {
        try {
            ec2Client = new Client("ec2.us-east-1.amazonaws.com");
            ec2Client.launchInstance(new LaunchConfiguration(
                            "t1.micro",            //instance type
                            "ami-98aa1cf0",        // machine image
                            "jakob.mass",            // key
                            "sg-001c416a"        //security group
                    ),this);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        ec2Client.terminateInstance();
        showInUi("Shutting down last instance.");
    }

    @Override
    public void onInstanceUpdate(Instance i) {
        if (ec2Client!=null) ec2Client.setInstance(i);
    }
}
