package com.igexin.log.restapi.rest;

import com.igexin.log.restapi.LogfulProperties;
import com.igexin.log.restapi.util.ControllerUtil;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.io.IOException;

@RestController
public class WeedRestControler extends BaseRestController {

    private static final Logger LOG = LoggerFactory.getLogger(WeedRestControler.class);

    private OkHttpClient client = new OkHttpClient();

    @Autowired
    private LogfulProperties logfulProperties;

    @RequestMapping(value = "/api/weed/dir/status",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String dirStatus() {
        String url = logfulProperties.weedUrl() + "/dir/status";
        return httpGet(url);
    }

    @RequestMapping(value = "/api/weed/volume/status",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String volumeStatus(@RequestParam("node") final String node) {
        String url = node + "/status";
        return httpGet(url);
    }

    @RequestMapping(value = "/api/weed/volume/stats/disk",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String volumeDiskStatus(@RequestParam("node") final String node) {
        String url = node + "/stats/disk";
        return httpGet(url);
    }

    @RequestMapping(value = "/api/weed/file/{fid}",
            method = RequestMethod.GET,
            produces = ControllerUtil.CONTENT_TYPE,
            headers = ControllerUtil.HEADER)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<InputStreamResource> getFile(@PathVariable String fid) {
        String url = logfulProperties.weedUrl() + "/" + fid;
        Request request = new Request.Builder().url(url).build();
        try {
            Response response = client.newCall(request).execute();
            if (HttpStatus.valueOf(response.code()) == HttpStatus.OK) {
                try {
                    Headers headers = response.headers();

                    HttpHeaders newHeaders = new HttpHeaders();
                    newHeaders.set("Etag", headers.get("Etag"));
                    newHeaders.set("Content-Type", headers.get("Content-Type"));
                    newHeaders.set("Content-Disposition", headers.get("Content-Disposition"));
                    InputStreamResource stream = new InputStreamResource(response.body().byteStream());
                    return new ResponseEntity<>(stream, newHeaders, HttpStatus.OK);
                } catch (FileNotFoundException e) {
                    throw new NotFoundException();
                }
            }
        } catch (IOException e) {
            LOG.error("Exception", e);
        }
        throw new BadRequestException();
    }

    private String format(String url) {
        if (!url.contains("http://")) {
            url = "http://" + url;
        }
        return url;
    }

    private String httpGet(String url) {
        Request request = new Request.Builder().url(format(url)).build();
        try {
            Response response = client.newCall(request).execute();
            if (HttpStatus.valueOf(response.code()) == HttpStatus.OK) {
                return response.body().string();
            }
        } catch (IOException e) {
            LOG.error("Exception", e);
        }
        throw new BadRequestException();
    }
}
