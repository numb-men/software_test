package cn.hengyumo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * StudentExcel
 *
 * @author hengyumo
 * @version 1.0
 */
@Data
public class StudentExcel {

    /* 学号 */
    @ExcelProperty(index = 0)
    private String studentNumber;
}
