package org.rookit.player;

import static javafx.scene.media.MediaPlayer.Status.PLAYING;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.rookit.dm.track.Track;

import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

@SuppressWarnings("javadoc")
public class RookitPlayer {
	
	private static final Path AUDIO_TEMP = Paths.get("audio_temp");
	
	private Track current;
	private MediaPlayer player;
	private Runnable onEnd;

	@SuppressWarnings("unused")
	public RookitPlayer() {
		new JFXPanel();
		if(!Files.exists(AUDIO_TEMP)) {
			try {
				Files.createDirectory(AUDIO_TEMP);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public Optional<Track> getCurrent() {
		return Optional.ofNullable(current);
	}
	
	public synchronized void load(Track track) {
		final boolean wasMuted = isLoaded() && player.isMute();
		if(isLoaded()) {
			player.dispose();
		}
		final Path path = getPath(track);
		final Media media = new Media(path.toUri().toString());
		current = track;
		player = new MediaPlayer(media);
		player.setVolume(0.5);
		player.setMute(wasMuted);
		player.setOnPlaying(track::play);
		if(onEnd != null) {
			player.setOnEndOfMedia(onEnd);
		}
		System.out.println("Loaded: " + track.getLongFullTitle());
	}
	
	private Path getPath(Track track) {
		final Path path;
		try {
			if(track.getPath() != null) {
				path = AUDIO_TEMP.resolve(track.getIdAsString() + ".mp3");
				if(!Files.exists(path)) {
					Files.copy(track.getPath().toInput(), path);
				}
				return path;
			}
			throw new RuntimeException("Cannot find audio content for track: " + track);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public synchronized void onEnd(Runnable onEnd) {
		this.onEnd = onEnd;
		if(player != null) {
			player.setOnEndOfMedia(onEnd);
		}
	}
	
	public synchronized void play() {
		if(!isPlaying()) {
			player.play();
		}
	}
	
	public synchronized void pause() {
		if(isPlaying()) {
			player.pause();
		}
	}
	
	public void stop() {
		if(isPlaying()) {
			player.stop();
			player.dispose();
		}
	}
	
	public void seek(Duration duration) {
		if(isLoaded()) {
			player.seek(new javafx.util.Duration(duration.toMillis()));
		}
	}
	
	public boolean isPlaying() {
		return isLoaded() && PLAYING.equals(player.getStatus());
	}
	
	public void mute(boolean mute) {
		if(isLoaded()) {
			player.setMute(mute);
		}
	}
	
	public Duration getCurrentTime() {
		return isLoaded() ? player.getCurrentTime() : null;
	}
	
	public Duration getRemainingTime() {
		return isLoaded() ? player.getMedia().getDuration().subtract(getCurrentTime()) : null;
	}
	
	public void close() throws IOException {
		stop();
		player.dispose();
		Files.list(AUDIO_TEMP).forEach(this::delete);
	}
	
	private void delete(Path path) {
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			System.err.println("Could not delete: " + path);
		}
	}
	
	public boolean isLoaded() {
		return player != null;
	}

	public void clearCache() {
		try {
			Files.list(AUDIO_TEMP).forEach(a -> {
				try {
					Files.deleteIfExists(a);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
