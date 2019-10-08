package cn.hengyumo;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * StudentExcelReadListener
 *
 * @author hengyumo
 * @version 1.0
 */
public class StudentExcelReadListener extends AnalysisEventListener<StudentExcel> {

    private List<StudentExcel> studentExcels = new ArrayList<>();
    private static SoftwareTestUtil softwareTestUtil = new SoftwareTestUtil();

    @Override
    public void invoke(StudentExcel studentExcel, AnalysisContext analysisContext) {
        studentExcel.setStudentNumber(softwareTestUtil.trim(studentExcel.getStudentNumber()));
        this.studentExcels.add(studentExcel);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {

    }

    public boolean checkIn(String studentNumber) {
        for (StudentExcel studentExcel : studentExcels) {
            if (studentExcel.getStudentNumber().equals(studentNumber)) {
                return true;
            }
        }
        return false;
    }
}
