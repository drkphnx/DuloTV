package gd.dulo.tv;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import java.util.HashMap;
import java.util.Map;

public class PlayerActivity extends Activity {
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_REFERER = "referer";
    public static final String EXTRA_COOKIE = "cookie";
    public static final String EXTRA_UA = "ua";

    private ExoPlayer player;
    private PlayerView playerView;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        immersive();
        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null || url.isEmpty()) { finish(); return; }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        playerView = new PlayerView(this);
        playerView.setUseController(true);
        playerView.setControllerAutoShow(true);
        root.addView(playerView, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory();
        String ua = getIntent().getStringExtra(EXTRA_UA);
        if (ua != null && !ua.isEmpty()) http.setUserAgent(ua);
        Map<String,String> headers = new HashMap<>();
        String ref = getIntent().getStringExtra(EXTRA_REFERER);
        String cookie = getIntent().getStringExtra(EXTRA_COOKIE);
        if (ref != null && !ref.isEmpty()) headers.put("Referer", ref);
        if (cookie != null && !cookie.isEmpty()) headers.put("Cookie", cookie);
        if (!headers.isEmpty()) http.setDefaultRequestProperties(headers);

        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(http))
                .build();
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(url));
        player.prepare();
        player.play();
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) finish();
            }
        });
    }

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() == KeyEvent.ACTION_DOWN && player != null) {
            switch (e.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    player.seekTo(Math.max(0, player.getCurrentPosition() - 10000)); return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    player.seekTo(player.getCurrentPosition() + 10000); return true;
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    if (player.isPlaying()) player.pause(); else player.play(); return true;
                case KeyEvent.KEYCODE_BACK:
                    finish(); return true;
            }
        }
        return super.dispatchKeyEvent(e);
    }

    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override protected void onDestroy() {
        if (playerView != null) playerView.setPlayer(null);
        if (player != null) { player.release(); player = null; }
        super.onDestroy();
    }
}
