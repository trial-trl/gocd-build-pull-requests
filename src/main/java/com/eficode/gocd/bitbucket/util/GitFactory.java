package com.eficode.gocd.bitbucket.util;

import java.io.File;

import com.tw.go.plugin.model.GitConfig;

public class GitFactory {

    public ExtendedGitCmdHelper create(GitConfig config, File folder) {
        return new ExtendedGitCmdHelper(config, folder);
    }

}
