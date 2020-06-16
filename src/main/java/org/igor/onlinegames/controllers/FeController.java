package org.igor.onlinegames.controllers;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/fe")
@ResponseBody
public class FeController {
    @Value("${app.version}")
    private String appVersion;
    @Value("${config.name}")
    private String configName;
    @Autowired
    ServletContext servletContext;

    @GetMapping("/**")
    public ResponseEntity<byte[]> index() throws IOException {
        return new ResponseEntity<>(
                readFileToString("/index.html")
                        .replaceAll("@app\\.version@", appVersion)
                        .replaceAll("@config\\.name@", configName + " " + appVersion)
                        .getBytes(StandardCharsets.UTF_8),
                HttpStatus.OK
        );
    }

    private String readFileToString(String filePath) throws IOException {
        return IOUtils.toString(servletContext.getResourceAsStream(filePath), StandardCharsets.UTF_8);
    }
}
