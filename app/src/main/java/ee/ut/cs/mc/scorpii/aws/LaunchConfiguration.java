package ee.ut.cs.mc.scorpii.aws;

public class LaunchConfiguration {

    String instanceType;
    String imageId;
    String keyName;
    String securityGroupId;
    public String subnetId;

    public LaunchConfiguration(String instanceType, String imageId,
                               String keyName, String securityGroup) {
        this.instanceType = instanceType;
        this.imageId = imageId;
        this.keyName = keyName;
        this.securityGroupId = securityGroup;
    }


    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public void setSecurityGroup(String securityGroup) {
        this.securityGroupId = securityGroup;
    }

}
