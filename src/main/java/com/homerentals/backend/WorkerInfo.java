package com.homerentals.backend;

import java.net.SocketAddress;

public class WorkerInfo {
    private String address;
    private String port;

    public WorkerInfo(SocketAddress address, String port) {
        this.address = address.toString().split(":")[0].substring(1);
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
