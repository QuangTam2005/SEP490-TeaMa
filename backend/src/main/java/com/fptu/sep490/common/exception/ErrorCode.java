package com.fptu.sep490.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Granular domain-prefixed error codes shared between Backend and Frontend
 * so callers can pinpoint the exact error source beyond HTTP status codes.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // System & Generic Errors
    INTERNAL_SERVER_ERROR("SYS_500", HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống không mong muốn, vui lòng thử lại sau"),
    BAD_REQUEST("SYS_400", HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ"),
    RESOURCE_NOT_FOUND("SYS_404", HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên yêu cầu"),

    // Validation Errors
    VALIDATION_FAILED("VAL_001", HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ"),
    MALFORMED_JSON("VAL_002", HttpStatus.BAD_REQUEST, "Định dạng dữ liệu JSON không hợp lệ"),
    INVALID_PARAMETER_TYPE("VAL_003", HttpStatus.BAD_REQUEST, "Tham số yêu cầu không đúng kiểu dữ liệu"),

    // Authentication & Security Errors
    UNAUTHORIZED("AUTH_001", HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để tiếp tục"),
    INVALID_CREDENTIALS("AUTH_002", HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác"),
    TOKEN_EXPIRED("AUTH_003", HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn"),
    ACCESS_DENIED("AUTH_004", HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này"),
    EMAIL_ALREADY_EXISTS("AUTH_005", HttpStatus.CONFLICT, "Email đã được sử dụng"),
    INVALID_MAGIC_TOKEN("AUTH_006", HttpStatus.UNAUTHORIZED, "Magic Link không hợp lệ hoặc đã bị thu hồi"),
    INVALID_WEBHOOK_SIGNATURE("AUTH_007", HttpStatus.UNAUTHORIZED, "Chữ ký GitHub Webhook không hợp lệ"),

    // Project & Member Errors
    PROJECT_NOT_FOUND("PROJ_001", HttpStatus.NOT_FOUND, "Không tìm thấy dự án"),
    NOT_PROJECT_MEMBER("PROJ_002", HttpStatus.FORBIDDEN, "Bạn không phải thành viên của dự án này"),
    LEADER_ONLY_ACTION("PROJ_003", HttpStatus.FORBIDDEN, "Chỉ Nhóm trưởng (Leader) mới có quyền thực hiện thao tác này"),
    MEMBER_ALREADY_EXISTS("PROJ_004", HttpStatus.CONFLICT, "Thành viên đã tồn tại trong dự án"),
    USER_NOT_FOUND("PROJ_005", HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản người dùng"),
    REPOSITORY_NOT_FOUND("PROJ_006", HttpStatus.NOT_FOUND, "Không tìm thấy repository của dự án"),
    DUPLICATE_REPOSITORY("PROJ_007", HttpStatus.CONFLICT, "Repository này đã được liên kết với dự án"),

    // Milestone & Deliverable Errors
    MILESTONE_NOT_FOUND("MILE_001", HttpStatus.NOT_FOUND, "Không tìm thấy cột mốc"),
    DELIVERABLE_NOT_FOUND("MILE_002", HttpStatus.NOT_FOUND, "Không tìm thấy đầu mục nộp bài"),

    // File Upload & Storage Errors
    FILE_TOO_LARGE("FILE_413", HttpStatus.PAYLOAD_TOO_LARGE, "Dung lượng tệp vượt quá giới hạn cho phép (tối đa 50MB)"),
    INVALID_FILE_EXTENSION("FILE_001", HttpStatus.BAD_REQUEST, "Định dạng tệp không được hỗ trợ"),
    EMPTY_FILE("FILE_002", HttpStatus.BAD_REQUEST, "Tệp tải lên không được để trống"),
    INVALID_FILE_PATH("FILE_003", HttpStatus.BAD_REQUEST, "Tên hoặc đường dẫn tệp không hợp lệ"),
    FILE_NOT_FOUND("FILE_004", HttpStatus.NOT_FOUND, "Không tìm thấy tệp trên hệ thống"),
    FILE_STORAGE_ERROR("FILE_005", HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi lưu trữ hoặc đọc tệp"),

    // Planning (Report 2) Errors
    FEATURE_NOT_FOUND("PLAN_001", HttpStatus.NOT_FOUND, "Không tìm thấy tính năng (Feature)"),
    WBS_TASK_NOT_FOUND("PLAN_002", HttpStatus.NOT_FOUND, "Không tìm thấy công việc WBS"),
    DUPLICATE_CODE("PLAN_003", HttpStatus.CONFLICT, "Mã định danh đã tồn tại trong dự án"),
    RISK_NOT_FOUND("PLAN_004", HttpStatus.NOT_FOUND, "Không tìm thấy rủi ro dự án"),
    QUALITY_OBJECTIVE_NOT_FOUND("PLAN_005", HttpStatus.NOT_FOUND, "Không tìm thấy mục tiêu chất lượng"),

    // SRS (Report 3) Errors
    ACTOR_NOT_FOUND("SRS_001", HttpStatus.NOT_FOUND, "Không tìm thấy tác nhân hệ thống (Actor)"),
    USE_CASE_NOT_FOUND("SRS_002", HttpStatus.NOT_FOUND, "Không tìm thấy Use Case"),
    SCREEN_NOT_FOUND("SRS_003", HttpStatus.NOT_FOUND, "Không tìm thấy Screen / Non-screen Function"),
    SRS_ARTIFACT_NOT_FOUND("SRS_004", HttpStatus.NOT_FOUND, "Không tìm thấy tài liệu đặc tả SRS"),

    // Testing (Report 5) Errors
    TEST_CASE_NOT_FOUND("TEST_001", HttpStatus.NOT_FOUND, "Không tìm thấy Test Case"),
    MAX_EVIDENCE_EXCEEDED("TEST_002", HttpStatus.BAD_REQUEST, "Mỗi Test Case chỉ được đính kèm tối đa 2 ảnh minh chứng"),

    // Tracking Registers Errors
    TRACKING_LOG_NOT_FOUND("TRACK_001", HttpStatus.NOT_FOUND, "Không tìm thấy bản ghi theo dõi"),

    // Analytics & Peer-Review Errors
    CANNOT_REVIEW_SELF("ANALYTIC_001", HttpStatus.BAD_REQUEST, "Không thể tự đánh giá chéo chính mình"),

    // Export Errors
    EXPORT_GENERATION_FAILED("EXPORT_001", HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi tạo tệp xuất báo cáo");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
