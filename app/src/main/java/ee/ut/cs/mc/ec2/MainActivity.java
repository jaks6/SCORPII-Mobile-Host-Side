package ee.ut.cs.mc.ec2;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.ec2.R;

import ee.ut.cs.mc.ec2.aws.InstanceController;

public class MainActivity extends Activity {

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

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
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
