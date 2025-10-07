package com.doc.entity.project;

import com.doc.entity.user.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AssignmentResult {
    private User user;
    private String reason;

    public AssignmentResult(User user, String reason) {
        this.user = user;
        this.reason = reason;
    }
}