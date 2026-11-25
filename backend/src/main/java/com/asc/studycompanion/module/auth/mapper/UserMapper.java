package com.asc.studycompanion.module.auth.mapper;

import com.asc.studycompanion.module.auth.dto.UserResponse;
import com.asc.studycompanion.module.auth.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "grade", source = "grade")
    @Mapping(target = "linkedStudentName", source = "linkedStudentName")
    UserResponse toResponse(User user, Integer grade, String linkedStudentName);
}
