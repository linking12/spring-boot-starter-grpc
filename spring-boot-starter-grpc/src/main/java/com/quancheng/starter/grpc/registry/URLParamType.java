package com.quancheng.starter.grpc.registry;

import com.quancheng.starter.grpc.GrpcConstants;

public enum URLParamType {
    registrySessionTimeout("registrySessionTimeout", 1 * GrpcConstants.MINUTE_MILLS),
    group("group", "default"), 
    nodeType("nodeType", GrpcConstants.NODE_TYPE_SERVICE),
    codec("codec", "grpc"),
    check("check", "true"), 
    registryRetryPeriod("registryRetryPeriod", 30 * GrpcConstants.SECOND_MILLS),
    protocol("protocol", GrpcConstants.DEFAULT_PROTOCOL), 
    version("version", GrpcConstants.DEFAULT_VERSION);
    
    private String name;
    private String  value;
    private long    longValue;
    private int     intValue;
    private boolean boolValue;

    private URLParamType(String name, String value){
        this.name = name;
        this.value = value;
    }

    private URLParamType(String name, long longValue){
        this.name = name;
        this.value = String.valueOf(longValue);
        this.longValue = longValue;
    }

    private URLParamType(String name, int intValue){
        this.name = name;
        this.value = String.valueOf(intValue);
        this.intValue = intValue;
    }

    private URLParamType(String name, boolean boolValue){
        this.name = name;
        this.value = String.valueOf(boolValue);
        this.boolValue = boolValue;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public int getIntValue() {
        return intValue;
    }

    public long getLongValue() {
        return longValue;
    }

    public boolean getBooleanValue() {
        return boolValue;
    }

}
