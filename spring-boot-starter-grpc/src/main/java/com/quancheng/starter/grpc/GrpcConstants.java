package com.quancheng.starter.grpc;

import java.util.regex.Pattern;

import io.grpc.Metadata;

public class GrpcConstants {

    public static final String         NODE_TYPE_SERVICE        = "service";
    public static final String         NODE_TYPE_REFERER        = "referer";
    public static final String         METHOD_CONFIG_PREFIX     = "methodconfig.";
    public static final String         PROTOCOL_SEPARATOR       = "://";
    public static final String         PATH_SEPARATOR           = "/";
    public static final String         DEFAULT_VERSION          = "1.0";
    public static final int            SECOND_MILLS             = 1000;
    public static final String         DEFAULT_PROTOCOL         = "grpc";
    public static final String         DEFAULT_GROUP            = "default";
    public static final int            MINUTE_MILLS             = 60 * SECOND_MILLS;

    public static final int            DEFAULT_INT_VALUE        = 0;
    public static final String         DEFAULT_CHARACTER        = "utf-8";

    public static final Pattern        REGISTRY_SPLIT_PATTERN   = Pattern.compile("\\s*[|;]+\\s*");
    public static final Pattern        QUERY_PARAM_PATTERN      = Pattern.compile("\\s*[&]+\\s*");
    public static final Pattern        EQUAL_SIGN_PATTERN       = Pattern.compile("\\s*[=]\\s*");
    public static final Pattern        COMMA_SPLIT_PATTERN      = Pattern.compile("\\s*[,]+\\s*");
    public static final String         EQUAL_SIGN_SEPERATOR     = "=";

    public static final String         GRPC_IN_LOCAL_PROCESS    = "LocalProcess";

    public static Metadata.Key<byte[]> GRPC_CONTEXT_ATTACHMENTS = Metadata.Key.of("grpc_header_attachments-bin",
                                                                                  Metadata.BINARY_BYTE_MARSHALLER);

    public static Metadata.Key<byte[]> GRPC_CONTEXT_VALUES      = Metadata.Key.of("grpc_header_values-bin",
                                                                                  Metadata.BINARY_BYTE_MARSHALLER);

}
