package ee.ut.cs.mc.scorpii.scp;

import ee.ut.cs.mc.scorpii.MainActivity;

/**
 * Created by Jakob on 2.10.2014.
 */
public class SshThread extends Thread {

    MainActivity mainActivity;

    public SshThread(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    public void run() {

        SshClient sshClient = new SshClient(mainActivity);
        try {
            sshClient.connectAndRunCommand();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
