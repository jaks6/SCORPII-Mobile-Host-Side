package ee.ut.cs.mc.ec2.scp;

import android.util.Log;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.io.InputStream;

import ee.ut.cs.mc.ec2.MainActivity;

public class SshClient {

    final String TAG = getClass().getName();
    ExtendedJSch jsch;
    MainActivity activity;
    private String username;
    private String host;

    public SshClient(MainActivity activity) {
        this.activity = activity;
        this.jsch = new ExtendedJSch(activity);
    }

    protected void connectAndRunCommand() throws Exception {
        Log.i(TAG, "starting session connect");

        jsch.setIdentityKey("massKey.pem");

        username = "ubuntu";
        host = "54.68.46.234";
        Session session=jsch.getSession(username, host, 22);
        session.connect();

        //run stuff
        String command = "whoami;hostname";

        ChannelExec channel = (ChannelExec)session.openChannel("exec");
        Log.i(TAG, "starting channel connect2");
        channel.setCommand(command);
        channel.connect();

        readAndLogResponse(channel);

        channel.disconnect();
        session.disconnect();
    }

    private void readAndLogResponse(final ChannelExec channel) throws IOException, InterruptedException {
        InputStream input = channel.getInputStream();

        //start reading the input from the executed commands on the shell
        byte[] tmp = new byte[1024];
        Thread.sleep(1000);
        while (input.available() > 0) {
            int i = input.read(tmp, 0, 1024);
            if (i < 0) break;
            final String response = new String(tmp, 0, i);
            Log.i(TAG, response);
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.showInUi(response);
                }
            });

        }
        if (channel.isClosed()){
            final int exitStatus = channel.getExitStatus();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.showInUi("exit-status: " + exitStatus);
                }
            });

        }
    }
}
