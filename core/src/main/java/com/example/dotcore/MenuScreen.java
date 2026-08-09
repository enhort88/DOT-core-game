package com.example.dotcore;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
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
    private enum Mode { MAIN, SLOTS, DIFFICULTY, BESTIARY, SETTINGS, ABOUT, DELETE_CONFIRM }
    private final DotCoreGame game;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new FitViewport(1080,1920,camera);
    private final ShapeRenderer sr = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final Array<Star> stars = new Array<>();
    private final Array<MenuEnemy> menuEnemies = new Array<>();
    private final Vector3 pointer = new Vector3();
    private Mode mode = Mode.MAIN;
    private int pendingDeleteSlot = -1;
    private int pendingNewSlot = -1;
    private int dotTapCount = 0;
    private float dotTapWindow = 0f;
    private float cheatMessageTime = 0f;
    private String cheatMessage = "";
    private int draggingSlider = 0; // 1=sfx, 2=music

    private static class Star { float x,y,s,v; Star(float x,float y,float s,float v){this.x=x;this.y=y;this.s=s;this.v=v;} }
    private static class MenuEnemy { float x,y,r,vx,phase; int type; MenuEnemy(float x,float y,float r,float vx,float phase,int type){this.x=x;this.y=y;this.r=r;this.vx=vx;this.phase=phase;this.type=type;} }

    public MenuScreen(DotCoreGame game) {
        this.game=game;
        for(int i=0;i<90;i++) stars.add(new Star(MathUtils.random(0f,1080f),MathUtils.random(0f,1920f),MathUtils.random(1f,3.5f),MathUtils.random(7f,28f)));
        for(int i=0;i<7;i++) menuEnemies.add(new MenuEnemy(MathUtils.random(70f,1010f),MathUtils.random(390f,1390f),MathUtils.random(18f,38f),MathUtils.randomBoolean()?MathUtils.random(16f,34f):-MathUtils.random(16f,34f),MathUtils.random(0f,MathUtils.PI2),i%4));
        Gdx.input.setInputProcessor(new InputAdapter(){
            @Override public boolean touchDown(int screenX,int screenY,int pointerId,int button){
                Vector3 p=viewport.unproject(new Vector3(screenX,screenY,0));
                if(mode==Mode.SETTINGS && beginSlider(p.x,p.y)) return true;
                click(p.x,p.y); return true;
            }
            @Override public boolean touchDragged(int screenX,int screenY,int pointerId){
                if(draggingSlider==0) return false;
                Vector3 p=viewport.unproject(new Vector3(screenX,screenY,0));
                updateSlider(p.x); return true;
            }
            @Override public boolean touchUp(int screenX,int screenY,int pointerId,int button){ draggingSlider=0; return false; }
            @Override public boolean keyDown(int keycode){
                if(keycode==Input.Keys.ESCAPE || keycode==Input.Keys.BACK){
                    if(mode==Mode.DELETE_CONFIRM){pendingDeleteSlot=-1;mode=Mode.SLOTS;return true;}
                    if(mode==Mode.DIFFICULTY){pendingNewSlot=-1;mode=Mode.SLOTS;return true;}
                    if(mode==Mode.BESTIARY){mode=Mode.MAIN;return true;}
                    if(mode==Mode.SLOTS||mode==Mode.SETTINGS||mode==Mode.ABOUT){mode=Mode.MAIN;return true;}
                    // Main menu has an explicit Exit button; Android Back should not accidentally close the game.
                    return true;
                }
                return false;
            }
        });
        Gdx.input.setCatchKey(Input.Keys.BACK,true);
    }

    private Rectangle btn(float centerY){ return new Rectangle(250,centerY-55,580,110); }
    private boolean anySave(){ for(int i=1;i<=SaveRepository.SLOT_COUNT;i++) if(game.saves.exists(i)) return true; return false; }

    private void click(float x,float y){
        if(mode==Mode.MAIN){
            // Easter egg: ten taps on the word DOT toggle free purchases.
            if(new Rectangle(305,1695,205,115).contains(x,y)){
                if(dotTapWindow<=0f)dotTapCount=0;
                dotTapWindow=6.0f;
                dotTapCount++;
                if(dotTapCount>=10){
                    game.settings.cheatsEnabled=!game.settings.cheatsEnabled;
                    game.settings.save();
                    cheatMessage=game.assets.t(game.settings.cheatsEnabled?"cheats_on":"cheats_off");
                    cheatMessageTime=2.8f;
                    dotTapCount=0;dotTapWindow=0f;
                    game.assets.play(game.assets.buy,game.settings,.35f);
                }
                return;
            }
            if(btn(1180).contains(x,y)){ mode=Mode.SLOTS; return; }
            if(btn(1035).contains(x,y) && anySave()){ game.playSlot(game.saves.lastSlot()); return; }
            if(btn(890).contains(x,y)){ mode=Mode.BESTIARY; return; }
            if(btn(745).contains(x,y)){ mode=Mode.SETTINGS; return; }
            if(btn(600).contains(x,y)){ mode=Mode.ABOUT; return; }
            if(btn(455).contains(x,y)){ Gdx.app.exit(); }
        } else if(mode==Mode.SLOTS){
            for(int i=1;i<=SaveRepository.SLOT_COUNT;i++){
                Rectangle r=new Rectangle(150,1330-(i-1)*190,780,140);
                Rectangle del=new Rectangle(835,r.y+22,82,96);
                if(game.saves.exists(i)&&del.contains(x,y)){pendingDeleteSlot=i;mode=Mode.DELETE_CONFIRM;return;}
                if(r.contains(x,y)){ if(game.saves.exists(i))game.playSlot(i); else {pendingNewSlot=i;mode=Mode.DIFFICULTY;} return; }
            }
            if(btn(260).contains(x,y)) mode=Mode.MAIN;
        } else if(mode==Mode.DIFFICULTY){
            if(new Rectangle(170,1120,740,150).contains(x,y)){game.playNewSlot(pendingNewSlot,0);return;}
            if(new Rectangle(170,885,740,150).contains(x,y)){game.playNewSlot(pendingNewSlot,1);return;}
            if(new Rectangle(170,650,740,150).contains(x,y)){game.playNewSlot(pendingNewSlot,2);return;}
            if(btn(340).contains(x,y)){pendingNewSlot=-1;mode=Mode.SLOTS;return;}
        } else if(mode==Mode.BESTIARY){
            if(btn(210).contains(x,y)){mode=Mode.MAIN;return;}
        } else if(mode==Mode.SETTINGS){
            if(new Rectangle(270,1280,540,110).contains(x,y)){
                game.applyLanguage("ru".equals(game.settings.language)?"en":"ru"); return;
            }
            if(new Rectangle(270,650,540,110).contains(x,y)){ game.settings.vibration=!game.settings.vibration; game.settings.save(); return; }
            if(new Rectangle(270,510,540,110).contains(x,y)){ game.settings.highEffects=!game.settings.highEffects; game.settings.save(); return; }
            if(new Rectangle(270,370,540,110).contains(x,y)){ game.showIntroAgain(); return; }
            if(btn(210).contains(x,y)) mode=Mode.MAIN;
        } else if(mode==Mode.ABOUT){ if(btn(340).contains(x,y)) mode=Mode.MAIN; }
        else if(mode==Mode.DELETE_CONFIRM){
            if(new Rectangle(180,720,330,120).contains(x,y)){ if(pendingDeleteSlot>0)game.saves.delete(pendingDeleteSlot); pendingDeleteSlot=-1;mode=Mode.SLOTS;return; }
            if(new Rectangle(570,720,330,120).contains(x,y)){pendingDeleteSlot=-1;mode=Mode.SLOTS;return;}
        }
    }

    @Override public void render(float delta){
        updateStars(delta);updateMenuEnemies(delta);
        if(dotTapWindow>0f){dotTapWindow-=delta;if(dotTapWindow<=0f)dotTapCount=0;}
        if(cheatMessageTime>0f)cheatMessageTime-=delta;
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
        if(mode==Mode.MAIN) drawMenuEnemies();
        sr.end();

        batch.begin();
        Ui.text(batch,game.assets.font,"DOT//CORE",322,1780,1.65f,Color.WHITE);
        Ui.text(batch,game.assets.font,"ALPHA 0.12.1 // ECOLOGY PATCH",350,1700,0.54f,new Color(Ui.CYAN.r,Ui.CYAN.g,Ui.CYAN.b,0.8f));
        if(mode==Mode.MAIN && game.settings.playerName!=null&&!game.settings.playerName.trim().isEmpty())
            Ui.centered(batch,game.assets.font,game.assets.t("welcome_back")+", "+game.settings.playerName,new Rectangle(150,1565,780,82),.70f,new Color(.72f,.88f,1f,1));
        if(cheatMessageTime>0f){
            Ui.centered(batch,game.assets.font,cheatMessage,new Rectangle(150,1420,780,110),.96f,game.settings.cheatsEnabled?Ui.GREEN:Ui.RED);
        }
        batch.end();

        if(mode==Mode.MAIN) drawMain();
        if(mode==Mode.SLOTS) drawSlots();
        if(mode==Mode.DIFFICULTY) drawDifficulty();
        if(mode==Mode.BESTIARY) drawBestiary();
        if(mode==Mode.SETTINGS) drawSettings();
        if(mode==Mode.ABOUT) drawAbout();
        if(mode==Mode.DELETE_CONFIRM) drawDeleteConfirm();
    }

    private void drawButton(Rectangle r,String text,boolean enabled){
        sr.begin(ShapeRenderer.ShapeType.Filled); Ui.button(sr,r,enabled,false); sr.end();
        batch.begin(); Ui.centered(batch,game.assets.font,text,r,1.00f,enabled?Color.WHITE:new Color(0.45f,0.48f,0.53f,1)); batch.end();
    }

    private void drawMain(){
        drawButton(btn(1180),game.assets.t("play"),true);
        drawButton(btn(1035),game.assets.t("continue"),anySave());
        drawButton(btn(890),game.assets.t("bestiary"),true);
        drawButton(btn(745),game.assets.t("settings"),true);
        drawButton(btn(600),game.assets.t("about"),true);
        drawButton(btn(455),game.assets.t("exit"),true);
    }

    private void drawSlots(){
        batch.begin(); Ui.text(batch,game.assets.font,game.assets.t("save_slots"),285,1545,1.12f,Color.WHITE); batch.end();
        for(int i=1;i<=SaveRepository.SLOT_COUNT;i++){
            Rectangle r=new Rectangle(150,1330-(i-1)*190,780,140);
            sr.begin(ShapeRenderer.ShapeType.Filled); Ui.button(sr,r,true,false);
            if(game.saves.exists(i)){Rectangle del=new Rectangle(835,r.y+22,82,96);Ui.button(sr,del,true,false);}
            sr.end();
            SaveData s=game.saves.load(i);
            batch.begin();
            Ui.text(batch,game.assets.font,game.assets.t("slot")+" "+i+(game.settings.playerName==null||game.settings.playerName.isEmpty()?"":"  •  "+game.settings.playerName),r.x+32,r.y+104,0.76f,Ui.CYAN);
            if(game.saves.exists(i)){
                Ui.text(batch,game.assets.font,game.assets.t("last_wave")+": "+s.wave+"     C "+formatCredits(s.credits),r.x+32,r.y+52,0.64f,Color.WHITE);
                Ui.text(batch,game.assets.font,"T "+s.turretCount+"  •  D "+s.droneCount(),r.x+510,r.y+103,0.52f,new Color(.62f,.80f,.90f,1));
                Texture delIcon=game.assets.icon("delete_save");if(delIcon!=null)batch.draw(delIcon,846,r.y+36,60,60);
            }else Ui.text(batch,game.assets.font,game.assets.t("empty"),r.x+32,r.y+50,0.64f,Color.WHITE);
            batch.end();
        }
        drawButton(btn(260),game.assets.t("back"),true);
    }

    private void drawDifficulty(){
        batch.begin();Ui.centered(batch,game.assets.font,game.assets.t("choose_difficulty"),new Rectangle(120,1450,840,120),1.14f,Color.WHITE);batch.end();
        drawDifficultyCard(new Rectangle(170,1120,740,150),game.assets.t("difficulty_recon"),game.assets.t("difficulty_recon_desc"),new Color(.26f,.82f,1f,1));
        drawDifficultyCard(new Rectangle(170,885,740,150),game.assets.t("difficulty_invasion"),game.assets.t("difficulty_invasion_desc"),Ui.GOLD);
        drawDifficultyCard(new Rectangle(170,650,740,150),game.assets.t("difficulty_apocalypse"),game.assets.t("difficulty_apocalypse_desc"),Ui.RED);
        drawButton(btn(340),game.assets.t("back"),true);
    }
    private void drawDifficultyCard(Rectangle r,String title,String desc,Color accent){
        sr.begin(ShapeRenderer.ShapeType.Filled);sr.setColor(.012f,.030f,.043f,1);sr.rect(r.x,r.y,r.width,r.height);sr.setColor(accent);sr.rect(r.x,r.y,4,r.height);sr.end();
        batch.begin();Ui.text(batch,game.assets.font,title,r.x+34,r.y+108,.78f,accent);Ui.text(batch,game.assets.font,desc,r.x+34,r.y+52,.53f,new Color(.78f,.88f,.94f,1));batch.end();
    }

    private void drawBestiary(){
        long[] c=bestCounts();String[] names={"basic","fast","tank","elite","star","guardian","phase","fire_resist","ice_resist","lightning_resist","element_ward","infector","boss_catalog"};
        batch.begin();
        Ui.centered(batch,game.assets.font,game.assets.t("bestiary"),new Rectangle(120,1585,840,82),1.06f,Color.WHITE);
        Ui.centered(batch,game.assets.font,game.assets.t("bestiary_hint"),new Rectangle(105,1518,870,48),.43f,new Color(.58f,.78f,.9f,1));
        batch.end();
        for(int i=0;i<names.length;i++){
            int col=i%2,row=i/2;
            Rectangle r=new Rectangle(70+col*480,1340-row*170,450,148);
            long kills=game.settings.cheatsEnabled?999:c[i];
            boolean known=kills>=50||i==12&&kills>=1;
            boolean full=game.settings.cheatsEnabled||kills>=100||i==12&&kills>=1;
            Color accent=known?bestiaryColor(i):new Color(.18f,.25f,.31f,1);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(.010f,.025f,.037f,1);sr.rect(r.x,r.y,r.width,r.height);
            sr.setColor(accent);sr.rect(r.x,r.y,3,r.height);
            drawBestiarySpecimen(r.x+58f,r.y+74f,i,known,accent);
            sr.end();
            batch.begin();
            String title=known?game.assets.t("enemy_"+names[i]):"???";
            Ui.text(batch,game.assets.font,title,r.x+116,r.y+107,.54f,known?Color.WHITE:new Color(.45f,.52f,.58f,1));
            String progress=i==12?(known?game.assets.t("catalog_identified"):game.assets.t("kills")+" "+kills+"/1"):(game.assets.t("kills")+" "+Math.min(kills,100)+"/100");
            Ui.text(batch,game.assets.font,progress,r.x+116,r.y+66,.38f,Ui.CYAN);
            if(known)Ui.text(batch,game.assets.font,game.assets.t(full?"enemy_"+names[i]+"_weak":"enemy_"+names[i]+"_desc"),r.x+116,r.y+29,.285f,new Color(.68f,.80f,.88f,1));
            batch.end();
        }
        drawButton(btn(185),game.assets.t("back"),true);
    }

    private void drawBestiarySpecimen(float cx,float cy,int i,boolean known,Color accent){
        // Each unlocked entry uses the same visual language as the battlefield enemy,
        // rather than a generic circular placeholder.
        sr.setColor(.008f,.025f,.040f,1);sr.circle(cx,cy,47,32);
        if(!known){
            sr.setColor(.09f,.13f,.17f,1);sr.circle(cx,cy,31,28);
            sr.setColor(.18f,.25f,.31f,1);sr.circle(cx,cy,19,24);
            return;
        }
        float t=saveTimeForMenu();
        sr.setColor(accent.r,accent.g,accent.b,.10f);sr.circle(cx,cy,43,32);
        sr.setColor(accent.r,accent.g,accent.b,.30f);sr.circle(cx,cy,35,32);
        sr.setColor(.012f,.030f,.055f,1);
        switch(i){
            case 0 -> { // Sphere
                sr.circle(cx,cy,28,36);sr.setColor(accent);sr.circle(cx,cy,7,20);sr.setColor(1,1,1,.65f);sr.circle(cx-7,cy+8,3,12);
            }
            case 1 -> { // Interceptor
                menuPolygon(cx,cy,31,3,-MathUtils.PI/2f);sr.setColor(accent);menuPolygon(cx,cy,10,3,-MathUtils.PI/2f);sr.setColor(1,1,1,.55f);sr.circle(cx,cy+4,3,10);
            }
            case 2 -> { // Tank
                menuPolygon(cx,cy,30,4,MathUtils.PI/4f);sr.setColor(accent);menuPolygon(cx,cy,9,4,MathUtils.PI/4f);for(int k=0;k<4;k++){float a=k*MathUtils.PI2/4f+t*.25f;sr.circle(cx+MathUtils.cos(a)*35,cy+MathUtils.sin(a)*35,3,8);}
            }
            case 3 -> { // Elite
                menuPolygon(cx,cy,30,5,t*.08f);sr.setColor(accent);menuPolygon(cx,cy,9,5,-t*.15f);
            }
            case 4 -> { // Star artillery
                menuStar(cx,cy,33,14,5,t*.15f);sr.setColor(Ui.GOLD);sr.circle(cx,cy,6,16);
            }
            case 5 -> { // Guardian
                menuPolygon(cx,cy,29,6,t*.08f);sr.setColor(accent);sr.circle(cx,cy,7,16);for(int k=0;k<10;k++){float a=k*MathUtils.PI2/10f-t*.35f;sr.circle(cx+MathUtils.cos(a)*41,cy+MathUtils.sin(a)*41,2.6f,8);}
            }
            case 6 -> { // Phase unit + tap shield
                menuPolygon(cx,cy,27,4,MathUtils.PI/4f+t*.1f);sr.setColor(accent);menuPolygon(cx,cy,8,4,MathUtils.PI/4f);sr.setColor(.55f,.92f,1f,.55f);for(int k=0;k<8;k++){float a=k*MathUtils.PI2/8f+t*.45f;sr.circle(cx+MathUtils.cos(a)*40,cy+MathUtils.sin(a)*40,2.8f,8);}
            }
            case 7,8,9 -> { // Element-resistant variants
                int sides=i==7?5:i==8?6:4;menuPolygon(cx,cy,28,sides,t*.09f);Color c=i==7?new Color(1f,.28f,.05f,1):i==8?new Color(.35f,.85f,1f,1):new Color(.55f,.45f,1f,1);sr.setColor(c);sr.circle(cx,cy,6,16);for(int k=0;k<3;k++){float a=k*MathUtils.PI2/3f+t*.85f;menuPolygon(cx+MathUtils.cos(a)*39,cy+MathUtils.sin(a)*39,5.5f,4,a);}
            }
            case 10 -> { // Element ward
                menuPolygon(cx,cy,29,6,t*.08f);Color[] cs={new Color(1f,.28f,.05f,1),new Color(.35f,.85f,1f,1),new Color(.55f,.45f,1f,1)};for(int k=0;k<9;k++){float a=k*MathUtils.PI2/9f+t*.38f;Color c=cs[k%3];sr.setColor(c);sr.circle(cx+MathUtils.cos(a)*41,cy+MathUtils.sin(a)*41,2.8f,8);}sr.setColor(accent);sr.circle(cx,cy,6,16);
            }
            case 11 -> { // Infector / parasite ship
                menuStar(cx,cy,30,18,6,-t*.16f);sr.setColor(.92f,.16f,1f,1);sr.circle(cx,cy,7,18);for(int k=0;k<5;k++){float a=k*MathUtils.PI2/5f-t*.9f;sr.circle(cx+MathUtils.cos(a)*38,cy+MathUtils.sin(a)*38,3.2f,8);}
            }
            case 12 -> { // Boss command ship
                menuPolygon(cx,cy,34,7,t*.06f);sr.setColor(Ui.RED);menuPolygon(cx,cy,13,3,-MathUtils.PI/2f);sr.setColor(1f,.75f,.75f,.65f);sr.circle(cx-7,cy+9,3.2f,10);
            }
        }
    }

    private void menuPolygon(float cx,float cy,float r,int sides,float rot){
        for(int k=0;k<sides;k++){float a1=rot+k*MathUtils.PI2/sides,a2=rot+(k+1)*MathUtils.PI2/sides;sr.triangle(cx,cy,cx+MathUtils.cos(a1)*r,cy+MathUtils.sin(a1)*r,cx+MathUtils.cos(a2)*r,cy+MathUtils.sin(a2)*r);}
    }
    private void menuStar(float cx,float cy,float outer,float inner,int points,float rot){
        int n=points*2;for(int k=0;k<n;k++){float a1=rot+k*MathUtils.PI2/n,a2=rot+(k+1)*MathUtils.PI2/n;float r1=k%2==0?outer:inner,r2=(k+1)%2==0?outer:inner;sr.triangle(cx,cy,cx+MathUtils.cos(a1)*r1,cy+MathUtils.sin(a1)*r1,cx+MathUtils.cos(a2)*r2,cy+MathUtils.sin(a2)*r2);}
    }

    private float saveTimeForMenu(){return (System.currentTimeMillis()%12000L)/12000f*MathUtils.PI2;}

    private Color bestiaryColor(int i){return switch(i){case 4->Ui.GOLD;case 5->Ui.GREEN;case 6->Ui.CYAN;case 7->new Color(1f,.3f,.08f,1);case 8->new Color(.35f,.85f,1f,1);case 9->new Color(.55f,.45f,1f,1);case 10->new Color(.7f,.3f,1f,1);case 11->new Color(.92f,.16f,1f,1);case 12->Ui.RED;default->new Color(.35f,.72f,1f,1);};}
    private long[] bestCounts(){long[] a=new long[13];for(int slot=1;slot<=SaveRepository.SLOT_COUNT;slot++){if(!game.saves.exists(slot))continue;SaveData s=game.saves.load(slot);a[0]+=s.bestiaryBasic;a[1]+=s.bestiaryFast;a[2]+=s.bestiaryTank;a[3]+=s.bestiaryElite;a[4]+=s.bestiaryStar;a[5]+=s.bestiaryGuardian;a[6]+=s.bestiaryPhase;a[7]+=s.bestiaryFireResist;a[8]+=s.bestiaryIceResist;a[9]+=s.bestiaryLightningResist;a[10]+=s.bestiaryWard;a[11]+=s.bestiaryInfector;a[12]+=s.bestiaryBoss;}return a;}

    private void drawSettings(){
        batch.begin(); Ui.text(batch,game.assets.font,game.assets.t("settings"),360,1545,1.16f,Color.WHITE); batch.end();
        drawButton(new Rectangle(270,1280,540,110),game.assets.t("language")+": "+("ru".equals(game.settings.language)?game.assets.t("russian"):game.assets.t("english")),true);
        drawSlider(game.assets.t("sound_volume"),game.settings.soundVolume,1080);
        drawSlider(game.assets.t("music_volume"),game.settings.musicVolume,880);
        drawButton(new Rectangle(270,650,540,110),game.assets.t("vibration")+": "+(game.settings.vibration?game.assets.t("on"):game.assets.t("off")),true);
        drawButton(new Rectangle(270,510,540,110),game.assets.t("effects")+": "+(game.settings.highEffects?game.assets.t("on"):game.assets.t("off")),true);
        drawButton(new Rectangle(270,370,540,110),game.assets.t("show_intro"),true);
        drawButton(btn(210),game.assets.t("back"),true);
    }

    private void drawSlider(String label,float value,float y){
        float x=270,w=540,trackY=y+26;
        batch.begin();
        Ui.text(batch,game.assets.font,label,x,y+92,.76f,Color.WHITE);
        String pct=Math.round(value*100f)+"%";float tw=game.assets.font.width(pct,.66f);Ui.text(batch,game.assets.font,pct,x+w-tw,y+92,.66f,Ui.CYAN);
        batch.end();
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(.025f,.055f,.085f,.96f);sr.rect(x,trackY,w,20);
        sr.setColor(Ui.CYAN.r,Ui.CYAN.g,Ui.CYAN.b,.25f);sr.rect(x,trackY,w*value,20);
        sr.setColor(Ui.CYAN);sr.rect(x,trackY,w*value,20);
        float knobX=x+w*value;sr.setColor(.03f,.12f,.18f,1);sr.circle(knobX,trackY+10,23,24);sr.setColor(Color.WHITE);sr.circle(knobX,trackY+10,10,20);
        sr.end();
    }

    private boolean beginSlider(float x,float y){
        if(new Rectangle(240,1085,600,85).contains(x,y)){draggingSlider=1;updateSlider(x);return true;}
        if(new Rectangle(240,885,600,85).contains(x,y)){draggingSlider=2;updateSlider(x);return true;}
        return false;
    }

    private void updateSlider(float x){
        float value=MathUtils.clamp((x-270f)/540f,0f,1f);
        if(draggingSlider==1){game.settings.soundVolume=value;game.settings.sound=value>.001f;}
        else if(draggingSlider==2){game.settings.musicVolume=value;game.assets.syncMusic(game.settings);}
        game.settings.save();
    }

    private void drawAbout(){
        Rectangle p=new Rectangle(130,650,820,720);
        sr.begin(ShapeRenderer.ShapeType.Filled); Ui.panel(sr,p,Ui.CYAN); sr.end();
        batch.begin();
        Ui.text(batch,game.assets.font,game.assets.t("about"),355,1290,1.18f,Color.WHITE);
        Ui.text(batch,game.assets.font,game.assets.t("about_text"),205,1150,0.72f,new Color(0.82f,0.92f,1f,1));
        Ui.text(batch,game.assets.font,game.assets.t("developer")+": Ponikarov Artem",205,975,0.68f,Color.WHITE);
        Ui.text(batch,game.assets.font,"enhort@gmail.com",205,895,0.68f,Ui.CYAN);
        Ui.text(batch,game.assets.font,"DOT//CORE  Alpha 0.12.1",205,795,0.58f,new Color(.64f,.82f,.94f,1));
        batch.end();
        drawButton(btn(340),game.assets.t("back"),true);
    }

    private void drawDeleteConfirm(){
        sr.begin(ShapeRenderer.ShapeType.Filled);sr.setColor(0,0,0,.66f);sr.rect(0,0,1080,1920);Rectangle p=new Rectangle(125,620,830,500);Ui.panel(sr,p,Ui.RED);sr.end();
        batch.begin();Ui.centered(batch,game.assets.font,game.assets.t("delete_confirm"),new Rectangle(155,940,770,115),1.02f,Color.WHITE);Ui.centered(batch,game.assets.font,game.assets.t("slot")+" "+pendingDeleteSlot,new Rectangle(155,855,770,80),.78f,Ui.CYAN);batch.end();
        drawButton(new Rectangle(180,720,330,120),game.assets.t("delete_yes"),true);drawButton(new Rectangle(570,720,330,120),game.assets.t("delete_no"),true);
    }

    private void drawMenuEnemies(){
        for(MenuEnemy e:menuEnemies){Color c=switch(e.type){case 1->new Color(1f,.31f,.22f,1);case 2->new Color(.78f,.25f,1f,1);case 3->new Color(.20f,.94f,1f,1);default->new Color(.25f,.72f,1f,1);};float pulse=1f+.04f*MathUtils.sin(e.phase);sr.setColor(c.r,c.g,c.b,.035f);sr.circle(e.x,e.y,e.r*1.75f*pulse,32);sr.setColor(c.r,c.g,c.b,.14f);sr.circle(e.x,e.y,e.r*1.25f*pulse,28);sr.setColor(c.r,c.g,c.b,.62f);sr.circle(e.x,e.y,e.r,28);sr.setColor(.01f,.028f,.055f,.94f);sr.circle(e.x,e.y,e.r*.78f,28);sr.setColor(c.r,c.g,c.b,.35f);sr.circle(e.x,e.y,e.r*.48f,24);sr.setColor(1,1,1,.55f);sr.circle(e.x-e.r*.25f,e.y+e.r*.25f,Math.max(2f,e.r*.10f),12);}
    }



    private void line(ShapeRenderer r,float x1,float y1,float x2,float y2,float width){float dx=x2-x1,dy=y2-y1,len=(float)Math.sqrt(dx*dx+dy*dy),ang=MathUtils.atan2(dy,dx)*MathUtils.radiansToDegrees;r.rect(x1,y1-width/2,0,width,len,width,1,1,ang);}

    private String formatCredits(double value){
        if(value>=1_000_000_000d)return String.format(java.util.Locale.US,"%.1fB",value/1_000_000_000d);
        if(value>=1_000_000d)return String.format(java.util.Locale.US,"%.1fM",value/1_000_000d);
        if(value>=10_000d)return String.format(java.util.Locale.US,"%.1fK",value/1_000d);
        return Long.toString((long)value);
    }
    private void updateMenuEnemies(float d){for(MenuEnemy e:menuEnemies){e.x+=e.vx*d;e.phase+=d*(.7f+Math.abs(e.vx)*.01f);e.y+=MathUtils.sin(e.phase)*2.2f*d;if(e.x<-70)e.x=1150;if(e.x>1150)e.x=-70;}}
    private void updateStars(float d){ for(Star s:stars){ s.y-=s.v*d; if(s.y<0){s.y=1920;s.x=MathUtils.random(0f,1080f);} } }
    @Override public void resize(int width,int height){ viewport.update(width,height,true); }
    @Override public void dispose(){ sr.dispose(); batch.dispose(); }
}
