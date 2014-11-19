package ee.ut.cs.mc.ec2.scp;

import android.content.res.AssetManager;
import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Jakob on 14.11.2014.
 */
public class ScpManager {

    public final String TAG = ScpManager.class.getSimpleName();
    ExtendedJSch jsch;

    String keyFileName;

    String username;
    String host;
    int port;

    Session session;

    public ScpManager() {
        this.jsch = new ExtendedJSch();
    }

    public void configureSession(String username, String host, int port, AssetManager manager, String keyFile) {
        this.username = username;
        this.host = host;
        this.port = port;
        this.keyFileName = keyFile;
        try {
            jsch.setIdentityKey(manager, keyFileName);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSchException e) {
            e.printStackTrace();
        }
    }

    public void initFileTransfer(final InputStream fileInStream, String filename, final Long size) {
        final String localFile = filename;
        final String remoteFile = localFile;

        new Thread(new Runnable() {
            public void run() {
                try {
                    openToChannelAndTransfer(remoteFile, localFile, fileInStream, size);

                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        }).start();
    }

    public void createSession() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    session = jsch.getSession(username, host, port);
                    session.connect();
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        }).start();
    }
    public void killSession() {
        new Thread(new Runnable() {
            public void run() {
                    session.disconnect();
                    session = null;
            }
        }).start();
    }

    private void openToChannelAndTransfer(String remoteFile, String localFile, InputStream fileInStream, Long size) throws JSchException, IOException {
        // exec 'scp -t rfile' remotely
        String command = "scp -t " + remoteFile;
        Channel channel = openChannelWithCommand(command);
        channel.connect();

        // get I/O streams for remote scp
        OutputStream remoteOut = channel.getOutputStream();
        InputStream remoteIn = channel.getInputStream();
        failIfNotAcknowledged(remoteIn);

        // send "C0644 filesize filename", where filename should not include '/'
        command = "C0644 " + size + " ";
        if (localFile.lastIndexOf('/') > 0) {
            localFile= localFile.substring(localFile.lastIndexOf('/') + 1);
        }
        command += localFile + "\n";

        remoteOut.write(command.getBytes());
        remoteOut.flush();
        failIfNotAcknowledged(remoteIn);

        // send content of file
        byte[] buf = new byte[1024];
        writeFileToRemoteOut(fileInStream, remoteOut, buf);

        // send '\0'
        buf[0] = 0;
        remoteOut.write(buf, 0, 1);
//        remoteOut.write(0); //!TODO CHECK IF THIS ALSO WORKS
        remoteOut.flush();
        failIfNotAcknowledged(remoteIn);

        channel.disconnect();
        remoteOut.close();
        remoteIn.close();
    }

    private Channel openChannelWithCommand(String command) throws JSchException {
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);
        return channel;
    }

    private void writeFileToRemoteOut(InputStream fileInStream, OutputStream remoteOut, byte[] buf) throws IOException {
        while (true) {
            int len = fileInStream.read(buf, 0, buf.length);
            if (len <= 0) break;
            remoteOut.write(buf, 0, len);
            remoteOut.flush();
        }
        fileInStream.close();
//        fileInStream = null;
    }

    private void failIfNotAcknowledged(InputStream remoteIn) throws IOException {
        if (checkAck(remoteIn) != 0) {
            System.exit(0);
        }
    }

    static int checkAck(InputStream in) throws IOException {
        int b = in.read();
        // b may be 0 for success,
        // 1 for error,
        // 2 for fatal error,
        // -1
        if (b == 0) return b;
        if (b == -1) return b;

        if (b == 1 || b == 2) {
            StringBuffer sb = new StringBuffer();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            }
            while (c != '\n');
            if (b == 1) { // error
                System.out.print(sb.toString());
            }
            if (b == 2) { // fatal error
                System.out.print(sb.toString());
            }
        }
        return b;
    }

    protected void connectAndRunCommand(String command) throws Exception {
        Log.i(TAG, "starting session connect");
        Session session=jsch.getSession(username, host, port);
        session.connect();

        Channel channel = openChannelWithCommand(command);
        channel.connect();

        readAndLogResponse((ChannelExec)channel);

        channel.disconnect();
        session.disconnect();
    }

    private void readAndLogResponse(final ChannelExec channel) throws IOException, InterruptedException {
        InputStream input = channel.getInputStream();

        //start reading the input from the executed commands on the shell
        byte[] tmp = new byte[1024];
//        Thread.sleep(1000);
        while (input.available() > 0) {
            int i = input.read(tmp, 0, 1024);
            if (i < 0) break;
            final String response = new String(tmp, 0, i);
            Log.i(TAG, response);
        }
        if (channel.isClosed()){
            final int exitStatus = channel.getExitStatus();
            Log.i(TAG, "" + exitStatus);
        }
    }


    /** Gets the given files InputStream and length,
     *  calls the initFileTransfer function with these values
     * @param assets
     * @param assetName
     */
    public void sendFile(AssetManager assets, String assetName) {
        try {
            Long length = assets.openFd(assetName).getLength();
            InputStream inputStream = assets.open(assetName);

            createSession();
            initFileTransfer(inputStream, assetName, length);
            killSession();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }
}
