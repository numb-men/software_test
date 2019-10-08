package cn.hengyumo;

import org.apache.http.util.TextUtils;

import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * SoftwareTestUtil
 *
 * @author hengyumo
 * @version 1.0
 */
public class SoftwareTestUtil {

    private static final int BUFFER_SIZE = 512;

    /**普通的英文半角空格Unicode编码*/
    private static final int SPACE_32 = 32;

    /**中文全角空格Unicode编码(一个中文宽度)*/
    private static final int SPACE_12288 = 12288;

    /**普通的英文半角空格但不换行Unicode编码(== &nbsp; == &#xA0; == no-break space)*/
    private static final int SPACE_160 = 160;

    /**半个中文宽度(== &ensp; == en空格)*/
    private static final int SPACE_8194 = 8194;

    /**一个中文宽度(== &emsp; == em空格)*/
    private static final int SPACE_8195 = 8195;

    /**四分之一中文宽度(四分之一em空格)*/
    private static final int SPACE_8197 = 8197;

    /**窄空格*/
    private static final int SPACE_8201 = 8201;


    /**
     * 获取classPath
     *
     * @throws ClassNotFoundException e
     */
    public String getClassPath(String relativePath) throws ClassNotFoundException {
        ClassLoader classLoader = this.getClass().getClassLoader();
        URL classUrl = classLoader.getResource(relativePath);
        if (classUrl == null) {
            throw new ClassNotFoundException();
        }
        String classPath = classUrl.getPath();
        if (classPath.startsWith("/")) {
            classPath = classPath.substring(1);
        }
        return classPath;
    }

    /**
     * zip解压
     *
     * @param srcPath    zip源文件
     * @param destDirPath 解压后的目标文件夹
     * @throws Exception 解压失败会抛出运行时异常
     */
    public void unzip(String srcPath, String destDirPath) throws Exception {

        File srcFile = new File(srcPath);
        long start = System.currentTimeMillis();

        // 判断源文件是否存在
        if (!srcFile.exists()) {
            throw new Exception(srcFile.getPath() + "所指文件不存在");
        }
        File destDir = new File(destDirPath);
        if (! destDir.exists()) {
            if (! destDir.mkdirs()) {
                throw new Exception("创建文件夹" + destDir.getPath() + "失败");
            }
        }
        // 开始解压
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(srcFile);
            Enumeration<?> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                // System.out.println("解压" + entry.getName());
                // 如果是文件夹，就创建个文件夹
                if (entry.isDirectory()) {
                    String dirPath = destDirPath + "/" + entry.getName();
                    File dir = new File(dirPath);
                    if (! dir.mkdirs()) {
                        throw new Exception("创建文件夹" + dir.getPath() + "失败");
                    }
                } else {
                    // 如果是文件，就先创建一个文件，然后用io流把内容copy过去
                    File targetFile = new File(destDirPath + "/" + entry.getName());
                    // 保证这个文件的父文件夹必须要存在
                    if (! targetFile.getParentFile().exists()) {
                        if (! targetFile.getParentFile().mkdirs()) {
                            throw new Exception("创建文件夹" + targetFile.getParentFile().getPath() + "失败");
                        }
                    }
                    if (! targetFile.createNewFile()) {
                        throw new Exception("创建文件" + targetFile.getPath() + "失败");
                    }
                    // 将压缩文件内容写入到这个文件中
                    InputStream is = zipFile.getInputStream(entry);
                    FileOutputStream fos = new FileOutputStream(targetFile);
                    int len;
                    byte[] buf = new byte[BUFFER_SIZE];
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                    // 关流顺序，先打开的后关闭
                    fos.close();
                    is.close();
                }
            }
            long end = System.currentTimeMillis();
            System.out.println(srcPath + " 解压完成，耗时：" + (end - start) + " ms");
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new Exception("解压失败");
        }
        finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new Exception("关闭文件失败");
                }
            }
        }
    }

    public int execCmd(String command, StringBuilder log) throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder();
        InputStreamReader reader = null;
        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        // 合并错误输出流
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        reader = new InputStreamReader(process.getInputStream(), "gbk");

        char[] buffer = new char[1024];
        int length = 0;
        while ((length = reader.read(buffer)) != -1) {
            sb.append(buffer, 0, length);
        }
        reader.close();
        // 保存到日志
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss");
        log.append(String.format("\n【时间：%s】\n【指令：%s】\n【指令输出】\n",
                dateFormat.format(new Date()), command));
        log.append(sb.toString());
        int result = process.waitFor();
        log.append(String.format("【结果： %d】\n", result));

        return result;
    }

    public File findFile(String path, String fileName) throws IOException {
        File file = new File(path);

        if (! file.exists()) {
            throw new IOException("dir not exists");
        }

        if (file.isFile()) {
            if (path.endsWith(fileName)) {
                return file;
            }
        }
        if (file.isDirectory()) {
            String[] fileNames = file.list();
            if (fileNames != null) {
                for (String fn : fileNames) {
                    file = findFile(path + File.separator + fn, fileName);
                    if (file != null) {
                        return file;
                    }
                }
            }
        }
        return null;
    }

    public String projectType(String path, List<File> files) {
        List<File> javaFiles = new ArrayList<>();
        List<File> cFiles = new ArrayList<>();
        List<File> cppFiles = new ArrayList<>();
        List<File> pythonFiles = new ArrayList<>();
        List<File> jsFiles = new ArrayList<>();
        try {
            getFilesBySuffix(path, new String[] {".java", ".class", ".pom", ".jar"}, javaFiles);
            getFilesBySuffix(path, new String[] {".c", ".h", ".sln", ".vcxproj"}, cFiles);
            getFilesBySuffix(path, new String[] {".cpp", ".h", ".sln", ".vcxproj"}, cppFiles);
            getFilesBySuffix(path,  new String[] {".py", ".pyc"}, pythonFiles);
            getFilesBySuffix(path,  new String[] {".js", ".json"}, jsFiles);

            String projectType = "";
            if (javaFiles.size() > cFiles.size()) {
                files.clear();
                files.addAll(javaFiles);
                projectType = "java";
            }
            else {
                files.clear();
                files.addAll(cFiles);
                projectType = "c";
            }
            if (cppFiles.size() > files.size()) {
                files.clear();
                files.addAll(cppFiles);
                projectType = "cpp";
            }
            if (pythonFiles.size() > files.size()) {
                files.clear();
                files.addAll(pythonFiles);
                projectType = "py";
            }
            if (jsFiles.size() > files.size()) {
                files.clear();
                files.addAll(jsFiles);
                projectType = "js";
            }
            return projectType;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 去除字符串前后的空格, 包括半角空格和全角空格(中文)等各种空格, java的string.trim()只能去英文半角空格
     * @param str
     */
    public String trim(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }

        char[] val = str.toCharArray();
        int st = 0;
        int len = val.length;
        while ((st < len) && isSpace(val[st])) {
            st++;
        }
        while ((st < len) && isSpace(val[len - 1])) {
            len--;
        }
        return ((st > 0) || (len < val.length)) ? str.substring(st, len) : str;
    }

    public boolean isSpace(char aChar) {
        return aChar == SPACE_32 || aChar == SPACE_12288 || aChar == SPACE_160 || aChar == SPACE_8194
                || aChar == SPACE_8195 || aChar == SPACE_8197 || aChar == SPACE_8201;
    }

    /**
     * 根据文件后缀名获取某一路径下的所有该类型文件
     *
     * @param path   文件路径
     * @param suffix 文件后缀
     * @param list 存放的列表
     * @return boolean
     */
    public boolean getFilesBySuffix(String path, String[] suffix, List<File> list) throws IOException {
        File file = new File(path);

        if (! file.exists()) {
            throw new IOException("dir not exists");
        }

        if (file.isFile()) {
            for (String s : suffix) {
                if (path.endsWith(s)) {
                    list.add(file);
                    return false;
                }
            }
        }
        if (file.isDirectory()) {
            String[] fileNames = file.list();
            if (fileNames != null) {
                for (String fileName : fileNames) {
                    getFilesBySuffix(path + File.separator + fileName, suffix, list);
                }
            }
        }
        return true;
    }

    public String formatPath(String path) {
        path = path.replace('\\', '/');
        path = path.replace("//", "/");
        return path;
    }

    /**
     * 删除整个目录、或文件
     *
     * @param path 路径
     * @throws IOException e
     */
    public void deleteDirOrFile(String path) throws IOException {
        path = formatPath(path);
        File file = new File(path);
        if (file.exists()) {
            if (file.isFile()) {
                if (! file.delete()) {
                    throw new IOException("delete fail");
                }
            }
            else if (file.isDirectory()) {
                String[] fileNames = file.list();
                if (fileNames == null || fileNames.length == 0) {
                    if (! file.delete()) {
                        throw new IOException("delete file fail");
                    }
                }
                else {
                    for (String fileName : fileNames) {
                        deleteDirOrFile(path + File.separator + fileName);
                    }
                    if (! file.delete()) {
                        throw new IOException("delete fail");
                    }
                }
            }
        }
    }


    public boolean checkFileEqual(String path1, String path2) throws IOException {

        StringBuilder s1 = new StringBuilder();
        StringBuilder s2 = new StringBuilder();
        FileReader reader1 = new FileReader(path1);
        FileReader reader2 = new FileReader(path2);
        int t;
        while ((t = reader1.read()) != -1) {
            char c1 = (char)t;
            if (! (c1 == ' ' || c1 == '\r' || c1 == '\n')) {
                s1.append(c1);
            }
        }
        while ((t = reader2.read()) != -1) {
            char c2 = (char)t;
            if (! (c2 == ' ' || c2 == '\r' || c2 == '\n')) {
                s2.append(c2);
            }
        }
        reader1.close();
        reader2.close();

        return s1.toString().equals(s2.toString());
    }

    public String getPathDir(String path) {
        path = formatPath(path);
        Pattern pattern = Pattern.compile("(.*)/.*?");
        Matcher matcher = pattern.matcher(path);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }
}
