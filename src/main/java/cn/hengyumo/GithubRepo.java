package cn.hengyumo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GithubRepo
 * 学生github仓库xlsx表格映射实体
 *
 * @author hengyumo
 * @version 1.0
 */
@Data
public class GithubRepo {

    /* 学号 */
    @ExcelProperty(index = 0)
    String studentNumber;

    /* 姓名 */
    @ExcelProperty(index = 1)
    String studentName;

    /* github仓库url */
    @ExcelProperty(index = 2)
    String githubRepoUrl;

    public boolean isNull () {
        return studentNumber == null || studentName == null || githubRepoUrl == null;
    }

    public String getDownloadUrl () {
        if (this.isNull()) return null;
        Pattern pattern = Pattern.compile("github\\.com/(.*)/(.*)$");
        Matcher matcher = pattern.matcher(this.githubRepoUrl);
        if (matcher.find()) {
            return String.format(
                    "https://codeload.github.com/%s/%s/zip/master",
                    matcher.group(1), matcher.group(2));
        }
        return null;
    }
}
