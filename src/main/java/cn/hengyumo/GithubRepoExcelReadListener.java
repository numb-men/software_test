package cn.hengyumo;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;

/**
 * GithubRepoExcelReadListener
 *
 * @author hengyumo
 * @version 1.0
 */
public class GithubRepoExcelReadListener extends AnalysisEventListener<GithubRepo> {

    private static SoftwareTestCore softwareTestCore = SoftwareTestCore.getSoftwareTestCore();
    private static SoftwareTestUtil softwareTestUtil = new SoftwareTestUtil();

    @Override
    public void invoke(GithubRepo githubRepo, AnalysisContext analysisContext) {
        githubRepo.setStudentNumber(softwareTestUtil.trim(githubRepo.getStudentNumber()));
        softwareTestCore.addGithubRepo(githubRepo);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        System.out.println("所有数据解析完成");
    }
}
