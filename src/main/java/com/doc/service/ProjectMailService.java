package com.doc.service;

import com.doc.entity.project.Project;
import com.doc.entity.client.Contact;

public interface ProjectMailService {
    void sendProjectCreatedMail(Project project, Contact contact);
}