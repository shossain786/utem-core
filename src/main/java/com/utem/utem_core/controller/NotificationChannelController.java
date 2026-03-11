package com.utem.utem_core.controller;

import com.utem.utem_core.dto.NotificationChannelDTO;
import com.utem.utem_core.service.NotificationChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/utem/notifications")
@RequiredArgsConstructor
public class NotificationChannelController {

    private final NotificationChannelService service;

    @GetMapping
    public List<NotificationChannelDTO> list() {
        return service.findAll();
    }

    @PostMapping
    public ResponseEntity<NotificationChannelDTO> create(@RequestBody NotificationChannelDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    public NotificationChannelDTO update(@PathVariable Long id, @RequestBody NotificationChannelDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<Void> test(@PathVariable Long id) {
        service.sendTest(id);
        return ResponseEntity.noContent().build();
    }
}
