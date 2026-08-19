package com.team1.cityfarm.repository;

import com.team1.cityfarm.entity.WebhookInbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebhookInboxRepository
        extends JpaRepository<WebhookInbox, Long> {

    boolean existsByWebhookId(String webhookId);

    Optional<WebhookInbox> findByWebhookId(String webhookId);

    List<WebhookInbox> findByOrderByCreatedAtDesc();
}