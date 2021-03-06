package io.manebot.plugin.music.command.track;

import io.manebot.command.CommandSender;
import io.manebot.command.exception.CommandArgumentException;
import io.manebot.command.exception.CommandExecutionException;
import io.manebot.command.executor.CommandExecutor;
import io.manebot.database.Database;
import io.manebot.plugin.music.Music;
import io.manebot.plugin.music.database.model.Tag;
import io.manebot.plugin.music.database.model.Track;
import io.manebot.plugin.music.database.model.TrackTag;
import io.manebot.security.Grant;
import io.manebot.security.Permission;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class TrackUntagCommand implements CommandExecutor {
    private final Music music;
    private final Database database;

    public TrackUntagCommand(Music music, Database database) {
        this.music = music;
        this.database = database;
    }

    @Override
    public void execute(CommandSender sender, String label, String[] tags) throws CommandExecutionException {
        Permission.checkPermission("music.track.tag", Grant.ALLOW);

        if (tags.length <= 0)
            throw new CommandArgumentException("No tags supplied.");

        Track track = music.getPlayedTrack(sender.getConversation());

        if (track == null)
            throw new CommandArgumentException("No tracks were played on this channel recently.");

        untag(sender, track, new HashSet<>(Arrays.asList(tags)));
    }

    private void untag(CommandSender sender, Track track, Set<String> tagNames) throws CommandArgumentException {
        tagNames = tagNames.stream().map(String::toLowerCase).collect(Collectors.toSet());

        Set<Tag> tags = music.getMusicManager().getTags(tagNames);
        Set<TrackTag> added = track.removeTags(tags);

        if (added.size() > 0) {
            sender.sendMessage("Track tag(s) " +
                    String.join(", ", added.stream().map(TrackTag::getTag)
                            .map(Tag::getName).collect(Collectors.toSet())) + " removed.");
        } else {
            throw new CommandArgumentException("Track not tagged with " +
                    String.join(", ", tagNames) + ".");
        }
    }
}
