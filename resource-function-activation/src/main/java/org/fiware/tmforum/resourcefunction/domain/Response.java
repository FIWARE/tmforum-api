package org.fiware.tmforum.resourcefunction.domain;

import lombok.Data;

import java.util.List;

@Data
public class Response {

    private String body;
    private String statusCode;
    private List<HeaderItem> header;

}
