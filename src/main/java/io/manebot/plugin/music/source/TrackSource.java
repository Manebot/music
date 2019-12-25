package io.manebot.plugin.music.source;

import io.manebot.command.CommandSender;

import io.manebot.lambda.ThrowingFunction;
import io.manebot.plugin.audio.mixer.input.AudioProvider;
import io.manebot.plugin.music.database.model.Community;
import io.manebot.plugin.music.database.model.Track;
import io.manebot.plugin.music.database.model.TrackRepository;
import io.manebot.plugin.music.repository.Repository;

import java.io.*;
import java.net.URL;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public interface TrackSource {

    /**
     * Gets the name of the track source.
     * @return track source name.
     */
    String getName();

    /**
     * Gets if the track source instance is responsible for obtaining resources indicated by the provided URL.
     * @param url URL to find
     * @return true if <b>find(URL)</b> can be called for this URL, false otherwise.
     */
    boolean canFind(URL url);

    /**
     * Attempts to find a track by the specified URL.
     * @param community community to search from or createAssociation videos to.
     * @param url URL of track to obtain.
     * @return Result instance if one is suggested, <i>null</i> otherwise.
     * @throws IOException if there is a problem obtaining track metadata.
     * @throws IllegalArgumentException if there is a problem validating the url argument.
     */
    Result find(Community community, URL url) throws IOException, IllegalArgumentException;

    /**
     * The Result class handles found and identified tracks.
     */
    abstract class Result {
        private final Community community;
        private final URL url;
        private final ResultPriority priority;
        private final Function<AudioProtocol, AudioProvider> open;
        private final Function<Community, Track> trackFunction;

        private Track track;

        public Result(Community community,
                      URL url,
                      ResultPriority priority,
                      Function<Community, Track> trackFunction,
                      ThrowingFunction<AudioProtocol, AudioProvider, IOException> open) {
            this.community = community;
            this.url = url;
            this.priority = priority;
            this.trackFunction = trackFunction;
            this.open = open;
        }

        public Community getCommunity() {
            return community;
        }

        public URL getUrl() {
            return url;
        }

        public ResultPriority getPriority() {
            return priority;
        }

        public Track getTrack() {
            return track != null ? track : (track = trackFunction.apply(community));
        }

        public abstract String getFormat();

        public AudioProvider openDirect(AudioProtocol protocol) {
            return open.apply(protocol);
        }

        public Repository.Resource get() throws IOException {
            TrackRepository repository = Objects.requireNonNull(community.getRepository());
            Repository instance = Objects.requireNonNull(repository.getInstance());
            return instance.get(this);
        }

        public abstract InputStream openConnection() throws IOException;

        public abstract boolean isLocal();
    }

    class DownloadResult extends Result {
        private final String format;

        public DownloadResult(Community community,
                              URL url,
                              String format,
                              ResultPriority priority,
                              Consumer<Track.Builder> constructor,
                              ThrowingFunction<AudioProtocol, AudioProvider, IOException> open) {
            super(community,
                    url,
                    priority,
                    selectedCommunity -> selectedCommunity.getOrCreateTrack(url, constructor),
                    open);

            this.format = format;
        }

        @Override
        public String getFormat() {
            return format;
        }

        @Override
        public InputStream openConnection() throws IOException {
            return getUrl().openStream();
        }

        @Override
        public boolean isLocal() {
            return false;
        }
    }

    enum ResultPriority {
        IDEAL(3),
        PREFERRED(2),
        LOW(1);

        private final int ordinal;

        ResultPriority(int ordinal) {
            this.ordinal = ordinal;
        }

        public int getOrdinal() {
            return ordinal;
        }
    }

}
