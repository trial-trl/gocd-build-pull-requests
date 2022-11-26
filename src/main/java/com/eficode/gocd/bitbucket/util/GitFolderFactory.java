package com.eficode.gocd.bitbucket.util;

import java.io.File;

public class GitFolderFactory {

    public File create(String folderName) {
        return new File(folderName);
    }
}
