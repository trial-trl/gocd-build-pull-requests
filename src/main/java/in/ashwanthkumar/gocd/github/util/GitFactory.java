package in.ashwanthkumar.gocd.github.util;

import com.makeandship.gocd.git.provider.bitbucket.GitCmdHelperBitBucket;
import com.tw.go.plugin.GitHelper;
import com.tw.go.plugin.HelperFactory;
import com.tw.go.plugin.git.GitCmdHelper;
import com.tw.go.plugin.model.GitConfig;

import java.io.File;

public class GitFactory {

    public GitHelper create(GitConfig config, File folder) {
        return new GitCmdHelperBitBucket(config, folder); //HelperFactory.gitCmd(config, folder);
    }

}
