package com.nba2kassistant.core.player;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * The columns below are the projection the coaching rules engine (Milestone 4)
 * reads. {@link #rawAttributes} keeps the full nba2kapi payload verbatim so a
 * missing/renamed field there doesn't lose data, only the derived column.
 */
@Entity
@Table(name = "player_attributes")
@Getter
@Setter
@NoArgsConstructor
public class PlayerAttributes {

    @Id
    private Long playerId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "player_id")
    private Player player;

    private Short threePt;
    private Short midRange;
    private Short layup;
    private Short dunk;
    private Short speed;
    private Short strength;
    private Short postDefense;
    private Short perimeterDefense;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> rawAttributes;
}
