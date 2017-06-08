package com.makeandship.gocd.git.provider.bitbucket;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tw.go.plugin.cmd.Console;
import com.tw.go.plugin.cmd.ConsoleResult;
import com.tw.go.plugin.cmd.ProcessOutputStreamConsumer;
import com.tw.go.plugin.git.GitCmdHelper;
import com.tw.go.plugin.model.GitConfig;
import com.tw.go.plugin.util.ListUtil;

public class GitCmdHelperBitBucket extends GitCmdHelper {

	private static final Logger LOG = LoggerFactory.getLogger(GitCmdHelperBitBucket.class);
	
	public GitCmdHelperBitBucket(GitConfig gitConfig, File workingDir) {
		super(gitConfig, workingDir);
	}
	
	@Override
	public void cloneOrFetch(String refSpec) {
        if (!isGitRepository() || !isSameRepository()) {
            setupWorkingDir();
            cloneRepository();
        }

        fetchAndResetToHead(refSpec);
    }
	
	@Override
	public void fetchAndReset(String refSpec, String revision) {
        stdOut.consumeLine(String.format("[GIT] Fetch and reset in working directory %s", workingDir));
        cleanAllUnversionedFiles();
        if (isSubmoduleEnabled()) {
            removeSubmoduleSectionsFromGitConfig();
        }
        fetch(refSpec);
        checkoutRemoteBranchToLocal();
        gc();
        resetHard(revision);
        if (isSubmoduleEnabled()) {
            checkoutAllModifiedFilesInSubmodules();
            updateSubmoduleWithInit();
        }
        cleanAllUnversionedFiles();
    }
	
	private boolean isGitRepository() {
        File dotGit = new File(workingDir, ".git");
        return workingDir.exists() && dotGit.exists() && dotGit.isDirectory();
    }

    public boolean isSameRepository() {
        try {
            return workingRepositoryUrl().equals(gitConfig.getEffectiveUrl());
        } catch (Exception e) {
            return false;
        }
    }

    private void setupWorkingDir() {
        FileUtils.deleteQuietly(workingDir);
        try {
            FileUtils.forceMkdir(workingDir);
        } catch (IOException e) {
            new RuntimeException("Could not create directory: " + workingDir.getAbsolutePath());
        }
    }
	
	@Override
	public void cloneRepository() {
		LOG.info("Overrided clone repository");
        List<String> args = new ArrayList<String>(Arrays.asList("clone", String.format("--branch=%s", gitConfig.getEffectiveBranch())));
        if (gitConfig.isShallowClone()) {
            args.add("--depth=1");
        }
        args.add(gitConfig.getEffectiveUrl());
        args.add(workingDir.getAbsolutePath());
        CommandLine gitClone = Console.createCommand(ListUtil.toArray(args));
        //runAndGetOutput(gitClone, null, stdOut, stdErr);
        ConsoleResult result = runOrBomb(gitClone, workingDir, stdOut, stdErr);
        LOG.info("out: " + result.stdOut().toString());
        LOG.info("err: " + result.stdErr().toString());
    }
	
	public ConsoleResult runOrBomb(CommandLine commandLine, File workingDir, ProcessOutputStreamConsumer stdOut, ProcessOutputStreamConsumer stdErr) {
        Executor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler(stdOut, stdErr));
        if (workingDir != null) {
        	LOG.info("workingDir: " + workingDir.getAbsolutePath());
            executor.setWorkingDirectory(workingDir);
        }

        try {
        	LOG.info("Executing: " + commandLine);
            int exitCode = executor.execute(commandLine);
            LOG.info("Executed: " + commandLine + " exitCode " + exitCode);
            
            if (exitCode != 0) {
                throw new RuntimeException(getMessage("Error", commandLine, workingDir));
            }

            return new ConsoleResult(exitCode, stdOut.output(), stdErr.output());
        } catch (Exception e) {
        	LOG.info("Error to execute");
            throw new RuntimeException(getMessage("Exception", commandLine, workingDir), e);
        }
    }
	
	private static String getMessage(String type, CommandLine commandLine, File workingDir) {
        return String.format("%s Occurred: %s - %s", type, commandLine.toString(), workingDir);
    }
}
