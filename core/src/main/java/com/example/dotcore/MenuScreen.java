package com.example.dotcore;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class MenuScreen extends ScreenAdapter {
    private enum Mode { MAIN, SLOTS, SETTINGS, ABOUT }
    private final DotCoreGame game;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new FitViewport(1080,1920,camera);
    private final ShapeRenderer sr = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final Array<Star> stars = new Array<>();
    private final Vector3 pointer = new Vector3();
    private Mode mode = Mode.MAIN;

    private static class Star { float x,y,s,v; Star(float x,float y,float s,float v){this.x=x;this.y=y;this.s=s;this.v=v;} }

    public MenuScreen(DotCoreGame game) {
        this.game=game;
        for(int i=0;i<90;i++) stars.add(new Star(MathUtils.random(0f,1080f),MathUtils.random(0f,1920f),MathUtils.random(1f,3.5f),MathUtils.random(7f,28f)));
        Gdx.input.setInputProcessor(new InputAdapter(){
            @Override public boolean touchDown(int screenX,int screenY,int pointerId,int button){
                Vector3 p=viewport.unproject(new Vector3(screenX,screenY,0));
                click(p.x,p.y); return true;
            }
            @Override public boolean keyDown(int keycode){
                if(keycode== Input.Keys.ESCAPE){ if(mode!=Mode.MAIN) mode=Mode.MAIN; else Gdx.app.exit(); return true;} return false;
            }
        });
    }

    private Rectangle btn(float centerY){ return new Rectangle(250,centerY-55,580,110); }
    private boolean anySave(){ for(int i=1;i<=SaveRepository.SLOT_COUNT;i++) if(game.saves.exists(i)) return true; return false; }

    private void click(float x,float y){
        if(mode==Mode.MAIN){
            if(btn(1120).contains(x,y)){ mode=Mode.SLOTS; return; }
            if(btn(960).contains(x,y) && anySave()){ game.playSlot(game.saves.lastSlot()); return; }
            if(btn(800).contains(x,y)){ mode=Mode.SETTINGS; return; }
            if(btn(640).contains(x,y)){ mode=Mode.ABOUT; return; }
            if(btn(480).contains(x,y)){ Gdx.app.exit(); }
        } else if(mode==Mode.SLOTS){
            for(int i=1;i<=SaveRepository.SLOT_COUNT;i++){
                Rectangle r=new Rectangle(150,1330-(i-1)*190,780,140);
                Rectangle del=new Rectangle(840,r.y+25,70,90);
                if(game.saves.exists(i)&&del.contains(x,y)){game.saves.delete(i);return;}
                if(r.contains(x,y)){ game.playSlot(i); return; }
            }
            if(btn(260).contains(x,y)) mode=Mode.MAIN;
        } else if(mode==Mode.SETTINGS){
            if(new Rectangle(270,1190,540,110).contains(x,y)){
                game.applyLanguage("ru".equals(game.settings.language)?"en":"ru"); return;
            }
            if(new Rectangle(270,1010,540,110).contains(x,y)){ game.settings.sound=!game.settings.sound; game.settings.save(); return; }
            if(new Rectangle(270,830,540,110).contains(x,y)){ game.settings.vibration=!game.settings.vibration; game.settings.save(); return; }
            if(new Rectangle(270,650,540,110).contains(x,y)){ game.settings.highEffects=!game.settings.highEffects; game.settings.save(); return; }
            if(btn(340).contains(x,y)) mode=Mode.MAIN;
        } else if(mode==Mode.ABOUT){ if(btn(340).contains(x,y)) mode=Mode.MAIN; }
    }

    @Override public void render(float delta){
        updateStars(delta);
        viewport.apply();
        camera.update();
        Gdx.gl.glClearColor(Ui.BG.r,Ui.BG.g,Ui.BG.b,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        sr.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.01f,0.02f,0.05f,1);
        sr.rect(0,0,1080,1920);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        for(Star s:stars){
            sr.setColor(0.15f,0.7f,1f,0.18f+s.s*0.08f); sr.circle(s.x,s.y,s.s*2.1f,10);
            sr.setColor(0.7f,0.95f,1f,0.75f); sr.circle(s.x,s.y,s.s*0.55f,8);
        }
        // Decorative reactor core.
        float pulse=1f+MathUtils.sinDeg((System.currentTimeMillis()%6000L)/6000f*360f)*0.08f;
        sr.setColor(0.05f,0.65f,1f,0.08f); sr.circle(540,1550,215*pulse,64);
        sr.setColor(0.05f,0.8f,1f,0.15f); sr.circle(540,1550,145*pulse,64);
        sr.setColor(0.75f,0.98f,1f,0.8f); sr.circle(540,1550,28,32);
        sr.end();

        batch.begin();
        Ui.text(batch,game.assets.font,"DOT//CORE",322,1780,1.65f,Color.WHITE);
        Ui.text(batch,game.assets.font,"ALPHA 0.6 // DRONE & EFFECTS BUILD",348,1700,0.48f,new Color(Ui.CYAN.r,Ui.CYAN.g,Ui.CYAN.b,0.8f));
        batch.end();

        if(mode==Mode.MAIN) drawMain();
        if(mode==Mode.SLOTS) drawSlots();
        if(mode==Mode.SETTINGS) drawSettings();
        if(mode==Mode.ABOUT) drawAbout();
    }

    private void drawButton(Rectangle r,String text,boolean enabled){
        sr.begin(ShapeRenderer.ShapeType.Filled); Ui.button(sr,r,enabled,false); sr.end();
        batch.begin(); Ui.centered(batch,game.assets.font,text,r,0.90f,enabled?Color.WHITE:new Color(0.45f,0.48f,0.53f,1)); batch.end();
    }

    private void drawMain(){
        drawButton(btn(1120),game.assets.t("play"),true);
        drawButton(btn(960),game.assets.t("continue"),anySave());
        drawButton(btn(800),game.assets.t("settings"),true);
        drawButton(btn(640),game.assets.t("about"),true);
        drawButton(btn(480),game.assets.t("exit"),true);
    }

    private void drawSlots(){
        batch.begin(); Ui.text(batch,game.assets.font,game.assets.t("save_slots"),285,1545,1.12f,Color.WHITE); batch.end();
        for(int i=1;i<=SaveRepository.SLOT_COUNT;i++){
            Rectangle r=new Rectangle(150,1330-(i-1)*190,780,140);
            sr.begin(ShapeRenderer.ShapeType.Filled); Ui.button(sr,r,true,false);
            if(game.saves.exists(i)){Rectangle del=new Rectangle(840,r.y+25,70,90);sr.setColor(.35f,.035f,.055f,.92f);sr.rect(del.x,del.y,del.width,del.height);sr.setColor(Ui.RED);line(sr,del.x+20,del.y+25,del.x+50,del.y+65,6);line(sr,del.x+50,del.y+25,del.x+20,del.y+65,6);}
            sr.end();
            SaveData s=game.saves.load(i);
            batch.begin();
            Ui.text(batch,game.assets.font,game.assets.t("slot")+" "+i,r.x+32,r.y+101,0.78f,Ui.CYAN);
            if(game.saves.exists(i)){
                Ui.text(batch,game.assets.font,game.assets.t("last_wave")+": "+s.wave+"     C "+formatCredits(s.credits),r.x+32,r.y+52,0.55f,Color.WHITE);
                Ui.text(batch,game.assets.font,"T "+s.turretCount+"  •  D "+s.droneCount(),r.x+520,r.y+101,0.44f,new Color(.62f,.80f,.90f,1));
            }else Ui.text(batch,game.assets.font,game.assets.t("empty"),r.x+32,r.y+50,0.55f,Color.WHITE);
            batch.end();
        }
        drawButton(btn(260),game.assets.t("back"),true);
    }

    private void drawSettings(){
        batch.begin(); Ui.text(batch,game.assets.font,game.assets.t("settings"),360,1480,1.12f,Color.WHITE); batch.end();
        drawButton(new Rectangle(270,1190,540,110),game.assets.t("language")+": "+("ru".equals(game.settings.language)?game.assets.t("russian"):game.assets.t("english")),true);
        drawButton(new Rectangle(270,1010,540,110),game.assets.t("sound")+": "+(game.settings.sound?game.assets.t("on"):game.assets.t("off")),true);
        drawButton(new Rectangle(270,830,540,110),game.assets.t("vibration")+": "+(game.settings.vibration?game.assets.t("on"):game.assets.t("off")),true);
        drawButton(new Rectangle(270,650,540,110),game.assets.t("effects")+": "+(game.settings.highEffects?game.assets.t("on"):game.assets.t("off")),true);
        drawButton(btn(340),game.assets.t("back"),true);
    }

    private void drawAbout(){
        Rectangle p=new Rectangle(150,650,780,720);
        sr.begin(ShapeRenderer.ShapeType.Filled); Ui.panel(sr,p,Ui.CYAN); sr.end();
        batch.begin();
        Ui.text(batch,game.assets.font,game.assets.t("about"),370,1285,1.08f,Color.WHITE);
        Ui.text(batch,game.assets.font,game.assets.t("about_text"),220,1165,0.58f,new Color(0.8f,0.9f,1f,1));
        Ui.text(batch,game.assets.font,game.assets.t("developer")+": Ponikarov Artem",220,1020,0.58f,Color.WHITE);
        Ui.text(batch,game.assets.font,"enhort@gmail.com",220,955,0.58f,Ui.CYAN);
        Ui.text(batch,game.assets.font,"Finger  /  Turrets  /  Drones\nFire  /  Ice  /  Lightning  /  Gravity\n5 save slots",220,835,0.52f,new Color(.65f,.82f,.92f,1));
        batch.end();
        drawButton(btn(340),game.assets.t("back"),true);
    }


    private void line(ShapeRenderer r,float x1,float y1,float x2,float y2,float width){float dx=x2-x1,dy=y2-y1,len=(float)Math.sqrt(dx*dx+dy*dy),ang=MathUtils.atan2(dy,dx)*MathUtils.radiansToDegrees;r.rect(x1,y1-width/2,0,width,len,width,1,1,ang);}

    private String formatCredits(double value){
        if(value>=1_000_000_000d)return String.format(java.util.Locale.US,"%.1fB",value/1_000_000_000d);
        if(value>=1_000_000d)return String.format(java.util.Locale.US,"%.1fM",value/1_000_000d);
        if(value>=10_000d)return String.format(java.util.Locale.US,"%.1fK",value/1_000d);
        return Long.toString((long)value);
    }
    private void updateStars(float d){ for(Star s:stars){ s.y-=s.v*d; if(s.y<0){s.y=1920;s.x=MathUtils.random(0f,1080f);} } }
    @Override public void resize(int width,int height){ viewport.update(width,height,true); }
    @Override public void dispose(){ sr.dispose(); batch.dispose(); }
}
