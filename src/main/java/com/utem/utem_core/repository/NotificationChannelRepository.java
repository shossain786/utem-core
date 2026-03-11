package com.utem.utem_core.repository;

import com.utem.utem_core.entity.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, Long> {
    List<NotificationChannel> findAllByEnabledTrueOrderByCreatedAtAsc();
}
