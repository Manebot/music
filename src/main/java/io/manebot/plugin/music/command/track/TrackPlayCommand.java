package io.manebot.plugin.music.command.track;

import io.manebot.command.CommandSender;
import io.manebot.command.exception.CommandArgumentException;
import io.manebot.command.exception.CommandExecutionException;
import io.manebot.command.executor.chained.AnnotatedCommandExecutor;
import io.manebot.command.executor.chained.ChainPriority;
import io.manebot.command.executor.chained.argument.CommandArgumentString;
import io.manebot.command.executor.chained.argument.CommandArgumentURL;
import io.manebot.command.search.CommandArgumentSearch;
import io.manebot.database.Database;
import io.manebot.database.search.Search;
import io.manebot.plugin.music.Music;
import io.manebot.plugin.music.database.model.Community;
import io.manebot.plugin.music.database.model.Track;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;

public class TrackPlayCommand extends AnnotatedCommandExecutor {
    private final Music music;
    private final Database database;

    public TrackPlayCommand(Music music, Database database) {
        this.music = music;
        this.database = database;
    }

    @Command(description = "Plays a track by its URL", permission = "music.track.play")
    public void play(CommandSender sender,
                     @CommandArgumentURL.Argument() URL url)
            throws CommandExecutionException {
        try {
            Track track = music.play(sender, url);
            sender.sendMessage("(Playing \"" + track.getName() + "\")");
        } catch (Exception e) {
            throw new CommandExecutionException(e);
        }
    }

    @Command(description = "Plays a track by its URL", permission = "music.track.play")
    public void play(CommandSender sender,
                     @CommandArgumentSearch.Argument() Search search) throws CommandExecutionException {
        Community community = music.getCommunity(sender);
        if (community == null)
            throw new CommandArgumentException("There is no music community associated with this conversation.");

        Track track;

        try {
            track = Track.createSearch(database, community).search(search, sender.getChat().getDefaultPageSize())
                    .getResults()
                    .stream()
                    .reduce((a, b) -> {
                        throw new IllegalStateException("Query ambiguation: more than 1 result found");
                    })
                    .orElseThrow(() -> new CommandArgumentException("No results found."));
        } catch (SQLException e) {
            throw new CommandExecutionException(e);
        }

        try {
            track = music.play(sender, track.toURL());
            sender.sendMessage("(Playing \"" + track.getName() + "\")");
        } catch (Exception e) {
            throw new CommandExecutionException(e);
        }
    }
}
