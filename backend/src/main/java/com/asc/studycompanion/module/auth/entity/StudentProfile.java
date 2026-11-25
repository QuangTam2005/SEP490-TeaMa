package com.asc.studycompanion.module.auth.entity;

import com.asc.studycompanion.common.entity.BaseTimestampEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Grade 6 student study profile paired 1-to-1 with a STUDENT user account.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "student_profiles")
public class StudentProfile extends BaseTimestampEntity {

    public static final int MIN_GRADE = 1;
    public static final int MAX_GRADE = 12;
    public static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(name = "grade", nullable = false)
    private Integer grade;

    @Builder.Default
    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone = DEFAULT_TIMEZONE;
}
