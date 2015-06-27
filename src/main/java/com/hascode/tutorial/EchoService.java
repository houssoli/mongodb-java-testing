package com.hascode.tutorial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EchoService.class);

    private int version = 0;
    private String name = "";

    public String echo(String message) {
        LOGGER.debug("message received => {}", message);
        return message;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
