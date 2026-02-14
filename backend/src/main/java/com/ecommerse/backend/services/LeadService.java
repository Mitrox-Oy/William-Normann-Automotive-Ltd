package com.ecommerse.backend.services;

import com.ecommerse.backend.dto.LeadRequest;
import com.ecommerse.backend.entities.Lead;
import com.ecommerse.backend.repositories.LeadRepository;
import com.ecommerse.backend.services.notifications.WhatsAppService;
import org.springframework.stereotype.Service;

@Service
public class LeadService {

    private final LeadRepository leadRepository;
    private final WhatsAppService whatsAppService;

    public LeadService(LeadRepository leadRepository, WhatsAppService whatsAppService) {
        this.leadRepository = leadRepository;
        this.whatsAppService = whatsAppService;
    }

    public LeadSubmissionResult submitLead(LeadRequest request) {
        Lead lead = mapRequest(request);

        if (isHoneypotTriggered(request.getWebsite())) {
            lead.setWhatsappStatus("SPAM_BLOCKED");
            lead.setWhatsappError("Honeypot field contains data");
            leadRepository.save(lead);
            return LeadSubmissionResult.blockedAsSpam();
        }

        lead.setWhatsappStatus("PENDING");
        lead = leadRepository.save(lead);

        WhatsAppService.SendResult sendResult = whatsAppService.sendLead(lead);
        if (sendResult.sent()) {
            lead.setWhatsappStatus("SENT");
            lead.setWhatsappRecipient(sendResult.recipient());
            lead.setWhatsappMessageId(sendResult.messageId());
            lead.setWhatsappError(null);
            leadRepository.save(lead);
            return LeadSubmissionResult.sent(lead.getId(), sendResult.recipient(), sendResult.messageId());
        }

        lead.setWhatsappStatus("FAILED");
        lead.setWhatsappError(trimToSize(sendResult.error(), 2000));
        leadRepository.save(lead);
        return LeadSubmissionResult.failed(lead.getId(), sendResult.error());
    }

    private Lead mapRequest(LeadRequest request) {
        Lead lead = new Lead();
        lead.setName(trimToSize(request.getName(), 120));
        lead.setEmail(trimToSize(request.getEmail(), 160));
        lead.setPhone(trimToSize(request.getPhone(), 60));
        lead.setInterest(trimToSize(request.getInterest(), 120));
        lead.setMessage(trimToSize(request.getMessage(), 4000));
        lead.setConsent(Boolean.TRUE.equals(request.getConsent()));
        lead.setSource(trimToSize(request.getSource(), 80));
        lead.setWebsite(trimToSize(request.getWebsite(), 255));
        lead.setProductName(trimToSize(request.getProduct(), 200));
        lead.setPartNumber(trimToSize(request.getPartNumber(), 120));
        lead.setRequestedQuantity(trimToSize(request.getQuantity(), 30));
        return lead;
    }

    private boolean isHoneypotTriggered(String website) {
        return website != null && !website.trim().isEmpty();
    }

    private String trimToSize(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > maxLen ? trimmed.substring(0, maxLen) : trimmed;
    }

    public record LeadSubmissionResult(
            boolean accepted,
            boolean sent,
            boolean spamBlocked,
            Long leadId,
            String recipient,
            String messageId,
            String error) {
        public static LeadSubmissionResult sent(Long leadId, String recipient, String messageId) {
            return new LeadSubmissionResult(true, true, false, leadId, recipient, messageId, null);
        }

        public static LeadSubmissionResult failed(Long leadId, String error) {
            return new LeadSubmissionResult(false, false, false, leadId, null, null, error);
        }

        public static LeadSubmissionResult blockedAsSpam() {
            return new LeadSubmissionResult(true, false, true, null, null, null, null);
        }
    }
}