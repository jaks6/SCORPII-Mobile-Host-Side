package ee.ut.cs.mc.scorpii;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * glassfish/bootstrap/legal/CDDLv1.0.txt or
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * glassfish/bootstrap/legal/CDDLv1.0.txt.  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */

/*
 * @(#)ASCIIUtility.java  1.10 05/08/29
 *
 * Copyright 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 */

public class Utils {

    //EC2 AMIs
    public static final String AMI_ODE_SNAPSHOT = "ami-905c0bf8";
    public static final String AMI_UBUNTU = "ami-98aa1cf0";

    //For SSH-ing into EC2 instance
    public static final String SHELL_USER = "ubuntu";
    public static final String KEY_FILE = "jakobmass.pem";
    public static final int PORT = 22;

    //FILES
    public static final String BPEL_FILENAME = "bpel.zip";
    public static final String BPEL_FOLDERNAME = "HelloWorld";
    public static final String EC2_INSTANCE_SETTINGS = "Ec2PrefsFile";

    // MIRRORS
    public static final String APACHEODE_MIRROR_URL = "http://mirror.symnds.com/software/Apache/ode/apache-ode-war-1.3.6.zip";

    //INTENT KEYS
    public static final String INTENT_KEY_NO_OF_DEVICES = "NO_OF_DEVICES";
    public static final String INTENT_KEY_ACTION = "ACTION";
    public static final String INTENT_KEY_USE_CLOUD = "USE_CLOUD";

    //Intent actions
    public static final int INTENT_ACTION_DEFAULT = 0;
    public static final int INTENT_ACTION_START_FLOW = 1;


    public static final int NO_OF_DEVICES = 5;
    public static final int INSTANCE_RUNNING = 0;
    public static final String PARSE_ARGUMENT = "http://sweet.jpl.nasa.gov/2.2/quanTemperature.owl#Temperature";
    public static final long TIMEOUT_PARSE = 6;


    public static byte[] getBytes(InputStream is) throws IOException {

        int len;
        int size = 1024;
        byte[] buf;

        if (is instanceof ByteArrayInputStream) {
            size = is.available();
            buf = new byte[size];
            len = is.read(buf, 0, size);
        } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            buf = new byte[size];
            while ((len = is.read(buf, 0, size)) != -1)
                bos.write(buf, 0, len);
            buf = bos.toByteArray();
        }
        return buf;
    }

    /** Defines callbacks for service binding, passed to bindService() */
//    public static class ScorpiiServiceConnection implements ServiceConnection {
//        ScorpiiService s;
//        ScorpiiServiceConnection(){
//        }
//        public void onServiceConnected(ComponentName className, IBinder binder) {
//            ScorpiiService.ScorpiiServiceBinder b = (ScorpiiService.ScorpiiServiceBinder) binder;
//            s = b.getService();
//        }
//        public void onServiceDisconnected(ComponentName className) {
//            s = null;
//        }
//        public ScorpiiService getService(){ return s; }
//    };


}