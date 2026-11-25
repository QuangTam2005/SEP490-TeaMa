package com.asc.studycompanion.module.timetable.repository;

import com.asc.studycompanion.module.timetable.entity.Subject;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findAllByIsActiveTrueOrderByIdAsc();

    Optional<Subject> findByCode(SubjectCode code);
}
