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
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/** First-run profile entry + story briefing, fully styled inside the game. */
public class IntroScreen extends ScreenAdapter {
    private final DotCoreGame game;
    private final boolean replay;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new FitViewport(1080,1920,camera);
    private final ShapeRenderer sr = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private int page = 0;
    private boolean nameEntry;
    private String typedName = "";
    private float time = 0f;

    private final Rectangle nameBox = new Rectangle(170,1010,740,150);
    private final Rectangle continueName = new Rectangle(250,760,580,120);

    public IntroScreen(DotCoreGame game, boolean replay) {
        this.game=game;this.replay=replay;
        typedName=game.settings.playerName==null?"":game.settings.playerName.trim();
        nameEntry=!replay && typedName.isEmpty();
        if(nameEntry) showKeyboard();
        Gdx.input.setInputProcessor(new InputAdapter(){
            @Override public boolean touchDown(int sx,int sy,int pointer,int button){
                Vector3 p=viewport.unproject(new Vector3(sx,sy,0));
                if(nameEntry){
                    if(nameBox.contains(p.x,p.y)){showKeyboard();return true;}
                    if(continueName.contains(p.x,p.y) && !typedName.trim().isEmpty()){acceptName();return true;}
                    return true;
                }
                if(new Rectangle(790,70,220,110).contains(p.x,p.y)){finish();return true;}
                if(new Rectangle(70,70,300,110).contains(p.x,p.y)&&replay){startNameEdit();return true;}
                page++;if(page>3)finish();return true;
            }
            @Override public boolean keyTyped(char ch){
                if(!nameEntry)return false;
                if(ch=='\b'){if(!typedName.isEmpty())typedName=typedName.substring(0,typedName.length()-1);return true;}
                if(ch=='\r'||ch=='\n'){if(!typedName.trim().isEmpty())acceptName();return true;}
                if(!Character.isISOControl(ch) && typedName.length()<18){typedName+=ch;return true;}
                return false;
            }
            @Override public boolean keyDown(int keycode){
                if(nameEntry && keycode==Input.Keys.DEL){
                    if(!typedName.isEmpty())typedName=typedName.substring(0,typedName.length()-1);return true;
                }
                if(keycode==Input.Keys.ESCAPE){if(nameEntry && replay){nameEntry=false;hideKeyboard();return true;}finish();return true;}return false;
            }
        });
    }

    private void startNameEdit(){typedName=game.settings.playerName==null?"":game.settings.playerName;nameEntry=true;showKeyboard();}
    private void showKeyboard(){try{Gdx.input.setOnscreenKeyboardVisible(true);}catch(Exception ignored){}}
    private void hideKeyboard(){try{Gdx.input.setOnscreenKeyboardVisible(false);}catch(Exception ignored){}}
    private void acceptName(){String v=typedName.trim();if(v.isEmpty())return;if(v.length()>18)v=v.substring(0,18);game.settings.playerName=v;game.settings.save();typedName=v;nameEntry=false;hideKeyboard();page=0;}
    private void finish(){hideKeyboard();game.settings.introSeen=true;game.settings.save();game.openMenu();}

    @Override public void render(float delta){
        time+=delta;viewport.apply();camera.update();Gdx.gl.glClearColor(.004f,.008f,.025f,1);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        sr.setProjectionMatrix(camera.combined);batch.setProjectionMatrix(camera.combined);Gdx.gl.glEnable(GL20.GL_BLEND);
        sr.begin(ShapeRenderer.ShapeType.Filled);drawSpace();sr.end();
        if(nameEntry) drawNameEntry(); else drawBriefing();
    }

    private void drawNameEntry(){
        sr.begin(ShapeRenderer.ShapeType.Filled);
        // Earth horizon / command terminal frame.
        sr.setColor(.02f,.12f,.22f,.28f);sr.circle(540,-85,780,80);
        sr.setColor(.025f,.065f,.105f,.96f);sr.rect(95,620,890,850);
        sr.setColor(Ui.CYAN.r,Ui.CYAN.g,Ui.CYAN.b,.22f);sr.rect(95,620,890,4);sr.rect(95,1466,890,4);
        sr.setColor(.01f,.025f,.045f,1);sr.rect(nameBox.x,nameBox.y,nameBox.width,nameBox.height);
        sr.setColor(Ui.CYAN.r,Ui.CYAN.g,Ui.CYAN.b,.55f);outline(nameBox,4f);
        Ui.button(sr,continueName,!typedName.trim().isEmpty(),false);
        sr.end();

        batch.begin();
        Ui.centered(batch,game.assets.font,"DOT//CORE",new Rectangle(150,1630,780,130),1.55f,Color.WHITE);
        Ui.centered(batch,game.assets.font,game.assets.t("enter_name_title"),new Rectangle(110,1320,860,150),.88f,new Color(.85f,.95f,1f,1));
        String shown=typedName.isEmpty()?game.assets.t("enter_name_hint"):typedName;
        Color c=typedName.isEmpty()?new Color(.40f,.55f,.65f,1):Color.WHITE;
        Ui.centered(batch,game.assets.font,shown,new Rectangle(nameBox.x+20,nameBox.y+5,nameBox.width-40,nameBox.height-10),1.00f,c);
        if(!typedName.isEmpty() && ((int)(time*2f)%2==0)){
            float tw=game.assets.font.width(typedName,1.0f);float cx=Math.min(nameBox.x+nameBox.width-50,nameBox.x+(nameBox.width-tw)/2f+tw+14);
            Ui.text(batch,game.assets.font,"|",cx,nameBox.y+100,.72f,Ui.CYAN);
        }
        Ui.centered(batch,game.assets.font,game.assets.t("continue"),continueName,1.00f,!typedName.trim().isEmpty()?Color.WHITE:new Color(.42f,.46f,.52f,1));
        Ui.centered(batch,game.assets.font,"DOT//CORE // FIRST CONTACT",new Rectangle(190,650,700,70),.50f,new Color(Ui.CYAN.r,Ui.CYAN.g,Ui.CYAN.b,.78f));
        batch.end();
    }

    private void drawBriefing(){
        sr.begin(ShapeRenderer.ShapeType.Filled);drawBriefingFrame();sr.end();
        batch.begin();
        Ui.centered(batch,game.assets.font,"DOT//CORE // "+game.assets.t("briefing"),new Rectangle(100,1715,880,105),1.12f,Color.WHITE);
        drawPagePortraits();
        String name=(game.settings.playerName==null||game.settings.playerName.trim().isEmpty())?game.assets.t("hero"):game.settings.playerName;
        String text=switch(page){
            case 0 -> game.assets.t("intro_0")+"\n"+name+".";
            case 1 -> game.assets.t("intro_1");
            case 2 -> game.assets.t("intro_2");
            default -> game.assets.t("intro_3");
        };
        // Deliberately large story text: this is a briefing, not HUD microcopy.
        Ui.text(batch,game.assets.font,text,82,610,.82f,new Color(.90f,.96f,1f,1));
        Ui.centered(batch,game.assets.font,page<3?game.assets.t("tap_continue"):game.assets.t("tap_start"),new Rectangle(190,245,700,80),.68f,Ui.CYAN);
        if(replay)Ui.text(batch,game.assets.font,game.assets.t("change_name"),70,145,.58f,Ui.GOLD);
        Ui.text(batch,game.assets.font,game.assets.t("skip"),840,145,.58f,new Color(.68f,.78f,.88f,1));
        batch.end();
    }

    private void drawBriefingFrame(){
        sr.setColor(.012f,.028f,.055f,.97f);sr.rect(60,710,960,900);
        sr.setColor(Ui.CYAN.r,Ui.CYAN.g,Ui.CYAN.b,.22f);sr.rect(60,710,960,4);sr.rect(60,1606,960,4);
        sr.setColor(.03f,.13f,.19f,.45f);for(int i=0;i<7;i++)sr.rect(85+i*150,735,2,835);
        sr.setColor(.04f,.14f,.22f,.40f);sr.rect(88,735,904,44);
    }

    private void drawPagePortraits(){
        Texture general=game.assets.generalPortrait, officer=game.assets.officerPortrait;
        // Story pages now use the characters as the actual scene, not as tiny HUD thumbnails.
        if(page==0){
            if(general!=null)batch.draw(general,230,735,620,875);
        }else if(page==1){
            if(officer!=null)batch.draw(officer,230,735,620,875);
        }else if(page==2){
            if(officer!=null)batch.draw(officer,230,735,620,875);
            batch.end();sr.begin(ShapeRenderer.ShapeType.Filled);
            float pulse=1f+.05f*MathUtils.sin(time*3f);
            sr.setColor(.45f,.10f,.78f,.16f);sr.circle(830,900,105*pulse,48);
            sr.setColor(.75f,.30f,1f,.72f);regularPolygon(830,900,70*pulse,6,time*.25f);
            sr.end();batch.begin();
        }else{
            if(general!=null)batch.draw(general,105,900,385,544);
            if(officer!=null)batch.draw(officer,590,900,385,544);
            batch.end();sr.begin(ShapeRenderer.ShapeType.Filled);
            float pulse=1f+.05f*MathUtils.sin(time*3f);sr.setColor(.45f,.10f,.78f,.15f);sr.circle(540,815,88*pulse,48);sr.setColor(.75f,.30f,1f,.7f);regularPolygon(540,815,58*pulse,6,time*.25f);sr.end();batch.begin();
        }
    }

    private void drawSpace(){
        sr.setColor(.004f,.008f,.026f,1);sr.rect(0,0,1080,1920);
        sr.setColor(.12f,.03f,.24f,.09f);sr.circle(920,1500,660,64);sr.setColor(.02f,.18f,.34f,.07f);sr.circle(80,1280,560,64);
        for(int i=0;i<90;i++){float x=(i*173f+29f)%1080f,y=(i*397f+77f)%1920f;float a=.25f+.35f*MathUtils.sin(time*.7f+i);sr.setColor(.55f,.82f,1f,MathUtils.clamp(a,.12f,.58f));sr.circle(x,y,1.2f+(i%3)*.65f,8);}
    }

    private void outline(Rectangle r,float t){sr.rect(r.x,r.y,r.width,t);sr.rect(r.x,r.y+r.height-t,r.width,t);sr.rect(r.x,r.y,t,r.height);sr.rect(r.x+r.width-t,r.y,t,r.height);}
    private void regularPolygon(float cx,float cy,float r,int sides,float rot){if(sides<3)return;float prevX=cx+MathUtils.cos(rot)*r,prevY=cy+MathUtils.sin(rot)*r;for(int i=1;i<=sides;i++){float a=rot+i*MathUtils.PI2/sides;float x=cx+MathUtils.cos(a)*r,y=cy+MathUtils.sin(a)*r;line(prevX,prevY,x,y,5f);prevX=x;prevY=y;}}
    private void line(float x1,float y1,float x2,float y2,float width){float dx=x2-x1,dy=y2-y1,len=(float)Math.sqrt(dx*dx+dy*dy),ang=MathUtils.atan2(dy,dx)*MathUtils.radiansToDegrees;sr.rect(x1,y1-width/2,0,width,len,width,1,1,ang);}
    @Override public void resize(int width,int height){viewport.update(width,height,true);}
    @Override public void dispose(){hideKeyboard();sr.dispose();batch.dispose();}
}
