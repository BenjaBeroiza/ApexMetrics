package com.apexmetrics.shared.exception;

public class CsvInvalidSchemaException extends RuntimeException {

    private final String missingColumn;

    public CsvInvalidSchemaException(String message, String missingColumn) {
        super(message);
        this.missingColumn = missingColumn;
    }

    public String getMissingColumn() {
        return missingColumn;
    }
}
