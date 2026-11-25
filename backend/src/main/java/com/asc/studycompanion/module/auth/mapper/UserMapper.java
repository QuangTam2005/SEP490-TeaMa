package com.asc.studycompanion.module.auth.mapper;

import com.asc.studycompanion.module.auth.dto.UserResponse;
import com.asc.studycompanion.module.auth.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
