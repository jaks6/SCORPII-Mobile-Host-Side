package ee.ut.cs.mc.ec2.scp;

import android.content.Context;

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
    Context context;
    ExtendedJSch jsch;

    String keyFileName;

    String username;
    String host;
    int port;

    Session session;

    public ScpManager(Context ctx) {
        this.context = ctx;
        this.jsch = new ExtendedJSch(ctx);
    }

    public void configureSession(String username, String host, int port, String keyFile) {
        this.username = username;
        this.host = host;
        this.port = port;
        this.keyFileName = keyFile;
        try {
            jsch.setIdentityKey(keyFileName);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSchException e) {
            e.printStackTrace();
        }
    }

//    public void transferFile(String filename){
//        new Thread(new Runnable() {
//            public void run() {
////                Bitmap b = loadImageFromNetwork("http://example.com/image.png");
////                mImageView.setImageBitmap(b);
//            }
//        }).start();
//
//    }

    public void transferFile(final InputStream fileInStream, String filename) {
//        final InputStream fileInStream = context.getResources().openRawResource(fileResource);
//        final InputStream fInStream = fileInStream.;
        final String localFile = filename;
        final String remoteFile = localFile;

        new Thread(new Runnable() {
            public void run() {
                try {
                    session = jsch.getSession(username, host, port);
                    session.connect();

                    boolean ptimestamp = true; //preserve timestamps of original file?
                    execTransferCommands(ptimestamp, remoteFile, localFile, fileInStream);

                    session.disconnect();
                } catch (Exception e) {
                    System.out.println(e);
//            try{if(fis!=null)fis.close();}catch(Exception ee){}
                }
            }
            }).start();
    }

    private void execTransferCommands(boolean ptimestamp, String remoteFile, String localFile,InputStream fileInStream) throws JSchException, IOException {
        // exec 'scp -t rfile' remotely
        String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + remoteFile;
        Channel channel = getChannelWithCommand(command);

        // get I/O streams for remote scp
        OutputStream remoteOut = channel.getOutputStream();
        InputStream remoteIn = channel.getInputStream();

        channel.connect();
        failIfNotAcknowledged(remoteIn);

//            if(ptimestamp){
//                command="T "+(_lfile.lastModified()/1000)+" 0";
//            // The access time should be sent here,
//            // but it is not accessible with JavaAPI ;-<
//                command+=(" "+(_lfile.lastModified()/1000)+" 0\n");
//                remoteOut.write(command.getBytes()); remoteOut.flush();
//                if(checkAck(remoteIn)!=0){
//                    System.exit(0);
//                }
//            }

        int filesize = fileInStream.available(); // this might provide unreliable results in some cases

        // send "C0644 filesize filename", where filename should not include '/'
        command = "C0644 " + filesize + " ";
        if (localFile.lastIndexOf('/') > 0) {
            command += localFile.substring(localFile.lastIndexOf('/') + 1);
        } else {
            command += localFile;
        }
        command += "\n";

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

    private Channel getChannelWithCommand(String command) throws JSchException {
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);
        return channel;
    }

    private void writeFileToRemoteOut(InputStream fileInStream, OutputStream remoteOut, byte[] buf) throws IOException {
        while (true) {
            int len = fileInStream.read(buf, 0, buf.length);
            if (len <= 0) break;
            remoteOut.write(buf, 0, len); //out.flush();

        }
        fileInStream.close();
        fileInStream = null;
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
}
