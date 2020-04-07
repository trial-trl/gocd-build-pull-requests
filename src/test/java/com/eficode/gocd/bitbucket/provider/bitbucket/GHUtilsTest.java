package com.eficode.gocd.bitbucket.provider.bitbucket;

import org.junit.Test;

import static com.eficode.gocd.bitbucket.provider.bitbucket.BitbucketUtils.parseBBUrl;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class GHUtilsTest {
    @Test
    public void shouldParseSSH() {
        assertThat(parseBBUrl("git@github.com:ashwanthkumar/gocd-build-github-pull-requests.git"), is("ashwanthkumar/gocd-build-github-pull-requests"));
        assertThat(parseBBUrl("git@github.com:ashwanthkumar/gocd-build-github-pull-requests"), is("ashwanthkumar/gocd-build-github-pull-requests"));
        assertThat(parseBBUrl("git@Github.Com:ashwanthkumar/gocd-build-github-pull-requests"), is("ashwanthkumar/gocd-build-github-pull-requests"));
        assertThat(parseBBUrl("git@code.corp.yourcompany.com:username/repo"), is("username/repo"));
    }

    @Test
    public void shouldParseHTTPS() {
        assertThat(parseBBUrl("https://github.com/ashwanthkumar/gocd-build-github-pull-requests.git"), is("ashwanthkumar/gocd-build-github-pull-requests"));
        assertThat(parseBBUrl("https://github.com/ashwanthkumar/gocd-build-github-pull-requests"), is("ashwanthkumar/gocd-build-github-pull-requests"));
        assertThat(parseBBUrl("https://Github.Com/ashwanthkumar/gocd-build-github-pull-requests"), is("ashwanthkumar/gocd-build-github-pull-requests"));
        assertThat(parseBBUrl("https://Github.Com/Ashwanthkumar/gocd-build-github-pull-requests"), is("Ashwanthkumar/gocd-build-github-pull-requests"));
        assertThat(parseBBUrl("http://code.corp.yourcompany.com:username/repo"), is("username/repo"));
        assertThat(parseBBUrl("https://github.company.com/user/test-repo.git"), is("user/test-repo"));
    }

    @Test
    public void shouldExtractPullRequestIdFromDiffUrl() {
        assertThat(BitbucketUtils.prIdFrom("https://github.com/phanan/htaccess/pull/13.diff"), is(13));
        assertThat(BitbucketUtils.prIdFrom("https://github.com/phanan/htaccess/pull/133.diff"), is(133));
        assertThat(BitbucketUtils.prIdFrom("https://github.com/phanan/htaccess/pull/1.diff"), is(1));
    }
}
