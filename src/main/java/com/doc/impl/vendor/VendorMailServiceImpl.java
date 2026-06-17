package com.doc.impl.vendor;

import com.doc.entity.vendor.Vendor;
import com.doc.service.vendor.VendorMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VendorMailServiceImpl implements VendorMailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Override
    public void sendVendorOnboardedMail(Vendor vendor) {

        if (vendor == null || !StringUtils.hasText(vendor.getEmail())) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(vendor.getEmail());
        message.setSubject("Vendor Onboarding Confirmation - Corpseed");

        message.setText(
                "Dear " + vendor.getName() + ",\n\n" +
                        "Greetings from Corpseed.\n\n" +
                        "We are pleased to inform you that your vendor profile has been successfully onboarded with Corpseed.\n\n" +
                        "Vendor Details:\n" +
                        "Vendor Name: " + vendor.getName() + "\n" +
                        "Email: " + vendor.getEmail() + "\n" +
                        "Mobile: " + vendor.getMobile() + "\n" +
                        "GST Number: " + vendor.getGstNumber() + "\n" +
                        "PAN Number: " + vendor.getPanNumber() + "\n\n" +
                        "Our procurement team may contact you for quotations and service-related requirements as per business needs.\n\n" +
                        "Regards,\n" +
                        "Corpseed Team"
        );

        javaMailSender.send(message);
    }
}