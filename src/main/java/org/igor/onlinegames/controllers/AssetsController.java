package org.igor.onlinegames.controllers;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.igor.onlinegames.exceptions.OnlinegamesException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class AssetsController {
    public static final String ASSETS = "assets";
    @Value("${app.version}")
    private String appVersion;
    @Autowired
    ServletContext servletContext;

    @GetMapping("/" + AssetsController.ASSETS + "/{assetType:js|css|img|sound}/**")
    @ResponseBody
    public ResponseEntity<byte[]> versionedAssets(HttpServletRequest request) throws IOException {

        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        List<String> pathElems = Arrays.asList(path.split("/")).stream()
                .filter(str -> !ASSETS.equals(str) && !appVersion.equals(str) && StringUtils.isNoneBlank(str))
                .collect(Collectors.toList());
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(getMediaType(path));
        return new ResponseEntity<>(
                getBytes(StringUtils.join(pathElems, "/")),
                responseHeaders,
                HttpStatus.OK
        );
    }

    private MediaType getMediaType(String path) {
        if (path.endsWith(".js")) {
            return MediaType.valueOf("application/javascript;charset=UTF-8");
        } else if (path.endsWith(".css")) {
            return MediaType.valueOf("text/css;charset=UTF-8");
        } else if (path.endsWith(".png")) {
            return MediaType.valueOf("image/png");
        } else if (path.endsWith(".mp3")) {
            return MediaType.valueOf("audio/mpeg");
        } else {
            throw new OnlinegamesException("Cannot determine MediaType for path '" + path + "'");
        }
    }

    private byte[] getBytes(String filePath) throws IOException {
        return IOUtils.toByteArray(servletContext.getResourceAsStream(filePath));
    }
}
