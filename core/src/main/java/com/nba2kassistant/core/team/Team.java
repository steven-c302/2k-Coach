package com.nba2kassistant.core.team;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String abbreviation;

    @Column(nullable = false)
    private String eraTag = "CURRENT";

    /** nba2kapi's teamType: curr, class, or allt. */
    @Column(nullable = false)
    private String teamType = "curr";
}
