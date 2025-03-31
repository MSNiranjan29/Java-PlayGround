package com.JavaPlayground.model;

public class CompilationResponse {
    private String output;
    private boolean success;
    private String error;

    public String getOutput() {
        return output;
    }
    public void setOutput(String output) {
        this.output = output;
    }
    public boolean isSuccess() {
        return success;
    }
    public void setSuccess(boolean success) {
        this.success = success;
    }
    public String getError() {
        return error;
    }
    public void setError(String error) {
        this.error = error;
    }
}
