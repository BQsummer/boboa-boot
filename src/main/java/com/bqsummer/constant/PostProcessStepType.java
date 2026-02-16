package com.bqsummer.constant;

public enum PostProcessStepType {
    REMOVE_TAG_BLOCK("remove_tag_block"),
    REMOVE_FENCE_BLOCK("remove_fence_block"),
    REGEX_REPLACE("regex_replace"),
    STRIP("strip"),
    TRUNCATE("truncate"),
    JSON_EXTRACT("json_extract"),
    SAFETY_FILTER("safety_filter");

    private final String code;

    PostProcessStepType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static PostProcessStepType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (PostProcessStepType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
