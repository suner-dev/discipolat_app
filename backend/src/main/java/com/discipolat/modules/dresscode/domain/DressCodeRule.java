package com.discipolat.modules.dresscode.domain;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Dress Code Rule — a rule for a specific group within a dress code.
 * Groups: "Hommes", "Femmes", "Garçons", "Filles", "Lead", "Chœurs"
 */
@Entity
@Table(name = "dress_code_rule", indexes = {
        @Index(name = "idx_dress_code_rule_dress_code", columnList = "dress_code_id")
})
public class DressCodeRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "dress_code_id", nullable = false)
    private UUID dressCodeId;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getDressCodeId() { return dressCodeId; }
    public void setDressCodeId(UUID dressCodeId) { this.dressCodeId = dressCodeId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
