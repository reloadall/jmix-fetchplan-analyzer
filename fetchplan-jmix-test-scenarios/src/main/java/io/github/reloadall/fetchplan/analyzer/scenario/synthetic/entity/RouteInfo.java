package io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity;

public class RouteInfo {

    private String code;
    private VendorInfo vendorInfo;
    private GroupInfo groupInfo;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public VendorInfo getVendorInfo() {
        return vendorInfo;
    }

    public void setVendorInfo(VendorInfo vendorInfo) {
        this.vendorInfo = vendorInfo;
    }

    public GroupInfo getGroupInfo() {
        return groupInfo;
    }

    public void setGroupInfo(GroupInfo groupInfo) {
        this.groupInfo = groupInfo;
    }
}