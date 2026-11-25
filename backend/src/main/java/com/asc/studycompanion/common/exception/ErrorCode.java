package com.asc.studycompanion.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Granular domain-prefixed error codes shared between Backend and Frontend
 * for AI Study Companion - Grade 6.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // System & Generic Errors
    INTERNAL_SERVER_ERROR("SYS_500", HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống không mong muốn, vui lòng thử lại sau"),
    BAD_REQUEST("SYS_400", HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ"),
    RESOURCE_NOT_FOUND("SYS_404", HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên yêu cầu"),
    RATE_LIMIT_EXCEEDED("SYS_429", HttpStatus.TOO_MANY_REQUESTS, "Thao tác quá nhanh, vui lòng thử lại sau ít phút"),

    // Validation Errors
    VALIDATION_FAILED("VAL_001", HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ"),
    MALFORMED_JSON("VAL_002", HttpStatus.BAD_REQUEST, "Định dạng dữ liệu JSON không hợp lệ"),
    INVALID_PARAMETER_TYPE("VAL_003", HttpStatus.BAD_REQUEST, "Tham số yêu cầu không đúng kiểu dữ liệu"),

    // Authentication & Link Errors
    UNAUTHORIZED("AUTH_001", HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để tiếp tục"),
    INVALID_CREDENTIALS("AUTH_002", HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác"),
    TOKEN_EXPIRED("AUTH_003", HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn"),
    ACCESS_DENIED("AUTH_004", HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này"),
    EMAIL_ALREADY_EXISTS("AUTH_005", HttpStatus.CONFLICT, "Email đã được sử dụng"),
    USER_NOT_FOUND("AUTH_006", HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản người dùng"),
    INVALID_LINK_CODE("AUTH_007", HttpStatus.BAD_REQUEST, "Mã liên kết không hợp lệ hoặc đã hết hạn"),
    LINK_ALREADY_EXISTS("AUTH_008", HttpStatus.CONFLICT, "Tài khoản đã được liên kết trước đó"),

    // Schedule & Timetable Errors
    TIMETABLE_NOT_FOUND("SCH_001", HttpStatus.NOT_FOUND, "Chưa có thời khóa biểu tuần"),
    INVALID_SUBJECT_CODE("SCH_002", HttpStatus.BAD_REQUEST, "Mã môn học không thuộc danh mục lớp 6"),
    INVALID_WEEKDAY("SCH_003", HttpStatus.BAD_REQUEST, "Ngày trong tuần không hợp lệ"),

    // Study Task & Submission Errors
    STUDY_TASK_NOT_FOUND("STUDY_001", HttpStatus.NOT_FOUND, "Không tìm thấy nhiệm vụ học tập"),
    SUBMISSION_NOT_FOUND("STUDY_002", HttpStatus.NOT_FOUND, "Không tìm thấy bài nộp"),
    REVIEW_REQUIRED_BEFORE_PREVIEW("STUDY_003", HttpStatus.BAD_REQUEST, "Em cần nộp bài Ôn bài cũ (Review) trước khi mở Soạn bài mới (Preview)"),
    CONTENT_LENGTH_OUT_OF_RANGE("STUDY_004", HttpStatus.BAD_REQUEST, "Nội dung bài viết phải từ 400 đến 3.000 ký tự"),
    MAX_REVISIONS_EXCEEDED("STUDY_005", HttpStatus.BAD_REQUEST, "Em đã sử dụng hết tối đa 3 phiên bản (v1–v3) cho nhiệm vụ này"),
    SUBMISSION_EVALUATING_LOCKED("STUDY_006", HttpStatus.CONFLICT, "Bài làm đang được AI đánh giá, vui lòng đợi hoàn tất trước khi gửi bản mới"),

    // AI Evaluation & Provider Errors
    EVALUATION_JOB_NOT_FOUND("AI_001", HttpStatus.NOT_FOUND, "Không tìm thấy tác vụ đánh giá AI"),
    AI_PROVIDER_UNAVAILABLE("AI_002", HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ đánh giá AI tạm thời gián đoạn"),
    AI_RESPONSE_INVALID("AI_003", HttpStatus.BAD_GATEWAY, "Phản hồi từ mô hình AI không đúng định dạng"),
    AI_RETRY_NOT_ALLOWED("AI_004", HttpStatus.BAD_REQUEST, "Chỉ có thể thử lại khi tác vụ đánh giá đang ở trạng thái lỗi"),

    // Guardian Errors
    GUARDIAN_NOT_LINKED("GRD_001", HttpStatus.FORBIDDEN, "Tài khoản Guardian chưa được liên kết với hồ sơ học sinh này");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
