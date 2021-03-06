package com.wg.gpm;

import com.google.common.collect.Lists;
import com.wg.gpm.properties.ConfigurationAccess;
import com.wg.gpm.properties.ConfigurationProperty;
import com.wg.gpm.properties.DefaultConfigurationAccess;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.eclipse.jgit.api.errors.GitAPIException;
import com.wg.gpm.git.DefaultGitAccess;
import com.wg.gpm.git.GitAccess;
import com.wg.gpm.gmail.GmailBuilder;
import com.wg.gpm.gmail.MailService;
import com.wg.gpm.message.MessageFileWriter;
import com.wg.gpm.parser.*;

import java.io.IOException;
import java.util.Optional;

/**
 * Created by aidan on 01/11/16.
 */
public class GitMailer {

    private void scanAndCommit(GitAccess gitAccess, MailService service, LineParser lineParser){
        MessageFileWriter writer = new MessageFileWriter(gitAccess.getGitDirectory(), lineParser);
        service.pollForMessages().stream()
                .map(writer::writeMessageToFile)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(gitAccess::createFileInGitAndCommit);
    }

    public static void main(String[] args) throws ConfigurationException, IOException, GitAPIException {
        ConfigurationAccess config = new DefaultConfigurationAccess(buildConfiguration(args));
        GitAccess gitAccess = DefaultGitAccess.buildGitAccess(config);
        gitAccess.syncIfMissing();
        GmailBuilder builder = new GmailBuilder(config);
        String requiredSender = config.getPropertyValue(ConfigurationProperty.REQUIRED_SENDER);
        LineParser parser = buildOptionalLineParser(config.getPropertyValue(ConfigurationProperty.WEBSITE_ROOT_URL));
        new GitMailer().scanAndCommit(gitAccess, builder.getGmailService(requiredSender), parser);
    }

    public static LineParser buildOptionalLineParser(String repourl) {
        OptionalLineParser first = new TitleImageLineParser(repourl);
        OptionalLineParser second = new ImageLineParser(repourl, Optional.of("500"), Optional.empty());
        OptionalLineParser third = new LinkLineParser();
        return new DefaultLineParser(Lists.newArrayList(first, second, third));
    }

    private static Configuration buildConfiguration(String[] args) throws ConfigurationException {
        if(args == null || args.length == 0){
            return new Configurations().properties("properties.txt");
        }
        return new Configurations().properties(args[0]);
    }
}
