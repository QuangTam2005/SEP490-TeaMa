package com.asc.studycompanion.module.timetable.repository;

import com.asc.studycompanion.module.timetable.entity.Timetable;
import com.asc.studycompanion.module.timetable.entity.TimetableStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TimetableRepository extends JpaRepository<Timetable, Long> {

    Optional<Timetable> findFirstByStudentIdAndStatusOrderByVersionDesc(
            Long studentId,
            TimetableStatus status
    );

    Optional<Timetable> findTopByStudentIdOrderByVersionDesc(Long studentId);

    List<Timetable> findAllByStudentIdAndStatus(Long studentId, TimetableStatus status);
}
