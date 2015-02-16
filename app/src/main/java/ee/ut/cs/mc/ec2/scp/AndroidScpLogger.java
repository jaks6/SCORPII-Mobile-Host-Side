package ee.ut.cs.mc.ec2.scp;

import android.util.Log;

import com.jcraft.jsch.Logger;

/**
 * Created by Jakob on 13.02.2015.
 */
public class AndroidScpLogger implements Logger {
    private static final String TAG = AndroidScpLogger.class.getName();

    public AndroidScpLogger() {
    }

    @Override
    public boolean isEnabled(int level) {
        return true;
    }

    @Override
    public void log(int level, String message) {
        Log.v(TAG,message);
    }
}
