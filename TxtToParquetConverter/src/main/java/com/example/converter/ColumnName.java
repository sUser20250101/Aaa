package com.example.converter;

public enum ColumnName {
    TRANSACTION_ID("Person ID"),
    FIRST_NAME("First Name"),
    LAST_NAME("Last Name"),
    MIDDLE_NAME("Middle Name"),
    SUFFIX("Name Suffix"),
    ADDRESS_LINE1("addressLine1"),
    ADDRESS_LINE2("addressLine2"),
    CITY("city"),
    STATE("state"),
    ZIP_CODE("zipCode"),
    DOB("DOB"),
    SSN("SSN"),
    GENDER("gender"),
    EMAIL("Email Address"),
    PHONE_NUMBER("Phone Number"),
    MM("MRN"),
    SUBSCRIBER_ID("insuranceMemberId"),
    OEIDP_ID("insuranceFamilySequence"),
    UPI("insuranceRelationshipCode"),
    RETURN_CODE("return_code"),
    MATCH_SCORE("match_score");

    private final String key;

    ColumnName(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

}
