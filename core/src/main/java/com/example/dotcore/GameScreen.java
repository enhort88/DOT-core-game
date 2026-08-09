package com.example.dotcore;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameScreen extends ScreenAdapter {
    private static final float W=1080f, H=1920f, GROUND_Y=150f;
    // Keep the combat layer visually separated from the top wave/boss HUD.
    // Enemies enter below this boundary instead of crawling underneath the progress line.
    private static final float COMBAT_TOP_Y=1818f;
    private static final long FIRE_UNLOCK_COST=12_000L;
    private static final long ICE_UNLOCK_COST=30_000L;
    private static final long LIGHTNING_UNLOCK_COST=75_000L;
    private static final int MAX_ENEMIES_BEFORE_OVERRUN=46;

    private final DotCoreGame game;
    private final SaveData save;
    private final OrthographicCamera camera=new OrthographicCamera();
    private final Viewport viewport=new FitViewport(W,H,camera);
    private final ShapeRenderer sr=new ShapeRenderer();
    private final SpriteBatch batch=new SpriteBatch();
    private final Array<Enemy> enemies=new Array<>();
    private final Array<Projectile> projectiles=new Array<>();
    private final Array<HostileProjectile> hostileProjectiles=new Array<>();
    private final Array<Particle> particles=new Array<>();
    private final Array<Beam> beams=new Array<>();
    private final Array<BlackHole> holes=new Array<>();
    private final Array<TrailFx> trails=new Array<>();
    private final Array<Shockwave> shockwaves=new Array<>();
    private final Array<Bonus> bonuses=new Array<>();
    private final Array<Turret> turrets=new Array<>();
    private final Array<Drone> drones=new Array<>();
    private final Vector3 tmp3=new Vector3();
    private final Touch[] touches=new Touch[10];

    private boolean shopOpen=false, elementConfigOpen=false, effectShopOpen=false, debuffShopOpen=false, paused=false, defeated=false, shopSellMode=false;
    private int shopTab=0;
    private int shopGesturePointer=-1;
    private float shopGestureStartX=0f, shopGestureStartY=0f, shopGestureX=0f, shopGestureY=0f, shopPageShift=0f;
    private boolean shopGestureActive=false, shopGestureMoved=false;
    private float spawnTimer=0f, waveClock=0f, passiveClock=0f, saveClock=0f, hostilePulseClock=0f, bonusTimer=14f, overdriveTime=0f;
    private float bannerTime=2.2f;
    private String banner="";
    private boolean bossActive=false;
    private float bossTimer=0f;
    private Enemy bossEnemy=null;
    private float lastTapTime=-10f;
    private float autoRepairClock=0f;
    private int activeTwoFingerMask=0;
    private float uiPulseTime=0f, uiPulseX=0f, uiPulseY=0f;
    private float annihilationUseTime=0f, annihilationCooldown=0f;
    private boolean annihilationWasActive=false;
    private long lastBossUnlockNotice=0;
    private Color uiPulseColor=new Color(Ui.CYAN);

    private static final Color FIRE=new Color(1f,.27f,.05f,1f);
    private static final Color ICE=new Color(.25f,.82f,1f,1f);
    private static final Color ELEC=new Color(.22f,.72f,1f,1f);
    private static final Color GRAV=new Color(.48f,.12f,.75f,1f);

    private enum EnemyKind { NORMAL, FAST, TANK, ELITE, BOSS }
    private enum EnemyArchetype { BASIC, FAST, TANK, ELITE, STAR, GUARDIAN, PHASE, FIRE_RESIST, ICE_RESIST, LIGHTNING_RESIST, ELEMENT_WARD, INFECTOR, BOSS }
    private enum EnemyAttackKind { NORMAL, CORROSION, PARASITE, DISRUPTION }
    private enum ShotKind { BULLET, ROCKET }
    private enum BonusType { CREDIT, HEAL, OVERDRIVE }

    private static class Touch {
        boolean down; float x,y,startX,startY,startTime,lastTrailTime,lastTrailX,lastTrailY,chargeFxClock; boolean dragged,twoFinger;
    }
    private static class Enemy {
        EnemyKind kind; EnemyArchetype archetype=EnemyArchetype.BASIC; float x,y,r,hp,maxHp,speed,reward,attackCd;
        float burnTime,burnDps,slowTime=0,slow=1f,freezeTime=0,chill=0,tapShieldTime=0,tapShieldCooldown=0;
        int shapeSides=0;
        Element resistElement=Element.NEUTRAL;
        float auraRadius=0f,auraReduction=0f;
        boolean dead=false;
    }
    private static class Projectile {
        float x,y,vx,vy,r,damage,life=3f,aoe=0; Element element; ShotKind kind; Enemy target;
    }
    private static class HostileProjectile {
        float x,y,vx,vy,life=4f,damage,r=7f; Turret turretTarget; Drone droneTarget; boolean planetTarget=false; EnemyAttackKind attackKind=EnemyAttackKind.NORMAL;
    }
    private static class Particle {
        float x,y,vx,vy,life,maxLife,size; Color color;
    }
    private static class Beam {
        float x1,y1,x2,y2,life,maxLife,width; Color color;
    }
    private static class BlackHole {
        float x,y,radius,life,maxLife,damage,pullStrength; boolean collapsed=false;
    }
    private static class TrailFx {
        float x1,y1,x2,y2,life,maxLife,seed,damagePulse,tick,width; Element element;
    }
    private static class Shockwave {
        float x,y,radius,maxRadius,life,maxLife; Color color;
    }
    private static class Bonus {
        BonusType type; float x,y,life=8f,bob; boolean dead=false;
    }
    private static class Turret {
        float x,y,shield,maxShield,cooldown,aimX,aimY,angle,targetAngle,recoil;
        float corrosionTime=0f,jamTime=0f;int infectionHits=0;boolean broken=false;
    }
    private static class Drone {
        DroneType type; float x,y,angle,shield,maxShield,cooldown,auraTick,respawn=0;
        float orbitRadiusX,orbitRadiusY,orbitSpeed,orbitPhase,centerOffsetX,centerOffsetY,heading; Enemy target; Drone supportTarget;
        float corrosionTime=0f,jamTime=0f;int infectionHits=0;boolean alive=true;
    }
    private static class ShopEntry {
        String id,label; long cost; boolean enabled; ShopEntry(String id,String label,long cost,boolean enabled){this.id=id;this.label=label;this.cost=cost;this.enabled=enabled;}
    }

    public GameScreen(DotCoreGame game, SaveData save){
        this.game=game; this.save=save;
        // Repair is a skill-by-use branch. Old/test saves are kept in a sane range.
        if(this.save.repairSkillLevel < 1) this.save.repairSkillLevel = 1;
        if(this.save.repairSkillLevel > 20) this.save.repairSkillLevel = 20;
        for(int i=0;i<touches.length;i++) touches[i]=new Touch();
        rebuildDefenses();
        banner=game.assets.t("wave")+"  "+save.wave;
        Gdx.input.setInputProcessor(new InputAdapter(){
            @Override public boolean touchDown(int sx,int sy,int pointer,int button){
                if(pointer>=touches.length) return false;
                Vector3 p=viewport.unproject(tmp3.set(sx,sy,0));
                if(shopOpen){beginShopGesture(pointer,p.x,p.y);return true;}
                if(handleUiDown(p.x,p.y)) return true;
                if(debuffShopOpen||effectShopOpen||elementConfigOpen||paused||defeated) return true;
                Touch t=touches[pointer]; t.down=true;t.x=t.startX=p.x;t.y=t.startY=p.y;t.lastTrailX=p.x;t.lastTrailY=p.y;t.startTime=save.playSeconds;t.lastTrailTime=save.playSeconds;t.chargeFxClock=0;t.dragged=false;t.twoFinger=false;
                markTwoFinger();
                return true;
            }
            @Override public boolean touchDragged(int sx,int sy,int pointer){
                if(pointer>=touches.length) return false;
                Vector3 p=viewport.unproject(tmp3.set(sx,sy,0));
                if(shopOpen && shopGestureActive && pointer==shopGesturePointer){updateShopGesture(p.x,p.y);return true;}
                if(debuffShopOpen||effectShopOpen||elementConfigOpen||paused||defeated) return false;
                Touch t=touches[pointer]; if(!t.down) return false;
                float oldX=t.x,oldY=t.y; t.x=p.x;t.y=p.y;
                if(Vector2.dst(t.startX,t.startY,t.x,t.y)>24) t.dragged=true;
                markTwoFinger();
                if(twoTouchesDown() && hasAnnihilation() && annihilationCooldown<=0f && annihilationUseTime<3f){
                    doTwoFingerRift();
                } else if(t.dragged && hasTrail() && save.playSeconds-t.lastTrailTime>0.028f){
                    doTrail(t.lastTrailX,t.lastTrailY,t.x,t.y); t.lastTrailX=t.x; t.lastTrailY=t.y; t.lastTrailTime=save.playSeconds;
                }
                return true;
            }
            @Override public boolean touchUp(int sx,int sy,int pointer,int button){
                if(pointer>=touches.length) return false;
                Vector3 p=viewport.unproject(tmp3.set(sx,sy,0));
                if(shopOpen && shopGestureActive && pointer==shopGesturePointer){finishShopGesture(p.x,p.y);return true;}
                Touch t=touches[pointer];
                if(debuffShopOpen||effectShopOpen||elementConfigOpen||paused||defeated){t.down=false;return true;}
                boolean wasTwo=t.twoFinger||twoTouchesDown();
                float held=save.playSeconds-t.startTime;
                t.x=p.x;t.y=p.y;
                if(!wasTwo){
                    if(held>0.42f && !t.dragged && hasPlasma()) doPlasma(t.x,t.y,MathUtils.clamp(held/1.6f,.35f,1f));
                    else if(!t.dragged) doTap(t.x,t.y);
                } else if(hasAnnihilation() && save.fingerElement==Element.GRAVITY && hasGravity()){
                    holes.add(makeHole(t.x,t.y,260,2.5f,170f*save.generalDamageMultiplier()));
                }
                t.down=false;
                if(wasTwo && annihilationWasActive && !twoTouchesDown() && annihilationCooldown<=0f){
                    annihilationCooldown=Math.max(8f,28f-save.ultimateSkillLevel*.35f);annihilationUseTime=0f;annihilationWasActive=false;
                }
                return true;
            }
            @Override public boolean keyDown(int key){
                if(key==Input.Keys.ESCAPE || key==Input.Keys.BACK){
                    resetTouchState(true);
                    if(effectShopOpen){effectShopOpen=false;elementConfigOpen=true;return true;}
                    if(elementConfigOpen){elementConfigOpen=false;return true;}
                    if(debuffShopOpen){debuffShopOpen=false;shopSellMode=false;game.saves.save(save);return true;}
                    if(shopOpen){shopOpen=false;shopSellMode=false;game.saves.save(save);return true;}
                    if(paused){paused=false;return true;}
                    paused=true;return true;
                }
                if(key==Input.Keys.S){shopOpen=!shopOpen;return true;}
                if(key==Input.Keys.SPACE){paused=!paused;return true;}
                if(key==Input.Keys.F){save.fingerElement=save.fingerElement.nextCombat(true,save,cheatsEnabled());return true;}
                if(key==Input.Keys.T){save.turretElement=save.turretElement.nextCombat(false,save,cheatsEnabled());return true;}
                if(key==Input.Keys.D){save.droneElement=save.droneElement.nextCombat(false,save,cheatsEnabled());return true;}
                return false;
            }
        });
        Gdx.input.setCatchKey(Input.Keys.BACK,true);
    }

    private void beginShopGesture(int pointer,float x,float y){
        shopGesturePointer=pointer;shopGestureStartX=shopGestureX=x;shopGestureStartY=shopGestureY=y;
        shopGestureActive=true;shopGestureMoved=false;shopPageShift=0f;
    }
    private void updateShopGesture(float x,float y){
        shopGestureX=x;shopGestureY=y;
        float dx=x-shopGestureStartX,dy=y-shopGestureStartY;
        if(Math.abs(dx)>18f && Math.abs(dx)>Math.abs(dy)*.75f)shopGestureMoved=true;
        if(shopGestureMoved)shopPageShift=MathUtils.clamp(dx*.22f,-105f,105f);
    }
    private void finishShopGesture(float x,float y){
        float dx=x-shopGestureStartX,dy=y-shopGestureStartY;
        boolean horizontal=Math.abs(dx)>115f && Math.abs(dx)>Math.abs(dy)*1.15f;
        if(horizontal){
            int next=shopTab+(dx<0?1:-1);
            next=MathUtils.clamp(next,0,2);
            if(next!=shopTab){shopTab=next;shopPageShift=dx<0?125f:-125f;game.assets.play(game.assets.buy,game.settings,.045f);}
            else shopPageShift=0f;
        }else if(dx*dx+dy*dy<42f*42f){
            shopClick(x,y);
        }
        shopGestureActive=false;shopGesturePointer=-1;shopGestureMoved=false;
    }
    private void changeShopTab(int next){
        next=MathUtils.clamp(next,0,2);
        if(next==shopTab)return;
        shopPageShift=next>shopTab?125f:-125f;shopTab=next;
    }

    private void markTwoFinger(){
        if(!twoTouchesDown()) return;
        int marked=0;
        for(Touch t:touches) if(t.down && marked<2){t.twoFinger=true;marked++;}
    }
    private boolean twoTouchesDown(){int n=0;for(Touch t:touches)if(t.down&&++n>=2)return true;return false;}

    private boolean anyTouchState(){for(Touch t:touches)if(t.down||t.twoFinger)return true;return annihilationWasActive;}
    private void resetTouchState(boolean startCooldown){
        if(startCooldown && annihilationWasActive && annihilationCooldown<=0f)annihilationCooldown=Math.max(8f,28f-save.ultimateSkillLevel*.35f);
        for(Touch t:touches){t.down=false;t.dragged=false;t.twoFinger=false;t.chargeFxClock=0f;}
        activeTwoFingerMask=0;annihilationUseTime=0f;annihilationWasActive=false;
    }

    private boolean handleUiDown(float x,float y){
        if(defeated){
            if(new Rectangle(190,690,700,120).contains(x,y)){ resetSlot(); return true; }
            if(new Rectangle(190,520,700,120).contains(x,y)){ game.openMenu(); return true; }
            return true;
        }
        if(paused){
            if(new Rectangle(190,860,700,125).contains(x,y)){paused=false;return true;}
            if(new Rectangle(190,680,700,125).contains(x,y)){game.saves.save(save);game.openMenu();return true;}
            return true;
        }
        if(shopOpen){ shopClick(x,y); return true; }
        if(debuffShopOpen){ debuffShopClick(x,y); return true; }
        if(effectShopOpen){ effectShopClick(x,y); return true; }
        if(elementConfigOpen){ elementConfigClick(x,y); return true; }
        if(new Rectangle(932,0,96,96).contains(x,y)){resetTouchState(true);triggerUiPulse(980,48,Ui.CYAN);shopOpen=true;return true;}
        if(effectsSystemUnlocked()&&new Rectangle(827,0,96,96).contains(x,y)){resetTouchState(true);triggerUiPulse(875,48,ELEC);elementConfigOpen=true;return true;}
        if(new Rectangle(722,0,96,96).contains(x,y)){resetTouchState(true);triggerUiPulse(770,48,Ui.RED);debuffShopOpen=true;shopSellMode=false;return true;}
        if(new Rectangle(20,24,104,104).contains(x,y)){resetTouchState(true);paused=true;return true;}
        if(tryCollectBonus(x,y)) return true;
        if(tryRepairTurret(x,y)) return true;
        if(tryRepairDrone(x,y)) return true;
        return false;
    }


    private boolean tryRepairTurret(float x,float y){
        for(Turret t:turrets){
            if(dist2(x,y,t.x,t.y)>68f*68f) continue;
            if(t.infectionHits>0){
                long cleanCost=Math.max(6,14+save.wave*2L);
                if(!cheatsEnabled()&&save.credits<cleanCost){banner=game.assets.t("not_enough");bannerTime=1f;return true;}
                if(!cheatsEnabled())save.credits-=cleanCost;t.infectionHits--;gainRepairXp(1.3f);weldFx(t.x,t.y,new Color(.86f,.25f,1f,1));
                if(t.infectionHits<=0){banner=game.assets.t("cleansed");bannerTime=.8f;}game.saves.save(save);return true;
            }
            if(!t.broken && t.shield>=t.maxShield-0.5f) return true;
            int baseTaps=5+save.turretShieldLevel*2;
            int effectiveTaps=Math.max(3,baseTaps-Math.max(0,(save.repairSkillLevel-1)/4));
            float amount=t.maxShield/effectiveTaps;
            long repairCost=Math.max(8,16+save.wave*2L+save.turretShieldLevel*2L);
            if(!cheatsEnabled() && save.credits<repairCost){banner=game.assets.t("not_enough");bannerTime=1.1f;return true;}
            if(!cheatsEnabled()) save.credits-=repairCost;
            t.shield=Math.min(t.maxShield,t.shield+amount);
            if(t.shield>=t.maxShield-.5f){t.shield=t.maxShield;t.broken=false;}
            save.totalRepairs++;gainRepairXp(1f);weldFx(t.x,t.y,Ui.GREEN);game.assets.play(game.assets.buy,game.settings,.12f);vibrate(10);
            banner=game.assets.t("repair")+"  "+Math.round(t.shield/t.maxShield*100f)+"%";bannerTime=.7f;game.saves.save(save);return true;
        }
        return false;
    }

    private boolean tryRepairDrone(float x,float y){
        for(int i=0;i<drones.size;i++){
            Drone d=drones.get(i);if(!d.alive||dist2(x,y,d.x,d.y)>58f*58f)continue;
            if(d.infectionHits<=0)return false;
            long cost=Math.max(5,10+save.wave*2L);if(!cheatsEnabled()&&save.credits<cost){banner=game.assets.t("not_enough");bannerTime=1f;return true;}
            if(!cheatsEnabled())save.credits-=cost;d.infectionHits--;gainRepairXp(1f);weldFx(d.x,d.y,new Color(.86f,.25f,1f,1));
            if(d.infectionHits<=0){banner=game.assets.t("cleansed");bannerTime=.8f;}game.saves.save(save);return true;
        }
        return false;
    }

    private float repairXpNeed(int level){ return 8f + Math.max(1,level)*4.5f; }

    private void gainRepairXp(float amount){
        if(save.repairSkillLevel>=20){save.repairSkillLevel=20;save.repairXp=0f;return;}
        save.repairXp+=amount;
        float need=repairXpNeed(save.repairSkillLevel);
        while(save.repairXp>=need && save.repairSkillLevel<20){
            save.repairXp-=need;save.repairSkillLevel++;need=repairXpNeed(save.repairSkillLevel);
        }
        if(save.repairSkillLevel>=20){save.repairSkillLevel=20;save.repairXp=0f;}
    }

    private void weldFx(float x,float y,Color c){
        burst(x,y,c,12);for(int i=0;i<5;i++){float a=MathUtils.random(MathUtils.PI*.15f,MathUtils.PI*.85f);Particle p=new Particle();p.x=x;p.y=y;p.vx=MathUtils.cos(a)*MathUtils.random(70f,160f);p.vy=MathUtils.sin(a)*MathUtils.random(70f,170f);p.life=p.maxLife=MathUtils.random(.18f,.4f);p.size=MathUtils.random(2f,5f);p.color=new Color(Color.WHITE);particles.add(p);}
    }

    @Override public void render(float delta){
        float d=Math.min(delta,0.05f);
        // Android can occasionally miss a touchUp after multitouch/system gestures. Never leave the finger state latched.
        if(!Gdx.input.isTouched() && anyTouchState())resetTouchState(true);
        if(uiPulseTime>0f)uiPulseTime=Math.max(0f,uiPulseTime-d);
        if(!shopGestureActive && Math.abs(shopPageShift)>.25f)shopPageShift=MathUtils.lerp(shopPageShift,0f,Math.min(1f,d*11f));
        // Shop / debuffs / effect setup / pause are true pauses: no credits, waves, cooldowns or play time advance.
        if(!paused&&!shopOpen&&!debuffShopOpen&&!elementConfigOpen&&!effectShopOpen&&!defeated) update(d);
        draw();
    }

    private void update(float d){
        save.playSeconds+=d; waveClock+=d; spawnTimer-=d; passiveClock+=d; saveClock+=d; hostilePulseClock+=d; autoRepairClock+=d; bonusTimer-=d;
        if(overdriveTime>0) overdriveTime-=d;
        if(bannerTime>0) bannerTime-=d;
        if(annihilationCooldown>0f) annihilationCooldown=Math.max(0f,annihilationCooldown-d);
        if(twoTouchesDown() && hasAnnihilation() && annihilationCooldown<=0f){
            annihilationWasActive=true;annihilationUseTime+=d;gainSkill("ultimate",d*9f);
            if(annihilationUseTime>=3f){annihilationUseTime=0f;annihilationWasActive=false;annihilationCooldown=Math.max(8f,28f-save.ultimateSkillLevel*.35f);vibrate(38);}
        }
        if(passiveClock>=1f){int n=(int)passiveClock;passiveClock-=n;save.credits+=save.passiveIncomePerSecond()*n;}
        if(saveClock>5f){saveClock=0;game.saves.save(save);}

        if(bossActive){ bossTimer-=d; if(bossTimer<=0 && bossEnemy!=null && !bossEnemy.dead){ save.integrity=0; defeat(); } }
        float waveDuration=save.wave==1?34f:35f;
        if(waveClock>=waveDuration){
            waveClock-=waveDuration; save.wave++;
            banner=game.assets.t("wave")+"  "+save.wave;bannerTime=2.2f;game.assets.play(game.assets.wave,game.settings,.55f);vibrate(45);
            if(save.wave%5==0) spawnBoss();
        }

        float waveRush=(save.wave>1&&waveClock>24f)?1.65f:1f;
        if(spawnTimer<=0 && !bossActive){
            int batchCount=1;
            if(save.wave>=6 && MathUtils.random()<Math.min(.38f,(save.wave-5)*.018f))batchCount++;
            if(save.difficulty>=2 && MathUtils.random()<.24f)batchCount++;
            if(save.difficulty<=0 && batchCount>1)batchCount=1;
            float extraChance=Math.min(.78f,save.densityLevel*.085f);
            if(save.densityLevel>0&&MathUtils.random()<extraChance)batchCount++;
            if(save.densityLevel>=5&&MathUtils.random()<Math.min(.38f,(save.densityLevel-4)*.055f))batchCount++;
            for(int i=0;i<batchCount;i++) spawnEnemy(false);
            float baseSpawn=save.wave==1?2.35f:Math.max(.52f,1.06f-(save.wave-2)*.030f);
            baseSpawn/=save.difficultyDensityMultiplier();
            spawnTimer=(baseSpawn/(save.spawnMultiplier()*waveRush))*MathUtils.random(.78f,1.18f);
        }

        if(bonusTimer<=0){spawnBonus();bonusTimer=MathUtils.random(18f,30f);}
        updateFingerChargeFx(d);
        updateEnemies(d); updateTurrets(d); updateDrones(d); updateProjectiles(d); updateHostileProjectiles(d); updateHoles(d); updateTrails(d); updateShockwaves(d); updateBonuses(d); updateParticles(d); updateBeams(d);

        if(enemies.size>MAX_ENEMIES_BEFORE_OVERRUN){ save.integrity-=(enemies.size-MAX_ENEMIES_BEFORE_OVERRUN)*1.5f*d; }
        if(save.wave>1 && hostilePulseClock>2.1f){hostilePulseClock=0;hostilePulse();}
        if((cheatsEnabled()||save.autoRepairUnlocked) && autoRepairClock>1f){autoRepairClock=0;autoRepairTick();}
        if(save.integrity<=0) defeat();
    }

    private void updateFingerChargeFx(float d){
        if(!hasPlasma())return;
        for(Touch t:touches){
            if(!t.down||t.dragged||t.twoFinger)continue;
            float held=save.playSeconds-t.startTime;if(held<.16f)continue;
            float charge=MathUtils.clamp((held-.16f)/1.35f,0f,1f);
            t.chargeFxClock-=d;if(t.chargeFxClock>0f)continue;t.chargeFxClock=MathUtils.lerp(.07f,.025f,charge);
            Color c=elementColor(save.fingerElement);
            int n=game.settings.highEffects?2:1;
            for(int i=0;i<n;i++){
                float a=MathUtils.random(0f,MathUtils.PI2),r=MathUtils.random(54f+charge*28f,92f+charge*52f);
                Particle p=new Particle();p.x=t.x+MathUtils.cos(a)*r;p.y=t.y+MathUtils.sin(a)*r;
                p.vx=(t.x-p.x)*MathUtils.random(1.7f,2.8f);p.vy=(t.y-p.y)*MathUtils.random(1.7f,2.8f);
                p.life=p.maxLife=MathUtils.random(.22f,.48f);p.size=MathUtils.random(2.8f,7f)*(1f+charge*.45f);p.color=new Color(c);particles.add(p);
            }
        }
    }

    private void spawnEnemy(boolean boss){
        Enemy e=new Enemy();
        if(boss){
            e.kind=EnemyKind.BOSS;e.archetype=EnemyArchetype.BOSS;e.r=112;e.maxHp=(1650+save.wave*390)*save.enemyHealthMultiplier();e.speed=10.5f;e.reward=(1000+save.wave*110)*save.creditMultiplier();e.shapeSides=save.wave>=10?3:6;
        } else if(save.wave==1){
            e.kind=EnemyKind.NORMAL;e.archetype=EnemyArchetype.BASIC;e.r=24;e.maxHp=15f*save.enemyHealthMultiplier();e.speed=19f;e.reward=18f*save.creditMultiplier();e.shapeSides=0;
        } else {
            float q=MathUtils.random();
            float eliteChance=Math.min(.17f,.052f+save.wave*.0043f);
            float tankCut=eliteChance+.16f+Math.min(.08f,save.wave*.003f);
            float fastCut=tankCut+.22f+Math.min(.08f,save.wave*.0025f);
            if(q<eliteChance){e.kind=EnemyKind.ELITE;e.archetype=EnemyArchetype.ELITE;e.r=45;e.maxHp=(86+save.wave*8.5f)*save.enemyHealthMultiplier();e.speed=23;e.reward=(44+save.wave*3.2f)*save.creditMultiplier();}
            else if(q<tankCut){e.kind=EnemyKind.TANK;e.archetype=EnemyArchetype.TANK;e.r=38;e.maxHp=(55+save.wave*6f)*save.enemyHealthMultiplier();e.speed=18;e.reward=(30+save.wave*2.3f)*save.creditMultiplier();}
            else if(q<fastCut){e.kind=EnemyKind.FAST;e.archetype=EnemyArchetype.FAST;e.r=17;e.maxHp=(12+save.wave*1.75f)*save.enemyHealthMultiplier();e.speed=52;e.reward=(9+save.wave*.95f)*save.creditMultiplier();}
            else {e.kind=EnemyKind.NORMAL;e.archetype=EnemyArchetype.BASIC;e.r=25;e.maxHp=(23+save.wave*2.7f)*save.enemyHealthMultiplier();e.speed=30;e.reward=(14+save.wave*1.3f)*save.creditMultiplier();}

            int techWave=cheatsEnabled()?Math.max(save.wave,12):save.wave;
            // Tactical archetypes are layered over the physical class. The weights are deliberately mild:
            // the game reacts to a build, but never hard-counters it every wave.
            boolean fingerHeavy=save.turretCount<=1 && save.droneCount()<=1;
            float special=MathUtils.random();
            if(techWave>=10 && save.bestiaryStar==0){
                makeStar(e);
            }else if(techWave>=12 && special<.045f){
                makeElementWard(e);
            }else if(techWave>=8 && special<.10f){
                makeInfector(e);
            }else if(save.wave>=7 && special<(.16f+(fingerHeavy?.08f:0f))){
                makePhase(e,fingerHeavy);
            }else if(save.wave>=6 && special<(.23f+(fingerHeavy?.055f:0f))){
                makeGuardian(e);
            }else if(techWave>=7 && special<.36f){
                int pick=MathUtils.random(0,2);makeResistant(e,pick==0?Element.FIRE:pick==1?Element.ICE:Element.LIGHTNING);
            }else if(techWave>=10 && special<.43f){
                makeStar(e);
            }

            if(e.shapeSides==0){
                if(save.wave<5)e.shapeSides=0;
                else if(save.wave<10)e.shapeSides=e.kind==EnemyKind.TANK?4:e.kind==EnemyKind.ELITE?6:e.kind==EnemyKind.FAST?5:(MathUtils.randomBoolean()?5:6);
                else e.shapeSides=e.kind==EnemyKind.TANK?4:(e.kind==EnemyKind.FAST||MathUtils.random()<.45f?3:5+MathUtils.random(0,1));
            }
        }
        e.speed*=save.enemySpeedMultiplier();
        // Finger-heavy builds see phase shields a little more often; other builds still encounter them.
        if(e.archetype==EnemyArchetype.PHASE)e.tapShieldCooldown=MathUtils.random(1.4f,3.0f);
        else if(save.wave>=7&&(e.kind==EnemyKind.ELITE||e.kind==EnemyKind.BOSS))e.tapShieldCooldown=MathUtils.random(3.0f,6.0f);
        else e.tapShieldCooldown=999f;
        e.hp=e.maxHp;e.x=MathUtils.random(75f,W-75f);e.y=boss?1650f:COMBAT_TOP_Y-e.r;e.attackCd=MathUtils.random(1.2f,3.6f);enemies.add(e);
    }

    private void makeStar(Enemy e){
        e.archetype=EnemyArchetype.STAR;e.kind=EnemyKind.ELITE;e.r=39f;e.maxHp*=1.45f;e.hp=e.maxHp;e.speed*=.86f;e.reward*=1.85f;e.shapeSides=5;
    }
    private void makeGuardian(Enemy e){
        e.archetype=EnemyArchetype.GUARDIAN;e.kind=EnemyKind.TANK;e.r=53f;e.maxHp*=3.2f;e.hp=e.maxHp;e.speed*=.62f;e.reward*=2.6f;e.shapeSides=6;e.auraRadius=235f;e.auraReduction=.70f;
    }
    private void makePhase(Enemy e,boolean fingerHeavy){
        e.archetype=EnemyArchetype.PHASE;e.kind=EnemyKind.ELITE;e.r=Math.max(e.r,34f);e.maxHp*=1.25f;e.hp=e.maxHp;e.reward*=1.45f;e.shapeSides=4;e.tapShieldCooldown=fingerHeavy?1.2f:2.5f;
    }
    private void makeResistant(Enemy e,Element element){
        e.resistElement=element;e.maxHp*=1.18f;e.hp=e.maxHp;e.reward*=1.35f;e.shapeSides=6;
        e.archetype=element==Element.FIRE?EnemyArchetype.FIRE_RESIST:element==Element.ICE?EnemyArchetype.ICE_RESIST:EnemyArchetype.LIGHTNING_RESIST;
    }
    private void makeElementWard(Enemy e){
        e.archetype=EnemyArchetype.ELEMENT_WARD;e.kind=EnemyKind.ELITE;e.r=49f;e.maxHp*=2.7f;e.hp=e.maxHp;e.speed*=.74f;e.reward*=2.25f;e.shapeSides=6;e.auraRadius=255f;e.auraReduction=.65f;
    }
    private void makeInfector(Enemy e){
        e.archetype=EnemyArchetype.INFECTOR;e.kind=EnemyKind.FAST;e.r=29f;e.maxHp*=1.32f;e.hp=e.maxHp;e.speed*=1.08f;e.reward*=1.7f;e.shapeSides=3;
    }

    private void spawnBoss(){
        resetTouchState(true);bossActive=true;bossTimer=save.difficulty<=0?36f:(save.difficulty>=2?29f:32f);spawnEnemy(true);bossEnemy=enemies.peek();banner=game.assets.t("boss_incoming");bannerTime=3f;game.assets.play(game.assets.boss,game.settings,.75f);vibrate(100);
    }

    private void updateEnemies(float d){
        for(int i=enemies.size-1;i>=0;i--){
            Enemy e=enemies.get(i);
            if(e.dead){enemies.removeIndex(i);continue;}
            if(e.burnTime>0){e.burnTime-=d;damageRaw(e,e.burnDps*d);}
            if(e.freezeTime>0){e.freezeTime-=d;e.slow=.08f;}
            else if(e.slowTime>0){e.slowTime-=d;} else {e.slow=1f;e.chill=Math.max(0,e.chill-d*.45f);}
            if(e.tapShieldTime>0f)e.tapShieldTime-=d;else if(e.tapShieldCooldown<900f){e.tapShieldCooldown-=d;if(e.tapShieldCooldown<=0f){e.tapShieldTime=e.archetype==EnemyArchetype.PHASE?2.7f:2.2f;e.tapShieldCooldown=e.archetype==EnemyArchetype.PHASE?MathUtils.random(4.0f,6.5f):MathUtils.random(7f,10.5f);}}
            float moveSpeed=e.speed*e.slow;
            // Stars are artillery: once they reach the upper combat band, they advance much more slowly and keep firing.
            if(e.archetype==EnemyArchetype.STAR && e.y<1450f)moveSpeed*=.28f;
            e.y-=moveSpeed*d;
            // Gravity/other movement may never pull an enemy back into the wave HUD lane.
            e.y=Math.min(e.y,COMBAT_TOP_Y-e.r);
            e.attackCd-=d;
            if(save.wave>1 && e.attackCd<=0 && (e.kind==EnemyKind.BOSS || e.archetype==EnemyArchetype.STAR || e.archetype==EnemyArchetype.INFECTOR || e.y<980 || hasDroneNear(e.x,e.y,460f))) { e.attackCd=e.archetype==EnemyArchetype.STAR?MathUtils.random(1.15f,1.8f):MathUtils.random(2f,4.2f); enemyAttack(e); }
            if(e.y-e.r<=GROUND_Y){
                save.integrity-=e.kind==EnemyKind.BOSS?60:(e.kind==EnemyKind.ELITE?13:e.kind==EnemyKind.TANK?9:5);
                explode(e.x,e.y,colorFor(e),e.kind==EnemyKind.BOSS?55:22);e.dead=true;
            }
        }
    }

    private void enemyAttack(Enemy e){
        if(turrets.size+drones.size==0)return;
        float dmg=(e.kind==EnemyKind.BOSS?18f:e.kind==EnemyKind.ELITE?7f:3.5f)*save.enemyDamageMultiplier();
        int volley=e.archetype==EnemyArchetype.STAR?3:1;
        for(int shot=0;shot<volley;shot++){
            HostileProjectile p=new HostileProjectile();p.x=e.x+(shot-1)*9f;p.y=e.y;p.damage=dmg;p.r=e.kind==EnemyKind.BOSS?10f:7f;
            float roll=MathUtils.random();
            if(e.archetype==EnemyArchetype.INFECTOR)p.attackKind=EnemyAttackKind.PARASITE;
            else if(save.wave>=8 && (e.kind==EnemyKind.ELITE||e.kind==EnemyKind.BOSS) && roll<.18f)p.attackKind=EnemyAttackKind.PARASITE;
            else if(save.wave>=5 && roll<.38f)p.attackKind=EnemyAttackKind.CORROSION;
            else if(save.wave>=10 && roll<.50f)p.attackKind=EnemyAttackKind.DISRUPTION;
            else p.attackKind=EnemyAttackKind.NORMAL;
            if(p.attackKind==EnemyAttackKind.PARASITE)p.damage*=.35f;else if(p.attackKind==EnemyAttackKind.CORROSION)p.damage*=.55f;else if(p.attackKind==EnemyAttackKind.DISRUPTION)p.damage*=.45f;

            // Kamikaze drones are an obvious incoming bomb, so aliens prioritize them when visible.
            Drone nearestDrone=null;float droneBest=Float.MAX_VALUE;
            for(int i=0;i<drones.size;i++){
                Drone q=drones.get(i);if(!q.alive)continue;float ds=dist2(e.x,e.y,q.x,q.y);
                float score=ds*(q.type==DroneType.KAMIKAZE?.18f:1f);
                if(p.attackKind==EnemyAttackKind.PARASITE)score*=.62f;
                if(score<droneBest){droneBest=score;nearestDrone=q;}
            }
            if(nearestDrone!=null && (p.attackKind==EnemyAttackKind.PARASITE || droneBest<520f*520f)){
                p.droneTarget=nearestDrone;setHostileVelocity(p,nearestDrone.x,nearestDrone.y,e.kind==EnemyKind.BOSS?540f:430f);hostileProjectiles.add(p);continue;
            }
            Turret nearestTurret=null;float turretBest=Float.MAX_VALUE;
            for(int i=0;i<turrets.size;i++){Turret q=turrets.get(i);if(q.broken)continue;float ds=dist2(e.x,e.y,q.x,q.y);if(ds<turretBest){turretBest=ds;nearestTurret=q;}}
            if(nearestTurret!=null){p.turretTarget=nearestTurret;setHostileVelocity(p,nearestTurret.x,nearestTurret.y,e.kind==EnemyKind.BOSS?540f:430f);hostileProjectiles.add(p);}
        }
    }

    private boolean hasDroneNear(float x,float y,float radius){float r2=radius*radius;for(int i=0;i<drones.size;i++){Drone d=drones.get(i);if(d.alive&&dist2(x,y,d.x,d.y)<r2)return true;}return false;}

    private void setHostileVelocity(HostileProjectile p,float tx,float ty,float speed){
        float dx=tx-p.x,dy=ty-p.y,len=(float)Math.sqrt(dx*dx+dy*dy);if(len<1f)len=1f;p.vx=dx/len*speed;p.vy=dy/len*speed;
    }

    private void updateTurrets(float d){
        float overdrive=overdriveTime>0?1.55f:1f;
        for(Turret t:turrets){
            t.recoil=Math.max(0f,t.recoil-d*115f);
            if(t.corrosionTime>0f){t.corrosionTime-=d;t.shield-=Math.max(1.5f,t.maxShield*.018f)*d;if(t.shield<=0){t.shield=0;t.broken=true;explode(t.x,t.y,new Color(.45f,1f,.16f,1),20);}}
            if(t.jamTime>0f)t.jamTime-=d;
            if(t.broken)continue;
            if(t.infectionHits>0){t.cooldown-=d;if(t.cooldown<=0f){infectedTurretAttack(t);t.cooldown=.72f;}continue;}
            if(t.jamTime>0f)continue;
            Enemy target=nearestEnemy(t.x,t.y,1600);
            if(target!=null){
                float previewSpeed=save.turretWeapon==2?520f:1180f;
                Vector2 aim=save.turretWeapon==0?predictIntercept(t.x,t.y,target,previewSpeed):new Vector2(target.x,target.y);
                t.aimX=aim.x;t.aimY=aim.y;
                t.targetAngle=MathUtils.atan2(t.aimY-t.y,t.aimX-t.x)*MathUtils.radiansToDegrees;
            }else{
                t.targetAngle=90f+MathUtils.sin(save.playSeconds*.75f+t.x*.012f)*28f;
            }
            float diff=((t.targetAngle-t.angle+540f)%360f)-180f;
            t.angle+=diff*Math.min(1f,d*7.5f);
            t.cooldown-=d;if(t.cooldown>0||target==null)continue;
            float rate=(1f+save.turretRateLevel*.20f)*save.generalRateMultiplier()*(1f+save.turretSkillLevel*.006f)*overdrive;
            if(save.turretWeapon==1&&hasLaser()){
                float dmg=18f*(1f+save.turretDamageLevel*.18f)*save.generalDamageMultiplier()*(1f+save.turretSkillLevel*.008f);
                dealDamage(target,dmg,save.turretElement,true);gainSkill("turret",dmg);beam(t.x,t.y,target.x,target.y,elementColor(save.turretElement),6,0.11f);game.assets.play(game.assets.laser,game.settings,.16f);
                t.recoil=8f;t.cooldown=Math.max(.06f,.84f/rate);
            }else if(save.turretWeapon==2&&hasRockets()){
                float rd=18f*(1f+save.turretDamageLevel*.20f)*save.generalDamageMultiplier()*(1f+save.turretSkillLevel*.008f);fireProjectile(t.x,t.y,target,rd,save.turretElement,ShotKind.ROCKET,72);gainSkill("turret",rd);game.assets.play(game.assets.rocket,game.settings,.13f);
                t.recoil=14f;t.cooldown=Math.max(.22f,1.55f/rate);
            }else{
                float bd=8f*(1f+save.turretDamageLevel*.17f)*save.generalDamageMultiplier()*(1f+save.turretSkillLevel*.008f);fireProjectile(t.x,t.y,target,bd,save.turretElement,ShotKind.BULLET,0);gainSkill("turret",bd);game.assets.play(game.assets.shot,game.settings,.09f);
                t.recoil=10f;t.cooldown=Math.max(.04f,.56f/rate);
            }
        }
    }

    private void updateDrones(float d){
        final float detectRange=620f; // normal targets: drones must fly into the fight.
        for(int di=0;di<drones.size;di++){
            Drone dr=drones.get(di);
            if(!dr.alive){
                dr.respawn-=d;
                if(dr.respawn<=0){
                    dr.alive=true;dr.shield=dr.maxShield;dr.cooldown=.6f;dr.target=null;dr.supportTarget=null;
                    // Every respawn comes from the planet/base area.
                    dr.x=540f+MathUtils.random(-150f,150f);dr.y=270f+MathUtils.random(0f,70f);
                    burst(dr.x,dr.y,Ui.CYAN,18);
                }
                continue;
            }
            if(dr.corrosionTime>0f){
                dr.corrosionTime-=d;dr.shield-=Math.max(1.2f,dr.maxShield*.02f)*d;
                if(dr.shield<=0){killDrone(dr);continue;}
            }
            if(dr.jamTime>0f)dr.jamTime-=d;
            if(dr.infectionHits>0){
                dr.cooldown-=d;if(dr.cooldown<=0f){infectedDroneAttack(dr);dr.cooldown=.65f;}continue;
            }

            dr.angle+=d*dr.orbitSpeed;
            float oldX=dr.x,oldY=dr.y;

            // Support drones have global awareness of damaged allies and immediately fly to them.
            if(dr.type==DroneType.SUPPORT){
                dr.supportTarget=mostDamagedDrone(dr);
                if(dr.supportTarget!=null){
                    float dx=dr.supportTarget.x-dr.x,dy=dr.supportTarget.y-dr.y,len=(float)Math.sqrt(dx*dx+dy*dy);
                    float desired=105f;
                    if(len>desired){
                        float speed=220f+save.droneRateLevel*3.5f;
                        dr.x+=dx/Math.max(1f,len)*speed*d;dr.y+=dy/Math.max(1f,len)*speed*d;
                    }else{
                        float tangent=28f*d;dr.x+=-dy/Math.max(1f,len)*tangent;dr.y+=dx/Math.max(1f,len)*tangent;
                    }
                }else{
                    moveDroneOnPatrol(dr,d);
                }
                dr.x=MathUtils.clamp(dr.x,55f,W-55f);dr.y=MathUtils.clamp(dr.y,245f,1500f);
                if(dist2(oldX,oldY,dr.x,dr.y)>.01f)dr.heading=MathUtils.atan2(dr.y-oldY,dr.x-oldX);
                dr.cooldown-=d;dr.auraTick-=d;
                if(dr.jamTime<=0f)healNearbyDrones(dr,d);
                continue;
            }

            // A live boss is a global priority target. Timed boss fights must never be lost because a drone did not "see" it.
            if(bossActive && bossEnemy!=null && !bossEnemy.dead){
                dr.target=bossEnemy;
            }else if(dr.target==null||dr.target.dead||dist2(dr.x,dr.y,dr.target.x,dr.target.y)>detectRange*detectRange*1.5f){
                dr.target=nearestEnemy(dr.x,dr.y,detectRange);
            }

            if(dr.type==DroneType.KAMIKAZE && dr.target!=null){
                float dx=dr.target.x-dr.x,dy=dr.target.y-dr.y,len=(float)Math.sqrt(dx*dx+dy*dy);
                if(len<dr.target.r+29f){
                    float boom=(7f+save.droneDamageLevel*1.6f)*save.generalDamageMultiplier()*(1f+save.droneSkillLevel*.008f)*5.5f;
                    aoe(dr.target.x,dr.target.y,135,boom,save.droneElement);gainSkill("drone",boom);
                    explode(dr.target.x,dr.target.y,elementColor(save.droneElement),40);killDrone(dr);dr.target=null;
                    game.assets.play(game.assets.explosion,game.settings,.25f);vibrate(22);continue;
                }
                float speed=355f+save.droneRateLevel*7f;
                dr.x+=dx/Math.max(1f,len)*speed*d;dr.y+=dy/Math.max(1f,len)*speed*d;
                dr.heading=MathUtils.atan2(dy,dx);applyDroneAura(dr,d);continue;
            }

            if(dr.target!=null){
                // Gun/missile drones close to standoff range instead of firing from the other side of the screen.
                float dx=dr.target.x-dr.x,dy=dr.target.y-dr.y,len=(float)Math.sqrt(dx*dx+dy*dy);float desired=145f;
                if(len>desired){
                    float speed=185f+save.droneRateLevel*3.5f;
                    dr.x+=dx/Math.max(1f,len)*speed*d;dr.y+=dy/Math.max(1f,len)*speed*d;
                }else{
                    float tangent=55f*d;dr.x+=-dy/Math.max(1f,len)*tangent;dr.y+=dx/Math.max(1f,len)*tangent;
                }
            }else{
                moveDroneOnPatrol(dr,d);
            }

            dr.x=MathUtils.clamp(dr.x,55f,W-55f);dr.y=MathUtils.clamp(dr.y,245f,1500f);
            if(dist2(oldX,oldY,dr.x,dr.y)>.01f)dr.heading=MathUtils.atan2(dr.y-oldY,dr.x-oldX);
            dr.cooldown-=d;dr.auraTick-=d;applyDroneAura(dr,d);
            if(dr.jamTime>0f)continue;
            if(dr.cooldown>0||dr.target==null)continue;

            int near=nearbyDroneCount(dr,190);
            float auraDamage=1f+(save.droneAuraElement==Element.FIRE?near*.07f:near*.02f);
            float auraRate=1f+(save.droneAuraElement==Element.LIGHTNING?near*.07f:save.droneAuraElement==Element.ICE?near*.03f:0f);
            float rate=(1f+save.droneRateLevel*.18f)*save.generalRateMultiplier()*auraRate*(1f+save.droneSkillLevel*.006f)*(overdriveTime>0?1.55f:1f);
            float dmg=(7f+save.droneDamageLevel*1.6f)*save.generalDamageMultiplier()*auraDamage*(1f+save.droneSkillLevel*.008f);
            if(dr.type==DroneType.GUN){
                fireProjectile(dr.x,dr.y,dr.target,dmg,save.droneElement,ShotKind.BULLET,0);gainSkill("drone",dmg);
                dr.cooldown=Math.max(.06f,.55f/rate);game.assets.play(game.assets.shot,game.settings,.055f);
            }else if(dr.type==DroneType.MISSILE){
                fireProjectile(dr.x,dr.y,dr.target,dmg*1.9f,save.droneElement,ShotKind.ROCKET,62+save.droneAuraLevel*3);gainSkill("drone",dmg*1.9f);
                dr.cooldown=Math.max(.22f,1.5f/rate);game.assets.play(game.assets.rocket,game.settings,.08f);
            }
        }
    }

    private void moveDroneOnPatrol(Drone dr,float d){
        float baseX=540+dr.centerOffsetX+MathUtils.cos(dr.angle+dr.orbitPhase)*dr.orbitRadiusX+MathUtils.sin(dr.angle*.43f+dr.orbitPhase)*58f;
        float baseY=650+dr.centerOffsetY+MathUtils.sin(dr.angle*.71f+dr.orbitPhase)*dr.orbitRadiusY+MathUtils.cos(dr.angle*.37f)*38f;
        baseX=MathUtils.clamp(baseX,85f,W-85f);baseY=MathUtils.clamp(baseY,340f,1120f);
        float move=Math.min(1f,d*(.75f+dr.type.ordinal()*.08f));
        dr.x=MathUtils.lerp(dr.x,baseX,move);dr.y=MathUtils.lerp(dr.y,baseY,move);
    }

    private Drone mostDamagedDrone(Drone support){
        Drone best=null;float bestRatio=1.001f;float bestDist=Float.MAX_VALUE;
        for(int i=0;i<drones.size;i++){
            Drone d=drones.get(i);
            if(d==support||!d.alive||d.maxShield<=0f||d.shield>=d.maxShield-.5f)continue;
            float ratio=d.shield/d.maxShield;float dd=dist2(support.x,support.y,d.x,d.y);
            if(ratio<bestRatio-.02f || (Math.abs(ratio-bestRatio)<.02f && dd<bestDist)){
                best=d;bestRatio=ratio;bestDist=dd;
            }
        }
        return best;
    }

    private void infectedTurretAttack(Turret source){
        Drone d=nearestFriendlyDrone(source.x,source.y);Turret t=nearestFriendlyTurret(source.x,source.y,source);HostileProjectile p=new HostileProjectile();p.x=source.x;p.y=source.y;p.damage=6f+save.wave*.35f;p.attackKind=EnemyAttackKind.PARASITE;p.r=6f;
        if(d!=null&&(t==null||dist2(source.x,source.y,d.x,d.y)<dist2(source.x,source.y,t.x,t.y))){p.droneTarget=d;setHostileVelocity(p,d.x,d.y,520f);hostileProjectiles.add(p);}
        else if(t!=null){p.turretTarget=t;setHostileVelocity(p,t.x,t.y,520f);hostileProjectiles.add(p);}
    }
    private void infectedDroneAttack(Drone source){
        Turret t=nearestFriendlyTurret(source.x,source.y,null);Drone d=nearestFriendlyDroneExcept(source.x,source.y,source);HostileProjectile p=new HostileProjectile();p.x=source.x;p.y=source.y;p.damage=4.5f+save.wave*.25f;p.attackKind=EnemyAttackKind.PARASITE;p.r=5f;
        if(MathUtils.random()<.28f || (t==null&&d==null)){p.planetTarget=true;setHostileVelocity(p,540f,GROUND_Y+8f,500f);hostileProjectiles.add(p);}
        else if(t!=null&&(d==null||dist2(source.x,source.y,t.x,t.y)<dist2(source.x,source.y,d.x,d.y))){p.turretTarget=t;setHostileVelocity(p,t.x,t.y,500f);hostileProjectiles.add(p);}
        else if(d!=null){p.droneTarget=d;setHostileVelocity(p,d.x,d.y,500f);hostileProjectiles.add(p);}
    }
    private Turret nearestFriendlyTurret(float x,float y,Turret exclude){Turret best=null;float bd=Float.MAX_VALUE;for(int i=0;i<turrets.size;i++){Turret t=turrets.get(i);if(t==exclude||t.broken)continue;float dd=dist2(x,y,t.x,t.y);if(dd<bd){bd=dd;best=t;}}return best;}
    private Drone nearestFriendlyDrone(float x,float y){return nearestFriendlyDroneExcept(x,y,null);}
    private Drone nearestFriendlyDroneExcept(float x,float y,Drone exclude){Drone best=null;float bd=Float.MAX_VALUE;for(int i=0;i<drones.size;i++){Drone d=drones.get(i);if(d==exclude||!d.alive)continue;float dd=dist2(x,y,d.x,d.y);if(dd<bd){bd=dd;best=d;}}return best;}

    private void applyDroneAura(Drone dr,float d){
        float radius=125+save.droneAuraLevel*13;
        if(dr.type==DroneType.SUPPORT)return;
        if(!hasDroneAura())return;
        if(save.droneAuraElement==Element.FIRE){for(Enemy e:enemies)if(dist2(dr.x,dr.y,e.x,e.y)<radius*radius){e.burnDps=Math.max(e.burnDps,2.5f+save.droneAuraLevel*.8f);e.burnTime=Math.max(e.burnTime,.6f);}}
        else if(save.droneAuraElement==Element.ICE){for(Enemy e:enemies)if(dist2(dr.x,dr.y,e.x,e.y)<radius*radius){e.slow=Math.min(e.slow,.55f);e.slowTime=Math.max(e.slowTime,.3f);}}
        else if(save.droneAuraElement==Element.LIGHTNING && dr.auraTick<=0){dr.auraTick=Math.max(.18f,.7f/(1+save.droneAuraLevel*.12f));Enemy e=nearestEnemy(dr.x,dr.y,radius);if(e!=null){dealDamage(e,2.5f+save.droneAuraLevel*1.2f,Element.LIGHTNING,true);}}
    }

    private void healNearbyDrones(Drone support,float d){
        float radius=175+save.droneAuraLevel*14;
        for(int i=0;i<drones.size;i++){
            Drone dr=drones.get(i);
            if(dr!=support&&dr.alive&&dist2(support.x,support.y,dr.x,dr.y)<radius*radius){
                dr.shield=Math.min(dr.maxShield,dr.shield+(10+save.droneAuraLevel*2.2f)*d);
            }
        }
    }

    private int nearbyDroneCount(Drone source,float radius){int n=0;float r2=radius*radius;for(int i=0;i<drones.size;i++){Drone d=drones.get(i);if(d!=source&&d.alive&&dist2(source.x,source.y,d.x,d.y)<r2)n++;}return n;}
    private void killDrone(Drone d){d.alive=false;d.shield=0;d.respawn=Math.max(5f,11f-save.droneShieldLevel*.4f);explode(d.x,d.y,Ui.RED,30);for(int i=0;i<4;i++){Particle p=new Particle();p.x=d.x;p.y=d.y;p.vx=MathUtils.random(-180f,180f);p.vy=MathUtils.random(-80f,220f);p.life=p.maxLife=MathUtils.random(.35f,.8f);p.size=MathUtils.random(4f,8f);p.color=new Color(.35f,.55f,.65f,1);particles.add(p);}}

    private void updateProjectiles(float d){
        for(int i=projectiles.size-1;i>=0;i--){
            Projectile p=projectiles.get(i);p.life-=d;
            if(p.kind==ShotKind.ROCKET && p.target!=null && !p.target.dead){
                float speed=(float)Math.sqrt(p.vx*p.vx+p.vy*p.vy);
                float dx=p.target.x-p.x,dy=p.target.y-p.y,len=(float)Math.sqrt(dx*dx+dy*dy);
                if(len>1f){float tvx=dx/len*speed,tvy=dy/len*speed;float steer=Math.min(1f,d*5.5f);p.vx=MathUtils.lerp(p.vx,tvx,steer);p.vy=MathUtils.lerp(p.vy,tvy,steer);}
            }
            p.x+=p.vx*d;p.y+=p.vy*d;
            Enemy hit=null;for(Enemy e:enemies)if(!e.dead&&dist2(p.x,p.y,e.x,e.y)<(e.r+p.r)*(e.r+p.r)){hit=e;break;}
            if(hit!=null){if(p.aoe>0){aoe(p.x,p.y,p.aoe,p.damage,p.element);explode(p.x,p.y,elementColor(p.element),22);}else{dealDamage(hit,p.damage,p.element,true);burst(p.x,p.y,elementColor(p.element),6);}projectiles.removeIndex(i);}
            else if(p.life<=0||p.x<0||p.x>W||p.y<0||p.y>H+100)projectiles.removeIndex(i);
        }
    }

    private void updateHostileProjectiles(float d){
        for(int i=hostileProjectiles.size-1;i>=0;i--){
            HostileProjectile p=hostileProjectiles.get(i);p.life-=d;p.x+=p.vx*d;p.y+=p.vy*d;boolean hit=false;
            if(p.droneTarget!=null && p.droneTarget.alive && dist2(p.x,p.y,p.droneTarget.x,p.droneTarget.y)<(p.r+23f)*(p.r+23f)){
                applyAlienHit(p.droneTarget,p);hit=true;
            }else if(p.turretTarget!=null && !p.turretTarget.broken && dist2(p.x,p.y,p.turretTarget.x,p.turretTarget.y)<(p.r+40f)*(p.r+40f)){
                applyAlienHit(p.turretTarget,p);hit=true;
            }else if(p.planetTarget && p.y<=GROUND_Y+26f){
                save.integrity-=Math.max(1.5f,p.damage*.55f);hit=true;vibrate(8);
            }
            if(hit){burst(p.x,p.y,alienAttackColor(p.attackKind),9);hostileProjectiles.removeIndex(i);}
            else if(p.life<=0||p.x<-30||p.x>W+30||p.y<-30||p.y>H+50)hostileProjectiles.removeIndex(i);
        }
    }

    private void applyAlienHit(Turret t,HostileProjectile p){
        t.shield-=p.damage;
        if(p.attackKind==EnemyAttackKind.CORROSION)t.corrosionTime=Math.max(t.corrosionTime,5.5f);
        else if(p.attackKind==EnemyAttackKind.PARASITE)t.infectionHits=Math.max(t.infectionHits,4);
        else if(p.attackKind==EnemyAttackKind.DISRUPTION)t.jamTime=Math.max(t.jamTime,3.5f);
        if(t.shield<=0){t.shield=0;t.broken=true;explode(t.x,t.y,Ui.RED,28);}
    }
    private void applyAlienHit(Drone dr,HostileProjectile p){
        dr.shield-=p.damage;
        if(p.attackKind==EnemyAttackKind.CORROSION)dr.corrosionTime=Math.max(dr.corrosionTime,5f);
        else if(p.attackKind==EnemyAttackKind.PARASITE)dr.infectionHits=Math.max(dr.infectionHits,3);
        else if(p.attackKind==EnemyAttackKind.DISRUPTION)dr.jamTime=Math.max(dr.jamTime,3f);
        if(dr.shield<=0)killDrone(dr);
    }

    private void updateHoles(float d){
        for(int i=holes.size-1;i>=0;i--){BlackHole h=holes.get(i);h.life-=d;float pull=300f*d;for(Enemy e:enemies){float dx=h.x-e.x,dy=h.y-e.y,ds=dx*dx+dy*dy;if(ds<h.radius*h.radius&&ds>20){float len=(float)Math.sqrt(ds);float strength=(1f-len/h.radius)*gravityPullFactor(e,h.pullStrength);e.x+=dx/len*pull*strength;e.y+=dy/len*pull*strength;damageRaw(e,h.damage*d*.08f);}}if(h.life<=0){aoe(h.x,h.y,h.radius,h.damage,Element.NEUTRAL);explode(h.x,h.y,GRAV,45);holes.removeIndex(i);vibrate(50);}}
    }

    private float gravityPullFactor(Enemy e,float power){
        power=MathUtils.clamp(power,0f,1.5f);
        return switch(e.kind){
            case BOSS -> MathUtils.clamp(.05f+power*.30f,.05f,.42f);
            case TANK -> MathUtils.clamp(.32f+power*.28f,.32f,.72f);
            case ELITE -> MathUtils.clamp(.50f+power*.25f,.50f,.86f);
            default -> 1f;
        };
    }
    private void updateTrails(float d){
        for(int i=trails.size-1;i>=0;i--){
            TrailFx t=trails.get(i);t.life-=d;t.tick-=d;
            if(t.tick<=0f){
                t.tick=.11f;
                for(int j=0;j<enemies.size;j++){
                    Enemy e=enemies.get(j);if(e.dead)continue;
                    if(distToSegment(e.x,e.y,t.x1,t.y1,t.x2,t.y2)<t.width*.5f+e.r){
                        if(t.element==Element.GRAVITY&&hasGravity()){
                            float mx=(t.x1+t.x2)*.5f,my=(t.y1+t.y2)*.5f;float gf=gravityPullFactor(e,.35f);e.x+=MathUtils.clamp(mx-e.x,-12f,12f)*gf;e.y+=MathUtils.clamp(my-e.y,-12f,12f)*gf;damageRaw(e,t.damagePulse*.75f);
                        }else dealDamage(e,t.damagePulse,t.element,false);
                    }
                }
            }
            if(t.life<=0)trails.removeIndex(i);
        }
    }
    private void updateShockwaves(float d){
        for(int i=shockwaves.size-1;i>=0;i--){Shockwave w=shockwaves.get(i);w.life-=d;float p=1f-Math.max(0f,w.life)/w.maxLife;w.radius=w.maxRadius*p;if(w.life<=0)shockwaves.removeIndex(i);}
    }
    private void updateBonuses(float d){
        for(int i=bonuses.size-1;i>=0;i--){Bonus b=bonuses.get(i);b.life-=d;b.bob+=d*4f;b.y-=8f*d;if(b.life<=0||b.dead)bonuses.removeIndex(i);}
    }
    private void spawnBonus(){
        if(bonuses.size>0)return;
        Bonus b=new Bonus();
        float r=MathUtils.random();b.type=r<.45f?BonusType.CREDIT:r<.75f?BonusType.HEAL:BonusType.OVERDRIVE;
        b.x=MathUtils.random(120f,W-120f);b.y=MathUtils.random(760f,1450f);b.bob=MathUtils.random(0f,MathUtils.PI2);bonuses.add(b);
        banner=game.assets.t("bonus_incoming");bannerTime=.9f;
    }
    private boolean tryCollectBonus(float x,float y){
        for(int i=0;i<bonuses.size;i++){
            Bonus b=bonuses.get(i);float by=b.y+MathUtils.sin(b.bob)*8f;
            if(dist2(x,y,b.x,by)>58f*58f)continue;
            b.dead=true;
            if(b.type==BonusType.CREDIT){double gain=250+save.wave*45;save.credits+=gain;banner="+C "+(long)gain;game.assets.play(game.assets.buy,game.settings,.22f);}
            else if(b.type==BonusType.HEAL){save.integrity=Math.min(100f,save.integrity+22f);banner=game.assets.t("bonus_heal");game.assets.play(game.assets.plasma,game.settings,.18f);}
            else {overdriveTime=Math.max(overdriveTime,10f);banner=game.assets.t("bonus_overdrive");game.assets.play(game.assets.electric,game.settings,.22f);}
            bannerTime=1.2f;explode(b.x,by,b.type==BonusType.HEAL?Ui.GREEN:(b.type==BonusType.OVERDRIVE?ELEC:Ui.GOLD),26);vibrate(22);return true;
        }
        return false;
    }
    private void updateParticles(float d){for(int i=particles.size-1;i>=0;i--){Particle p=particles.get(i);p.life-=d;p.x+=p.vx*d;p.y+=p.vy*d;p.vx*=.985f;p.vy*=.985f;if(p.life<=0)particles.removeIndex(i);}}
    private void updateBeams(float d){for(int i=beams.size-1;i>=0;i--){Beam b=beams.get(i);b.life-=d;if(b.life<=0)beams.removeIndex(i);}}

    private void fireProjectile(float x,float y,Enemy target,float damage,Element element,ShotKind kind,float aoe){
        Projectile p=new Projectile();p.x=x;p.y=y;p.damage=damage;p.element=element;p.kind=kind;p.aoe=aoe;p.r=kind==ShotKind.ROCKET?10:5;p.target=target;
        float sp=kind==ShotKind.ROCKET?520:1180;
        Vector2 aim=kind==ShotKind.BULLET?predictIntercept(x,y,target,sp):new Vector2(target.x,target.y);
        float dx=aim.x-x,dy=aim.y-y,len=(float)Math.sqrt(dx*dx+dy*dy);if(len<1f)len=1f;p.vx=dx/len*sp;p.vy=dy/len*sp;projectiles.add(p);
    }

    /** Lead a vertically moving enemy so fast projectiles meet it instead of shooting behind it. */
    private Vector2 predictIntercept(float sx,float sy,Enemy target,float projectileSpeed){
        float rx=target.x-sx, ry=target.y-sy;
        float vx=0f, vy=-target.speed*target.slow;
        float a=vx*vx+vy*vy-projectileSpeed*projectileSpeed;
        float b=2f*(rx*vx+ry*vy);
        float c=rx*rx+ry*ry;
        float t=-1f;
        float disc=b*b-4f*a*c;
        if(Math.abs(a)<0.0001f){if(Math.abs(b)>0.0001f)t=-c/b;}
        else if(disc>=0){float root=(float)Math.sqrt(disc);float t1=(-b-root)/(2f*a),t2=(-b+root)/(2f*a);if(t1>0&&t2>0)t=Math.min(t1,t2);else t=Math.max(t1,t2);}
        if(t<0||t>2.5f)t=(float)Math.sqrt(c)/projectileSpeed;
        return new Vector2(target.x+vx*t,target.y+vy*t);
    }

    private void doTap(float x,float y){
        if(save.playSeconds-lastTapTime<.035f)return;lastTapTime=save.playSeconds;
        if(save.fingerElement==Element.GRAVITY&&hasGravity()){holes.add(makeHole(x,y,105+save.tapDamageLevel*2.5f,.85f,18f*(1+save.tapDamageLevel*.18f)));gainSkill("tap",12f);game.assets.play(game.assets.plasma,game.settings,.12f);vibrate(18);return;}
        Enemy target=nearestEnemy(x,y,85);float dmg=18f*(1+save.tapDamageLevel*.22f)*(1f+save.tapSkillLevel*.01f);
        if(target!=null){
            if(target.tapShieldTime>0f){shieldImpact(target);return;}
            dealDamage(target,dmg,save.fingerElement,true);gainSkill("tap",dmg);
        }
        burst(x,y,elementColor(save.fingerElement),10);vibrate(7);
    }

    private void doTrail(float x1,float y1,float x2,float y2){
        float dx=x2-x1,dy=y2-y1,len=(float)Math.sqrt(dx*dx+dy*dy);int steps=Math.max(1,(int)(len/34f));
        float dmg=4.5f*(1+save.tapDamageLevel*.13f)*save.generalDamageMultiplier()*(1f+save.trailSkillLevel*.008f);
        if(save.fingerElement==Element.GRAVITY&&hasGravity()){
            for(Enemy e:enemies){float dist=distToSegment(e.x,e.y,x1,y1,x2,y2);if(dist<95){float tx=(x1+x2)*.5f,ty=(y1+y2)*.5f;float gf=gravityPullFactor(e,.30f);e.x+=MathUtils.clamp(tx-e.x,-18,18)*gf;e.y+=MathUtils.clamp(ty-e.y,-18,18)*gf;damageRaw(e,dmg*.32f);}}
            stylizedTrail(x1,y1,x2,y2,Element.GRAVITY,dmg);gainSkill("trail",dmg*steps*.20f);return;
        }
        for(int s=0;s<=steps;s++){
            float a=s/(float)steps,px=x1+dx*a,py=y1+dy*a;
            for(Enemy e:enemies)if(!e.dead&&dist2(px,py,e.x,e.y)<(e.r+26)*(e.r+26))dealDamage(e,dmg*.32f,save.fingerElement,false);
        }
        stylizedTrail(x1,y1,x2,y2,save.fingerElement,dmg);gainSkill("trail",dmg*steps*.28f);
    }

    private void stylizedTrail(float x1,float y1,float x2,float y2,Element element,float damage){
        TrailFx fx=new TrailFx();fx.x1=x1;fx.y1=y1;fx.x2=x2;fx.y2=y2;fx.element=element;fx.seed=MathUtils.random(0f,999f);
        fx.maxLife=fx.life=element==Element.LIGHTNING?1.05f:1.28f;fx.damagePulse=damage*.22f;fx.tick=.08f;
        fx.width=element==Element.GRAVITY?92f:element==Element.FIRE?48f:element==Element.ICE?44f:element==Element.LIGHTNING?38f:36f;
        trails.add(fx);
        float dx=x2-x1,dy=y2-y1,len=(float)Math.sqrt(dx*dx+dy*dy);if(len<1f)return;
        int motes=Math.max(2,(int)(len/34f));
        if(element==Element.LIGHTNING){
            float nx=-dy/len,ny=dx/len;float px=x1,py=y1;int parts=Math.max(3,Math.min(12,(int)(len/25f)));
            for(int i=1;i<=parts;i++){float a=i/(float)parts;float wob=(i==parts?0:MathUtils.random(-24f,24f));float qx=x1+dx*a+nx*wob,qy=y1+dy*a+ny*wob;beam(px,py,qx,qy,ELEC,10,.18f);beam(px,py,qx,qy,Color.WHITE,2.2f,.12f);px=qx;py=qy;}
        }
        for(int i=0;i<motes;i++){
            float a=MathUtils.random(),px=x1+dx*a,py=y1+dy*a;Particle q=new Particle();q.x=px;q.y=py;
            if(element==Element.FIRE){q.vx=MathUtils.random(-38f,38f);q.vy=MathUtils.random(65f,165f);q.color=new Color(MathUtils.randomBoolean()?FIRE:new Color(1f,.72f,.16f,1));}
            else if(element==Element.ICE){q.vx=MathUtils.random(-48f,48f);q.vy=MathUtils.random(-20f,60f);q.color=new Color(MathUtils.randomBoolean()?ICE:Color.WHITE);}
            else if(element==Element.GRAVITY){q.vx=(x1+dx*.5f-px)*.7f;q.vy=(y1+dy*.5f-py)*.7f;q.color=new Color(GRAV);}
            else if(element==Element.LIGHTNING){q.vx=MathUtils.random(-55f,55f);q.vy=MathUtils.random(-55f,55f);q.color=new Color(ELEC);}
            else {q.vx=MathUtils.random(-30f,30f);q.vy=MathUtils.random(-15f,50f);q.color=new Color(Ui.CYAN);}
            q.life=q.maxLife=MathUtils.random(.35f,.9f);q.size=MathUtils.random(2.5f,7.5f);particles.add(q);
        }
    }

    private void doPlasma(float x,float y,float charge){
        float radius=95+charge*145;float dmg=(38+charge*105)*(1+save.tapDamageLevel*.14f)*save.generalDamageMultiplier()*(1f+save.plasmaSkillLevel*.01f);gainSkill("plasma",dmg);
        if(save.fingerElement==Element.GRAVITY&&hasGravity()){holes.add(makeHole(x,y,radius*1.3f,1.5f+charge*1.4f,dmg*1.25f));}
        else{aoe(x,y,radius,dmg,save.fingerElement);explode(x,y,elementColor(save.fingerElement),(int)(28+charge*35));}
        game.assets.play(game.assets.plasma,game.settings,.32f);vibrate((int)(30+charge*65));
    }

    private void doTwoFingerRift(){
        if(!hasAnnihilation()||annihilationCooldown>0f||annihilationUseTime>=3f)return;annihilationWasActive=true;
        Touch a=null,b=null;for(Touch t:touches)if(t.down){if(a==null)a=t;else{b=t;break;}}if(a==null||b==null)return;
        float dmg=7.5f*save.generalDamageMultiplier()*(1+save.tapDamageLevel*.08f);
        if(save.fingerElement==Element.GRAVITY&&hasGravity()){for(Enemy e:enemies){float dist=distToSegment(e.x,e.y,a.x,a.y,b.x,b.y);if(dist<120){float mx=(a.x+b.x)*.5f,my=(a.y+b.y)*.5f;float gf=gravityPullFactor(e,.95f);e.x+=MathUtils.clamp(mx-e.x,-26,26)*gf;e.y+=MathUtils.clamp(my-e.y,-26,26)*gf;damageRaw(e,dmg);}}beam(a.x,a.y,b.x,b.y,GRAV,26,.14f);}
        else{for(Enemy e:enemies)if(distToSegment(e.x,e.y,a.x,a.y,b.x,b.y)<72+e.r)dealDamage(e,dmg,save.fingerElement,false);beam(a.x,a.y,b.x,b.y,elementColor(save.fingerElement),24,.14f);}
    }

    private BlackHole makeHole(float x,float y,float r,float life,float damage){BlackHole h=new BlackHole();h.x=x;h.y=y;h.radius=r;h.life=h.maxLife=life;h.damage=damage;h.pullStrength=MathUtils.clamp((r-80f)/210f+.15f,0.15f,1.35f);return h;}
    private void aoe(float x,float y,float r,float dmg,Element elem){for(int i=0;i<enemies.size;i++){Enemy e=enemies.get(i);if(!e.dead&&dist2(x,y,e.x,e.y)<(r+e.r)*(r+e.r))dealDamage(e,dmg,elem,true);}}

    private void dealDamage(Enemy e,float dmg,Element elem,boolean chain){
        if(e==null||e.dead)return;
        float mult=directDamageMultiplier(e,elem);float applied=dmg*mult;damageRaw(e,applied);elementImpact(e,elem,applied);
        if(elem==Element.FIRE){float burnRes=e.resistElement==Element.FIRE?.30f:1f;e.burnDps=Math.max(e.burnDps,dmg*.14f*burnRes);e.burnTime=Math.max(e.burnTime,3.3f*burnRes+.7f);}
        else if(elem==Element.ICE){e.chill+=Math.max(.25f,mult);e.slow=Math.max(.72f,.56f+(1f-mult)*.22f);e.slowTime=Math.max(e.slowTime,2.2f);if(e.chill>=4f){e.freezeTime=Math.max(e.freezeTime,1.1f*mult);e.chill=1.5f;}if(chain)game.assets.play(game.assets.ice,game.settings,.035f);}
        else if(elem==Element.LIGHTNING&&chain){chainLightning(e,dmg*.62f,2+(hasLightning()?1:0));game.assets.play(game.assets.electric,game.settings,.045f);}
    }
    private float directDamageMultiplier(Enemy target,Element elem){
        float m=1f;
        if(target.resistElement==elem && elem!=Element.NEUTRAL)m*=.30f;
        for(int i=0;i<enemies.size;i++){
            Enemy aura=enemies.get(i);if(aura.dead||aura==target||aura.auraRadius<=0f)continue;
            if(dist2(target.x,target.y,aura.x,aura.y)>aura.auraRadius*aura.auraRadius)continue;
            if(aura.archetype==EnemyArchetype.GUARDIAN)m*=1f-aura.auraReduction;
            else if(aura.archetype==EnemyArchetype.ELEMENT_WARD && elem!=Element.NEUTRAL && elem!=Element.GRAVITY)m*=1f-aura.auraReduction;
        }
        return MathUtils.clamp(m,.08f,1f);
    }
    private void damageRaw(Enemy e,float dmg){e.hp-=dmg;if(e.hp<=0&&!e.dead)killEnemy(e);}
    private void chainLightning(Enemy from,float dmg,int jumps){
        Enemy cur=from;Array<Enemy> used=new Array<>();used.add(from);for(int j=0;j<jumps;j++){Enemy next=null;float best=250*250;for(int i=0;i<enemies.size;i++){Enemy e=enemies.get(i);if(e.dead||used.contains(e,true))continue;float dd=dist2(cur.x,cur.y,e.x,e.y);if(dd<best){best=dd;next=e;}}if(next==null)break;lightningArc(cur.x,cur.y,next.x,next.y);dealDamage(next,dmg*(1f-j*.13f),Element.LIGHTNING,false);used.add(next);cur=next;}
    }

    private void lightningArc(float x1,float y1,float x2,float y2){
        float dx=x2-x1,dy=y2-y1,len=(float)Math.sqrt(dx*dx+dy*dy);if(len<1f)return;float nx=-dy/len,ny=dx/len;int parts=Math.max(4,Math.min(10,(int)(len/28f)));float px=x1,py=y1;
        for(int i=1;i<=parts;i++){float q=i/(float)parts;float off=i==parts?0f:MathUtils.random(-18f,18f);float qx=x1+dx*q+nx*off,qy=y1+dy*q+ny*off;beam(px,py,qx,qy,ELEC,8f,.14f);beam(px,py,qx,qy,Color.WHITE,2f,.11f);if(i%2==0&&i<parts){float branch=MathUtils.random(-1f,1f);beam(qx,qy,qx+nx*branch*30f-dx/len*12f,qy+ny*branch*30f-dy/len*12f,ELEC,2.4f,.10f);}px=qx;py=qy;}
    }

    private void elementImpact(Enemy e,Element elem,float dmg){
        if(elem==Element.FIRE){for(int i=0;i<7;i++){Particle p=new Particle();p.x=e.x+MathUtils.random(-e.r*.5f,e.r*.5f);p.y=e.y;p.vx=MathUtils.random(-50f,50f);p.vy=MathUtils.random(90f,190f);p.life=p.maxLife=MathUtils.random(.25f,.65f);p.size=MathUtils.random(3f,7f);p.color=new Color(i%2==0?FIRE:new Color(1f,.8f,.15f,1));particles.add(p);}}
        else if(elem==Element.ICE){for(int i=0;i<6;i++){float a=MathUtils.random(MathUtils.PI2);Particle p=new Particle();p.x=e.x;p.y=e.y;p.vx=MathUtils.cos(a)*MathUtils.random(60f,130f);p.vy=MathUtils.sin(a)*MathUtils.random(60f,130f);p.life=p.maxLife=MathUtils.random(.3f,.7f);p.size=MathUtils.random(3f,6f);p.color=new Color(i%2==0?ICE:Color.WHITE);particles.add(p);}}
        else if(elem==Element.LIGHTNING){for(int i=0;i<3;i++){float a=MathUtils.random(MathUtils.PI2);lightningArc(e.x,e.y,e.x+MathUtils.cos(a)*MathUtils.random(25f,65f),e.y+MathUtils.sin(a)*MathUtils.random(25f,65f));}}
    }

    private void shieldImpact(Enemy e){
        Color c=new Color(.62f,.94f,1f,1);Shockwave w=new Shockwave();w.x=e.x;w.y=e.y;w.maxRadius=e.r*1.65f;w.life=w.maxLife=.18f;w.color=c;shockwaves.add(w);burst(e.x,e.y,c,6);vibrate(4);
    }


    private void killEnemy(Enemy e){
        e.dead=true;save.credits+=e.reward;save.totalKills++;recordBestiaryKill(e);if(e.kind==EnemyKind.BOSS){save.totalBossKills++;bossActive=false;bossEnemy=null;bossTimer=0;if(save.totalBossKills==5){banner=game.assets.t("new_technology")+" — "+game.assets.t("effects");bannerTime=3.2f;}else{banner=game.assets.t("boss_destroyed");bannerTime=2.5f;}}
        explode(e.x,e.y,colorFor(e),e.kind==EnemyKind.BOSS?65:(e.kind==EnemyKind.TANK||e.kind==EnemyKind.ELITE?30:16));game.assets.play(e.kind==EnemyKind.BOSS?game.assets.explosion:game.assets.pop,game.settings,e.kind==EnemyKind.BOSS?.55f:.09f);if(e.kind==EnemyKind.BOSS)vibrate(120);
    }

    private void recordBestiaryKill(Enemy e){
        switch(e.archetype){
            case FAST -> save.bestiaryFast++;case TANK -> save.bestiaryTank++;case ELITE -> save.bestiaryElite++;case STAR -> save.bestiaryStar++;
            case GUARDIAN -> save.bestiaryGuardian++;case PHASE -> save.bestiaryPhase++;case FIRE_RESIST -> save.bestiaryFireResist++;
            case ICE_RESIST -> save.bestiaryIceResist++;case LIGHTNING_RESIST -> save.bestiaryLightningResist++;case ELEMENT_WARD -> save.bestiaryWard++;
            case INFECTOR -> save.bestiaryInfector++;case BOSS -> save.bestiaryBoss++;default -> save.bestiaryBasic++;
        }
    }

    private Enemy nearestEnemy(float x,float y,float max){Enemy bestE=null;float best=max*max;for(Enemy e:enemies){if(e.dead)continue;float d=dist2(x,y,e.x,e.y);if(d<best){best=d;bestE=e;}}return bestE;}

    private void hostilePulse(){
        int attackers=Math.max(0,enemies.size/9)+(bossActive?2:0);for(int i=0;i<attackers;i++){if(enemies.size==0)break;Enemy e=enemies.random();if(!e.dead)enemyAttack(e);}
    }
    private void autoRepairTick(){
        for(Turret t:turrets)if(t.broken||t.shield<t.maxShield*.5f){double cost=6+save.wave*.5;if(cheatsEnabled()||save.credits>=cost){if(!cheatsEnabled())save.credits-=cost;t.shield=Math.min(t.maxShield,t.shield+12+save.turretShieldLevel*2);if(t.shield>t.maxShield*.25f)t.broken=false;}}
    }

    private void rebuildDefenses(){
        turrets.clear();
        int tc=Math.min(save.turretCount,save.turretCap());
        for(int i=0;i<tc;i++){
            Turret t=new Turret();
            if(tc==1)t.x=W*.5f;
            else {
                float span=Math.min(840f,240f+(tc-1)*150f);
                t.x=W*.5f-span*.5f+i*(span/(tc-1));
            }
            t.y=205;
            t.maxShield=100+save.turretShieldLevel*20;t.shield=t.maxShield;t.cooldown=MathUtils.random(.1f,.5f);t.aimX=t.x;t.aimY=t.y+200;turrets.add(t);
        }
        drones.clear();int index=0;for(int i=0;i<save.gunDrones;i++)drones.add(newDrone(DroneType.GUN,index++));for(int i=0;i<save.missileDrones;i++)drones.add(newDrone(DroneType.MISSILE,index++));for(int i=0;i<save.kamikazeDrones;i++)drones.add(newDrone(DroneType.KAMIKAZE,index++));for(int i=0;i<save.supportDrones;i++)drones.add(newDrone(DroneType.SUPPORT,index++));
    }
    private Drone newDrone(DroneType type,int index){
        Drone d=new Drone();d.type=type;d.angle=index*.73f;d.orbitPhase=index*1.17f+type.ordinal()*.61f;
        d.orbitRadiusX=215f+(index%4)*58f+MathUtils.random(-24f,24f);d.orbitRadiusY=105f+(index%5)*34f+MathUtils.random(-18f,18f);
        d.orbitSpeed=.22f+(index%4)*.055f+type.ordinal()*.018f;d.centerOffsetX=((index%3)-1)*82f;d.centerOffsetY=((index%4)-1.5f)*46f;
        d.x=MathUtils.clamp(540+d.centerOffsetX+MathUtils.cos(d.orbitPhase)*d.orbitRadiusX,85f,W-85f);
        d.y=MathUtils.clamp(650+d.centerOffsetY+MathUtils.sin(d.orbitPhase)*d.orbitRadiusY,340f,1120f);
        d.heading=0f;d.maxShield=60+save.droneShieldLevel*15;d.shield=d.maxShield;d.cooldown=.4f+index*.12f;return d;
    }

    private void resetSlot(){SaveData fresh=game.saves.fresh(save.slot);fresh.difficulty=save.difficulty;game.saves.save(fresh);game.changeScreen(new GameScreen(game,fresh));}
    private void defeat(){if(defeated)return;defeated=true;save.integrity=0;game.saves.save(save);vibrate(180);}

    private void draw(){
        viewport.apply();camera.update();Gdx.gl.glClearColor(Ui.BG.r,Ui.BG.g,Ui.BG.b,1);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        sr.setProjectionMatrix(camera.combined);batch.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
        // Background first, then the boss countdown behind all combat objects.
        sr.begin(ShapeRenderer.ShapeType.Filled);drawBackground();sr.end();
        if(bossActive)drawBossCountdownBackdrop();
        // SpriteBatch/ShapeRenderer switching on some Android GPUs can leave blending state dirty. Reassert it.
        Gdx.gl.glEnable(GL20.GL_BLEND);Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.begin(ShapeRenderer.ShapeType.Filled);drawGround();drawHoles();drawEnemies();drawFingerCharge();drawTrails();drawShockwaves();drawBonuses();drawProjectiles();drawHostileProjectiles();drawTurrets();drawDrones();drawParticles();drawBeams();sr.end();
        drawBonusIcons();
        drawHud();
        if(bannerTime>0)drawBanner();
        if(shopOpen)drawShop();
        if(debuffShopOpen)drawDebuffShop();
        if(effectShopOpen)drawEffectShop();
        if(elementConfigOpen)drawElementConfig();
        if(paused)drawPause();
        if(defeated)drawDefeat();
        if(uiPulseTime>0f)drawUiPulse();
    }


    private void drawBossCountdownBackdrop(){
        if(!bossActive)return;
        int seconds=Math.max(0,(int)Math.ceil(bossTimer));String text=Integer.toString(seconds);
        boolean urgent=seconds<=10;float pulse=urgent?(0.17f+0.045f*(.5f+.5f*MathUtils.sin(save.playSeconds*8f))):0.105f;
        Color c=urgent?new Color(1f,.16f,.18f,pulse):new Color(.72f,.82f,.95f,pulse);
        batch.begin();
        Ui.centered(batch,game.assets.font,text,new Rectangle(0,650,W,620),urgent?7.2f:6.8f,c);
        batch.end();
    }

    private void drawBackground(){
        sr.setColor(.004f,.008f,.028f,1);sr.rect(0,0,W,H);
        // Deep-space haze / nebula. Deliberately subtle so combat stays readable.
        sr.setColor(.10f,.02f,.24f,.075f);sr.circle(1650,1450,980,72);
        sr.setColor(.015f,.19f,.34f,.065f);sr.circle(-480,980,820,72);
        sr.setColor(.08f,.30f,.42f,.035f);sr.circle(520,1720,520,64);
        float time=save.playSeconds*.7f;
        for(int i=0;i<105;i++){
            float x=(i*197f+37f)%W;
            float y=(i*431f+83f-time*(.22f+(i%5)*.06f))%(H-GROUND_Y);
            if(y<0)y+=H-GROUND_Y;y+=GROUND_Y;
            float sz=1.1f+(i%4)*.65f;
            float tw=.30f+.22f*MathUtils.sin(save.playSeconds*(.7f+(i%7)*.09f)+i*2.17f);
            sr.setColor(i%9==0?.45f:.62f,i%11==0?.62f:.83f,1f,MathUtils.clamp(tw,.12f,.72f));sr.circle(x,y,sz,8);
        }
        // A few distant colored stars.
        for(int i=0;i<13;i++){float x=(i*83f+113f)%W,y=GROUND_Y+((i*247f+660f)%(H-GROUND_Y-120));sr.setColor(.25f,.72f,1f,.12f);sr.circle(x,y,7f+(i%3)*4f,18);}
        sr.setColor(.05f,.65f,1f,.028f);sr.circle(540,80,620,64);
    }
    private void drawGround(){sr.setColor(.006f,.055f,.078f,1f);sr.rect(0,0,W,GROUND_Y);sr.setColor(Ui.CYAN);sr.rect(0,GROUND_Y,W,3);}

    private void drawEnemies(){
        for(Enemy e:enemies){
            if(e.dead)continue;Color c=colorFor(e);float hp=MathUtils.clamp(e.hp/e.maxHp,0,1);float pulse=1f+.035f*MathUtils.sin(save.playSeconds*4f+e.x*.01f);
            sr.setColor(c.r,c.g,c.b,.045f);drawEnemyShape(e,e.r*1.85f*pulse);
            sr.setColor(c.r,c.g,c.b,.11f);drawEnemyShape(e,e.r*1.36f*pulse);
            sr.setColor(c.r,c.g,c.b,.70f);drawEnemyShape(e,e.r*1.04f);
            sr.setColor(.018f,.035f,.065f,.92f);drawEnemyShape(e,e.r*.86f);
            sr.setColor(c.r,c.g,c.b,.22f);drawEnemyShape(e,e.r*.68f);
            sr.setColor(c.r,c.g,c.b,.78f);drawEnemyShape(e,e.r*.23f);
            sr.setColor(1,1,1,.72f);sr.circle(e.x-e.r*.28f,e.y+e.r*.28f,Math.max(2.5f,e.r*.11f),20);
            if(e.burnTime>0)drawBurningEnemy(e);
            if(e.slow<1f||e.freezeTime>0)drawFrozenEnemy(e);
            if(e.tapShieldTime>0f)drawTapShield(e);
            drawEnemySpecialFx(e);
            if(e.r>34){sr.setColor(.04f,.06f,.09f,.92f);sr.rect(e.x-e.r,e.y+e.r+12,e.r*2,6);sr.setColor(hp>.35f?Ui.GREEN:Ui.RED);sr.rect(e.x-e.r,e.y+e.r+12,e.r*2*hp,6);}
        }
    }

    private void drawEnemyShape(Enemy e,float r){
        if(e.archetype==EnemyArchetype.STAR){filledStar(e.x,e.y,r,r*.45f,5,save.playSeconds*.22f);return;}
        if(e.shapeSides<3){sr.circle(e.x,e.y,r,40);return;}filledRegularPolygon(e.x,e.y,r,e.shapeSides,save.playSeconds*.18f+(e.x+e.y)*.001f);
    }
    private void filledStar(float cx,float cy,float outer,float inner,int points,float rot){
        int n=points*2;for(int i=0;i<n;i++){float a1=rot+i*MathUtils.PI2/n,a2=rot+(i+1)*MathUtils.PI2/n;float r1=(i%2==0)?outer:inner,r2=((i+1)%2==0)?outer:inner;sr.triangle(cx,cy,cx+MathUtils.cos(a1)*r1,cy+MathUtils.sin(a1)*r1,cx+MathUtils.cos(a2)*r2,cy+MathUtils.sin(a2)*r2);}
    }
    private void filledRegularPolygon(float cx,float cy,float r,int sides,float rot){
        for(int i=0;i<sides;i++){float a1=rot+i*MathUtils.PI2/sides,a2=rot+(i+1)*MathUtils.PI2/sides;sr.triangle(cx,cy,cx+MathUtils.cos(a1)*r,cy+MathUtils.sin(a1)*r,cx+MathUtils.cos(a2)*r,cy+MathUtils.sin(a2)*r);}
    }
    private void drawBurningEnemy(Enemy e){
        for(int i=0;i<6;i++){float a=i*MathUtils.PI2/6f+save.playSeconds*.8f;float bx=e.x+MathUtils.cos(a)*e.r*.72f,by=e.y+MathUtils.sin(a)*e.r*.72f;float h=e.r*(.45f+.18f*MathUtils.sin(save.playSeconds*7f+i));sr.setColor(i%2==0?new Color(1f,.25f,.03f,.52f):new Color(1f,.75f,.08f,.48f));sr.triangle(bx-5,by,bx+5,by,bx+MathUtils.cos(a)*h,by+MathUtils.sin(a)*h);}
        sr.setColor(1f,.35f,.04f,.13f);sr.circle(e.x,e.y,e.r*1.30f,32);
    }
    private void drawFrozenEnemy(Enemy e){
        sr.setColor(.55f,.9f,1f,e.freezeTime>0?.34f:.18f);drawEnemyShape(e,e.r*1.18f);
        sr.setColor(.82f,.98f,1f,.72f);for(int i=0;i<6;i++){float a=i*MathUtils.PI2/6f;float x=e.x+MathUtils.cos(a)*e.r*1.05f,y=e.y+MathUtils.sin(a)*e.r*1.05f;float nx=-MathUtils.sin(a),ny=MathUtils.cos(a);sr.triangle(x+nx*3,y+ny*3,x-nx*3,y-ny*3,x+MathUtils.cos(a)*14,y+MathUtils.sin(a)*14);}
    }
    private void drawTapShield(Enemy e){
        float r=e.r*1.45f*(1f+.03f*MathUtils.sin(save.playSeconds*9f));sr.setColor(.55f,.92f,1f,.12f);if(e.shapeSides>=3)filledRegularPolygon(e.x,e.y,r,Math.max(6,e.shapeSides),save.playSeconds*.45f);else sr.circle(e.x,e.y,r,40);sr.setColor(.8f,.98f,1f,.55f);for(int i=0;i<8;i++){float a=i*MathUtils.PI2/8f+save.playSeconds*.4f;sr.circle(e.x+MathUtils.cos(a)*r,e.y+MathUtils.sin(a)*r,3.2f,10);}
    }
    private void drawEnemySpecialFx(Enemy e){
        if(e.archetype==EnemyArchetype.GUARDIAN){
            float r=e.auraRadius;sr.setColor(.18f,.95f,.78f,.035f);sr.circle(e.x,e.y,r,48);for(int i=0;i<14;i++){float a=i*MathUtils.PI2/14f-save.playSeconds*.35f;sr.setColor(.25f,1f,.78f,.50f);sr.circle(e.x+MathUtils.cos(a)*r,e.y+MathUtils.sin(a)*r,3.2f,8);}
        }else if(e.archetype==EnemyArchetype.ELEMENT_WARD){
            float r=e.auraRadius;sr.setColor(.58f,.26f,1f,.025f);sr.circle(e.x,e.y,r,48);Color[] cs={FIRE,ICE,ELEC};for(int i=0;i<12;i++){float a=i*MathUtils.PI2/12f+save.playSeconds*.38f;Color c=cs[i%3];sr.setColor(c.r,c.g,c.b,.56f);sr.circle(e.x+MathUtils.cos(a)*r,e.y+MathUtils.sin(a)*r,3.6f,8);}
        }else if(e.resistElement!=Element.NEUTRAL){
            Color c=elementColor(e.resistElement);float r=e.r*1.38f;for(int i=0;i<3;i++){float a=i*MathUtils.PI2/3f+save.playSeconds*.85f;float x=e.x+MathUtils.cos(a)*r,y=e.y+MathUtils.sin(a)*r;sr.setColor(c.r,c.g,c.b,.82f);filledRegularPolygon(x,y,8f,4,a);}
        }else if(e.archetype==EnemyArchetype.INFECTOR){
            for(int i=0;i<5;i++){float a=i*MathUtils.PI2/5f-save.playSeconds*.9f;float r=e.r*1.28f;sr.setColor(.92f,.16f,1f,.68f);sr.circle(e.x+MathUtils.cos(a)*r,e.y+MathUtils.sin(a)*r,3.5f,8);}
        }
    }

    private void drawFingerCharge(){
        if(!hasPlasma())return;
        for(Touch t:touches){
            if(!t.down||t.dragged||t.twoFinger)continue;
            float held=save.playSeconds-t.startTime;if(held<.16f)continue;
            float charge=MathUtils.clamp((held-.16f)/1.35f,0f,1f);Color c=elementColor(save.fingerElement);
            float pulse=1f+.06f*MathUtils.sin(save.playSeconds*12f);float r=(50f+charge*58f)*pulse;
            sr.setColor(c.r,c.g,c.b,.055f+.08f*charge);sr.circle(t.x,t.y,r,48);
            sr.setColor(c.r,c.g,c.b,.12f+.15f*charge);sr.circle(t.x,t.y,25f+charge*18f,36);
            sr.setColor(1f,1f,1f,.18f+.42f*charge);sr.circle(t.x,t.y,5f+charge*4f,20);
            int dots=24;for(int i=0;i<dots;i++){float a=i*MathUtils.PI2/dots+save.playSeconds*(1.2f+charge*2.2f);float rr=r+MathUtils.sin(i*1.9f+save.playSeconds*7f)*5f;float px=t.x+MathUtils.cos(a)*rr,py=t.y+MathUtils.sin(a)*rr;sr.setColor(c.r,c.g,c.b,(i%3==0?.55f:.22f)*(.45f+.55f*charge));sr.circle(px,py,i%3==0?3.8f:2.2f,10);}
        }
    }

    private void drawProjectiles(){
        for(Projectile p:projectiles){Color c=elementColor(p.element);
            if(p.kind==ShotKind.ROCKET){sr.setColor(c.r,c.g,c.b,.15f);sr.circle(p.x-p.vx*.03f,p.y-p.vy*.03f,15,16);sr.setColor(c);filledRegularPolygon(p.x,p.y,9,4,MathUtils.atan2(p.vy,p.vx));}
            else if(p.element==Element.FIRE){sr.setColor(1f,.18f,.02f,.18f);sr.circle(p.x-p.vx*.012f,p.y-p.vy*.012f,12,12);sr.setColor(1f,.72f,.12f,.95f);sr.circle(p.x,p.y,5.5f,12);sr.setColor(Color.WHITE);sr.circle(p.x,p.y,2f,8);}
            else if(p.element==Element.ICE){sr.setColor(.30f,.80f,1f,.18f);sr.circle(p.x,p.y,11,12);sr.setColor(.78f,.97f,1f,.95f);filledRegularPolygon(p.x,p.y,6.5f,4,MathUtils.PI*.25f);}
            else if(p.element==Element.LIGHTNING){
                sr.setColor(ELEC.r,ELEC.g,ELEC.b,.16f);sr.circle(p.x,p.y,13,12);sr.setColor(1f,1f,1f,.96f);sr.circle(p.x,p.y,3.8f,10);
                float sp=(float)Math.sqrt(p.vx*p.vx+p.vy*p.vy),tx=sp>1f?-p.vx/sp:0f,ty=sp>1f?-p.vy/sp:0f,nx=-ty,ny=tx;
                float px=p.x,py=p.y;for(int k=1;k<=3;k++){float off=MathUtils.sin(save.playSeconds*34f+k*2.7f+p.x*.01f)*8f;float qx=p.x+tx*k*17f+nx*off,qy=p.y+ty*k*17f+ny*off;sr.setColor(ELEC.r,ELEC.g,ELEC.b,.78f);lineRect(px,py,qx,qy,4f);sr.setColor(1f,1f,1f,.82f);lineRect(px,py,qx,qy,1.3f);px=qx;py=qy;}
            }
            else {sr.setColor(c.r,c.g,c.b,.18f);sr.circle(p.x,p.y,10,12);sr.setColor(c);sr.circle(p.x,p.y,4,12);}
        }
    }


    private void drawHostileProjectiles(){
        for(HostileProjectile p:hostileProjectiles){
            Color c=alienAttackColor(p.attackKind);float sp=(float)Math.sqrt(p.vx*p.vx+p.vy*p.vy),nx=sp>1f?-p.vx/sp:0,ny=sp>1f?-p.vy/sp:0;
            sr.setColor(c.r,c.g,c.b,.15f);lineRect(p.x,p.y,p.x+nx*28f,p.y+ny*28f,p.r*2.4f);
            if(p.attackKind==EnemyAttackKind.PARASITE){sr.setColor(c.r,c.g,c.b,.78f);sr.circle(p.x,p.y,p.r*1.45f,6);sr.setColor(.12f,.01f,.15f,.95f);sr.circle(p.x,p.y,p.r*.65f,10);}
            else if(p.attackKind==EnemyAttackKind.CORROSION){sr.setColor(c.r,c.g,c.b,.92f);sr.circle(p.x,p.y,p.r,12);sr.setColor(1f,1f,.42f,.72f);sr.circle(p.x-2,p.y+2,Math.max(2f,p.r*.28f),8);}
            else if(p.attackKind==EnemyAttackKind.DISRUPTION){sr.setColor(c.r,c.g,c.b,.9f);filledRegularPolygon(p.x,p.y,p.r*1.2f,4,save.playSeconds*4f);}
            else {sr.setColor(c.r,c.g,c.b,.95f);sr.circle(p.x,p.y,p.r,14);sr.setColor(1f,.92f,.65f,.8f);sr.circle(p.x,p.y,Math.max(2f,p.r*.35f),12);}
        }
    }


    private void drawTurrets(){
        for(Turret t:turrets){
            float hp=t.maxShield<=0?0:t.shield/t.maxShield;Color accent=t.broken?Ui.RED:elementColor(save.turretElement);
            float rad=t.angle*MathUtils.degreesToRadians,fx=MathUtils.cos(rad),fy=MathUtils.sin(rad),nx=-fy,ny=fx;
            // Shield glow and armored tripod base.
            if(t.infectionHits>0){sr.setColor(.82f,.18f,1f,.12f);sr.circle(t.x,t.y,78,40);}else if(t.corrosionTime>0){sr.setColor(.45f,1f,.12f,.10f);sr.circle(t.x,t.y,74,40);}else if(t.jamTime>0){sr.setColor(1f,.58f,.10f,.10f);sr.circle(t.x,t.y,72,40);}
            sr.setColor(accent.r,accent.g,accent.b,t.broken?.05f:.055f+.04f*hp);sr.circle(t.x,t.y,64,40);
            sr.setColor(t.broken?new Color(.22f,.055f,.06f,1):new Color(.025f,.12f,.18f,1));
            sr.triangle(t.x-48,t.y-34,t.x-14,t.y+15,t.x-2,t.y-48);sr.triangle(t.x+48,t.y-34,t.x+14,t.y+15,t.x+2,t.y-48);
            sr.setColor(t.broken?new Color(.34f,.08f,.08f,1):new Color(.055f,.26f,.34f,1));sr.circle(t.x,t.y,43,12);
            sr.setColor(.008f,.028f,.05f,1);sr.circle(t.x,t.y,29,24);
            sr.setColor(accent.r,accent.g,accent.b,.78f);sr.circle(t.x,t.y,13,24);
            sr.setColor(1f,1f,1f,t.broken?.15f:.58f);sr.circle(t.x-4,t.y+5,4,16);
            // Rotating twin-rail head with visible recoil.
            float baseDist=20f,barrelLen=76f-t.recoil;
            float bx=t.x+fx*baseDist,by=t.y+fy*baseDist,ex=t.x+fx*barrelLen,ey=t.y+fy*barrelLen;
            sr.setColor(t.broken?new Color(.25f,.07f,.08f,1):new Color(.015f,.12f,.18f,1));
            lineRect(bx+nx*10,by+ny*10,ex+nx*10,ey+ny*10,12);lineRect(bx-nx*10,by-ny*10,ex-nx*10,ey-ny*10,12);
            sr.setColor(accent);lineRect(bx+nx*10,by+ny*10,ex+nx*10,ey+ny*10,4);lineRect(bx-nx*10,by-ny*10,ex-nx*10,ey-ny*10,4);
            sr.setColor(.03f,.08f,.11f,1);sr.circle(bx,by,22,20);sr.setColor(accent.r,accent.g,accent.b,.75f);sr.circle(bx,by,7,16);
            // Shield meter.
            sr.setColor(.025f,.045f,.07f,.94f);sr.rect(t.x-48,t.y-61,96,8);sr.setColor(hp>.4?Ui.GREEN:Ui.RED);sr.rect(t.x-48,t.y-61,96*Math.max(0,hp),8);
        }
    }

    private void drawDrones(){
        for(Drone d:drones){
            if(!d.alive)continue;Color c=d.type==DroneType.SUPPORT?Ui.GREEN:elementColor(save.droneElement);Color auraColor=d.type==DroneType.SUPPORT?Ui.GREEN:elementColor(save.droneAuraElement);float aura=125+save.droneAuraLevel*13;
            if(d.infectionHits>0){sr.setColor(.82f,.18f,1f,.11f);sr.circle(d.x,d.y,70,36);}else if(d.corrosionTime>0){sr.setColor(.45f,1f,.12f,.09f);sr.circle(d.x,d.y,65,36);}else if(d.jamTime>0){sr.setColor(1f,.58f,.10f,.09f);sr.circle(d.x,d.y,65,36);}
            sr.setColor(auraColor.r,auraColor.g,auraColor.b,.030f);sr.circle(d.x,d.y,aura,48);
            float fx=MathUtils.cos(d.heading),fy=MathUtils.sin(d.heading),nx=-fy,ny=fx;
            float noseX=d.x+fx*31f,noseY=d.y+fy*31f,backX=d.x-fx*23f,backY=d.y-fy*23f;
            // Common wing silhouette, intentionally larger and more readable than 0.4.
            sr.setColor(.018f,.07f,.10f,.98f);
            sr.triangle(noseX,noseY,backX+nx*34f,backY+ny*34f,backX+nx*10f,backY+ny*10f);
            sr.triangle(noseX,noseY,backX-nx*34f,backY-ny*34f,backX-nx*10f,backY-ny*10f);
            sr.setColor(c.r,c.g,c.b,.42f);
            sr.triangle(d.x+fx*10f,d.y+fy*10f,d.x-fx*18f+nx*29f,d.y-fy*18f+ny*29f,d.x-fx*5f+nx*7f,d.y-fy*5f+ny*7f);
            sr.triangle(d.x+fx*10f,d.y+fy*10f,d.x-fx*18f-nx*29f,d.y-fy*18f-ny*29f,d.x-fx*5f-nx*7f,d.y-fy*5f-ny*7f);
            // Type-specific equipment.
            if(d.type==DroneType.MISSILE){sr.setColor(Ui.GOLD.r,Ui.GOLD.g,Ui.GOLD.b,.75f);sr.circle(d.x+nx*22f,d.y+ny*22f,7,14);sr.circle(d.x-nx*22f,d.y-ny*22f,7,14);}
            else if(d.type==DroneType.KAMIKAZE){sr.setColor(Ui.RED.r,Ui.RED.g,Ui.RED.b,.72f);sr.triangle(noseX+fx*7f,noseY+fy*7f,d.x+nx*10f,d.y+ny*10f,d.x-nx*10f,d.y-ny*10f);}
            else if(d.type==DroneType.SUPPORT){sr.setColor(Ui.GREEN.r,Ui.GREEN.g,Ui.GREEN.b,.78f);lineRect(d.x-fx*14,d.y-fy*14,d.x+fx*14,d.y+fy*14,8);lineRect(d.x-nx*14,d.y-ny*14,d.x+nx*14,d.y+ny*14,8);}
            else {sr.setColor(c.r,c.g,c.b,.82f);lineRect(d.x,d.y,noseX,noseY,6);}
            sr.setColor(.006f,.025f,.045f,1);sr.circle(d.x,d.y,18,20);sr.setColor(c);sr.circle(d.x,d.y,8,16);sr.setColor(1,1,1,.58f);sr.circle(d.x-fx*2+nx*2,d.y-fy*2+ny*2,2.7f,12);
            float hp=d.shield/d.maxShield;sr.setColor(.025f,.045f,.065f,.92f);sr.rect(d.x-30,d.y-45,60,6);sr.setColor(hp>.35?Ui.GREEN:Ui.RED);sr.rect(d.x-30,d.y-45,60*hp,6);
        }
    }

    private void drawParticles(){for(Particle p:particles){float a=Math.max(0,p.life/p.maxLife);sr.setColor(p.color.r,p.color.g,p.color.b,a*.8f);sr.circle(p.x,p.y,p.size*a,10);}}
    private void drawBeams(){for(Beam b:beams){float a=b.life/b.maxLife;sr.setColor(b.color.r,b.color.g,b.color.b,a*.75f);lineRect(b.x1,b.y1,b.x2,b.y2,b.width);}}
    private void drawHoles(){for(BlackHole h:holes){float a=h.life/h.maxLife;sr.setColor(.16f,.02f,.25f,.75f);sr.circle(h.x,h.y,h.radius*.20f,48);sr.setColor(GRAV.r,GRAV.g,GRAV.b,.10f+.10f*a);sr.circle(h.x,h.y,h.radius*(.85f+.1f*MathUtils.sin(save.playSeconds*8)),64);sr.setColor(.85f,.45f,1f,.55f);sr.circle(h.x,h.y,h.radius*.07f,32);}}
    private void drawTrails(){
        for(TrailFx t:trails){
            float a=MathUtils.clamp(t.life/t.maxLife,0f,1f);
            if(t.element==Element.LIGHTNING) drawLightningTrail(t,a);
            else drawEnergyTrail(t,a);
        }
    }

    private void drawEnergyTrail(TrailFx t,float alpha){
        float dx=t.x2-t.x1,dy=t.y2-t.y1,len=(float)Math.sqrt(dx*dx+dy*dy);if(len<1f)return;
        Color mid=elementColor(t.element);
        Color core=t.element==Element.FIRE?new Color(1f,.95f,.55f,1f):t.element==Element.ICE?Color.WHITE:t.element==Element.GRAVITY?new Color(.90f,.58f,1f,1f):Color.WHITE;
        float outerW=t.element==Element.GRAVITY?66f:t.element==Element.FIRE?44f:t.element==Element.ICE?38f:36f;
        float midW=t.element==Element.GRAVITY?30f:t.element==Element.FIRE?19f:t.element==Element.ICE?16f:15f;
        float coreW=t.element==Element.GRAVITY?5f:4.5f;
        // Continuous ribbon. No per-segment wobble: adjacent drag samples share the same endpoints,
        // so the path reads as one stroke instead of disconnected sticks.
        sr.setColor(mid.r,mid.g,mid.b,(t.element==Element.GRAVITY?.12f:.10f)*alpha);lineRect(t.x1,t.y1,t.x2,t.y2,outerW*(.65f+.35f*alpha));
        sr.setColor(mid.r,mid.g,mid.b,.38f*alpha);lineRect(t.x1,t.y1,t.x2,t.y2,midW*(.75f+.25f*alpha));
        sr.setColor(core.r,core.g,core.b,.86f*alpha);lineRect(t.x1,t.y1,t.x2,t.y2,coreW);
        // Round caps hide joins between samples.
        sr.setColor(mid.r,mid.g,mid.b,.28f*alpha);sr.circle(t.x1,t.y1,midW*.55f,14);sr.circle(t.x2,t.y2,midW*.55f,14);
        sr.setColor(core.r,core.g,core.b,.72f*alpha);sr.circle(t.x2,t.y2,Math.max(2.4f,coreW*.75f),12);
        // A small moving shimmer along the lingering path.
        int dots=Math.max(1,Math.min(5,(int)(len/55f)));
        for(int i=0;i<dots;i++){
            float q=(i+1f)/(dots+1f);float phase=save.playSeconds*1.7f+t.seed*.013f+i*.31f;
            float px=t.x1+dx*q,py=t.y1+dy*q;
            float nx=-dy/len,ny=dx/len;float off=MathUtils.sin(phase*4f)*3.5f;
            sr.setColor(core.r,core.g,core.b,.34f*alpha);sr.circle(px+nx*off,py+ny*off,2.4f+alpha*1.8f,10);
        }
        if(t.element==Element.FIRE){
            float nx=-dy/len,ny=dx/len;int flames=Math.max(2,Math.min(7,(int)(len/42f)));
            for(int i=0;i<flames;i++){float q=(i+.5f)/flames;float px=t.x1+dx*q,py=t.y1+dy*q;float side=MathUtils.sin(t.seed+i*2.3f+save.playSeconds*5f)*10f;float h=18f+10f*MathUtils.sin(save.playSeconds*8f+i);sr.setColor(1f,.30f,.03f,.42f*alpha);sr.triangle(px+nx*(side-6),py+ny*(side-6),px+nx*(side+6),py+ny*(side+6),px+nx*side,py+ny*side+h);sr.setColor(1f,.88f,.25f,.55f*alpha);sr.circle(px+nx*side,py+ny*side+4f,3.5f,10);}
        }else if(t.element==Element.ICE){
            float nx=-dy/len,ny=dx/len;int shards=Math.max(2,Math.min(7,(int)(len/48f)));for(int i=0;i<shards;i++){float q=(i+.5f)/shards;float px=t.x1+dx*q,py=t.y1+dy*q;float side=(i%2==0?1:-1)*(11f+4f*MathUtils.sin(t.seed+i));sr.setColor(.78f,.96f,1f,.62f*alpha);sr.triangle(px+nx*(side-3),py+ny*(side-3),px+nx*(side+3),py+ny*(side+3),px+nx*side+nx*12f,py+ny*side+ny*12f);}
        }
    }

    private void drawLightningTrail(TrailFx t,float alpha){
        float dx=t.x2-t.x1,dy=t.y2-t.y1,len=(float)Math.sqrt(dx*dx+dy*dy);if(len<1f)return;
        float nx=-dy/len,ny=dx/len;int parts=Math.max(4,Math.min(12,(int)(len/24f)+1));
        float px=t.x1,py=t.y1;float flick=save.playSeconds*31f+t.seed*.17f;
        for(int i=1;i<=parts;i++){
            float q=i/(float)parts;float jitter=i==parts?0f:(MathUtils.sin(flick+i*3.17f)*13f+MathUtils.sin(flick*1.73f+i*5.1f)*7f);
            float qx=t.x1+dx*q+nx*jitter,qy=t.y1+dy*q+ny*jitter;
            sr.setColor(ELEC.r,ELEC.g,ELEC.b,.12f*alpha);lineRect(px,py,qx,qy,21f);
            sr.setColor(ELEC.r,ELEC.g,ELEC.b,.78f*alpha);lineRect(px,py,qx,qy,5.2f);
            sr.setColor(1f,1f,1f,.96f*alpha);lineRect(px,py,qx,qy,1.55f);
            if(i<parts && i%3==0){float side=((i/3)%2==0?1f:-1f);float bl=18f+10f*Math.abs(MathUtils.sin(flick+i));float bx=qx+nx*side*bl-dx/len*8f,by=qy+ny*side*bl-dy/len*8f;sr.setColor(ELEC.r,ELEC.g,ELEC.b,.58f*alpha);lineRect(qx,qy,bx,by,2.2f);sr.setColor(1f,1f,1f,.65f*alpha);lineRect(qx,qy,bx,by,.8f);}
            px=qx;py=qy;
        }
    }

    private void drawShockwaves(){for(Shockwave w:shockwaves){float a=MathUtils.clamp(w.life/w.maxLife,0f,1f);sr.setColor(w.color.r,w.color.g,w.color.b,.12f*a);sr.circle(w.x,w.y,w.radius,48);sr.setColor(1f,1f,1f,.10f*a);sr.circle(w.x,w.y,Math.max(5f,w.radius*.45f),36);}}
    private void drawBonuses(){for(Bonus b:bonuses){float by=b.y+MathUtils.sin(b.bob)*8f;Color c=b.type==BonusType.CREDIT?Ui.GOLD:(b.type==BonusType.HEAL?Ui.GREEN:ELEC);float pulse=1f+.08f*MathUtils.sin(save.playSeconds*8f+b.x);sr.setColor(c.r,c.g,c.b,.08f);sr.circle(b.x,by,58f*pulse,32);sr.setColor(c.r,c.g,c.b,.30f);sr.circle(b.x,by,42f*pulse,32);sr.setColor(.01f,.03f,.06f,.88f);sr.circle(b.x,by,32f,28);}}
    private void drawBonusIcons(){if(bonuses.size==0)return;batch.begin();for(Bonus b:bonuses){Texture t=game.assets.icon(b.type==BonusType.CREDIT?"bonus_credit":b.type==BonusType.HEAL?"bonus_heal":"bonus_overdrive");if(t!=null){float by=b.y+MathUtils.sin(b.bob)*8f;batch.setColor(1,1,1,Math.min(1f,b.life));batch.draw(t,b.x-30,by-30,60,60);}}batch.setColor(Color.WHITE);batch.end();}

    private void drawHud(){
        float integrity=MathUtils.clamp(save.integrity/100f,0f,1f);
        float progress=bossActive&&bossEnemy!=null?MathUtils.clamp(bossEnemy.hp/bossEnemy.maxHp,0f,1f):MathUtils.clamp(waveClock/(save.wave==1?34f:35f),0f,1f);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        // Wave / boss progress at the top edge.
        float wx=28,wy=1886,ww=1024,wh=13;Color wc=bossActive?Ui.RED:Ui.CYAN;
        sr.setColor(.015f,.035f,.055f,.88f);sr.rect(wx,wy,ww,wh);
        sr.setColor(wc.r,wc.g,wc.b,.22f);sr.rect(wx-2,wy-3,ww*progress+4,wh+6);sr.setColor(wc);sr.rect(wx,wy,ww*progress,wh);
        // Planet integrity at the bottom.
        float bx=215,by=112,bw=650,bh=31;Color hc=integrity>.55f?Ui.GREEN:(integrity>.28f?Ui.GOLD:Ui.RED);
        sr.setColor(hc.r,hc.g,hc.b,.08f);sr.rect(bx-16,by-13,bw+32,bh+26);
        sr.setColor(.012f,.035f,.055f,.98f);sr.rect(bx-8,by-7,bw+16,bh+14);sr.circle(bx,by+bh*.5f,bh*.72f,24);sr.circle(bx+bw,by+bh*.5f,bh*.72f,24);
        sr.setColor(.04f,.10f,.14f,.96f);sr.rect(bx,by,bw,bh);
        sr.setColor(hc.r,hc.g,hc.b,.24f);sr.rect(bx-4,by-4,(bw+8)*integrity,bh+8);sr.setColor(hc);sr.rect(bx,by,bw*integrity,bh);
        sr.setColor(1,1,1,.46f);sr.rect(bx,by+bh-4,bw*integrity,4);sr.setColor(.01f,.035f,.055f,.8f);for(int i=1;i<10;i++)sr.rect(bx+bw*i/10f-2,by,4,bh);
        if(hasAnnihilation()){
            float ax=300,ay=84,aw=480,ah=10;float ap;Color ac;
            if(annihilationCooldown>0f){float maxCd=Math.max(8f,28f-save.ultimateSkillLevel*.35f);ap=1f-MathUtils.clamp(annihilationCooldown/maxCd,0f,1f);ac=Ui.CYAN;}
            else {ap=1f-MathUtils.clamp(annihilationUseTime/3f,0f,1f);ac=ELEC;}
            sr.setColor(.02f,.04f,.07f,.92f);sr.rect(ax,ay,aw,ah);sr.setColor(ac.r,ac.g,ac.b,.35f);sr.rect(ax,ay,aw*ap,ah);sr.setColor(ac);sr.rect(ax,ay+3,aw*ap,4);
        }
        // Bottom action buttons moved down so they do not crowd the planet HP bar.
        sr.setColor(1,1,1,.76f);sr.rect(42,28,10,54);sr.rect(68,28,10,54);
        float[] cx={770,875,980};for(float c:cx){sr.setColor(.01f,.04f,.075f,.76f);sr.circle(c,48,42,32);sr.setColor(Ui.CYAN.r,Ui.CYAN.g,Ui.CYAN.b,.15f);sr.circle(c,48,47,32);}
        sr.end();
        batch.begin();
        String creditText=cheatsEnabled()?"C ∞":"C "+(long)save.credits;
        float cw=game.assets.font.width(creditText,.78f);Ui.text(batch,game.assets.font,creditText,W-34-cw,1838,.78f,Ui.GOLD);
        String income="+"+String.format(java.util.Locale.US,"%.1f",save.passiveIncomePerSecond())+"/s";
        float iw=game.assets.font.width(income,.49f);Ui.text(batch,game.assets.font,income,W-34-iw,1788,.49f,new Color(.58f,.82f,.92f,.94f));
        if(overdriveTime>0)Ui.text(batch,game.assets.font,"BOOST "+String.format(java.util.Locale.US,"%.0fs",overdriveTime),32,174,.38f,ELEC);
        if(hasAnnihilation()){String st=annihilationCooldown>0f?game.assets.t("annihilation_recharge")+" "+String.format(java.util.Locale.US,"%.0fs",annihilationCooldown):game.assets.t("annihilation_ready");Ui.centered(batch,game.assets.font,st,new Rectangle(220,45,500,40),.42f,annihilationCooldown>0f?new Color(.55f,.74f,.9f,1):ELEC);}
        Texture deb=game.assets.icon("debuff_button");if(deb!=null)batch.draw(deb,741,19,58,58);
        if(effectsSystemUnlocked()){Texture cfg=game.assets.icon("config");if(cfg!=null)batch.draw(cfg,846,19,58,58);}
        Texture shp=game.assets.icon("shop_button");if(shp!=null)batch.draw(shp,951,19,58,58);
        batch.end();
    }
    private void drawBanner(){
        boolean boss=banner.toUpperCase(java.util.Locale.ROOT).contains("BOSS")||banner.toUpperCase(java.util.Locale.ROOT).contains("БОСС");
        Color c=boss?Ui.RED:Color.WHITE;
        float alpha=MathUtils.clamp(bannerTime/.42f,0f,1f);
        float scale=boss?1.18f:1.24f;
        Rectangle r=new Rectangle(80,1360,920,155);
        batch.begin();
        Ui.centered(batch,game.assets.font,banner,new Rectangle(r.x+4,r.y-5,r.width,r.height),scale,new Color(0f,0f,0f,.72f*alpha));
        Ui.centered(batch,game.assets.font,banner,r,scale,new Color(c.r,c.g,c.b,alpha));
        batch.end();
    }

    private void drawPause(){drawOverlay();Rectangle p=new Rectangle(150,600,780,650);sr.begin(ShapeRenderer.ShapeType.Filled);Ui.panel(sr,p,Ui.CYAN);sr.end();batch.begin();Ui.centered(batch,game.assets.font,game.assets.t("pause"),new Rectangle(190,1100,700,105),1.18f,Color.WHITE);batch.end();drawOverlayButton(new Rectangle(190,860,700,125),game.assets.t("resume"),true);drawOverlayButton(new Rectangle(190,680,700,125),game.assets.t("main_menu"),true);}
    private void drawDefeat(){drawOverlay();Rectangle p=new Rectangle(140,430,800,620);sr.begin(ShapeRenderer.ShapeType.Filled);Ui.panel(sr,p,Ui.RED);sr.end();batch.begin();Ui.text(batch,game.assets.font,game.assets.t("defeat"),380,970,1.05f,Ui.RED);Ui.text(batch,game.assets.font,game.assets.t("wave")+" "+save.wave+"   •   Kills "+save.totalKills,325,870,.5f,Color.WHITE);batch.end();drawOverlayButton(new Rectangle(190,690,700,120),game.assets.t("restart"),true);drawOverlayButton(new Rectangle(190,520,700,120),game.assets.t("main_menu"),true);}
    private void drawOverlay(){sr.begin(ShapeRenderer.ShapeType.Filled);sr.setColor(0,0,0,.72f);sr.rect(0,0,W,H);sr.end();}
    private void drawOverlayButton(Rectangle r,String text,boolean enabled){sr.begin(ShapeRenderer.ShapeType.Filled);Ui.button(sr,r,enabled,false);sr.end();batch.begin();Ui.centered(batch,game.assets.font,text,r,.82f,Color.WHITE);batch.end();}

    // Unified dark-glass language used by every in-game shop/config screen.
    // Important: shop fills use deliberately dark RGB values instead of relying on alpha blending.
    // ShapeRenderer alpha is not guaranteed to blend in every path/device, which previously produced
    // the heavy cyan/orange/red blocks seen on Android. Colour is now an accent, never the surface.
    private Color uiShade(Color c,float k){return new Color(c.r*k,c.g*k,c.b*k,1f);}
    private void drawShopSheet(Rectangle r,Color accent){
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(.006f,.014f,.024f,1f);sr.rect(r.x,r.y,r.width,r.height);
        sr.setColor(.010f,.029f,.043f,1f);sr.rect(r.x,r.y+r.height-155f,r.width,155f);
        sr.setColor(uiShade(accent,.72f));sr.rect(r.x,r.y+r.height-3f,r.width,3f);
        sr.setColor(uiShade(accent,.22f));sr.rect(r.x,r.y,4f,r.height);
        sr.setColor(.045f,.095f,.125f,1f);sr.rect(r.x+28f,r.y+145f,r.width-56f,2f);
        // faint lower edge keeps the sheet visually continuous without boxing it in
        sr.setColor(.018f,.050f,.067f,1f);sr.rect(r.x+18f,r.y,r.width-36f,1.5f);
        sr.end();
    }
    private void drawSoftCard(Rectangle r,boolean active,Color accent,boolean selected){
        sr.begin(ShapeRenderer.ShapeType.Filled);
        if(selected)sr.setColor(uiShade(accent,.075f));
        else if(active)sr.setColor(.012f,.031f,.044f,1f);
        else sr.setColor(.010f,.018f,.025f,1f);
        sr.rect(r.x,r.y,r.width,r.height);
        sr.setColor(active?uiShade(accent,.62f):new Color(.055f,.075f,.085f,1f));sr.rect(r.x,r.y,3f,r.height);
        sr.setColor(active?new Color(.055f,.125f,.155f,1f):new Color(.030f,.045f,.055f,1f));sr.rect(r.x+18f,r.y,r.width-18f,1.5f);
        if(selected){
            sr.setColor(uiShade(accent,.34f));
            sr.rect(r.x+3f,r.y+r.height-2f,r.width-3f,2f);
        }
        sr.end();
    }
    private void drawSheetAction(Rectangle r,String text,Color accent,boolean enabled){
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(enabled?new Color(.012f,.032f,.045f,1f):new Color(.018f,.020f,.024f,1f));sr.rect(r.x,r.y,r.width,r.height);
        Color edge=enabled?uiShade(accent,.58f):new Color(.10f,.12f,.14f,1f);
        // outline instead of a filled colour button
        sr.setColor(edge);
        sr.rect(r.x,r.y,r.width,2f);sr.rect(r.x,r.y+r.height-2f,r.width,2f);
        sr.rect(r.x,r.y,2f,r.height);sr.rect(r.x+r.width-2f,r.y,2f,r.height);
        sr.setColor(enabled?uiShade(accent,.16f):new Color(.025f,.030f,.035f,1f));
        sr.rect(r.x+10f,r.y+8f,r.width-20f,2f);
        sr.end();
        batch.begin();Ui.centered(batch,game.assets.font,text,r,.72f,enabled?Color.WHITE:new Color(.45f,.48f,.52f,1));batch.end();
    }

    private void drawShop(){
        drawOverlay();
        Rectangle panel=new Rectangle(40,125,1000,1660);drawShopSheet(panel,Ui.CYAN);
        batch.begin();
        Ui.text(batch,game.assets.font,game.assets.t("shop"),72,1720,1.12f,Color.WHITE);
        Ui.text(batch,game.assets.font,cheatsEnabled()?"C ∞":"C "+(long)save.credits,742,1718,.78f,Ui.GOLD);
        batch.end();

        String[] tabs={game.assets.t("finger"),game.assets.t("turrets"),game.assets.t("drones")};
        String[] tabIcons={"tab_finger","tab_turrets","tab_drones"};
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(.010f,.026f,.038f,1f);sr.rect(70,1518,940,112);
        sr.setColor(.028f,.060f,.078f,1f);sr.rect(84,1523,912,1.5f);
        sr.end();
        for(int i=0;i<tabs.length;i++){
            Rectangle r=new Rectangle(90+i*300,1540,280,100);
            boolean active=i==shopTab;
            sr.begin(ShapeRenderer.ShapeType.Filled);
            if(active){sr.setColor(uiShade(Ui.CYAN,.065f));sr.rect(r.x-8,r.y-8,r.width+16,82);}
            sr.setColor(active?uiShade(Ui.CYAN,.78f):new Color(.045f,.080f,.095f,1f));
            sr.rect(r.x-8,r.y-8,r.width+16,active?3f:1.5f);
            sr.end();
            batch.begin();
            Texture ti=game.assets.icon(tabIcons[i]);if(ti!=null){batch.setColor(1,1,1,active?1f:.58f);batch.draw(ti,r.x+18,r.y+17,56,56);batch.setColor(Color.WHITE);}
            Ui.text(batch,game.assets.font,tabs[i],r.x+88,r.y+56,.50f,active?Color.WHITE:new Color(.58f,.70f,.78f,1));
            batch.end();
        }

        Array<ShopEntry> list=shopEntries();int shown=Math.min(10,list.size);
        float shift=shopPageShift;
        for(int i=0;i<shown;i++){
            ShopEntry e=list.get(i);int col=i%2,row=i/2;
            Rectangle r=new Rectangle(70+col*478+shift,1310-row*230,456,204);
            boolean active=shopSellMode?canSell(e):e.enabled;
            drawSoftCard(r,active,shopTab==0?Ui.CYAN:shopTab==1?Ui.GOLD:ELEC,false);
            batch.begin();
            Texture icon=game.assets.icon(game.assets.t("unknown_technology").equals(e.label)?"unknown":e.id);
            if(icon!=null){batch.setColor(1,1,1,active?1f:.36f);batch.draw(icon,r.x+18,r.y+43,112,112);batch.setColor(Color.WHITE);}
            Color tc=active?Color.WHITE:new Color(.5f,.53f,.58f,1);
            Ui.text(batch,game.assets.font,shopShortLabel(e),r.x+148,r.y+166,.56f,tc);
            String levelText=shopLevelText(e);
            Ui.text(batch,game.assets.font,levelText,r.x+148,r.y+112,.43f,new Color(.6f,.78f,.9f,active?1f:.65f));
            String price;
            if(shopSellMode){
                long refund=refundFor(e);price=active?game.assets.t("sell_refund")+" C "+refund:game.assets.t("not_sellable");
            }else if(e.id.equals("repairSkill")){
                price=save.repairSkillLevel>=20?game.assets.t("max"):(cheatsEnabled()?"C 0":"C "+e.cost);
            }else{
                price=cheatsEnabled()&&e.cost>0?"C 0":(e.cost<=0?game.assets.t("max"):(e.cost>=Long.MAX_VALUE/4?game.assets.t("locked"):"C "+e.cost));
            }
            Ui.text(batch,game.assets.font,price,r.x+148,r.y+40,.47f,shopSellMode?Ui.GREEN:(active?Ui.GOLD:new Color(.45f,.35f,.2f,1)));
            batch.end();
        }

        // One footer bar, not two floating buttons.
        drawSheetAction(new Rectangle(74,174,250,96),shopSellMode?game.assets.t("buy_mode"):game.assets.t("sell_mode"),Ui.CYAN,true);
        drawSheetAction(new Rectangle(350,174,380,96),game.assets.t("close"),Ui.CYAN,true);
    }

    private Array<ShopEntry> debuffEntries(){
        Array<ShopEntry>a=new Array<>();
        // Ordered by the visual groups in the Debuffs screen: threat, pressure, profit.
        a.add(entry("enemyHealth",game.assets.t("enemy_health")+"  Lv."+save.enemyHealthLevel,cost(70,1.52,save.enemyHealthLevel),true));
        a.add(entry("enemyDamage",game.assets.t("enemy_damage")+"  Lv."+save.enemyDamageLevel,cost(70,1.52,save.enemyDamageLevel),true));
        a.add(entry("enemySpeed",game.assets.t("enemy_speed")+"  Lv."+save.enemySpeedLevel,cost(65,1.50,save.enemySpeedLevel),true));
        a.add(entry("density",game.assets.t("density")+"  Lv."+save.densityLevel,cost(55,1.48,save.densityLevel),true));
        a.add(entry("spawn",game.assets.t("spawn_rate")+"  Lv."+save.spawnRateLevel,cost(60,1.50,save.spawnRateLevel),true));
        a.add(entry("value",game.assets.t("enemy_value")+"  Lv."+save.enemyValueLevel,cost(75,1.52,save.enemyValueLevel),true));
        return a;
    }

    private void drawDebuffShop(){
        drawOverlay();Rectangle panel=new Rectangle(70,250,940,1430);drawShopSheet(panel,Ui.RED);
        batch.begin();
        Ui.text(batch,game.assets.font,game.assets.t("debuffs"),110,1600,1.14f,Color.WHITE);
        Ui.text(batch,game.assets.font,cheatsEnabled()?"C ∞":"C "+(long)save.credits,745,1597,.78f,Ui.GOLD);
        Ui.text(batch,game.assets.font,game.assets.t("passive")+"  +"+String.format(java.util.Locale.US,"%.1f",save.passiveIncomePerSecond())+" C/s",110,1522,.58f,new Color(.70f,.86f,.94f,1));
        batch.end();
        // Three continuous bands instead of six framed buttons. Draw them BEFORE
        // section captions so the panel cannot clip the lower part of the glyphs.
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(.030f,.016f,.022f,1f);sr.rect(92,1174,876,238);
        sr.setColor(.010f,.032f,.044f,1f);sr.rect(92,814,876,238);
        sr.setColor(.030f,.027f,.012f,1f);sr.rect(92,454,876,238);
        // group markers: enough colour to scan the screen, never a full coloured slab
        sr.setColor(uiShade(Ui.RED,.54f));sr.rect(92,1174,3f,238);
        sr.setColor(uiShade(Ui.CYAN,.54f));sr.rect(92,814,3f,238);
        sr.setColor(uiShade(Ui.GOLD,.54f));sr.rect(92,454,3f,238);
        sr.end();
        batch.begin();
        // Keep the group captions clearly above their bands.
        Ui.text(batch,game.assets.font,game.assets.t("debuff_threat"),110,1450,.62f,Ui.RED);
        Ui.text(batch,game.assets.font,game.assets.t("debuff_pressure"),110,1090,.62f,Ui.CYAN);
        Ui.text(batch,game.assets.font,game.assets.t("debuff_profit"),110,730,.62f,Ui.GOLD);
        batch.end();
        Array<ShopEntry> list=debuffEntries();
        float[] ys={1200,1200,840,840,480,480};
        for(int i=0;i<list.size;i++){
            ShopEntry e=list.get(i);int col=i%2;Rectangle r=new Rectangle(105+col*445,ys[i],420,200);drawDebuffCard(r,e);
        }
        drawSheetAction(new Rectangle(130,300,310,105),shopSellMode?game.assets.t("buy_mode"):game.assets.t("sell_mode"),Ui.RED,true);
        drawSheetAction(new Rectangle(530,300,420,105),game.assets.t("close"),Ui.RED,true);
    }

    private void debuffShopClick(float x,float y){
        if(new Rectangle(530,300,420,105).contains(x,y)){debuffShopOpen=false;shopSellMode=false;game.saves.save(save);return;}
        if(new Rectangle(130,300,310,105).contains(x,y)){shopSellMode=!shopSellMode;return;}
        Array<ShopEntry> list=debuffEntries();float[] ys={1200,1200,840,840,480,480};
        for(int i=0;i<list.size;i++){int col=i%2;Rectangle r=new Rectangle(105+col*445,ys[i],420,200);if(r.contains(x,y)){if(shopSellMode)sell(list.get(i));else buy(list.get(i));return;}}
    }

    private void drawDebuffCard(Rectangle r,ShopEntry e){
        boolean active=shopSellMode?canSell(e):e.enabled;
        Color accent=e.id.equals("enemyHealth")||e.id.equals("enemyDamage")?Ui.RED:(e.id.equals("enemySpeed")||e.id.equals("density")?Ui.CYAN:Ui.GOLD);
        drawSoftCard(r,active,accent,false);
        batch.begin();
        Texture icon=game.assets.icon(game.assets.t("unknown_technology").equals(e.label)?"unknown":e.id);
        if(icon!=null){batch.setColor(1,1,1,active?1f:.38f);batch.draw(icon,r.x+18,r.y+49,96,96);batch.setColor(Color.WHITE);}
        Ui.text(batch,game.assets.font,shopShortLabel(e),r.x+128,r.y+155,.58f,active?Color.WHITE:new Color(.5f,.53f,.58f,1));
        Ui.text(batch,game.assets.font,shopLevelText(e),r.x+128,r.y+105,.49f,new Color(.65f,.80f,.92f,active?1f:.65f));
        String price;if(shopSellMode){long refund=refundFor(e);price=active?game.assets.t("sell_refund")+" C "+refund:game.assets.t("not_sellable");}else price=cheatsEnabled()?"C 0":"C "+e.cost;
        Ui.text(batch,game.assets.font,price,r.x+128,r.y+55,.50f,shopSellMode?Ui.GREEN:Ui.GOLD);
        if(!shopSellMode)Ui.text(batch,game.assets.font,"+"+String.format(java.util.Locale.US,"%.1f",passiveGainFor(e.id))+" C/s",r.x+265,r.y+55,.42f,Ui.GREEN);
        batch.end();
    }

    private float passiveGainFor(String id){
        return switch(id){case "density"->1.35f;case "spawn"->1.55f;case "value"->1.80f;case "enemySpeed"->1.35f;case "enemyDamage","enemyHealth"->1.60f;default->0f;};
    }

    private void drawEffectShop(){
        drawOverlay();Rectangle p=new Rectangle(115,420,850,1010);drawShopSheet(p,ELEC);
        batch.begin();
        Ui.text(batch,game.assets.font,game.assets.t("effect_shop"),160,1340,1.04f,Color.WHITE);
        Ui.text(batch,game.assets.font,cheatsEnabled()?"C ∞":"C "+(long)save.credits,715,1340,.68f,Ui.GOLD);
        batch.end();
        Rectangle r1=new Rectangle(165,1030,750,190),r2=new Rectangle(165,800,750,190),r3=new Rectangle(165,570,750,190);
        if(cheatsEnabled()){
            drawEffectCard(r1,"fire",game.assets.t("fire"),true,0,true);
            drawEffectCard(r2,"ice",game.assets.t("ice"),true,0,true);
            drawEffectCard(r3,"lightning",game.assets.t("lightning"),true,0,true);
        }else{
            int row=0;
            if(save.fireUnlocked){drawEffectCard(row++==0?r1:r2,"fire",game.assets.t("fire"),true,0,true);}
            if(save.iceUnlocked){Rectangle r=row++==0?r1:row==2?r2:r3;drawEffectCard(r,"ice",game.assets.t("ice"),true,0,true);}
            if(save.lightningUnlocked){Rectangle r=row++==0?r1:row==2?r2:r3;drawEffectCard(r,"lightning",game.assets.t("lightning"),true,0,true);}
            if(!save.fireUnlocked)drawEffectCard(r1,"unknown",game.assets.t("unknown_technology"),false,FIRE_UNLOCK_COST,save.totalBossKills>=5);
            else if(!save.iceUnlocked){Rectangle r=save.fireUnlocked?r2:r1;drawEffectCard(r,"unknown",game.assets.t("unknown_technology"),false,ICE_UNLOCK_COST,save.totalBossKills>=7);}
            else if(!save.lightningUnlocked){Rectangle r=r3;drawEffectCard(r,"unknown",game.assets.t("unknown_technology"),false,LIGHTNING_UNLOCK_COST,save.totalBossKills>=9);}
        }
        drawSheetAction(new Rectangle(310,455,460,96),game.assets.t("back"),ELEC,true);
    }

    private void drawEffectCard(Rectangle r,String iconName,String title,boolean unlocked,long cost,boolean ready){
        boolean active=unlocked||ready;
        Color accent=iconName.equals("fire")?FIRE:iconName.equals("ice")?ICE:iconName.equals("lightning")?ELEC:new Color(.45f,.55f,.65f,1);
        drawSoftCard(r,active,accent,unlocked);
        batch.begin();
        Texture ic=game.assets.icon(iconName);
        if(ic!=null){batch.setColor(1,1,1,ready||unlocked?1f:.35f);batch.draw(ic,r.x+32,r.y+37,116,116);batch.setColor(Color.WHITE);}
        Ui.text(batch,game.assets.font,title,r.x+180,r.y+135,.76f,unlocked?accent:(ready?Color.WHITE:new Color(.52f,.56f,.62f,1)));
        String price=unlocked?game.assets.t("max"):(ready?"C "+cost:game.assets.t("locked"));
        Ui.text(batch,game.assets.font,price,r.x+180,r.y+66,.58f,unlocked?Ui.GREEN:(ready?Ui.GOLD:new Color(.50f,.40f,.25f,1)));
        batch.end();
    }

    private void effectShopClick(float x,float y){
        if(new Rectangle(310,455,460,96).contains(x,y)){effectShopOpen=false;elementConfigOpen=true;return;}
        if(cheatsEnabled())return; // cheat mode already exposes every element in Effect Setup.
        if(!save.fireUnlocked && save.totalBossKills>=5 && new Rectangle(165,1030,750,190).contains(x,y)){buyEffect("fire",FIRE_UNLOCK_COST);return;}
        if(save.fireUnlocked&&!save.iceUnlocked && save.totalBossKills>=7 && new Rectangle(165,800,750,190).contains(x,y)){buyEffect("ice",ICE_UNLOCK_COST);return;}
        if(save.fireUnlocked&&save.iceUnlocked&&!save.lightningUnlocked && save.totalBossKills>=9 && new Rectangle(165,570,750,190).contains(x,y)){buyEffect("lightning",LIGHTNING_UNLOCK_COST);}
    }

    private void buyEffect(String id,long cost){
        if(!cheatsEnabled()&&save.credits<cost){banner=game.assets.t("not_enough");bannerTime=1.2f;return;}if(!cheatsEnabled())save.credits-=cost;
        if("fire".equals(id))save.fireUnlocked=true;else if("ice".equals(id))save.iceUnlocked=true;else if("lightning".equals(id))save.lightningUnlocked=true;game.assets.play(game.assets.buy,game.settings,.24f);game.saves.save(save);
    }

    private void drawElementConfig(){
        drawOverlay();
        Rectangle panel=new Rectangle(85,220,910,1360);drawShopSheet(panel,Ui.CYAN);
        batch.begin();Ui.text(batch,game.assets.font,game.assets.t("effect_setup"),130,1500,1.10f,Color.WHITE);batch.end();

        drawConfigGroup(new Rectangle(125,1120,830,260),"tab_finger",game.assets.t("finger"),Ui.CYAN);
        drawConfigChoice(new Rectangle(160,1160,760,125),game.assets.t("attack_element"),elementName(save.fingerElement),elementIconName(save.fingerElement),elementColor(save.fingerElement));

        drawConfigGroup(new Rectangle(125,775,830,300),"tab_turrets",game.assets.t("turrets"),Ui.GOLD);
        drawConfigChoice(new Rectangle(160,815,365,130),game.assets.t("attack_element"),elementName(save.turretElement),elementIconName(save.turretElement),elementColor(save.turretElement));
        String wIcon=save.turretWeapon==1?"laser":save.turretWeapon==2?"rockets":"buyTurret";
        drawConfigChoice(new Rectangle(555,815,365,130),game.assets.t("weapon"),turretWeaponName(),wIcon,Ui.GOLD);

        drawConfigGroup(new Rectangle(125,430,830,300),"tab_drones",game.assets.t("drones"),ELEC);
        drawConfigChoice(new Rectangle(160,470,365,130),game.assets.t("attack_element"),elementName(save.droneElement),elementIconName(save.droneElement),elementColor(save.droneElement));
        drawConfigChoice(new Rectangle(555,470,365,130),game.assets.t("aura_element"),elementName(save.droneAuraElement),elementIconName(save.droneAuraElement),elementColor(save.droneAuraElement));

        drawSheetAction(new Rectangle(150,285,360,96),game.assets.t("effect_shop"),Ui.CYAN,true);
        drawSheetAction(new Rectangle(570,285,360,96),game.assets.t("close"),Ui.CYAN,true);
    }

    private void drawConfigGroup(Rectangle r,String iconName,String title,Color edge){
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(.010f,.029f,.041f,1f);sr.rect(r.x,r.y,r.width,r.height);
        sr.setColor(uiShade(edge,.56f));sr.rect(r.x,r.y+r.height-2f,r.width,2f);
        sr.setColor(uiShade(edge,.23f));sr.rect(r.x,r.y,3f,r.height);
        sr.end();
        batch.begin();
        Texture ic=game.assets.icon(iconName);if(ic!=null){batch.setColor(1,1,1,.82f);batch.draw(ic,r.x+20,r.y+r.height-72,48,48);batch.setColor(Color.WHITE);}
        Ui.text(batch,game.assets.font,title,r.x+84,r.y+r.height-32,.64f,Color.WHITE);
        batch.end();
    }

    private void drawConfigChoice(Rectangle r,String title,String value,String iconName,Color accent){
        drawSoftCard(r,true,accent,false);
        batch.begin();
        Texture ic=game.assets.icon(iconName);if(ic!=null)batch.draw(ic,r.x+16,r.y+27,70,70);
        Ui.text(batch,game.assets.font,title,r.x+100,r.y+91,.43f,new Color(.66f,.78f,.88f,1));
        Ui.text(batch,game.assets.font,value,r.x+100,r.y+43,.53f,accent);
        batch.end();
    }

    private void drawElementRow(Rectangle r,String sourceIcon,String title,Element element,boolean gravityAllowed){
        sr.begin(ShapeRenderer.ShapeType.Filled);Ui.button(sr,r,true,false);sr.end();
        batch.begin();
        Texture sIcon=game.assets.icon(sourceIcon);if(sIcon!=null)batch.draw(sIcon,r.x+20,r.y+32,98,98);
        Texture eIcon=game.assets.icon(elementIconName(element));if(eIcon!=null)batch.draw(eIcon,r.x+r.width-126,r.y+33,96,96);
        Ui.text(batch,game.assets.font,title,r.x+138,r.y+116,.68f,Color.WHITE);
        Ui.text(batch,game.assets.font,elementName(element),r.x+138,r.y+55,.57f,elementColor(element));
        Ui.text(batch,game.assets.font,game.assets.t("tap_to_change"),r.x+420,r.y+53,.40f,new Color(.62f,.75f,.86f,1));
        batch.end();
    }

    private void drawWeaponRow(Rectangle r){
        sr.begin(ShapeRenderer.ShapeType.Filled);Ui.button(sr,r,true,false);sr.end();
        batch.begin();Texture src=game.assets.icon("tab_turrets");if(src!=null)batch.draw(src,r.x+20,r.y+32,98,98);
        String iconName=save.turretWeapon==1?"laser":save.turretWeapon==2?"rockets":"buyTurret";Texture wi=game.assets.icon(iconName);if(wi!=null)batch.draw(wi,r.x+r.width-126,r.y+33,96,96);
        Ui.text(batch,game.assets.font,game.assets.t("turret_weapon"),r.x+138,r.y+116,.68f,Color.WHITE);
        Ui.text(batch,game.assets.font,turretWeaponName(),r.x+138,r.y+55,.57f,Ui.GOLD);
        Ui.text(batch,game.assets.font,game.assets.t("tap_to_change"),r.x+420,r.y+53,.40f,new Color(.62f,.75f,.86f,1));batch.end();
    }

    private void elementConfigClick(float x,float y){
        if(new Rectangle(570,285,360,96).contains(x,y)){elementConfigOpen=false;game.saves.save(save);return;}
        if(new Rectangle(150,285,360,96).contains(x,y)){triggerUiPulse(330,333,ELEC);elementConfigOpen=false;effectShopOpen=true;return;}
        if(new Rectangle(160,1160,760,125).contains(x,y)){save.fingerElement=save.fingerElement.nextCombat(true,save,cheatsEnabled());game.saves.save(save);return;}
        if(new Rectangle(160,815,365,130).contains(x,y)){save.turretElement=save.turretElement.nextCombat(false,save,cheatsEnabled());game.saves.save(save);return;}
        if(new Rectangle(555,815,365,130).contains(x,y)){cycleTurretWeapon();game.saves.save(save);return;}
        if(new Rectangle(160,470,365,130).contains(x,y)){save.droneElement=save.droneElement.nextCombat(false,save,cheatsEnabled());game.saves.save(save);return;}
        if(new Rectangle(555,470,365,130).contains(x,y)){save.droneAuraElement=save.droneAuraElement.nextCombat(false,save,cheatsEnabled());game.saves.save(save);}
    }

    private String elementIconName(Element e){return switch(e){case FIRE->"fire";case ICE->"ice";case LIGHTNING->"lightning";case GRAVITY->"gravity";default->"neutral";};}

    private void drawShopTabIcon(int tab,Rectangle r,boolean hot){
        Color c=hot?Color.WHITE:Ui.CYAN;sr.setColor(c.r,c.g,c.b,hot?.92f:.68f);float cx=r.x+r.width/2,cy=r.y+58;
        if(tab==0){sr.rect(cx-30,cy-18,12,34);sr.rect(cx-7,cy-3,12,19);sr.rect(cx+16,cy-30,12,46);}
        else if(tab==1){sr.circle(cx,cy,16,20);sr.circle(cx,cy,5,16);}
        else if(tab==2){sr.rect(cx-24,cy-16,48,22);sr.rect(cx-5,cy+5,10,30);sr.rect(cx,cy+31,34,7);}
        else if(tab==3){sr.triangle(cx,cy+24,cx-26,cy-17,cx+26,cy-17);sr.circle(cx,cy,5,12);}
        else {sr.circle(cx-20,cy,8,16);sr.circle(cx,cy,8,16);sr.circle(cx+20,cy,8,16);}
    }

    private void drawShopIcon(String id,Rectangle r,boolean enabled){
        Color base=enabled?Ui.CYAN:new Color(.35f,.4f,.45f,1);float x=r.x+52,y=r.y+105;sr.setColor(base.r,base.g,base.b,.22f);sr.circle(x,y,43,28);sr.setColor(base);
        if(id.contains("Damage")||id.endsWith("Dmg")){sr.circle(x,y,24,24);sr.setColor(.02f,.05f,.08f,1);sr.circle(x,y,15,24);sr.setColor(base);sr.rect(x-3,y-35,6,70);sr.rect(x-35,y-3,70,6);}
        else if(id.toLowerCase().contains("rate")||id.equals("spawn")){for(int k=0;k<3;k++){float ox=x-25+k*23;sr.triangle(ox-5,y-17,ox-5,y+17,ox+13,y);}}
        else if(id.contains("Shield")||id.equals("repairTurrets")||id.equals("autoRepair")){sr.circle(x,y,27,6);sr.setColor(.02f,.05f,.08f,1);sr.circle(x,y,18,6);sr.setColor(base);sr.rect(x-4,y-22,8,44);}
        else if(id.toLowerCase().contains("drone")||id.equals("gunDrone")||id.equals("missileDrone")||id.equals("kamikaze")||id.equals("support")){sr.triangle(x,y+29,x-31,y-22,x+31,y-22);sr.circle(x,y-4,8,16);}
        else if(id.toLowerCase().contains("turret")||id.equals("laser")||id.equals("rockets")||id.equals("cycleWeapon")){sr.rect(x-30,y-21,60,22);sr.rect(x-6,y,12,36);sr.rect(x,y+31,34,7);}
        else if(id.equals("plasma")||id.equals("gravity")){sr.circle(x,y,29,28);sr.setColor(.02f,.05f,.08f,1);sr.circle(x,y,18,28);sr.setColor(base);sr.circle(x,y,7,20);}
        else if(id.equals("trail")){sr.rect(x-34,y-4,68,8);sr.circle(x+32,y,10,16);}
        else if(id.equals("ultimate")){sr.rect(x-30,y-4,60,8);sr.circle(x-32,y,10,16);sr.circle(x+32,y,10,16);}
        else if(id.equals("fire")){sr.triangle(x,y+31,x-25,y-24,x+25,y-24);sr.circle(x,y-8,12,16);}
        else if(id.equals("ice")){sr.rect(x-3,y-31,6,62);sr.rect(x-31,y-3,62,6);sr.rect(x-22,y-22,44,5);}
        else if(id.equals("lightning")){sr.triangle(x-7,y+31,x+8,y+5,x-3,y+5);sr.triangle(x-3,y+5,x+15,y-31,x-2,y-7);}
        else if(id.contains("Elem")){sr.circle(x-20,y,8,16);sr.circle(x,y,8,16);sr.circle(x+20,y,8,16);}
        else if(id.equals("density")){for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++)sr.circle(x+a*19,y+b*19,6,12);}
        else if(id.equals("value")||id.equals("yield")){sr.circle(x,y,27,28);sr.setColor(.02f,.05f,.08f,1);sr.circle(x,y,19,28);}
        else {sr.circle(x,y,22,24);}
    }

    private String shopShortLabel(ShopEntry e){
        if(game.assets.t("unknown_technology").equals(e.label))return e.label;
        return switch(e.id){
            case "tapDmg","turretDmg","droneDmg" -> game.assets.t("damage");
            case "turretRate","droneRate" -> game.assets.t("rate");
            case "density" -> game.assets.t("density");case "spawn" -> game.assets.t("spawn_rate");case "value" -> game.assets.t("enemy_value");
            case "enemySpeed" -> game.assets.t("enemy_speed");case "enemyDamage" -> game.assets.t("enemy_damage");case "enemyHealth" -> game.assets.t("enemy_health");
            case "plasma" -> game.assets.t("plasma");case "trail" -> game.assets.t("roscherk");case "ultimate" -> game.assets.t("annihilation");case "gravity" -> game.assets.t("gravity");
            case "buyTurret" -> game.assets.t("turrets");case "turretShield","droneShield" -> game.assets.t("shield");case "repairSkill" -> game.assets.t("repair_skill");case "autoRepair" -> game.assets.t("auto_repair");
            case "laser" -> game.assets.t("pulse_laser");case "rockets" -> game.assets.t("rockets");case "turretPlusTwo" -> "+2 "+game.assets.t("turrets");
            case "gunDrone" -> game.assets.t("gun_drone");case "missileDrone" -> game.assets.t("missile_drone");case "kamikaze" -> game.assets.t("kamikaze_drone");case "support" -> game.assets.t("support_drone");case "droneAura" -> game.assets.t("drone_aura");case "dronePlusTwo" -> "+2 "+game.assets.t("drones");
            case "fire" -> game.assets.t("fire");case "ice" -> game.assets.t("ice");case "lightning" -> game.assets.t("lightning");default -> e.label;
        };
    }

    private String shopLevelText(ShopEntry e){
        if(game.assets.t("unknown_technology").equals(e.label)){
            return switch(e.id){
                case "plasma" -> game.assets.t("damage")+" "+save.tapDamageLevel+"/20";
                case "trail" -> game.assets.t("plasma")+" Lv."+save.plasmaSkillLevel+"/5";
                case "ultimate" -> game.assets.t("roscherk")+" Lv."+save.trailSkillLevel+"/8";
                case "gravity" -> game.assets.t("annihilation")+" Lv."+save.ultimateSkillLevel+"/8\nB "+save.totalBossKills+"/10";
                case "laser" -> game.assets.t("damage")+" "+save.turretDamageLevel+"/8\n"+game.assets.t("rate")+" "+save.turretRateLevel+"/6";
                case "rockets" -> "Turret Lv."+save.turretSkillLevel+"/6";
                case "autoRepair" -> game.assets.t("repair_skill")+" "+save.repairSkillLevel+"/8\nB "+save.totalBossKills+"/3";
                case "turretPlusTwo" -> "B "+save.totalBossKills+"/10";
                case "missileDrone" -> "Drone Lv."+save.droneSkillLevel+"/4";
                case "support" -> "Drone Lv."+save.droneSkillLevel+"/7";
                case "kamikaze" -> "B "+save.totalBossKills+"/3";
                case "droneAura" -> "Drone Lv."+save.droneSkillLevel+"/10";
                case "dronePlusTwo" -> "Aura Lv."+save.droneAuraLevel+"/5\nB "+save.totalBossKills+"/10";
                default -> "";
            };
        }
        if(e.id.equals("buyTurret"))return save.turretCount+" / "+save.turretCap();
        if(e.id.equals("gunDrone")||e.id.equals("missileDrone")||e.id.equals("kamikaze")||e.id.equals("support"))return save.droneCount()+" / "+save.droneCap();
        if(e.id.equals("repairSkill")){
            if(save.repairSkillLevel>=20)return "Lv.20/20";
            return "Lv."+save.repairSkillLevel+"/20\nXP "+(int)save.repairXp+"/"+(int)Math.ceil(repairXpNeed(save.repairSkillLevel))+"  •  "+game.assets.t("repair_xp_short");
        }
        String l=e.label;int ix=l.indexOf("Lv.");if(ix>=0)return l.substring(ix);return e.cost==0?game.assets.t("max"):"";
    }

    private ShopEntry mystery(String id,long cost,boolean ready){return entry(id,game.assets.t("unknown_technology"),ready?cost:Long.MAX_VALUE/2,ready);}

    private Array<ShopEntry> shopEntries(){Array<ShopEntry>a=new Array<>();
        if(shopTab==0){
            a.add(entry("tapDmg",game.assets.t("tap")+" "+game.assets.t("damage")+"  Lv."+save.tapDamageLevel,cost(70,1.18,save.tapDamageLevel),true));
            if(cheatsEnabled()){
                a.add(entry("plasma",game.assets.t("plasma"),1200,!save.plasmaUnlocked));a.add(entry("trail",game.assets.t("roscherk"),2600,!save.trailUnlocked));a.add(entry("ultimate",game.assets.t("annihilation"),9000,!save.ultimateUnlocked));a.add(entry("gravity",game.assets.t("gravity"),35000,!save.gravityUnlocked));
            }else if(!save.plasmaUnlocked)a.add(mystery("plasma",1200,save.tapDamageLevel>=20));
            else {
                a.add(entry("plasma",game.assets.t("plasma")+" — "+game.assets.t("max"),0,false));
                if(!save.trailUnlocked)a.add(mystery("trail",2600,save.plasmaSkillLevel>=5));
                else {a.add(entry("trail",game.assets.t("roscherk")+" — "+game.assets.t("max"),0,false));
                    if(!save.ultimateUnlocked)a.add(mystery("ultimate",9000,save.trailSkillLevel>=8));
                    else {a.add(entry("ultimate",game.assets.t("annihilation")+" — "+game.assets.t("max"),0,false));
                        if(!save.gravityUnlocked)a.add(mystery("gravity",35000,save.ultimateSkillLevel>=8&&save.totalBossKills>=10));
                        else a.add(entry("gravity",game.assets.t("gravity")+" — "+game.assets.t("max"),0,false));}}
            }
        } else if(shopTab==1){
            a.add(entry("buyTurret",game.assets.t("buy_turret")+"  "+save.turretCount+"/"+save.turretCap(),save.turretCount>=save.turretCap()?0:cost(300,1.82,Math.max(0,save.turretCount)),save.turretCount<save.turretCap()));
            a.add(entry("turretDmg",game.assets.t("damage")+"  Lv."+save.turretDamageLevel,cost(180,1.52,save.turretDamageLevel),true));
            a.add(entry("turretRate",game.assets.t("rate")+"  Lv."+save.turretRateLevel,cost(220,1.55,save.turretRateLevel),true));
            a.add(entry("turretShield",game.assets.t("shield")+"  Lv."+save.turretShieldLevel,cost(180,1.50,save.turretShieldLevel),true));
            a.add(entry("repairSkill",game.assets.t("repair_skill")+"  Lv."+save.repairSkillLevel,save.repairSkillLevel>=20?0:cost(320,1.42,Math.max(0,save.repairSkillLevel-1)),save.repairSkillLevel<20));
            if(cheatsEnabled()){
                a.add(entry("laser",game.assets.t("pulse_laser"),3000,!save.turretLaserUnlocked));a.add(entry("rockets",game.assets.t("rockets"),5200,!save.turretRocketsUnlocked));a.add(entry("autoRepair",game.assets.t("auto_repair"),9500,!save.autoRepairUnlocked));a.add(entry("turretPlusTwo","+2 "+game.assets.t("turrets"),45000,!save.turretPlusTwo));
            }else if(!save.turretLaserUnlocked)a.add(mystery("laser",3000,save.turretDamageLevel>=8&&save.turretRateLevel>=6));
            else if(!save.turretRocketsUnlocked)a.add(mystery("rockets",5200,save.turretSkillLevel>=6));
            else if(!save.autoRepairUnlocked)a.add(mystery("autoRepair",9500,save.repairSkillLevel>=8&&save.totalBossKills>=3));
            else if(!save.turretPlusTwo)a.add(mystery("turretPlusTwo",45000,save.totalBossKills>=10));
        } else if(shopTab==2){
            boolean room=save.droneCount()<save.droneCap();long base=cost(700,1.46,save.droneCount());
            a.add(entry("gunDrone",game.assets.t("buy_gun_drone")+"  "+save.droneCount()+"/"+save.droneCap(),base,room));
            a.add(entry("droneDmg",game.assets.t("damage")+"  Lv."+save.droneDamageLevel,cost(220,1.52,save.droneDamageLevel),true));
            a.add(entry("droneRate",game.assets.t("rate")+"  Lv."+save.droneRateLevel,cost(260,1.55,save.droneRateLevel),true));
            if(hasMissileDroneTech())a.add(entry("missileDrone",game.assets.t("buy_missile_drone"),Math.max(1600,base+450),room));
            if(hasSupportDroneTech())a.add(entry("support",game.assets.t("buy_support"),Math.max(2600,base+1100),room));
            if(save.kamikazeUnlocked||cheatsEnabled())a.add(entry("kamikaze",game.assets.t("spawn_kamikaze"),Math.max(260,cost(260,1.14,save.kamikazeDrones)),room));
            if(hasDroneAura()){a.add(entry("droneShield",game.assets.t("shield")+"  Lv."+save.droneShieldLevel,cost(260,1.50,save.droneShieldLevel),true));a.add(entry("droneAura",game.assets.t("drone_aura")+"  Lv."+save.droneAuraLevel,cost(420,1.50,save.droneAuraLevel),true));}
            if(cheatsEnabled()){if(!save.dronePlusTwo)a.add(entry("dronePlusTwo","+2 "+game.assets.t("drones"),48000,true));}
            else {
                if(!save.missileDroneUnlocked)a.add(mystery("missileDrone",1600,save.droneSkillLevel>=4));
                else if(!save.supportDroneUnlocked)a.add(mystery("support",2600,save.droneSkillLevel>=7));
                else if(!save.kamikazeUnlocked)a.add(mystery("kamikaze",3200,save.totalBossKills>=3));
                else if(!save.droneAuraUnlocked)a.add(mystery("droneAura",5000,save.droneSkillLevel>=10));
                else if(!save.dronePlusTwo)a.add(mystery("dronePlusTwo",48000,save.droneAuraLevel>=5&&save.totalBossKills>=10));
            }
        }
        return a;
    }
    private ShopEntry entry(String id,String label,long cost,boolean enabled){return new ShopEntry(id,label,cost,enabled);}

    private void shopClick(float x,float y){
        if(new Rectangle(350,174,380,96).contains(x,y)){shopOpen=false;shopSellMode=false;game.saves.save(save);return;}
        if(new Rectangle(74,174,250,96).contains(x,y)){shopSellMode=!shopSellMode;return;}
        for(int i=0;i<3;i++)if(new Rectangle(90+i*300,1540,280,100).contains(x,y)){changeShopTab(i);return;}
        Array<ShopEntry> list=shopEntries();
        for(int i=0;i<Math.min(10,list.size);i++){int col=i%2,row=i/2;Rectangle r=new Rectangle(70+col*478,1310-row*230,456,204);if(r.contains(x,y)){if(shopSellMode)sell(list.get(i));else buy(list.get(i));return;}}
    }

    private boolean canSell(ShopEntry e){
        return switch(e.id){
            case "gDamage"->save.generalDamageLevel>0;case "gRate"->save.generalRateLevel>0;case "yield"->save.creditYieldLevel>0;case "density"->save.densityLevel>0;case "spawn"->save.spawnRateLevel>0;case "value"->save.enemyValueLevel>0;case "enemySpeed"->save.enemySpeedLevel>0;case "enemyDamage"->save.enemyDamageLevel>0;case "enemyHealth"->save.enemyHealthLevel>0;
            case "tapDmg"->save.tapDamageLevel>0;case "tapRate"->save.tapSpeedLevel>0;case "plasma"->save.plasmaUnlocked;case "trail"->save.trailUnlocked;case "ultimate"->save.ultimateUnlocked;case "gravity"->save.gravityUnlocked&&save.fingerElement!=Element.GRAVITY;
            case "buyTurret"->save.turretCount>0;case "turretDmg"->save.turretDamageLevel>0;case "turretRate"->save.turretRateLevel>0;case "turretShield"->save.turretShieldLevel>0;case "autoRepair"->save.autoRepairUnlocked;case "laser"->save.turretLaserUnlocked&&save.turretWeapon!=1;case "rockets"->save.turretRocketsUnlocked&&save.turretWeapon!=2;case "turretPlusTwo"->save.turretPlusTwo&&save.turretCount<=5;
            case "gunDrone"->save.gunDrones>0;case "missileDrone"->save.missileDrones>0;case "kamikaze"->save.kamikazeDrones>0;case "support"->save.supportDrones>0;case "droneDmg"->save.droneDamageLevel>0;case "droneRate"->save.droneRateLevel>0;case "droneAura"->save.droneAuraLevel>0;case "droneShield"->save.droneShieldLevel>0;case "dronePlusTwo"->save.dronePlusTwo&&save.droneCount()<=11;
            case "fire"->save.fireUnlocked&&save.fingerElement!=Element.FIRE&&save.turretElement!=Element.FIRE&&save.droneElement!=Element.FIRE;case "ice"->save.iceUnlocked&&save.fingerElement!=Element.ICE&&save.turretElement!=Element.ICE&&save.droneElement!=Element.ICE;case "lightning"->save.lightningUnlocked&&save.fingerElement!=Element.LIGHTNING&&save.turretElement!=Element.LIGHTNING&&save.droneElement!=Element.LIGHTNING;
            default->false;
        };
    }

    private long refundFor(ShopEntry e){
        double value=switch(e.id){
            case "gDamage"->cost(120,1.58,Math.max(0,save.generalDamageLevel-1));case "gRate"->cost(150,1.62,Math.max(0,save.generalRateLevel-1));case "yield"->cost(180,1.60,Math.max(0,save.creditYieldLevel-1));case "density"->cost(55,1.48,Math.max(0,save.densityLevel-1));case "spawn"->cost(60,1.50,Math.max(0,save.spawnRateLevel-1));case "value"->cost(75,1.52,Math.max(0,save.enemyValueLevel-1));case "enemySpeed"->cost(65,1.50,Math.max(0,save.enemySpeedLevel-1));case "enemyDamage"->cost(70,1.52,Math.max(0,save.enemyDamageLevel-1));case "enemyHealth"->cost(70,1.52,Math.max(0,save.enemyHealthLevel-1));
            case "tapDmg"->cost(70,1.18,Math.max(0,save.tapDamageLevel-1));case "tapRate"->cost(90,1.55,Math.max(0,save.tapSpeedLevel-1));case "plasma"->550;case "trail"->850;case "ultimate"->3800;case "gravity"->15000;
            case "buyTurret"->cost(300,1.90,Math.max(0,save.turretCount-1));case "turretDmg"->cost(180,1.56,Math.max(0,save.turretDamageLevel-1));case "turretRate"->cost(220,1.60,Math.max(0,save.turretRateLevel-1));case "turretShield"->cost(180,1.55,Math.max(0,save.turretShieldLevel-1));case "autoRepair"->5200;case "laser"->1800;case "rockets"->3200;case "turretPlusTwo"->30000;
            case "gunDrone"->cost(700,1.48,Math.max(0,save.droneCount()-1));case "missileDrone"->Math.max(1350,cost(700,1.48,Math.max(0,save.droneCount()-1))+500);case "kamikaze"->Math.max(1800,cost(700,1.48,Math.max(0,save.droneCount()-1))+850);case "support"->Math.max(2400,cost(700,1.48,Math.max(0,save.droneCount()-1))+1400);case "droneDmg"->cost(220,1.56,Math.max(0,save.droneDamageLevel-1));case "droneRate"->cost(260,1.60,Math.max(0,save.droneRateLevel-1));case "droneAura"->cost(300,1.62,Math.max(0,save.droneAuraLevel-1));case "droneShield"->cost(240,1.56,Math.max(0,save.droneShieldLevel-1));case "dronePlusTwo"->32000;case "fire"->FIRE_UNLOCK_COST;case "ice"->ICE_UNLOCK_COST;case "lightning"->LIGHTNING_UNLOCK_COST;default->0;
        };return (long)(value*.60);
    }

    private void sell(ShopEntry e){
        if(!canSell(e))return;long refund=refundFor(e);save.credits+=refund;
        switch(e.id){
            case "gDamage"->save.generalDamageLevel--;case "gRate"->save.generalRateLevel--;case "yield"->save.creditYieldLevel--;case "density"->save.densityLevel--;case "spawn"->save.spawnRateLevel--;case "value"->save.enemyValueLevel--;case "enemySpeed"->{save.enemySpeedLevel--;for(Enemy en:enemies)en.speed/=1.08f;}case "enemyDamage"->save.enemyDamageLevel--;case "enemyHealth"->save.enemyHealthLevel--;
            case "tapDmg"->save.tapDamageLevel--;case "tapRate"->save.tapSpeedLevel--;case "plasma"->save.plasmaUnlocked=false;case "trail"->save.trailUnlocked=false;case "ultimate"->save.ultimateUnlocked=false;case "gravity"->save.gravityUnlocked=false;
            case "buyTurret"->{save.turretCount--;rebuildDefenses();}case "turretDmg"->save.turretDamageLevel--;case "turretRate"->save.turretRateLevel--;case "turretShield"->{save.turretShieldLevel--;rebuildDefenses();}case "autoRepair"->save.autoRepairUnlocked=false;case "laser"->save.turretLaserUnlocked=false;case "rockets"->save.turretRocketsUnlocked=false;case "turretPlusTwo"->save.turretPlusTwo=false;
            case "gunDrone"->{save.gunDrones--;rebuildDefenses();}case "missileDrone"->{save.missileDrones--;rebuildDefenses();}case "kamikaze"->{save.kamikazeDrones--;rebuildDefenses();}case "support"->{save.supportDrones--;rebuildDefenses();}case "droneDmg"->save.droneDamageLevel--;case "droneRate"->save.droneRateLevel--;case "droneAura"->save.droneAuraLevel--;case "droneShield"->{save.droneShieldLevel--;rebuildDefenses();}case "dronePlusTwo"->save.dronePlusTwo=false;case "fire"->save.fireUnlocked=false;case "ice"->save.iceUnlocked=false;case "lightning"->save.lightningUnlocked=false;
        }
        banner=game.assets.t("sold")+" +C "+refund;bannerTime=1f;game.assets.play(game.assets.buy,game.settings,.18f);game.saves.save(save);
    }

    private void buy(ShopEntry e){
        if(!e.enabled)return;
        if(!cheatsEnabled() && e.cost>0&&save.credits<e.cost){banner=game.assets.t("not_enough");bannerTime=1.2f;return;}
        if(!cheatsEnabled() && e.cost>0)save.credits-=e.cost;
        String unlockedName=null;
        switch(e.id){
            case "density"->save.densityLevel++;case "spawn"->save.spawnRateLevel++;case "value"->save.enemyValueLevel++;case "enemySpeed"->{save.enemySpeedLevel++;for(int i=0;i<enemies.size;i++)enemies.get(i).speed*=1.08f;}case "enemyDamage"->save.enemyDamageLevel++;case "enemyHealth"->save.enemyHealthLevel++;
            case "tapDmg"->save.tapDamageLevel++;
            case "plasma"->{if(!save.plasmaUnlocked)unlockedName=game.assets.t("plasma");save.plasmaUnlocked=true;}
            case "trail"->{if(!save.trailUnlocked)unlockedName=game.assets.t("roscherk");save.trailUnlocked=true;}
            case "ultimate"->{if(!save.ultimateUnlocked)unlockedName=game.assets.t("annihilation");save.ultimateUnlocked=true;}
            case "gravity"->{if(!save.gravityUnlocked)unlockedName=game.assets.t("gravity");save.gravityUnlocked=true;}
            case "buyTurret"->{save.turretCount++;rebuildDefenses();}
            case "turretDmg"->save.turretDamageLevel++;case "turretRate"->save.turretRateLevel++;case "turretShield"->{save.turretShieldLevel++;rebuildDefenses();}
            case "repairSkill"->{if(save.repairSkillLevel<20){save.repairSkillLevel++;if(save.repairSkillLevel>=20)save.repairXp=0f;else gainRepairXp(0f);}}
            case "autoRepair"->{if(!save.autoRepairUnlocked)unlockedName=game.assets.t("auto_repair");save.autoRepairUnlocked=true;}
            case "laser"->{if(!save.turretLaserUnlocked)unlockedName=game.assets.t("pulse_laser");save.turretLaserUnlocked=true;}
            case "rockets"->{if(!save.turretRocketsUnlocked)unlockedName=game.assets.t("rockets");save.turretRocketsUnlocked=true;}
            case "turretPlusTwo"->{if(!save.turretPlusTwo)unlockedName="+2 "+game.assets.t("turrets");save.turretPlusTwo=true;}
            case "gunDrone"->{save.gunDrones++;rebuildDefenses();}
            case "missileDrone"->{if(!save.missileDroneUnlocked)unlockedName=game.assets.t("missile_drone");save.missileDroneUnlocked=true;save.missileDrones++;rebuildDefenses();}
            case "support"->{if(!save.supportDroneUnlocked)unlockedName=game.assets.t("support_drone");save.supportDroneUnlocked=true;save.supportDrones++;rebuildDefenses();}
            case "kamikaze"->{if(!save.kamikazeUnlocked)unlockedName=game.assets.t("kamikaze_drone");save.kamikazeUnlocked=true;save.kamikazeDrones++;rebuildDefenses();}
            case "droneDmg"->save.droneDamageLevel++;case "droneRate"->save.droneRateLevel++;
            case "droneAura"->{if(!save.droneAuraUnlocked){save.droneAuraUnlocked=true;save.droneAuraLevel=1;unlockedName=game.assets.t("drone_aura");}else save.droneAuraLevel++;}
            case "droneShield"->{save.droneShieldLevel++;rebuildDefenses();}case "dronePlusTwo"->{if(!save.dronePlusTwo)unlockedName="+2 "+game.assets.t("drones");save.dronePlusTwo=true;}
            case "fire"->save.fireUnlocked=true;case "ice"->save.iceUnlocked=true;case "lightning"->save.lightningUnlocked=true;
        }
        if(unlockedName!=null){banner=game.assets.t("new_technology")+" — "+unlockedName;bannerTime=2.7f;}
        game.assets.play(game.assets.buy,game.settings,.25f);vibrate(12);game.saves.save(save);
    }

    private void cycleTurretWeapon(){for(int k=0;k<3;k++){save.turretWeapon=(save.turretWeapon+1)%3;if(save.turretWeapon==0)return;if(save.turretWeapon==1&&hasLaser())return;if(save.turretWeapon==2&&hasRockets())return;}}
    private String turretWeaponName(){return save.turretWeapon==1?game.assets.t("pulse_laser"):save.turretWeapon==2?game.assets.t("rockets"):game.assets.t("pulse_gun");}
    private boolean cheatsEnabled(){ return game.settings.cheatsEnabled; }
    private boolean hasPlasma(){return cheatsEnabled()||save.plasmaUnlocked;}
    private boolean hasTrail(){return cheatsEnabled()||save.trailUnlocked;}
    private boolean hasAnnihilation(){return cheatsEnabled()||save.ultimateUnlocked;}
    private boolean hasGravity(){return cheatsEnabled()||save.gravityUnlocked;}
    private boolean hasLaser(){return cheatsEnabled()||save.turretLaserUnlocked;}
    private boolean hasRockets(){return cheatsEnabled()||save.turretRocketsUnlocked;}
    private boolean hasMissileDroneTech(){return cheatsEnabled()||save.missileDroneUnlocked;}
    private boolean hasSupportDroneTech(){return cheatsEnabled()||save.supportDroneUnlocked;}
    private boolean hasDroneAura(){return cheatsEnabled()||save.droneAuraUnlocked;}
    private boolean effectsSystemUnlocked(){return cheatsEnabled()||save.totalBossKills>=5;}
    private boolean hasFire(){return cheatsEnabled()||save.fireUnlocked;}
    private boolean hasIce(){return cheatsEnabled()||save.iceUnlocked;}
    private boolean hasLightning(){return cheatsEnabled()||save.lightningUnlocked;}
    private long cost(double base,double growth,int lvl){double v=base*Math.pow(growth,lvl);return (long)Math.min(v,9_000_000_000L);}

    private void gainSkill(String kind,float amount){
        float gain=Math.max(0.25f,amount*.035f);
        if("tap".equals(kind)){save.tapXp+=gain;while(save.tapXp>=skillNeed(save.tapSkillLevel)){save.tapXp-=skillNeed(save.tapSkillLevel);save.tapSkillLevel++;}}
        else if("plasma".equals(kind)){save.plasmaXp+=gain;while(save.plasmaXp>=skillNeed(save.plasmaSkillLevel)){save.plasmaXp-=skillNeed(save.plasmaSkillLevel);save.plasmaSkillLevel++;}}
        else if("trail".equals(kind)){save.trailXp+=gain;while(save.trailXp>=skillNeed(save.trailSkillLevel)){save.trailXp-=skillNeed(save.trailSkillLevel);save.trailSkillLevel++;}}
        else if("ultimate".equals(kind)){save.ultimateXp+=gain;while(save.ultimateXp>=skillNeed(save.ultimateSkillLevel)){save.ultimateXp-=skillNeed(save.ultimateSkillLevel);save.ultimateSkillLevel++;}}
        else if("turret".equals(kind)){save.turretXp+=gain;while(save.turretXp>=skillNeed(save.turretSkillLevel)){save.turretXp-=skillNeed(save.turretSkillLevel);save.turretSkillLevel++;}}
        else if("drone".equals(kind)){save.droneXp+=gain;while(save.droneXp>=skillNeed(save.droneSkillLevel)){save.droneXp-=skillNeed(save.droneSkillLevel);save.droneSkillLevel++;}}
    }
    private float skillNeed(int level){return 40f+(float)Math.pow(level,1.32)*18f;}

    private Color colorFor(Enemy e){
        if(e.archetype==EnemyArchetype.GUARDIAN)return new Color(.20f,1f,.72f,1);
        if(e.archetype==EnemyArchetype.ELEMENT_WARD)return new Color(.58f,.28f,1f,1);
        if(e.archetype==EnemyArchetype.INFECTOR)return new Color(.92f,.16f,1f,1);
        if(e.archetype==EnemyArchetype.STAR)return new Color(1f,.66f,.12f,1);
        return switch(e.kind){case FAST->new Color(.18f,.95f,1f,1);case TANK->new Color(1f,.33f,.22f,1);case ELITE->new Color(.82f,.25f,1f,1);case BOSS->new Color(1f,.1f,.3f,1);default->new Color(.25f,.75f,1f,1);};
    }
    private Color elementColor(Element e){return switch(e){case FIRE->FIRE;case ICE->ICE;case LIGHTNING->ELEC;case GRAVITY->GRAV;default->Ui.CYAN;};}
    private Color alienAttackColor(EnemyAttackKind k){return switch(k){case CORROSION->new Color(.42f,1f,.10f,1);case PARASITE->new Color(.88f,.16f,1f,1);case DISRUPTION->new Color(1f,.58f,.08f,1);default->new Color(1f,.28f,.06f,1);};}
    private String elementName(Element e){return switch(e){case FIRE->game.assets.t("fire");case ICE->game.assets.t("ice");case LIGHTNING->game.assets.t("lightning");case GRAVITY->game.assets.t("gravity");default->game.assets.t("neutral");};}

    private void burst(float x,float y,Color c,int count){int n=game.settings.highEffects?count:Math.max(2,count/2);for(int i=0;i<n;i++){float a=MathUtils.random(0,MathUtils.PI2),sp=MathUtils.random(45f,260f);Particle p=new Particle();p.x=x;p.y=y;p.vx=MathUtils.cos(a)*sp;p.vy=MathUtils.sin(a)*sp;p.life=p.maxLife=MathUtils.random(.18f,.65f);p.size=MathUtils.random(2.5f,8f);p.color=new Color(c);particles.add(p);}}
    private void explode(float x,float y,Color c,int count){
        burst(x,y,c,count);burst(x,y,Color.WHITE,Math.max(5,count/3));
        Shockwave a=new Shockwave();a.x=x;a.y=y;a.maxRadius=55+count*1.8f;a.life=a.maxLife=.34f;a.color=new Color(c);shockwaves.add(a);
        Shockwave b=new Shockwave();b.x=x;b.y=y;b.maxRadius=28+count*.9f;b.life=b.maxLife=.18f;b.color=new Color(Color.WHITE);shockwaves.add(b);
        int rays=Math.max(4,Math.min(14,count/4));
        for(int i=0;i<rays;i++){float ang=MathUtils.random(0f,MathUtils.PI2),len=MathUtils.random(35f,95f+count);beam(x,y,x+MathUtils.cos(ang)*len,y+MathUtils.sin(ang)*len,c,MathUtils.random(2f,5f),MathUtils.random(.08f,.18f));}
    }
    private void beam(float x1,float y1,float x2,float y2,Color c,float width,float life){Beam b=new Beam();b.x1=x1;b.y1=y1;b.x2=x2;b.y2=y2;b.width=width;b.life=b.maxLife=life;b.color=new Color(c);beams.add(b);}
    private void lineRect(float x1,float y1,float x2,float y2,float width){float dx=x2-x1,dy=y2-y1,len=(float)Math.sqrt(dx*dx+dy*dy),ang=MathUtils.atan2(dy,dx)*MathUtils.radiansToDegrees;sr.rect(x1,y1-width/2,0,width,len,width,1,1,ang);}
    private float dist2(float x1,float y1,float x2,float y2){float dx=x2-x1,dy=y2-y1;return dx*dx+dy*dy;}
    private float distToSegment(float px,float py,float x1,float y1,float x2,float y2){float dx=x2-x1,dy=y2-y1,l2=dx*dx+dy*dy;if(l2==0)return Vector2.dst(px,py,x1,y1);float t=((px-x1)*dx+(py-y1)*dy)/l2;t=MathUtils.clamp(t,0,1);float x=x1+t*dx,y=y1+t*dy;return Vector2.dst(px,py,x,y);}
    private void triggerUiPulse(float x,float y,Color color){
        uiPulseX=x;uiPulseY=y;uiPulseColor=new Color(color);uiPulseTime=.34f;
    }

    private void drawUiPulse(){
        float t=1f-MathUtils.clamp(uiPulseTime/.34f,0f,1f);
        float r=42f+t*62f;float a=(1f-t)*.42f;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(uiPulseColor.r,uiPulseColor.g,uiPulseColor.b,a*.35f);sr.circle(uiPulseX,uiPulseY,r+18f,40);
        sr.setColor(uiPulseColor.r,uiPulseColor.g,uiPulseColor.b,a);sr.circle(uiPulseX,uiPulseY,r,40);
        sr.setColor(.006f,.018f,.035f,.92f);sr.circle(uiPulseX,uiPulseY,Math.max(0f,r-7f),40);
        sr.end();
    }

    private void vibrate(int ms){if(game.settings.vibration){try{Gdx.input.vibrate(ms);}catch(Exception ignored){}}}

    @Override public void resize(int width,int height){viewport.update(width,height,true);}
    @Override public void hide(){game.saves.save(save);}
    @Override public void dispose(){sr.dispose();batch.dispose();}
}
