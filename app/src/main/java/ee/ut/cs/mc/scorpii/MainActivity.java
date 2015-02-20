package ee.ut.cs.mc.scorpii;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.example.ec2.R;

import ee.ut.cs.mc.scorpii.aws.InstanceController;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();





    private TextView consoleTextView;
    private ScrollView scrollView;
    private InstanceController ec2InstanceController;
    private Switch useSnapshotAMIswitch;
    private Switch useCloudSwitch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        useSnapshotAMIswitch = (Switch) findViewById(R.id.switch_use_snapshot);
        useCloudSwitch = (Switch) findViewById(R.id.switch_use_cloud_utility);
        consoleTextView = (TextView) findViewById(R.id.textview_console);
        scrollView = (ScrollView) findViewById(R.id.scrollView);


        startScorpiiService();
    }

    private void startScorpiiService() {
        boolean useCloud = useCloudSwitch.isEnabled();

        Intent i= new Intent(this, ScorpiiService.class);
        i.putExtra(Utils.INTENT_KEY_NO_OF_DEVICES, Utils.NO_OF_DEVICES);
        i.putExtra(Utils.INTENT_KEY_USE_CLOUD, useCloud);
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
