package com.fptu.sep490.module.auth.mapper;

import com.fptu.sep490.module.auth.dto.UserResponse;
import com.fptu.sep490.module.auth.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
