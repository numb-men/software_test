package cn.hengyumo;

import java.io.File;
import java.util.Scanner;

/**
 * Main
 *
 * @author hengyumo
 * @version 1.0
 */
public class Main {

    private static SoftwareTestCore softwareTestCore = SoftwareTestCore.getSoftwareTestCore();

    public static void main(String[] args) {
        while (true) {
            System.out.println(
                    "欢迎使用软工自动测试，请选择工作模式：\n" +
                            "\t1、下载并解压。\n" +
                            "\t2、载入日志尝试对下载失败的仓库进行重新下载。\n" +
                            "\t3、生成日志xlsx表格。\n" +
                            "\t4、对仓库进行编译。\n" +
                            "\t5、对仓库进行测试。\n" +
                            "\t6、载入日志对编译和测试失败仓库进行重新编译和测试。\n" +
                            "\t7、保存日志。\n" +
                            "\tq、退出并保存日志。\n"
            );
            System.out.print("请选择（1-7/q）：");
            Scanner scanner = new Scanner(System.in);
            while (true) {
                try {
                    String mode = scanner.nextLine();
                    if (mode.equals("q")) {
                        softwareTestCore.saveLog();
                        System.out.println("谢谢使用。");
                        return;
                    }
                    int m = Integer.valueOf(mode);
                    if (m < 1 || m > 7) {
                        throw new Exception();
                    }
                    if (check(m)) {
                        run(m);
                    }
                    else {
                        System.out.println("操作放弃。");
                    }
                    break;
                }
                catch (Exception e) {
                    System.out.print("请重新选择（1-7/q）：");
                }
            }
        }
    }

    private static boolean check(int mode) {
        if (mode == 1) {
            File logFile = new File("log.json");
            if (logFile.exists()) {
                System.out.println("系统检测到您之前已经进行过下载并解压，您接下来的操作，" +
                        "将会清空之前的下载和解压，并重新进行下载和解压。");
                Scanner scanner = new Scanner(System.in);
                while (true) {
                    System.out.print("请确定您的操作（y/n）：");
                    String option = scanner.nextLine();
                    if (option.equals("y")) {
                        return true;
                    }
                    else if (option.equals("n")) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void run(int mode) {
        if (mode == 1) {
            softwareTestCore.retryDownloadAndUnzip();
        }
        else if (mode == 2) {
            softwareTestCore.retryFailDownloadAndUnzip();
        }
        else if (mode == 3) {
            softwareTestCore.logToExcel();
        }
        else if (mode == 4) {
            softwareTestCore.retryCompile();
        }
        else if (mode == 5) {
            softwareTestCore.retryTest();
        }
        else if (mode == 6) {

        }
        else if (mode == 7) {
            softwareTestCore.saveLog();
        }
    }
}
