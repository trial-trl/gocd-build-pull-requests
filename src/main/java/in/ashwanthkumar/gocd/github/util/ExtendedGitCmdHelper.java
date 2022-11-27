package in.ashwanthkumar.gocd.github.util;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tw.go.plugin.cmd.Console;
import com.tw.go.plugin.cmd.ProcessOutputStreamConsumer;
import com.tw.go.plugin.git.GitCmdHelper;
import com.tw.go.plugin.model.GitConfig;
import org.apache.commons.exec.CommandLine;

public class ExtendedGitCmdHelper extends GitCmdHelper {

    public ExtendedGitCmdHelper(GitConfig gitConfig, File workingDir) {
        super(gitConfig, workingDir);
    }

    public ExtendedGitCmdHelper(GitConfig gitConfig, File workingDir, ProcessOutputStreamConsumer stdOut,
            ProcessOutputStreamConsumer stdErr) {
        super(gitConfig, workingDir, stdOut, stdErr);
    }

    public void checkoutNewBranch(String branchName) {
        CommandLine gitCheckout = Console.createCommand("checkout", "-B", branchName);
        Console.runOrBomb(gitCheckout, workingDir, stdOut, stdErr);
    }

    public Map<String, String> getBranchLatestRevisions(String pattern) {
        CommandLine gitCmd = Console.createCommand("show-ref");
        System.out.println(gitCmd);
        List<String> outputLines = Console.runOrBomb(gitCmd, workingDir, stdOut, stdErr).stdOut();
        Map<String, String> branchToRevisionMap = new HashMap<>();
        for (String line : outputLines) {
            if (line.contains(pattern)) {
                String[] parts = line.split(" ");
                String branch = parts[1].replace(pattern, "");
                String revision = parts[0];
                if (!branch.equals("HEAD")) {
                    branchToRevisionMap.put(branch, revision);
                }
            }
        }
        return branchToRevisionMap;
    }
}
