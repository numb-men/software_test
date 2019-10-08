package cn.hengyumo;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SoftwareTestCore
 *
 * @author hengyumo
 * @version 1.0
 */
public class SoftwareTestCore {

    private static SoftwareTestCore softwareTestCore;
    private List<SoftwareTestLog> softwareTestLogs;
    private List<GithubRepo> githubRepos;
    private SoftwareTestUtil softwareTestUtil;
    private GithubRepoDownloader githubRepoDownloader;
    private StudentExcelReadListener studentExcelReadListener;

    private SoftwareTestCore() {
        softwareTestLogs = new ArrayList<>();
        githubRepos = new ArrayList<>();
        softwareTestUtil = new SoftwareTestUtil();
        githubRepoDownloader = new GithubRepoDownloader();
        studentExcelReadListener = new StudentExcelReadListener();
    }

    public static SoftwareTestCore getSoftwareTestCore() {
        if (softwareTestCore == null) {
            softwareTestCore = new SoftwareTestCore();
        }
        return softwareTestCore;
    }

    public void addGithubRepo(GithubRepo githubRepo) {
        githubRepos.add(githubRepo);
    }

    private SoftwareTestLog handleGithubRepo(GithubRepo githubRepo) {
        SoftwareTestLog softwareTestLog = new SoftwareTestLog();
        softwareTestLog.setStudentNumber(githubRepo.getStudentNumber());
        String githubDownloadUrl = githubRepo.getDownloadUrl();
        if (githubRepo.isNull()) {
            softwareTestLog.setGithubDownloadUrl(null);
            softwareTestLog.setStatus(SoftwareTestStatus.EMPTY_GITHUB_REPO.ordinal());
        }
        else if (githubDownloadUrl == null) {
            softwareTestLog.setGithubDownloadUrl(null);
            softwareTestLog.setStatus(SoftwareTestStatus.BAD_GITHUB_REPO_URL.ordinal());
        }
        else {
            softwareTestLog.setGithubDownloadUrl(githubDownloadUrl);
            softwareTestLog.setStatus(SoftwareTestStatus.WAIT_TO_DOWNLOAD.ordinal());
            downloadAndUnzip(softwareTestLog);
        }
        return softwareTestLog;
    }

    private void downloadAndUnzip(SoftwareTestLog softwareTestLog) {
        // 下载
        try {
            SoftwareTestStatus downloadStatus = githubRepoDownloader.download(
                    softwareTestLog.getGithubDownloadUrl(), softwareTestLog.getStudentNumber());
            softwareTestLog.setStatus(downloadStatus.ordinal());
            if (downloadStatus == SoftwareTestStatus.DOWNLOAD_SUCCEED) {
                // 解压
                String savePath = githubRepoDownloader.getSavePath();
                softwareTestLog.setSavePath(savePath);
                try {
                    String unzipPath = "repo/unzip/" + softwareTestLog.getStudentNumber();
                    softwareTestUtil.unzip(savePath, unzipPath);
                    softwareTestLog.setStatus(SoftwareTestStatus.UNZIP_SUCCEED.ordinal());
                    softwareTestLog.setUnzipPath(unzipPath);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    softwareTestLog.setStatus(SoftwareTestStatus.UNZIP_FAIL.ordinal());
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            softwareTestLog.setStatus(SoftwareTestStatus.UNKNOWN_ERROR.ordinal());
        }
    }

    public void saveLog() {
        updateStatusComment();
        String jsonString = JSON.toJSONString(this.softwareTestLogs);
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("log.json"));
            bufferedWriter.write(jsonString);
            bufferedWriter.close();
            System.out.println("【日志已保存到 log.json】");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void retryDownloadAndUnzip() {
        String fileName;
        githubRepos = new ArrayList<>();
        try {
            fileName = softwareTestUtil.getClassPath("task3.xlsx");
        } catch (ClassNotFoundException e) {
            System.out.println("获取学生仓库表格失败：" + e.getMessage());
            return;
        }
        ExcelReader excelReader = EasyExcel.read(
                fileName, GithubRepo.class, new GithubRepoExcelReadListener()).build();
        ReadSheet readSheet = EasyExcel.readSheet(0).build();
        excelReader.read(readSheet);
        // 这里千万别忘记关闭，读的时候会创建临时文件，到时磁盘会崩的
        excelReader.finish();
        softwareTestLogs = new ArrayList<>();
        for (GithubRepo githubRepo : githubRepos) {
            softwareTestLogs.add(handleGithubRepo(githubRepo));
        }
        printUnzipSucceedNum();
    }

    public void retryFailDownloadAndUnzip() {
        if (softwareTestLogs.size() == 0) loadLog();
        githubRepos = new ArrayList<>();
        String fileName;
        try {
            fileName = softwareTestUtil.getClassPath("task3.xlsx");
        } catch (ClassNotFoundException e) {
            System.out.println("获取学生仓库表格失败：" + e.getMessage());
            return;
        }
        ExcelReader excelReader = EasyExcel.read(
                fileName, GithubRepo.class, new GithubRepoExcelReadListener()).build();
        ReadSheet readSheet = EasyExcel.readSheet(0).build();
        excelReader.read(readSheet);
        // 这里千万别忘记关闭，读的时候会创建临时文件，到时磁盘会崩的
        excelReader.finish();
        for (int i = 0; i < softwareTestLogs.size(); i ++) {
            SoftwareTestLog softwareTestLog = softwareTestLogs.get(i);
            if (softwareTestLog.getStatus() <= SoftwareTestStatus.TIMEOUT_FOR_CONNECT.ordinal()
                && softwareTestLog.getStatus() != SoftwareTestStatus.UNZIP_SUCCEED.ordinal()) {
                // 只对未解压成功的进行重新下载
                for (GithubRepo githubRepo : githubRepos) {
                    if (githubRepo.getStudentNumber().equals(softwareTestLog.getStudentNumber())) {
                        SoftwareTestLog newSoftwareTestLog = handleGithubRepo(githubRepo);
                        softwareTestLogs.set(i, newSoftwareTestLog);
                    }
                }
            }
        }
        printUnzipSucceedNum();
    }

    public void retryCompile() {
        if (softwareTestLogs.size() == 0) loadLog();
        for (SoftwareTestLog softwareTestLog : softwareTestLogs) {
            if (softwareTestLog.getStatus() == SoftwareTestStatus.UNZIP_SUCCEED.ordinal()
                || softwareTestLog.getStatus() > SoftwareTestStatus.TIMEOUT_FOR_CONNECT.ordinal()) {

                try {
                    compileOne(softwareTestLog);
                }
                catch (IOException e) {
                    e.printStackTrace();
                    softwareTestLog.setStatus(SoftwareTestStatus.COMPILE_DIR_OR_FILE_NOT_FOUND.ordinal());
                }
            }
        }
        printCompileSucceedNum();
    }

    public void printUnzipSucceedNum() {
        if (softwareTestLogs.size() == 0) loadLog();
        int unzipSucceedNum = 0;
        for (SoftwareTestLog softwareTestLog : softwareTestLogs) {
            if (softwareTestLog.getStatus() == SoftwareTestStatus.UNZIP_SUCCEED.ordinal()
                    || softwareTestLog.getStatus() > SoftwareTestStatus.TIMEOUT_FOR_CONNECT.ordinal()) {

                unzipSucceedNum ++;
            }
        }
        System.out.println("总计下载并解压成功数量：" + unzipSucceedNum);
    }

    public void printCompileSucceedNum() {
        if (softwareTestLogs.size() == 0) loadLog();
        int compileSucceedNum = 0;
        for (SoftwareTestLog softwareTestLog : softwareTestLogs) {
            if (softwareTestLog.getStatus() == SoftwareTestStatus.COMPILE_JAVA_SUCCEED.ordinal()
                || softwareTestLog.getStatus() == SoftwareTestStatus.COMPILE_CCPP_SUCCEED.ordinal()) {

                compileSucceedNum ++;
            }
        }
        System.out.println("总计编译成功数量：" + compileSucceedNum);
    }

    public void updateStatusComment() {
        if (softwareTestLogs.size() == 0) loadLog();
        for (SoftwareTestLog softwareTestLog : softwareTestLogs) {
            // 设置超时
            if (isTimeoutStudent(softwareTestLog.getStudentNumber())) {
                softwareTestLog.setStatus(SoftwareTestStatus.TEST_TIMEOUT.ordinal());
            }
            int status = softwareTestLog.getStatus();
            for (SoftwareTestStatus softwareTestStatus : SoftwareTestStatus.values()) {
                if (softwareTestStatus.ordinal() == status) {
                    softwareTestLog.setStatusComment(softwareTestStatus.getComment());
                }
            }
        }
    }

    public void compileOne(SoftwareTestLog softwareTestLog) throws IOException {
        String[] mainFileNames = {"Sudoku", "sudoku", "Suduku", "suduku"};
        String mainFileName = "";
        String path = "repo/unzip/" + softwareTestLog.getStudentNumber();
        path = new File(path).getCanonicalPath().replaceAll("\\\\", "/");
        String[] fileTypes = {".cpp", ".c", ".java"};
        StringBuilder log = new StringBuilder();
        // 默认初始状态为未找到
        String projectType = "";
        softwareTestLog.setStatus(SoftwareTestStatus.MAIN_PROGRAM_FILE_NOT_FOUND.ordinal());
        for (String fileType : fileTypes) {
            String mfn;
            File file = null;
            for (String amfn : mainFileNames) {
                mfn = amfn + fileType;
                file = softwareTestUtil.findFile(path, mfn);
                if (file != null) {
                    mainFileName = amfn;
                    break;
                }
            }
            if (file != null) {
                softwareTestLog.setStatus(SoftwareTestStatus.WAIT_TO_COMPILE.ordinal());
                mfn = file.getPath();
                projectType = fileType.split("\\.")[1];
                String compileOutFile = path + "/" + mainFileName;
                softwareTestLog.setProjectType(projectType);
                String startLog = String.format("【开始编译%s %s】",
                        softwareTestLog.getStudentNumber(), projectType);
                System.out.println(startLog);
                log.append(startLog);

                if (projectType.equals("cpp") || projectType.equals("c")) {
                    try {
                        compileOutFile += ".exe";
                        File compileLastTimeFile = new File(compileOutFile);
                        if (compileLastTimeFile.exists() && ! compileLastTimeFile.delete()) {
                            System.out.println("删除文件" + compileOutFile + "失败");
                            softwareTestLog.setStatus(SoftwareTestStatus.COMPILE_CCPP_FAIL.ordinal());
                            return;
                        }
                        String mainDir = softwareTestUtil.getPathDir(mfn);
                        List<File> cpps = new ArrayList<>();
                        softwareTestUtil.getFilesBySuffix(mainDir, new String[] {".cpp", ".c", ".h"}, cpps);
                        List<String> cppPaths = cpps.stream().map(f -> {
                            try {
                                return f.getCanonicalPath();
                            } catch (IOException e) {
                                e.printStackTrace();
                                return null;
                            }
                        }).collect(Collectors.toList());
                        // 统一收集同一个目录下的所有cpp/c文件，进行编译
                        String compileCmd = String.format("g++ -o %s %s",
                                compileOutFile, String.join(" ",cppPaths));
                        System.out.println(compileCmd);
                        softwareTestUtil.execCmd(compileCmd, log);
                        File compileOutputFile = new File(compileOutFile);
                        if (compileOutputFile.exists()) {
                            softwareTestLog.setStatus(SoftwareTestStatus.COMPILE_CCPP_SUCCEED.ordinal());
                            softwareTestLog.setCompileOutPutFile(compileOutputFile.getCanonicalPath());
                        }
                        else {
                            softwareTestLog.setStatus(SoftwareTestStatus.COMPILE_CCPP_FAIL.ordinal());
                        }
                    }
                    catch (Exception e) {
                        System.out.println(e.getMessage());
                        log.append(e.getMessage()).append("\n");;
                        softwareTestLog.setStatus(SoftwareTestStatus.COMPILE_CCPP_FAIL.ordinal());
                    }
                }
                else if (projectType.equals("java")) {
                    try {
                        // compileOutFile += ".jar";
                        String mainDir = softwareTestUtil.getPathDir(mfn);
                        String outputClassesDir = path + "/output_classes";

                        softwareTestUtil.deleteDirOrFile(outputClassesDir);
                        File classesDir = new File(outputClassesDir);
                        if (! classesDir.exists() && ! classesDir.mkdir()) {
                            System.out.println("创建文件夹" + outputClassesDir + "失败");
                            softwareTestLog.setStatus(SoftwareTestStatus.COMPILE_JAVA_FAIL.ordinal());
                            return;
                        }
                        String compileClassCmd = String.format(
                                "javac -d %s -encoding utf-8 -nowarn %s/*.java",
                                outputClassesDir, mainDir
                        );
                        System.out.println(compileClassCmd);
                        int result = softwareTestUtil.execCmd(compileClassCmd, log);
                        if (result == 0) {
                            String mfnClass = softwareTestUtil.findFile(
                                    outputClassesDir, mainFileName + ".class"
                            ).getCanonicalPath();

                            softwareTestLog.setStatus(SoftwareTestStatus.COMPILE_JAVA_SUCCEED.ordinal());

                            // 考虑到无法准确的指定 jar 入口包名主类，只能使用java main class的方式来运行
                            softwareTestLog.setCompileOutPutFile(mfnClass);

                            // // 打包jar e指定主类
                            // String packJarCmd = String.format(
                            //         "jar -cvfe %s %s %s/",
                            //         compileOutFile, mainFileName, outputClassesDir
                            // );
                            // System.out.println(packJarCmd);
                            // result = softwareTestUtil.execCmd(packJarCmd, log);
                            // if (result == 0) {
                            //     // succeed
                            //     softwareTestLog.setCompileOutPutFile(compileOutFile);
                            //     softwareTestLog.setStatus(SoftwareTestStatus.PACK_JAR_SUCCEED.ordinal());
                            // }
                            // else {
                            //     softwareTestLog.setStatus(SoftwareTestStatus.PACK_JAR_FAIL.ordinal());
                            // }
                        }
                        else {
                            softwareTestLog.setStatus(SoftwareTestStatus.COMPILE_JAVA_FAIL.ordinal());
                        }
                    }
                    catch (Exception e) {
                        System.out.println(e.getMessage());
                        log.append(e.getMessage()).append("\n");;
                        softwareTestLog.setStatus(SoftwareTestStatus.COMPILE_JAVA_FAIL.ordinal());
                    }
                }
                else {
                    softwareTestLog.setStatus(SoftwareTestStatus.UN_SUPPORT_COMPILE_TYPE.ordinal());
                }
                break;
            }
        }
        String endLog = "";
        if (softwareTestLog.getStatus() == SoftwareTestStatus.COMPILE_JAVA_SUCCEED.ordinal()
            || softwareTestLog.getStatus() == SoftwareTestStatus.COMPILE_CCPP_SUCCEED.ordinal()) {

            endLog = String.format("【编译成功%s %s】\n\n",
                    softwareTestLog.getStudentNumber(), projectType);
            System.out.println(endLog);
            log.append(endLog);
        }
        else {
            endLog = String.format("【编译失败%s %s】\n\n",
                    softwareTestLog.getStudentNumber(), projectType);
            System.out.println(endLog);
            log.append(endLog);
        }

        FileWriter writer = new FileWriter("log/compile.log.txt", true);
        writer.write(log.toString());
        writer.close();
    }

    public void logToExcel() {
        if (softwareTestLogs.size() == 0) loadLog();
        String fileName = "software_test_log.xlsx";
        // 这里 需要指定写用哪个class去读
        ExcelWriter excelWriter = EasyExcel.write(fileName, SoftwareTestLog.class).build();
        WriteSheet writeSheet = EasyExcel.writerSheet("sheet1").build();
        excelWriter.write(softwareTestLogs, writeSheet);
        /// 千万别忘记finish 会帮忙关闭流
        excelWriter.finish();
        System.out.println("日志已生成：" + fileName);
    }

    private void loadLog() {
        String jsonString = null;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("log.json"));
            jsonString = bufferedReader.readLine();
            bufferedReader.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        this.softwareTestLogs = JSON.parseObject(
                jsonString, new TypeReference<List<SoftwareTestLog>>(){});
    }

    public int size() {
        return this.softwareTestLogs.size();
    }

    private boolean isTimeoutStudent(String studentNumber) {
        String[] timeoutStudentNumbers = {
                "031702536", "031702242", "031702212", "031702539", "031702133",
                "031702140", "031702248"
        };
        for (String timeoutStudentNumber : timeoutStudentNumbers) {
            if (timeoutStudentNumber.equals(studentNumber))
                return true;
        }
        return false;
    }

    public void retryTest() {
        if (softwareTestLogs.size() == 0) loadLog();
        String fileName = "test_result.xlsx";
        List<List<String>> lines = new ArrayList<>();
        readStudentExcels();
        for (SoftwareTestLog softwareTestLog : softwareTestLogs) {
            if (! studentExcelReadListener.checkIn(softwareTestLog.getStudentNumber())){
                // 在学生表中的才测试添加分数
                continue;
            }
            if ((softwareTestLog.getStatus() == SoftwareTestStatus.COMPILE_CCPP_SUCCEED.ordinal()
                || softwareTestLog.getStatus() == SoftwareTestStatus.COMPILE_JAVA_SUCCEED.ordinal()
                || softwareTestLog.getStatus() > SoftwareTestStatus.TEST_TIMEOUT.ordinal())
                && ! isTimeoutStudent(softwareTestLog.getStudentNumber())) {

                // 测试
                List<String> line = null;
                try {
                    line = testOne(softwareTestLog);
                    System.out.println();
                    System.out.println(line);
                    if (line != null) {
                        lines.add(line);
                        System.out.println("【测试完毕】\n");
                    }
                    else {
                        softwareTestLog.setStatus(SoftwareTestStatus.TEST_FAIL.ordinal());
                        System.out.println("【测试失败】\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    softwareTestLog.setStatus(SoftwareTestStatus.TEST_FAIL.ordinal());
                    System.out.println("【测试失败】\n");
                }
            }
            else {
                System.out.println("\n【无法测试：" + softwareTestLog.getStudentNumber() + "】");
                List<String> line = new ArrayList<>();
                line.add(softwareTestLog.getStudentNumber());
                line.add(SoftwareTestStatus.getComment(softwareTestLog.getStatus()));
                line.add(softwareTestLog.getProjectType() == null ?
                        "" : softwareTestLog.getProjectType());
                line.add("0.0");
                if (softwareTestLog.getStatus() == SoftwareTestStatus.EMPTY_GITHUB_REPO.ordinal()) {
                    // 未提交仓库设置负分
                    line.add("-45.0");
                    line.add("-18.0");
                }
                else {
                    line.add("0.0");
                    line.add("0.0");
                }
                System.out.println(line);
                System.out.println();
                lines.add(line);
            }
        }
        EasyExcel.write(fileName)
                .head(head()).sheet("sheet1")
                .doWrite(lines);
        System.out.println("【全部测试完成，测试结果已生成】 " + fileName);
    }

    public void manualReTest(String studentNumber) throws Exception {
        if (softwareTestLogs.size() == 0) loadLog();
        SoftwareTestLog studentLog = null;
        for (SoftwareTestLog softwareTestLog : softwareTestLogs) {
            if (softwareTestLog.getStudentNumber().equals(studentNumber)) {
                studentLog = softwareTestLog;
            }
        }
        if (studentLog == null) {
            throw new Exception("学号不存在");
        }

        // 重新编译
        // compileOne(studentLog);

        StringBuilder log = new StringBuilder();
        String startLog = String.format("\n\n【超时重测： %s: %s: %s】", studentLog.getStudentNumber(),
                studentLog.getProjectType(), studentLog.getCompileOutPutFile());
        System.out.println(startLog);
        log.append(startLog);

        String outputDirPath = "repo/unzip/" + studentLog.getStudentNumber() + "/output/";
        try {
            softwareTestUtil.deleteDirOrFile(outputDirPath);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            log.append(e.getMessage());
            e.printStackTrace();
        }

        File outPutDir = new File(outputDirPath);
        if (! outPutDir.mkdir()) {
            System.out.println("创建文件夹" + outputDirPath + "失败");
        }
        outputDirPath = outPutDir.getCanonicalPath();
        String projectPath = new File("").getCanonicalPath();
        List<Double> scores = new ArrayList<>();
        for (int i = 3; i <= 9; i ++) {
            for (int j = 1; j <= 5; j ++) {
                System.out.print(String.format("test-%d-%d ", i, j));

                String testInputPath1 = softwareTestUtil.formatPath(
                        projectPath + String.format("/test_case/%d/input%d.txt", i, j));
                String testInputPath2 = softwareTestUtil.formatPath(
                        projectPath + String.format("/test_case2/%d/input%d.txt", i, j));
                String testResultPath1 = softwareTestUtil.formatPath(
                        projectPath + String.format("/test_case/%d/output%d.txt", i, j));
                String testResultPath2 = softwareTestUtil.formatPath(
                        projectPath + String.format("/test_case/%d/output%d.txt", i, j));
                String testOutputPath1 = softwareTestUtil.formatPath(
                        outputDirPath + String.format("/output_1_%d_%d.txt", i, j));
                String testOutputPath2 = softwareTestUtil.formatPath(
                        outputDirPath + String.format("/output_2_%d_%d.txt", i, j));
                // 第4个用例5个表盘，第5个10个表盘，其余只有1个
                int n = j == 4 ? 5 : j == 5 ? 10 : 1;
                if (studentLog.getProjectType().equals("cpp")
                        || studentLog.getProjectType().equals("c")) {

                    String testCmd1 = String.format("%s -m %d -n %d -i %s -o %s",
                            studentLog.getCompileOutPutFile(), i, n, testInputPath1, testOutputPath1);
                    String testCmd2 = String.format("%s -m %d -n %d -i %s -o %s",
                            studentLog.getCompileOutPutFile(), i, n, testInputPath2, testOutputPath2);

                    boolean result = false;
                    try {
                        softwareTestUtil.execCmd(testCmd1, log);
                        result = softwareTestUtil.checkFileEqual(testResultPath1, testOutputPath1);
                    } catch (Exception e) {
                        log.append(e.getMessage()).append("\n");
                    }
                    if (! result) {
                        try {
                            softwareTestUtil.execCmd(testCmd2, log);
                            result = softwareTestUtil.checkFileEqual(testResultPath2, testOutputPath2);
                        } catch (Exception e) {
                            log.append(e.getMessage()).append("\n");
                        }
                    }
                    // System.out.println(log.toString());
                    scores.add(getScore(i, result));
                    System.out.println(scores);
                    Double scoreSum = scores.stream().reduce(0D, Double::sum);
                    System.out.println(String.format("sum=%.1f, realSum=%.1f", scoreSum, scoreSum*0.4D));
                }
                else if (studentLog.getProjectType().equals("java")) {

                    // String testCmd1 = String.format("java -jar %s -m %d -n %d -i %s -o %s",
                    //         studentLog.getCompileOutPutFile(), i, n, testInputPath1, testOutputPath1);
                    // String testCmd2 = String.format("java -jar %s -m %d -n %d -i %s -o %s",
                    //         studentLog.getCompileOutPutFile(), i, n, testInputPath2, testOutputPath2);

                    // 运行主类而不是运行jar
                    String classesDir = new File(String.format(
                            "repo/unzip/%s/output_classes", studentLog.getStudentNumber()))
                            .getCanonicalPath();

                    String mainFile = studentLog.getCompileOutPutFile()
                            .substring(classesDir.length() + 1).split(".class")[0];
                    mainFile = mainFile.replaceAll("/", ".")
                            .replaceAll("\\\\", ".");
                    // System.out.println(mainFile);

                    // 注意 -classpath要在主类名之前
                    String testCmd1 = String.format("java -classpath %s %s -m %d -n %d -i %s -o %s",
                            classesDir, mainFile, i, n, testInputPath1, testOutputPath1);
                    String testCmd2 = String.format("java -classpath %s %s -m %d -n %d -i %s -o %s",
                            classesDir, mainFile, i, n, testInputPath2, testOutputPath2);

                    boolean result = false;
                    try {
                        softwareTestUtil.execCmd(testCmd1, log);
                        result = softwareTestUtil.checkFileEqual(testResultPath1, testOutputPath1);
                    } catch (Exception e) {
                        log.append(e.getMessage()).append("\n");
                    }
                    if (! result) {
                        try {
                            softwareTestUtil.execCmd(testCmd2, log);
                            result = softwareTestUtil.checkFileEqual(testResultPath2, testOutputPath2);
                        } catch (Exception e) {
                            log.append(e.getMessage()).append("\n");
                        }
                    }
                    // System.out.println(log.toString());
                    scores.add(getScore(i, result));
                    System.out.println(scores);
                    Double scoreSum = scores.stream().reduce(0D, Double::sum);
                    System.out.println(String.format("sum=%.1f, realSum=%.1f", scoreSum, scoreSum*0.4D));
                }
            }
        }
        System.out.println(scores);
        boolean allPass = scores.stream().allMatch(score -> score != 0);
        Double allPassScore = allPass ? 2 : 0D;
        Double sumScore = scores.stream().reduce(0D, Double::sum);
        sumScore += allPassScore;
        System.out.println(String.format("sum=%.1f, realSum=%.1f", sumScore, sumScore * 0.4D));

        FileWriter writer = new FileWriter("log/test.log.txt", true);
        writer.write(log.toString());
        writer.close();
    }

    public List<String> testOne(SoftwareTestLog softwareTestLog) throws IOException {

        softwareTestLog.setStatus(SoftwareTestStatus.WAIT_TO_TEST.ordinal());
        StringBuilder log = new StringBuilder();
        String startLog = String.format("\n\n【开始测试 %s: %s: %s】", softwareTestLog.getStudentNumber(),
                softwareTestLog.getProjectType(), softwareTestLog.getCompileOutPutFile());
        System.out.println(startLog);
        log.append(startLog);

        String outputDirPath = "repo/unzip/" + softwareTestLog.getStudentNumber() + "/output/";
        try {
            softwareTestUtil.deleteDirOrFile(outputDirPath);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            log.append(e.getMessage());
            e.printStackTrace();
            softwareTestLog.setStatus(SoftwareTestStatus.TEST_FAIL.ordinal());
            return null;
        }

        File outPutDir = new File(outputDirPath);
        if (! outPutDir.mkdir()) {
            System.out.println("创建文件夹" + outputDirPath + "失败");
            softwareTestLog.setStatus(SoftwareTestStatus.TEST_FAIL.ordinal());
            return null;
        }
        outputDirPath = outPutDir.getCanonicalPath();
        String projectPath = new File("").getCanonicalPath();
        List<Double> scores = new ArrayList<>();
        for (int i = 3; i <= 9; i ++) {
            for (int j = 1; j <= 5; j ++) {
                System.out.print(String.format("test-%d-%d ", i, j));

                String testInputPath1 = softwareTestUtil.formatPath(
                        projectPath + String.format("/test_case/%d/input%d.txt", i, j));
                String testInputPath2 = softwareTestUtil.formatPath(
                        projectPath + String.format("/test_case2/%d/input%d.txt", i, j));
                String testResultPath1 = softwareTestUtil.formatPath(
                        projectPath + String.format("/test_case/%d/output%d.txt", i, j));
                String testResultPath2 = softwareTestUtil.formatPath(
                        projectPath + String.format("/test_case/%d/output%d.txt", i, j));
                String testOutputPath1 = softwareTestUtil.formatPath(
                        outputDirPath + String.format("/output_1_%d_%d.txt", i, j));
                String testOutputPath2 = softwareTestUtil.formatPath(
                        outputDirPath + String.format("/output_2_%d_%d.txt", i, j));
                // 第4个用例5个表盘，第5个10个表盘，其余只有1个
                int n = j == 4 ? 5 : j == 5 ? 10 : 1;
                if (softwareTestLog.getProjectType().equals("cpp")
                    || softwareTestLog.getProjectType().equals("c")) {

                    String testCmd1 = String.format("%s -m %d -n %d -i %s -o %s",
                            softwareTestLog.getCompileOutPutFile(), i, n, testInputPath1, testOutputPath1);
                    String testCmd2 = String.format("%s -m %d -n %d -i %s -o %s",
                            softwareTestLog.getCompileOutPutFile(), i, n, testInputPath2, testOutputPath2);

                    boolean result = false;
                    try {
                        softwareTestUtil.execCmd(testCmd1, log);
                        result = softwareTestUtil.checkFileEqual(testResultPath1, testOutputPath1);
                    } catch (Exception e) {
                        log.append(e.getMessage()).append("\n");
                    }
                    if (! result) {
                        try {
                            softwareTestUtil.execCmd(testCmd2, log);
                            result = softwareTestUtil.checkFileEqual(testResultPath2, testOutputPath2);
                        } catch (Exception e) {
                            log.append(e.getMessage()).append("\n");
                        }
                    }
                    scores.add(getScore(i, result));
                }
                else if (softwareTestLog.getProjectType().equals("java")) {

                    // String testCmd1 = String.format("java -jar %s -m %d -n %d -i %s -o %s",
                    //         softwareTestLog.getCompileOutPutFile(), i, n, testInputPath1, testOutputPath1);
                    // String testCmd2 = String.format("java -jar %s -m %d -n %d -i %s -o %s",
                    //         softwareTestLog.getCompileOutPutFile(), i, n, testInputPath2, testOutputPath2);

                    // 运行主类而不是运行jar
                    String classesDir = new File(String.format(
                            "repo/unzip/%s/output_classes", softwareTestLog.getStudentNumber()))
                            .getCanonicalPath();

                    String mainFile = softwareTestLog.getCompileOutPutFile()
                            .substring(classesDir.length() + 1).split(".class")[0];
                    mainFile = mainFile.replaceAll("/", ".")
                            .replaceAll("\\\\", ".");
                    // System.out.println(mainFile);

                    // 注意 -classpath要在主类名之前
                    String testCmd1 = String.format("java -classpath %s %s -m %d -n %d -i %s -o %s",
                            classesDir, mainFile, i, n, testInputPath1, testOutputPath1);
                    String testCmd2 = String.format("java -classpath %s %s -m %d -n %d -i %s -o %s",
                            classesDir, mainFile, i, n, testInputPath2, testOutputPath2);

                    boolean result = false;
                    try {
                        softwareTestUtil.execCmd(testCmd1, log);
                        result = softwareTestUtil.checkFileEqual(testResultPath1, testOutputPath1);
                    } catch (Exception e) {
                        log.append(e.getMessage()).append("\n");
                    }
                    if (! result) {
                        try {
                            softwareTestUtil.execCmd(testCmd2, log);
                            result = softwareTestUtil.checkFileEqual(testResultPath2, testOutputPath2);
                        } catch (Exception e) {
                            log.append(e.getMessage()).append("\n");
                        }
                    }
                    scores.add(getScore(i, result));
                }
            }
        }

        FileWriter writer = new FileWriter("log/test.log.txt", true);
        writer.write(log.toString());
        writer.close();
        if (scores.stream().reduce(0D, Double::sum) == 0D) {
            softwareTestLog.setStatus(SoftwareTestStatus.TEST_FAIL.ordinal());
        }
        else {
            softwareTestLog.setStatus(SoftwareTestStatus.TEST_SUCCEED.ordinal());
        }

        return aLine(scores, softwareTestLog);
    }

    private double getScore(int m, boolean right) {
        // 3宫总分 5 * 5 = 25
        // 其余宫 5 * 0.6 = 3
        if (! right) return 0;
        if (m == 3) return 5;
        return 0.6;
    }

    private List<String> aLine(List<Double> scores, SoftwareTestLog softwareTestLog) {
        List<String> line = new ArrayList<>();
        boolean allPass = scores.stream().allMatch(score -> score != 0);
        Double allPassScore = allPass ? 2 : 0D;
        Double sumScore = scores.stream().reduce(0D, Double::sum);
        sumScore += allPassScore;
        line.add(softwareTestLog.getStudentNumber());
        line.add(SoftwareTestStatus.getComment(softwareTestLog.getStatus()));
        line.add(softwareTestLog.getProjectType());
        line.add(String.valueOf(allPassScore));
        line.add(String.format("%.1f", sumScore));
        line.add(String.format("%.1f", sumScore * 0.4D));
        for (Double score : scores) {
            line.add(String.valueOf(score));
        }
        return line;
    }

    private List<String> aHead(String name) {
        List<String> head = new ArrayList<>();
        head.add(name);
        return head;
    }

    private List<List<String>> head() {

        List<List<String>> heads = new ArrayList<>();

        heads.add(aHead("学号"));
        heads.add(aHead("状态"));
        heads.add(aHead("仓库类型"));
        heads.add(aHead("宫格全部通过加分"));
        heads.add(aHead("合计"));
        heads.add(aHead("换算总分"));

        for (int i = 3; i <= 9; i ++) {
            for (int j = 1; j <= 5; j ++) {
                heads.add(aHead(String.format("%d宫-%d", i, j)));
            }
        }
        return heads;
    }

    public void readStudentExcels() {
        String fileName;
        try {
            fileName = softwareTestUtil.getClassPath("students.xlsx");
        } catch (ClassNotFoundException e) {
            System.out.println("获取学生信息表格失败：" + e.getMessage());
            return;
        }
        ExcelReader excelReader = EasyExcel.read(
                fileName, StudentExcel.class, studentExcelReadListener).build();
        ReadSheet readSheet = EasyExcel.readSheet(0).build();
        excelReader.read(readSheet);
        // 这里千万别忘记关闭，读的时候会创建临时文件，到时磁盘会崩的
        excelReader.finish();
    }
}
