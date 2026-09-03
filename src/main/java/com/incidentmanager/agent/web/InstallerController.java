package com.incidentmanager.agent.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class InstallerController {

    @GetMapping("/api/installer")
    public ResponseEntity<Resource> installer() {
        Path installerPath = findInstaller();
        if (installerPath == null) {
            return ResponseEntity.notFound().build();
        }
        Resource installer = new PathResource(installerPath);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + installerPath.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(installer);
    }

    private static Path findInstaller() {
        for (String candidate : new String[] {
                "dist/IncidentManagerAgent-0.1.0.exe",
                "dist/IncidentManagerAgent.exe",
                "dist/IncidentManagerAgent.zip"
        }) {
            Path path = Path.of(candidate);
            if (Files.exists(path)) {
                return path;
            }
        }
        return null;
    }
}
