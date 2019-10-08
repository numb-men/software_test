package cn.hengyumo;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.*;

/**
 * GithubRepoDownloader
 *
 * @author hengyumo
 * @version 1.0
 */
public class GithubRepoDownloader {

    private CloseableHttpClient httpclient;

    private String savePath;

    public GithubRepoDownloader() {
        httpclient = HttpClients.createDefault();
    }

    public SoftwareTestStatus download(String githubDownloadUrl, String studentNumber) {
        HttpGet httpGet = new HttpGet(githubDownloadUrl);
        httpGet.setHeader("Content-Type", "application/octet-stream");
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(30000)
                .setConnectionRequestTimeout(10000)
                .build();
        httpGet.setConfig(requestConfig);
        try {
            CloseableHttpResponse response = httpclient.execute(httpGet);
            System.out.println(githubDownloadUrl + " " + response.getStatusLine());
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 400 || statusCode < 200) {
                response.close();
                return SoftwareTestStatus.DOWNLOAD_FAIL;
            }
            HttpEntity entity = response.getEntity();
            InputStream in = entity.getContent();
            savePath = "repo/zip/" + studentNumber + ".zip";
            FileOutputStream out = new FileOutputStream(new File(savePath));
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0){
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();
            response.close();
        }
        catch (ConnectionPoolTimeoutException e) {
            e.printStackTrace();
            return SoftwareTestStatus.TIMEOUT_FOR_CONNECT;
        }
        catch (IOException e) {
            e.printStackTrace();
            return SoftwareTestStatus.SAVE_FAIL;
        }
        catch (Exception e) {
            e.printStackTrace();
            return SoftwareTestStatus.DOWNLOAD_FAIL;
        }
        return SoftwareTestStatus.DOWNLOAD_SUCCEED;
    }

    public String getSavePath() {
        return savePath;
    }
}
