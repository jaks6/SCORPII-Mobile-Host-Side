package ee.ut.cs.mc.ec2;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.example.ec2.R;

import java.io.IOException;

import ee.ut.cs.mc.ec2.aws.Client;
import ee.ut.cs.mc.ec2.aws.InstanceLauncher;
import ee.ut.cs.mc.ec2.aws.LaunchConfiguration;
import ee.ut.cs.mc.ec2.aws.RunInstanceTask;
import ee.ut.cs.mc.ec2.scp.ScpThread;
import ee.ut.cs.mc.ec2.scp.SshThread;

public class MainActivity extends Activity {
	TextView consoleTextView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		consoleTextView = (TextView) findViewById(R.id.textview_console);

        //launchInstance();

//        connectSSH();
        doSCP();
    }

    private void connectSSH() {
        SshThread sshThread = new SshThread(this);
        sshThread.start();
    }

    private void doSCP() {
        ScpThread fileTransferThread = new ScpThread(this);

        fileTransferThread.configureSession("ubuntu", "54.69.166.70", 22);
        fileTransferThread.start();
    }

    public void launchInstance(){
        try {
            Client ec2Client = new Client("ec2.us-west-2.amazonaws.com");

            InstanceLauncher launcher = new InstanceLauncher(
                    ec2Client,
                    new LaunchConfiguration(
                            "t2.micro",			//instance type
                            "ami-89d794b9",		// machine image
                            "massKey",			// key
                            "sg-3b31a25e"		//security group
                        ));

            RunInstanceTask instanceTask = new RunInstanceTask(this);
            instanceTask.execute(launcher);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void showInUi(String msg){
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
}
