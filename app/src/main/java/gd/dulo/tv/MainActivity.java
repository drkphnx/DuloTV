package gd.dulo.tv;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String HOME = "https://dulo.gd/";
    private WebView webView;
    private ProgressBar progress;
    private TextView error;
    private FrameLayout root;
    private FrameLayout splash;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        immersive();

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));

        progress = new ProgressBar(this);
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(64, 64, Gravity.CENTER);
        pp.topMargin = 150;
        root.addView(progress, pp);

        error = new TextView(this);
        error.setTextColor(Color.WHITE);
        error.setTextSize(20);
        error.setGravity(Gravity.CENTER);
        error.setPadding(48, 48, 48, 48);
        error.setVisibility(View.GONE);
        root.addView(error, new FrameLayout.LayoutParams(-1, -1));

        // Branded startup screen instead of a blank black screen.
        splash = new FrameLayout(this);
        splash.setBackgroundColor(Color.BLACK);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.dulo_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        int width = (int) (360 * getResources().getDisplayMetrics().density);
        int height = (int) (180 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height, Gravity.CENTER);
        splash.addView(logo, lp);
        root.addView(splash, new FrameLayout.LayoutParams(-1, -1));

        setContentView(root);
        configureWebView();
        webView.loadUrl(HOME);
    }

    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setLoadsImagesAutomatically(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) s.setOffscreenPreRaster(false);

        // Keep hardware acceleration for WebView/video, but remove expensive website effects in JS/CSS.
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView v, String url, Bitmap favicon) {
                progress.setVisibility(View.VISIBLE);
                error.setVisibility(View.GONE);
            }

            @Override public void onPageFinished(WebView v, String url) {
                progress.setVisibility(View.GONE);
                injectPerformanceMode();
                injectPopupBlocker();
                injectPlaybackLowMemoryMode();
                injectTvNavigation();
                splash.setVisibility(View.GONE);
                v.requestFocus();
            }

            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String scheme = r.getUrl().getScheme();
                if ("http".equals(scheme) || "https".equals(scheme)) {
                    v.loadUrl(r.getUrl().toString());
                }
                return true;
            }

            @Override public void onReceivedError(WebView v, WebResourceRequest r, android.webkit.WebResourceError e) {
                if (r.isForMainFrame()) {
                    progress.setVisibility(View.GONE);
                    splash.setVisibility(View.GONE);
                    error.setText("Dulo TV couldn't load.\n\nPress OK to retry.");
                    error.setVisibility(View.VISIBLE);
                    error.setFocusable(true);
                    error.requestFocus();
                }
            }

            @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                // Do not let an old/low-memory WebView renderer take the whole Activity with it.
                if (webView != null) {
                    root.removeView(webView);
                    webView.destroy();
                    webView = null;
                }
                error.setText("Dulo TV ran out of video memory.\n\nPress OK to restart.");
                error.setVisibility(View.VISIBLE);
                error.setFocusable(true);
                error.requestFocus();
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress >= 90) progress.setVisibility(View.GONE);
            }

            @Override public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) { callback.onCustomViewHidden(); return; }
                customView = view;
                customViewCallback = callback;
                webView.setVisibility(View.GONE);
                root.addView(view, new FrameLayout.LayoutParams(-1, -1));
                immersive();
            }

            @Override public void onHideCustomView() {
                if (customView == null) return;
                root.removeView(customView);
                customView = null;
                webView.setVisibility(View.VISIBLE);
                if (customViewCallback != null) customViewCallback.onCustomViewHidden();
                customViewCallback = null;
                immersive();
            }
        });
    }

    /** Low-end TV mode: removes GPU-heavy visual effects and makes images lazy/async. */
    private void injectPerformanceMode() {
        String js = "(function(){" +
            "if(window.__duloPerf)return;window.__duloPerf=true;" +
            "var s=document.createElement('style');s.id='dulo-tv-performance';" +
            "s.textContent='" +
            "*,*::before,*::after{" +
            "animation:none!important;transition:none!important;scroll-behavior:auto!important;" +
            "backdrop-filter:none!important;-webkit-backdrop-filter:none!important;" +
            "box-shadow:none!important;text-shadow:none!important;will-change:auto!important}" +
            "[class*=blur],[class*=backdrop],[class*=glass],[class*=glow]{" +
            "filter:none!important;backdrop-filter:none!important;-webkit-backdrop-filter:none!important}" +
            "html{scroll-behavior:auto!important}" +
            "';document.head.appendChild(s);" +
            "function tune(){document.querySelectorAll('img').forEach(function(i){i.loading='lazy';i.decoding='async';});}" +
            "tune();window.__duloPerfObserver=new MutationObserver(tune);window.__duloPerfObserver.observe(document.documentElement,{childList:true,subtree:true});" +
            "})();";
        js(js);
    }

    /** Removes the Join Discord modal/backdrop and removes it again if the SPA recreates it. */
    private void injectPopupBlocker() {
        String js = "(function(){" +
            "if(window.__duloPopupBlocker)return;window.__duloPopupBlocker=true;" +
            "function kill(){" +
            "var nodes=Array.prototype.slice.call(document.querySelectorAll('body *'));" +
            "nodes.forEach(function(el){" +
            "var t=(el.innerText||el.textContent||'').trim().toLowerCase();" +
            "if(t.indexOf('join discord')===-1)return;" +
            "var x=el;for(var n=0;n<7&&x&&x!==document.body;n++,x=x.parentElement){" +
            "var st=getComputedStyle(x),role=(x.getAttribute('role')||'').toLowerCase();" +
            "if(role==='dialog'||st.position==='fixed'){x.remove();break;}" +
            "}" +
            "});" +
            "document.querySelectorAll('[class*=overlay],[class*=modal-backdrop],[data-radix-portal]').forEach(function(x){" +
            "var t=(x.innerText||'').toLowerCase();if(t.indexOf('discord')!==-1)x.remove();});" +
            "document.documentElement.style.overflow='auto';document.body.style.overflow='auto';" +
            "document.body.style.pointerEvents='auto';" +
            "}" +
            "kill();window.__duloPopupObserver=new MutationObserver(kill);window.__duloPopupObserver.observe(document.documentElement,{childList:true,subtree:true});" +
            "})();";
        js(js);
    }

    /**
     * Ultra-low-memory playback mode for 1 GB TV sticks.
     * It activates only after a video starts playing, so normal browsing stays intact.
     * During playback it stops our DOM observers, unloads poster/background images,
     * removes navigation-hint overlays, and reduces compositing work behind the player.
     */
    private void injectPlaybackLowMemoryMode() {
        String js = "(function(){" +
            "if(window.__duloPlaybackLiteInstalled)return;window.__duloPlaybackLiteInstalled=true;" +
            "var active=false;" +
            "function text(e){return((e.innerText||e.textContent||'')+'').toLowerCase();}" +
            "function killHints(){" +
              "Array.prototype.slice.call(document.querySelectorAll('body *')).forEach(function(e){" +
                "var t=text(e);if(!t||t.length>700)return;" +
                "var hint=(t.indexOf('navigation')>=0&&(t.indexOf('hint')>=0||t.indexOf('arrow')>=0||t.indexOf('remote')>=0||t.indexOf('control')>=0))||" +
                         "(t.indexOf('use arrow')>=0)||(t.indexOf('use the arrow')>=0)||(t.indexOf('navigation hints')>=0);" +
                "if(!hint)return;var x=e;for(var i=0;i<5&&x&&x!==document.body;i++,x=x.parentElement){" +
                  "var s=getComputedStyle(x);if(s.position==='fixed'||s.position==='absolute'||x.getAttribute('role')==='dialog'){x.remove();break;}" +
                "}" +
              "});" +
            "}" +
            "function enter(v){if(active)return;active=true;window.__duloPlaybackLite=true;" +
              "killHints();" +
              "try{if(window.__duloPerfObserver)window.__duloPerfObserver.disconnect();}catch(e){}" +
              "try{if(window.__duloPopupObserver)window.__duloPopupObserver.disconnect();}catch(e){}" +
              "document.querySelectorAll('img').forEach(function(i){" +
                "if(v&&v.contains&&v.contains(i))return;i.removeAttribute('srcset');i.removeAttribute('sizes');" +
                "i.setAttribute('data-dulo-old-src',i.getAttribute('src')||'');i.removeAttribute('src');" +
              "});" +
              "document.querySelectorAll('video').forEach(function(x){if(x!==v){try{x.pause();x.removeAttribute('src');x.load();}catch(e){}}});" +
              "var st=document.getElementById('dulo-playback-lite');if(!st){st=document.createElement('style');st.id='dulo-playback-lite';" +
                "st.textContent='body.__dulo_playing *{animation:none!important;transition:none!important;backdrop-filter:none!important;-webkit-backdrop-filter:none!important;box-shadow:none!important;text-shadow:none!important;filter:none!important}'+" +
                "'body.__dulo_playing img{visibility:hidden!important} body.__dulo_playing video{visibility:visible!important;opacity:1!important;filter:none!important}';document.head.appendChild(st);}" +
              "document.body.classList.add('__dulo_playing');" +
            "}" +
            "function leave(){if(!active)return;active=false;window.__duloPlaybackLite=false;document.body.classList.remove('__dulo_playing');}" +
            "function bind(){document.querySelectorAll('video').forEach(function(v){if(v.__duloLiteBound)return;v.__duloLiteBound=true;" +
              "v.addEventListener('playing',function(){enter(v);},{passive:true});" +
              "v.addEventListener('play',function(){enter(v);},{passive:true});" +
              "v.addEventListener('ended',leave,{passive:true});" +
            "});killHints();}" +
            "bind();window.__duloVideoBinder=new MutationObserver(bind);window.__duloVideoBinder.observe(document.documentElement,{childList:true,subtree:true});" +
            "})();";
        js(js);
    }

    private void injectTvNavigation() {
        String js = "(function(){if(window.__duloTv)return;window.__duloTv=true;" +
          "var style=document.createElement('style');style.innerHTML='.__dulo_focus{outline:4px solid #fff!important;outline-offset:3px!important;z-index:2147483646!important}';document.head.appendChild(style);"+
          "function isMenuToggle(e){var tag=(e.tagName||'').toLowerCase(),label=((e.getAttribute('aria-label')||'')+' '+(e.getAttribute('title')||'')+' '+(e.getAttribute('data-testid')||'')).toLowerCase(),hasPopup=(e.getAttribute('aria-haspopup')||'').toLowerCase(),expanded=e.hasAttribute('aria-expanded');if(hasPopup==='menu')return true;if(tag==='button'&&(expanded||label==='menu'||label.indexOf('open menu')>=0||label.indexOf('navigation menu')>=0||label.indexOf('toggle menu')>=0||label.indexOf('hamburger')>=0))return true;return false;}"+
          "function els(){return Array.prototype.slice.call(document.querySelectorAll('a,button,input,select,textarea,[role=button],[tabindex],video')).filter(function(e){var r=e.getBoundingClientRect(),s=getComputedStyle(e);return r.width>2&&r.height>2&&s.visibility!==\"hidden\"&&s.display!==\"none\"&&!e.disabled&&!isMenuToggle(e);});}"+
          "document.addEventListener('keydown',function(ev){var k=ev.key||'';if(k==='ArrowUp'||k==='ArrowDown'||k==='ArrowLeft'||k==='ArrowRight'){ev.preventDefault();ev.stopImmediatePropagation();}},true);"+
          "document.addEventListener('keyup',function(ev){var k=ev.key||'';if(k==='ArrowUp'||k==='ArrowDown'||k==='ArrowLeft'||k==='ArrowRight'){ev.preventDefault();ev.stopImmediatePropagation();}},true);"+
          "function cur(){return document.querySelector('.__dulo_focus')||document.activeElement;}"+
          "function set(e){document.querySelectorAll('.__dulo_focus').forEach(function(x){x.classList.remove('__dulo_focus')});if(e){e.classList.add('__dulo_focus');try{e.focus({preventScroll:true})}catch(x){e.focus()}e.scrollIntoView({block:'center',inline:'center',behavior:'auto'});}}"+
          "function move(d){var a=els(),c=cur();if(!a.length)return;if(!c||a.indexOf(c)<0){set(a[0]);return;}var r=c.getBoundingClientRect(),cx=r.left+r.width/2,cy=r.top+r.height/2,b=null,bs=1e20;a.forEach(function(e){if(e===c)return;var q=e.getBoundingClientRect(),x=q.left+q.width/2,y=q.top+q.height/2,dx=x-cx,dy=y-cy,ok=(d==='l'&&dx< -3)||(d==='r'&&dx>3)||(d==='u'&&dy< -3)||(d==='d'&&dy>3);if(!ok)return;var main=(d==='l'||d==='r')?Math.abs(dx):Math.abs(dy),cross=(d==='l'||d==='r')?Math.abs(dy):Math.abs(dx),score=main+cross*2.2;if(score<bs){bs=score;b=e;}});if(b)set(b);}"+
          "window.__duloMove=move;window.__duloClick=function(){var c=cur();if(c){c.click();}};set(els()[0]);})();";
        js(js);
    }

    private void js(String code) {
        if (webView != null) webView.evaluateJavascript(code, null);
    }

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(e);
        int k = e.getKeyCode();

        if (error.getVisibility() == View.VISIBLE && (k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER)) {
            error.setVisibility(View.GONE);
            if (webView == null) { recreate(); return true; }
            splash.setVisibility(View.VISIBLE);
            webView.setVisibility(View.VISIBLE);
            webView.reload();
            return true;
        }

        if (customView != null) {
            if (k == KeyEvent.KEYCODE_BACK) {
                ((WebChromeClient) webView.getWebChromeClient()).onHideCustomView();
                return true;
            }
            if (k == KeyEvent.KEYCODE_DPAD_LEFT) {
                js("(function(){var v=document.querySelector('video');if(v)v.currentTime=Math.max(0,v.currentTime-10)})()");
                return true;
            }
            if (k == KeyEvent.KEYCODE_DPAD_RIGHT) {
                js("(function(){var v=document.querySelector('video');if(v)v.currentTime+=10})()");
                return true;
            }
            if (k == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || k == KeyEvent.KEYCODE_DPAD_CENTER) {
                js("(function(){var v=document.querySelector('video');if(v){v.paused?v.play():v.pause()}})()");
                return true;
            }
        }

        switch (k) {
            case KeyEvent.KEYCODE_DPAD_LEFT: js("window.__duloMove&&window.__duloMove('l')"); return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT: js("window.__duloMove&&window.__duloMove('r')"); return true;
            case KeyEvent.KEYCODE_DPAD_UP: js("window.__duloMove&&window.__duloMove('u')"); return true;
            case KeyEvent.KEYCODE_DPAD_DOWN: js("window.__duloMove&&window.__duloMove('d')"); return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER: js("window.__duloClick&&window.__duloClick()"); return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                js("(function(){var v=document.querySelector('video');if(v){v.paused?v.play():v.pause()}})()"); return true;
            case KeyEvent.KEYCODE_BACK:
                if (webView.canGoBack()) { webView.goBack(); return true; }
                break;
        }
        return super.dispatchKeyEvent(e);
    }

    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) immersive();
    }

    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (webView != null && level >= TRIM_MEMORY_RUNNING_LOW) {
            // Drop disposable WebView cache and tell the page to shed decoded poster images.
            webView.clearCache(false);
            js("(function(){if(window.__duloPlaybackLite){document.querySelectorAll('img').forEach(function(i){i.removeAttribute('srcset');i.removeAttribute('src');});}})()");
        }
    }

    @Override protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
