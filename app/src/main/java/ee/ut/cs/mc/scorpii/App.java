package ee.ut.cs.mc.scorpii;

import android.app.Application;
import android.content.Context;

/**
 * Created by jaks on 20/02/15.
 */
public class App extends Application {

    private static Context context;

    public void onCreate(){
        super.onCreate();
        App.context = getApplicationContext();
    }

    public static Context getContext() {
        return App.context;
    }
}
