package cn.hengyumo;

/**
 * SoftwareTestStatus
 *
 * @author hengyumo
 * @version 1.0
 */
public enum SoftwareTestStatus {

    
    EMPTY_GITHUB_REPO("空，未填github仓库地址"),

    
    BAD_GITHUB_REPO_URL("不合法的github仓库形式"),

    
    WAIT_TO_DOWNLOAD("链接无误，等待下载"),

    
    DOWNLOAD_SUCCEED("下载成功"),

    
    DOWNLOAD_FAIL("下载失败"),

    
    SAVE_FAIL("保存失败"),

    
    UNZIP_FAIL("解压失败"),

    
    UNZIP_SUCCEED("解压成功"),

    
    UNKNOWN_ERROR("未知错误"),

    
    TIMEOUT_FOR_CONNECT("连接超时"),

    
    MAIN_PROGRAM_FILE_NOT_FOUND("主程序未找到"),

    
    WAIT_TO_COMPILE("等待编译"),
    
    
    COMPILE_DIR_OR_FILE_NOT_FOUND("待编译文件夹或文件未找到"),

    
    COMPILE_CCPP_FAIL("编译C/C++失败"),

    
    COMPILE_CCPP_SUCCEED("编译C/C++成功"),

    
    COMPILE_JAVA_FAIL("编译Java失败"),

    
    COMPILE_JAVA_SUCCEED("编译Java成功"),

    
    PACK_JAR_FAIL("打包jar失败"),

    
    PACK_JAR_SUCCEED("打包jar成功"),

    
    UN_SUPPORT_COMPILE_TYPE("不支持编译类型"),


    TEST_TIMEOUT("测试超时"),


    WAIT_TO_TEST("准备测试"),


    TEST_FAIL("测试失败"),


    TEST_SUCCEED("测试成功");



    private String comment;

    SoftwareTestStatus(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    public static String getComment(int status) {
        for (SoftwareTestStatus softwareTestStatus : SoftwareTestStatus.values()) {
            if (softwareTestStatus.ordinal() == status) {
                return softwareTestStatus.getComment();
            }
        }
        return null;
    }
}
