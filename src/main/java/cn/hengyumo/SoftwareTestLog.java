package cn.hengyumo;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * SoftwareTestLog
 *
 * @author hengyumo
 * @version 1.0
 */
@Data
public class SoftwareTestLog {

    @ExcelProperty(value = "学号", index = 0)
    private String studentNumber;

    @ExcelIgnore
    private String savePath;

    @ExcelIgnore
    private String unzipPath;

    @ExcelProperty(value = "状态码", index = 1)
    private Integer status;

    @ExcelProperty(value = "状态码描述", index = 2)
    private String statusComment;

    @ExcelProperty(value = "仓库类型", index = 3)
    private String projectType;

    @ExcelIgnore
    private String githubDownloadUrl;

    @ExcelIgnore
    private String compileOutPutFile;
}
