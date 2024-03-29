package ee.ut.cs.mc.scorpii.scp;

import android.content.Context;
import android.content.res.AssetManager;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Jakob on 7.10.2014.
 */
public class ExtendedJSch extends JSch{
    Context context;
    public ExtendedJSch() {
        super();
    }

    public void setIdentityKey(AssetManager manager, String keyFileName) throws IOException, JSchException {
        InputStream input = manager.open(keyFileName);
        int size = input.available();
        byte[] buffer = new byte[size];
        input.read(buffer);
        input.close();

        addIdentity(keyFileName, buffer, null, null);
        setConfig("StrictHostKeyChecking", "no");
    }
}
