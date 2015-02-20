package ee.ut.cs.mc.scorpii.scp;

import android.content.Context;
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
        jsch.setLogger(new AndroidScpLogger());
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

        try {
            openToChannelAndTransfer(remoteFile, localFile, fileInStream, size);

        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    public void createSession() {
        Log.i(TAG, "createSession() ");
        int retriesLeft = 15;
        try {
            session = jsch.getSession(username, host, port);
        } catch (JSchException e) { e.printStackTrace(); }

        while (retriesLeft > 0 && !session.isConnected()) {
            retriesLeft--;
            try {
                session.connect(1500);
            } catch (JSchException e) {
//                Log.e(TAG, e.toString());
            }
        }
        if (session.isConnected()) Log.i(TAG, "***session connected");
    }

    public void killSession() {
        session.disconnect();
        session = null;
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
            localFile = localFile.substring(localFile.lastIndexOf('/') + 1);
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

    /** Executes the given command on the remote machine and logs the response */
    public void sendCommand(String command) {
        Log.v(TAG, "sendCommand()");
        try {
            createSession();

            Channel channel = openChannelWithCommand(command);
            channel.connect();

            readAndLogResponse((ChannelExec) channel);
            Log.v(TAG, "Disconnecting & killing session in sendCommand()");
            channel.disconnect();
            killSession();

        } catch (JSchException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readAndLogResponse(final ChannelExec channel) throws IOException, InterruptedException {
        InputStream input = channel.getInputStream();

        //start reading the input from the executed commands on the shell
        byte[] tmp = new byte[1024];
        while (true) {
            while (input.available() > 0) {
                int i = input.read(tmp, 0, 1024);
                if (i < 0) break;
                Log.i(TAG, new String(tmp, 0, i));
            }
            if (channel.isClosed()) {
                Log.i(TAG, "channel Closed with exit code=" + channel.getExitStatus());
                break;
            }
            Thread.sleep(1000);
        }
    }

    /**
     * Gets the given files InputStream and length,
     * calls the initFileTransfer function with these values
     *
     * @param resourceId
     */
    public void sendFileFromRawResources(Context context,int resourceId) throws IOException {
        InputStream inputStream = context.getResources().openRawResource(resourceId);
        String resourceName = context.getResources().getResourceEntryName(resourceId);
        //Danger, available() might be unreliable, see http://developer.android.com/reference/java/io/InputStream.html#available%28%29
        long length = inputStream.available();

        sendInputStreamAsFile(inputStream, resourceName, length);
    }

    /**
     * Gets the given files InputStream and length,
     * calls the initFileTransfer function with these values
     *
     * @param assets
     * @param assetName
     */
    public void sendFileFromAssets(AssetManager assets, String assetName) throws IOException {
        long length = assets.openFd(assetName).getLength();
        InputStream inputStream = assets.open(assetName);

        sendInputStreamAsFile(inputStream, assetName, length);
    }

    public void sendInputStreamAsFile(InputStream inputStream, String fileName, long length) throws IOException {

        createSession();
        initFileTransfer(inputStream, fileName, length);
        killSession();
    }


}
