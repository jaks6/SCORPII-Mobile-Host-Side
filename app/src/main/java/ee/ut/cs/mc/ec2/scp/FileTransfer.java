package ee.ut.cs.mc.ec2.scp;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import ee.ut.cs.mc.ec2.MainActivity;

/**
 * Created by Jakob on 3.10.2014.
 */
public class FileTransfer {

    void transfer(MainActivity activity){


        FileInputStream fis=null;
        try{
            JSch jsch = new JSch();
            String localFile = "";
            String remoteFile = "file.txt";

            String filename = "file.txt";
            InputStream fileInStream = activity.getAssets().open(filename);
            Session session = jsch.getSession("ubuntu", "54.69.166.70", 22);

            // username and password will be given via UserInfo interface.
            //        UserInfo ui=new MyUserInfo();
            //        session.setUserInfo(ui);
            session.connect();

            boolean ptimestamp = true; //preserve timestamps of original file?

            // exec 'scp -t rfile' remotely
            String command="scp " + (ptimestamp ? "-p" :"") +" -t " + remoteFile;
            Channel channel=session.openChannel("exec");
            ((ChannelExec)channel).setCommand(command);

            // get I/O streams for remote scp
            OutputStream remoteOut=channel.getOutputStream();
            InputStream remoteIn=channel.getInputStream();

            channel.connect();

            if(checkAck(remoteIn)!=0){
                System.exit(0);
            }

//            File _lfile = new File(localFile);
            Properties properties = new Properties();
            properties.load(fileInStream);

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
//
//            // send "C0644 filesize filename", where filename should not include '/'
//            long filesize=_lfile.length();
//            command="C0644 "+filesize+" ";
//            if(localFile.lastIndexOf('/')>0){
//                command += localFile.substring(localFile.lastIndexOf('/')+1);
//            }
//            else{
//                command += localFile;
//            }
//            command += "\n";
//            remoteOut.write(command.getBytes());
//            remoteOut.flush();
//            if(checkAck(remoteIn)!=0){
//                System.exit(0);
//            }

            // send a content of lfile
            fis=new FileInputStream(localFile);
            byte[] buf=new byte[1024];
            while(true){
                int len=fis.read(buf, 0, buf.length);
                if(len<=0) break;
                remoteOut.write(buf, 0, len); //out.flush();
            }
            fis.close();
            fis=null;
            // send '\0'
            buf[0]=0; remoteOut.write(buf, 0, 1); remoteOut.flush();
            if(checkAck(remoteIn)!=0){
                System.exit(0);
            }
            remoteOut.close();

            channel.disconnect();
            session.disconnect();

            System.exit(0);
        }
        catch(Exception e){
            System.out.println(e);
            try{if(fis!=null)fis.close();}catch(Exception ee){}
        }
    }

    static int checkAck(InputStream in) throws IOException {
        int b = in.read();
        // b may be 0 for success,
        // 1 for error,
        // 2 for fatal error,
        // -1
        if(b==0) return b;
        if(b==-1) return b;

        if(b==1 || b==2){
            StringBuffer sb=new StringBuffer();
            int c;
            do {
                c=in.read();
                sb.append((char)c);
            }
            while(c!='\n');
            if(b==1){ // error
                System.out.print(sb.toString());
            }
            if(b==2){ // fatal error
                System.out.print(sb.toString());
            }
        }
        return b;
    }
}
