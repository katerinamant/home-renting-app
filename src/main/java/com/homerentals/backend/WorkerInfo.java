package com.homerentals.backend;

public class WorkerInfo {
    private String address;
    private String port;

    public WorkerInfo(String address, String port) {
        this.address = address;
        this.port = port;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return String.format("Worker[%s:%s]", address, port);
    }
}
