package com.blog.blogsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 举报请求 DTO
 */
@Setter
@Getter
public class ReportRequest {

    @NotBlank(message = "举报对象类型不能为空")
    @Size(max = 16, message = "举报对象类型不合法")
    private String targetType;

    @NotNull(message = "举报对象ID不能为空")
    private Integer targetId;

    @NotBlank(message = "请选择举报原因")
    @Size(max = 32, message = "举报原因长度超限")
    private String reason;

    @Size(max = 500, message = "补充说明不能超过500字")
    private String detail;

}
