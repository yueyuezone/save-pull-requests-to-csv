package com.github.hcsp.io;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Crawler {
    static class GitHubPullRequest {
        // Pull request的编号
        String number;
        // Pull request的标题
        String title;
        // Pull request的作者的GitHub id
        String author;

        GitHubPullRequest(String number, String title, String author) {
            this.number = number;
            this.title = title;
            this.author = author;
        }
    }

    // 给定一个仓库名，例如"golang/go"，或者"gradle/gradle"，读取前n个Pull request并保存至csvFile指定的文件中，格式如下：
    // number,author,title
    // 12345,blindpirate,这是一个标题
    // 12345,FrankFang,这是第二个标题
    public static void savePullRequestsToCSV(String repo, int n, File csvFile) throws IOException {
        List<GitHubPullRequest> pullRequestList = getFirstPageOfPullRequests(repo);
        List<String> lines = new ArrayList<>();
        lines.add("number,author,title");
        for (int i = 0; i < n; ++i) {
            GitHubPullRequest pr = pullRequestList.get(i);
            lines.add(pr.number + "," + pr.author + "," + pr.title);
        }
        FileUtils.writeLines(csvFile, lines);
    }

    public static List<GitHubPullRequest> getFirstPageOfPullRequests(String repo) throws IOException {
        List<GitHubPullRequest> gitHubPullRequests = new ArrayList<>();
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("http://github.com/" + repo + "/pulls");
        CloseableHttpResponse response1 = httpclient.execute(httpGet);
        try {
            System.out.println(response1.getStatusLine());
            HttpEntity entity1 = response1.getEntity();
            InputStream inputStream = entity1.getContent();
            String html = IOUtils.toString(inputStream, "UTF-8");
            Document document = Jsoup.parse(html);
            ArrayList<Element> issues = document.select("js-issue-row");
            for (Element element : issues) {
                String[] nb = element.select("opened-by").text().split(" ");
                String author = nb[nb.length - 1];
                String number = nb[0].replace("#", "");
                String title = element.child(0).child(1).child(0).text();
                gitHubPullRequests.add(new GitHubPullRequest(number, title, author));
            }
            // do something useful with the response body
            // and ensure it is fully consumed
            EntityUtils.consume(entity1);
        } finally {
            response1.close();
        }
        return gitHubPullRequests;
    }
}
