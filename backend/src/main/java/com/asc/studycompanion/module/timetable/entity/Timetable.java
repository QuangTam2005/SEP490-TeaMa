package com.asc.studycompanion.module.timetable.entity;

import com.asc.studycompanion.common.entity.BaseTimestampEntity;
import com.asc.studycompanion.module.auth.entity.StudentProfile;
import com.asc.studycompanion.module.auth.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Versioned weekly timetable. Updating creates a new ACTIVE version and archives previous ones
 * so historical study plans and submissions remain immutable (BR-20).
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "timetables")
public class Timetable extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Builder.Default
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TimetableStatus status = TimetableStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "updated_by_user_id", nullable = false)
    private User updatedBy;

    @Builder.Default
    @OneToMany(mappedBy = "timetable", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("weekday ASC, id ASC")
    private List<TimetableEntry> entries = new ArrayList<>();
}
