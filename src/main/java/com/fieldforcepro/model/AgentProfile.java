package com.fieldforcepro.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "agent_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentProfile {

    @Id
    @Column(length = 36)
    private String id; // same as user id

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    private String photoUrl;

    private String territory;

    private Instant lastSeen;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AgentStatus status;
}
