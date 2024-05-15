package com.example.musicapp.fragment;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.spotify.android.appremote.api.*;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.PlayerApi.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.musicapp.R;
import com.example.musicapp.adapter.FetchAccessToken;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

import com.spotify.android.appremote.*;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.types.Empty;
import com.spotify.protocol.types.Track;

public class PlaySongFragment extends Fragment implements FetchAccessToken.AccessTokenCallback {

    private View view;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateSeekBarRunnable;
    private SpotifyAppRemote spotifyAppRemote;
    private TextView songname, artistname, duration_played, duration_total;
    private ImageView cover_art;
    private ImageButton repeateBtn, previousBtn, pauseBtn, nextBtn, shuffleBtn;
    private SeekBar seekBar;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private int position = -1;
    private FetchAccessToken fetchAccessToken;
    ConnectionParams connectionParams = new ConnectionParams.Builder("19380ddfc0344af29cb61de3c6655fda")
            .setRedirectUri("https://com.spotify.android.spotifysdkkotlindemo/callback")
            .showAuthView(true)
            .build();

    @Override
    public void onTokenReceived(String accessToken) {
        getTrack(accessToken);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.play_song, container, false);
        fetchAccessToken = new FetchAccessToken();
        fetchAccessToken.getTokenFromSpotify(this);
        initializeViews();
        return view;
    }

    private void initializeViews() {
        songname = view.findViewById(R.id.songNamePlay);
        artistname = view.findViewById(R.id.artistNamePlay);
        duration_played = view.findViewById(R.id.played);
        duration_total = view.findViewById(R.id.total);
        repeateBtn = view.findViewById(R.id.repeateBtn);
        previousBtn = view.findViewById(R.id.previousBtn);
        pauseBtn = view.findViewById(R.id.pauseBtn);
        nextBtn = view.findViewById(R.id.nextBtn);
        shuffleBtn = view.findViewById(R.id.shuffleBtn);
        cover_art = view.findViewById(R.id.imageCon);
        seekBar = view.findViewById(R.id.seekbar);
    }

    private void getTrack(String accessToken) {
        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://api.spotify.com/").addConverterFactory(GsonConverterFactory.create()).build();

        SpotifyApi apiService = retrofit.create(SpotifyApi.class);
        String songId = "7ouMYWpwJ422jRcDASZB7P";
        String authorization = "Bearer " + accessToken;

        Call<TrackModel> call = apiService.getTrack(authorization, songId);
        call.enqueue(new Callback<TrackModel>() {
            @Override
            public void onResponse(@NonNull Call<TrackModel> call, @NonNull Response<TrackModel> response) {
                if (response.isSuccessful()) {
                    TrackModel track = response.body();
                    if (track != null) {
                        setupTrack(track);
                    }
                } else {
                    showError(response);
                }
            }

            @Override
            public void onFailure(Call<TrackModel> call, Throwable throwable) {
                Log.e("Error fetching track", throwable.getMessage());
            }
        });
    }



    public void setupTrack(TrackModel track) {
        String songName = track.getName();
        String artistName = track.artists.get(0).getName();
        String imageUrl = track.album.images.get(0).getUrl();
        String playUrl = track.getPreview_url();

        songname.setText(songName);
        artistname.setText(artistName);
        Glide.with(getActivity()).load(imageUrl).into(cover_art);

//        setupMediaPlayer(playUrl);
        if (spotifyAppRemote != null) {
            spotifyAppRemote.getPlayerApi().play("spotify:track:7ouMYWpwJ422jRcDASZB7P");
        }
        setupSeekBar();
        setupPauseButton();
    }

    public void setUpSpotifyRemote(){
        ConnectionParams connectionParams = new ConnectionParams.Builder("19380ddfc0344af29cb61de3c6655fda")
                .setRedirectUri("https://com.spotify.android.spotifysdkkotlindemo/callback")
                .showAuthView(true)
                .build();
        spotifyAppRemote.connect(requireContext(), connectionParams, new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote appRemote) {
                spotifyAppRemote = appRemote;
                Log.d("MainActivity", "Connected! Yay!");
                // Now you can start interacting with App Remote
                if (spotifyAppRemote != null) {
                    // Play a playlist
                    String playlistURI = "spotify:track:7ouMYWpwJ422jRcDASZB7P";
                    spotifyAppRemote.getPlayerApi().play(playlistURI);
                    // Subscribe to PlayerState
                    spotifyAppRemote.getPlayerApi().subscribeToPlayerState().setEventCallback(playerState -> {
                        Track track = playerState.track;
                    });
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                Log.e("MainActivity", throwable.getMessage(), throwable);
                // Something went wrong when attempting to connect! Handle errors here
            }
        });
    }
    public void setupMediaPlayer(String playUrl) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build());
            mediaPlayer.setDataSource(playUrl);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                seekBar.setMax(mediaPlayer.getDuration() / 1000);
                duration_total.setText(formattedTime(mediaPlayer.getDuration() / 1000));
                mediaPlayer.start();
                isPlaying = true;
                handler.postDelayed(updateSeekBarRunnable, 0);
            });
        } catch (IOException e) {
            Log.e("Error setting up player", e.getMessage());
        }
    }

    public void setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress * 1000);
                }
                duration_played.setText(formattedTime(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                int currentPosition = mediaPlayer.getCurrentPosition() / 1000;
                seekBar.setProgress(currentPosition);
                duration_played.setText(formattedTime(currentPosition));
                handler.postDelayed(this, 500); // Update every 500 milliseconds
            }
        };
    }

    public void setupPauseButton() {
        pauseBtn.setOnClickListener(v -> {
            if (isPlaying) {
                mediaPlayer.pause();
                isPlaying = false;
                pauseBtn.setBackgroundResource(R.drawable.play);
            } else {
                mediaPlayer.start();
                pauseBtn.setBackgroundResource(R.drawable.pause);
                isPlaying = true;
            }
        });
    }

    public void showError(Response<TrackModel> response) {
        try {
            assert response.errorBody() != null;
            String errorReason = response.errorBody().string();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Error");
            builder.setMessage(errorReason);
            builder.setPositiveButton("OK", null);
            builder.create().show();
        } catch (IOException e) {
            Log.e("Error handling response", e.getMessage());
        }
    }

    @SuppressLint("DefaultLocale")
    private String formattedTime(int currentPosition) {
        int minutes = currentPosition / 60;
        int seconds = currentPosition % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updateSeekBarRunnable);
    }

    public interface SpotifyApi {
        @GET("v1/tracks/{songId}")
        Call<TrackModel> getTrack(@Header("Authorization") String authorization, @Path("songId") String songId);
    }

    public static class TrackModel {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("preview_url")
        private String preview_url;

        @SerializedName("artists")
        private List<ArtistModel> artists;

        @SerializedName("album")
        private AlbumModel album;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getPreview_url() {
            return preview_url;
        }

        public static class ArtistModel {
            @SerializedName("name")
            private String name;

            public String getName() {
                return name;
            }
        }

        public static class AlbumModel {
            @SerializedName("images")
            private List<ImageModel> images;

            public static class ImageModel {
                @SerializedName("url")
                private String url;

                public String getUrl() {
                    return url;
                }
            }
        }
    }
}