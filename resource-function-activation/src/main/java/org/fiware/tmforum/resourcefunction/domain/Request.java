package org.fiware.tmforum.resourcefunction.domain;

import lombok.Data;

import java.net.URL;
import java.util.List;

@Data
public class Request {

    private String body;
    private String method;
    private URL to;
    private List<HeaderItem> header;

}
