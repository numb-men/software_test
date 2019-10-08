import cn.hengyumo.*;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.metadata.ReadSheet;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * SomethingTest
 *
 * @author hengyumo
 * @version 1.0
 */
@Slf4j
public class SomethingTest {

    private static SoftwareTestUtil softwareTestUtil = new SoftwareTestUtil();
    private static SoftwareTestCore softwareTestCore = SoftwareTestCore.getSoftwareTestCore();

    @Test
    public void test1 () throws ClassNotFoundException {
        log.info(softwareTestUtil.getClassPath("task3.xlsx"));
    }
    
    @Test
    public void test2 () throws Exception {
        // 写法2：
        String fileName = softwareTestUtil.getClassPath("task3.xlsx");
        ExcelReader excelReader = EasyExcel.read(
                fileName, GithubRepo.class, new GithubRepoExcelReadListener()).build();
        ReadSheet readSheet = EasyExcel.readSheet(0).build();
        excelReader.read(readSheet);
        // 这里千万别忘记关闭，读的时候会创建临时文件，到时磁盘会崩的
        excelReader.finish();
    }

    @Test
    public void test3 () throws Exception {
        GithubRepoDownloader githubRepoDownloader = new GithubRepoDownloader();
        githubRepoDownloader.download(
                "https://codeload.github.com/bengi8/0217003525/zip/master",
                "0217003525");
    }

    @Test
    public void test4 () throws Exception {
        softwareTestUtil.unzip("repo/zip/0217003525.zip", "repo/unzip/");
    }
    
    @Test
    public void test5 () throws Exception {
        softwareTestUtil.execCmd("ping www.baidu.com", new StringBuilder());
    }

    @Test
    public void test6 () throws Exception {
        String slnPath = "repo/unzip/ 021700325/0217003525-master/Sudoku/Sudoku.sln";
        String cmd = "msbuild /t:Rebuild /p:Configuration=Release /p:Platform=x64 /p:Toolset=v141 " +
                "/p:OutDir=%s \"%s\"";
        softwareTestUtil.execCmd(String.format(cmd, "test", slnPath), new StringBuilder());
    }

    @Test
    public void test7 () throws Exception {
        String cdCmd = "cd repo/unzip/ 021700325/";
        String compileCmd = "g++ -o Sudoku.exe 0217003525-master/Sudoku/Sudoku/Sudoku.cpp";

    }

    @Test
    public void test8 () throws Exception {
        List<File> files = new ArrayList<>();
        String mainFileName = "Sudoku.cpp";
        String projectType = softwareTestUtil.projectType("repo/unzip/ 021700325/", files);
        System.out.println(projectType);
        for (File file : files) {
            System.out.println(file.getPath());
            if (file.getPath().endsWith(mainFileName)) {
                System.out.println(file.getCanonicalPath());
            }
        }
    }

    @Test
    public void test9 () throws Exception {
        File file = softwareTestUtil.findFile("repo/unzip/ 021700325/", "Sudoku.cpp");
        if (file != null) {
            System.out.println(file.getCanonicalPath());
        }
    }

    @Test
    public void test10 () throws Exception {
        String mainFileName = "Sudoku";
        // String path = "repo\\unzip\\021700325";
        String path = "repo\\unzip\\031702131";
        path = new File(path).getCanonicalPath().replaceAll("\\\\", "/");
        String cdCmd = "cmd.exe /C cd " + path;
        // System.out.println(cdCmd);
        // softwareTestUtil.execCmd(cdCmd);
        // softwareTestUtil.execCmd(new String[]{ cdCmd, "cmd.exe /C dir" });
        String[] fileTypes = {".cpp", ".c", ".py", ".js", ".java"};
        for (String fileType : fileTypes) {
            String mfn = mainFileName + fileType;
            File file = softwareTestUtil.findFile(path, mfn);
            if (file != null) {
                mfn = file.getPath().replaceAll("\\\\", "/");
                String projectType = fileType.split("\\.")[1];
                String compileOutFile = path + "/" + mainFileName;
                if (projectType.equals("cpp") || projectType.equals("c")) {
                    compileOutFile += ".exe";
                    String compileCmd = String.format("g++ -o %s \"%s\"",
                            compileOutFile, mfn);
                    // System.out.println(cdCmd);
                    // System.out.println("cmd.exe /C dir");
                    System.out.println(compileCmd);
                    softwareTestUtil.execCmd(compileCmd, new StringBuilder());
                }
                else if (projectType.equals("java")) {
                    compileOutFile += ".jar";
                    String compileClassCmd = "javac -encoding utf-8 " + mfn;
                    System.out.println(compileClassCmd);
                    int result = softwareTestUtil.execCmd(compileClassCmd, new StringBuilder());
                    if (result != 0) {
                        // 防止因为编码原因编译失败
                        result = softwareTestUtil.execCmd("javac " + mfn, new StringBuilder());
                    }
                    if (result == 0) {
                        // 打包jar e指定主类
                        String mfnClass = mfn.replace("\\.java", ".class");
                        String packJarCmd = String.format("jar cvfe %s %s %s", compileOutFile, mainFileName, mfnClass);
                        System.out.println(packJarCmd);
                        result = softwareTestUtil.execCmd(packJarCmd, new StringBuilder());
                    }
                    if (result == 0) {
                        // run jar

                    }
                }
                break;
            }
        }
    }

    @Test
    public void test11 () throws Exception {
        softwareTestCore.printCompileSucceedNum();
    }

    @Test
    public void test12 () throws Exception {
        softwareTestCore.printUnzipSucceedNum();
    }

    @Test
    public void test13 () throws Exception {
        softwareTestCore.saveLog();
    }
    
    @Test
    public void test14 () throws Exception {
        List<File> files = new ArrayList<>();
        softwareTestUtil.getFilesBySuffix("test_case2", new String[] {".txt"}, files);
        for (File file : files) {
            FileReader reader = new FileReader(file);
            StringBuilder stringBuilder = new StringBuilder();
            char[] buffer = new char[1024];
            int length;
            while ((length = reader.read(buffer)) != -1) {
                stringBuilder.append(buffer, 0, length);
            }
            reader.close();
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(stringBuilder.toString().replaceAll(" {2}", " "));
            fileWriter.close();
            System.out.println("空格转化完成：" + file.getPath());
        }
    }

    @Test
    public void test15 () throws Exception {
        softwareTestCore.logToExcel();
    }

    @Test
    public void test16 () throws Exception {
        String fileName = "test_result.xlsx";
        EasyExcel.write(fileName)
                // 这里放入动态头
                .head(head()).sheet("sheet1")
                // 当然这里数据也可以用 List<List<String>> 去传入
                .doWrite(data());
    }

    private List<String> aHead(String name) {
        List<String> head = new ArrayList<>();
        head.add(name);
        return head;
    }
    
    private List<List<String>> head() {

        List<List<String>> heads = new ArrayList<>();

        heads.add(aHead("学号"));
        heads.add(aHead("姓名"));
        heads.add(aHead("状态"));
        heads.add(aHead("仓库类型"));
        heads.add(aHead("总分"));

        for (int i = 3; i <= 9; i ++) {
            for (int j = 1; j <= 5; j ++) {
                heads.add(aHead(String.format("%d宫-%d", i, j)));
            }
        }
        return heads;
    }

    private List<List<String>> data() {
        return null;
    }

    @Test
    public void test17 () throws Exception {
        boolean result = softwareTestUtil.checkFileEqual("test_case/3/input1.txt", "test_case2/3/input1.txt");
        System.out.println(result);
    }

    @Test
    public void test18 () throws Exception {
        String s = "xxxxxxxxx.java";
        System.out.println(s.replace(".java", ".class"));
    }

    @Test
    public void test19() throws Exception {

        Pattern pattern = Pattern.compile("(.*)[/\\\\].*?");
        Matcher matcher = pattern.matcher("d:/xxxx/ss/132/sds.java");
        System.out.println(matcher.groupCount());

        System.out.println(matcher.matches());
        for (int i = 0; i <= matcher.groupCount(); i ++) {
            System.out.println(matcher.group(i));
        }
    }

    @Test
    public void test20() throws Exception {
        System.out.println(softwareTestUtil.getPathDir("d:/xxxx/ss/132/sds.java"));
        System.out.println(softwareTestUtil.getPathDir("d:/xxxx/ss\\ad\\sds.java"));
    }

    @Test
    public void test21() throws Exception {
        System.out.println(new File("").getCanonicalPath());
    }

    @Test
    public void test22() throws Exception {
        softwareTestUtil.deleteDirOrFile("D:\\code\\java\\software_test\\repo\\unzip\\031702509\\output_classes");
    }

    @Test
    public void test23() throws Exception {
        System.out.println("D:\\code\\java\\software_test\\repo\\unzip\\031702133\\output_classes\\Sudoku\\Sudoku.class"
                        .replaceAll("/", ".")
                        .replaceAll("\\\\", ".")
        );
    }

    @Test
    public void test24() throws Exception {
        softwareTestCore.readStudentExcels();
    }
    
    /*

    手动对超时仓库进行重测

    String[] timeoutStudentNumbers = {
                "031702536", "031702242", "031702212", "031702539", "031702133",
                "031702140", "031702248"
        };
     */
    
    @Test
    public void test25() throws Exception {
        softwareTestCore.manualReTest("031702536");
    }

    @Test
    public void test26() throws Exception {
        softwareTestCore.manualReTest("031702242");
    }

    @Test
    public void test27() throws Exception {
        softwareTestCore.manualReTest("031702212");
    }

    @Test
    public void test28() throws Exception {
        softwareTestCore.manualReTest("031702539");
    }

    @Test
    public void test29() throws Exception {
        // [5.0, 5.0, 5.0, 5.0, 5.0, 0.0, 0.6, 0.0, 0.0, 0.0, 0.6, 0.6, 0.6, 0.6, 0.6]
        // sum=28.6, realSum=11.4
        softwareTestCore.manualReTest("031702133");
    }

    @Test
    public void test30() throws Exception {
        softwareTestCore.manualReTest("031702140");
    }

    @Test
    public void test31() throws Exception {
        softwareTestCore.manualReTest("031702248");
    }

    /*
        g++ 不兼容重测
        031702113 y y
        031702114 y
        031702325 y
        031702337 y
        031702338 y
        031702409 y
        071703323 y
        031702536 y
     */

    @Test
    public void test32() throws Exception {
        // [5.0, 5.0, 5.0, 5.0, 5.0, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6]
        // sum=45.0, realSum=18.0
        softwareTestCore.manualReTest("031702113");
    }

    @Test
    public void test33() throws Exception {
        // 测试超时
        softwareTestCore.manualReTest("031702114");
    }

    @Test
    public void test34() throws Exception {
        // vs版本不一致 > 2017
        // 编译失败
        softwareTestCore.manualReTest("031702325");
    }

    @Test
    public void test35() throws Exception {
        // vs版本不一致 < 2017
        // 错误	MSB4018	“NativeCodeAnalysis”任务意外失败。
        // Microsoft.VisualStudio.CodeAnalysis.AnalysisResults.AnalysisResultException: CA0001 : An unknown error occurred while running Code Analysis. ---> System.IO.DirectoryNotFoundException: 未能找到路径“C:\Program Files (x86)\Microsoft Visual Studio 14.0\Team Tools\Static Analysis Tools\Rule Sets\NativeRecommendedRules.ruleset”的一部分。
        //    在 System.IO.__Error.WinIOError(Int32 errorCode, String maybeFullPath)
        //    在 System.IO.FileStream.Init(String path, FileMode mode, FileAccess access, Int32 rights, Boolean useRights, FileShare share, Int32 bufferSize, FileOptions options, SECURITY_ATTRIBUTES secAttrs, String msgPath, Boolean bFromProxy, Boolean useLongPath, Boolean checkHost)
        //    在 System.IO.FileStream..ctor(String path, FileMode mode, FileAccess access, FileShare share, Int32 bufferSize)
        //    在 System.Xml.XmlDownloadManager.GetStream(Uri uri, ICredentials credentials, IWebProxy proxy, RequestCachePolicy cachePolicy)
        //    在 System.Xml.XmlUrlResolver.GetEntity(Uri absoluteUri, String role, Type ofObjectToReturn)
        //    在 System.Xml.XmlTextReaderImpl.FinishInitUriString()
        //    在 System.Xml.XmlTextReaderImpl..ctor(String uriStr, XmlReaderSettings settings, XmlParserContext context, XmlResolver uriResolver)
        //    在 System.Xml.XmlReaderSettings.CreateReader(String inputUri, XmlParserContext inputContext)
        //    在 System.Xml.XmlReader.Create(String inputUri, XmlReaderSettings settings, XmlParserContext inputContext)
        //    在 Microsoft.VisualStudio.CodeAnalysis.RuleSets.RuleSetXmlProcessor.ReadFromFile(String filePath)
        //    在 Microsoft.VisualStudio.CodeAnalysis.RuleSets.RuleSet.LoadFromFile(String filePath, IEnumerable`1 ruleProviders)
        //    在 Microsoft.Build.Tasks.NativeCodeAnalysis.LoadRuleSet(String ruleSetFile)
        //    在 Microsoft.Build.Tasks.NativeCodeAnalysis.Execute()
        //    --- 内部异常堆栈跟踪的结尾 ---
        //    在 Microsoft.Build.Tasks.NativeCodeAnalysis.Execute()
        //    在 Microsoft.Build.BackEnd.TaskExecutionHost.Microsoft.Build.BackEnd.ITaskExecutionHost.Execute()
        //    在 Microsoft.Build.BackEnd.TaskBuilder.<ExecuteInstantiatedTask>d__26.MoveNext()	Sudoku	1	C:\Program Files (x86)\Microsoft Visual Studio\2017\Community\MSBuild\Microsoft\VisualStudio\v15.0\CodeAnalysis\Microsoft.CodeAnalysis.targets	407	生成
        // 编译失败
        softwareTestCore.manualReTest("031702337");
    }

    @Test
    public void test36() throws Exception {
        // vs版本不一致 < 2017
        // [5.0, 5.0, 5.0, 5.0, 5.0, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6]
        // sum=45.0, realSum=18.0
        softwareTestCore.manualReTest("031702338");
    }

    @Test
    public void test37() throws Exception {
        // vs版本不一致 > 2017

        // 编译失败

        softwareTestCore.manualReTest("031702409");
    }

    @Test
    public void test38() throws Exception {
        // vs版本不一致 > 2017
        // [5.0, 5.0, 5.0, 5.0, 0.0, 0.6, 0.6, 0.6, 0.6, 0.0, 0.6, 0.6, 0.6, 0.6, 0.0, 0.6, 0.6, 0.6, 0.6, 0.0, 0.6, 0.6, 0.6, 0.6, 0.0, 0.6, 0.6, 0.6, 0.6, 0.0, 0.6, 0.6, 0.6, 0.6, 0.0]
        // sum=34.4, realSum=13.8
        softwareTestCore.manualReTest("071703323");
    }

    @Test
    public void test39() throws Exception {

        // D:\code\java\software_test\repo\unzip\031702536\sudoku.exe -m 3 -n 1 -i D:/code/java/software_test/test_case/3/input1.txt -o D:/code/java/software_test/repo/unzip/031702113/output/output_1_3_1.txt
        // 【重测： 031702536: cpp: D:\code\java\software_test\repo\unzip\031702536\sudoku.exe】
        // 测试超时
        softwareTestCore.manualReTest("031702536");
    }
}
