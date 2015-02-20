package ee.ut.cs.mc.scorpii.aws;

import com.amazonaws.services.ec2.model.Instance;

/**
 * Created by Jakob on 14.11.2014.
 */
public interface OnAwsUpdate {
//    void onInstanceUpdate(Instance i);
    void onInstanceUpdate(Instance i, int code);
}
