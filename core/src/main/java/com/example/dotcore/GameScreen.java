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
    private Color uiPulseColor=new Color(Ui.CYAN);

    private static final Color FIRE=new Color(1f,.27f,.05f,1f);
    private static final Color ICE=new Color(.25f,.82f,1f,1f);
    private static final Color ELEC=new Color(.78f,.40f,1f,1f);
    private static final Color GRAV=new Color(.48f,.12f,.75f,1f);

    private enum EnemyKind { NORMAL, FAST, TANK, ELITE, BOSS }
    private enum ShotKind { BULLET, ROCKET }
    private enum BonusType { CREDIT, HEAL, OVERDRIVE }

    private static class Touch {
        boolean down; float x,y,startX,startY,startTime,lastTrailTime,lastTrailX,lastTrailY,chargeFxClock; boolean dragged,twoFinger;
    }
    private static class Enemy {
        EnemyKind kind; float x,y,r,hp,maxHp,speed,reward,attackCd;
        float burnTime,burnDps,slowTime=0,slow=1f,freezeTime=0,chill=0;
        boolean dead=false;
    }
    private static class Projectile {
        float x,y,vx,vy,r,damage,life=3f,aoe=0; Element element; ShotKind kind; Enemy target;
    }
    private static class HostileProjectile {
        float x,y,vx,vy,life=4f,damage,r=7f; Turret turretTarget; Drone droneTarget;
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
        float x,y,shield,maxShield,cooldown,aimX,aimY,angle,targetAngle,recoil; boolean broken=false;
    }
    private static class Drone {
        DroneType type; float x,y,angle,shield,maxShield,cooldown,auraTick,respawn=0;
        float orbitRadiusX,orbitRadiusY,orbitSpeed,orbitPhase,centerOffsetX,centerOffsetY,heading; Enemy target; boolean alive=true;
    }
    private static class ShopEntry {
        String id,label; long cost; boolean enabled; ShopEntry(String id,String label,long cost,boolean enabled){this.id=id;this.label=label;this.cost=cost;this.enabled=enabled;}
    }

    public GameScreen(DotCoreGame game, SaveData save){
        this.game=game; this.save=save;
        for(int i=0;i<touches.length;i++) touches[i]=new Touch();
        rebuildDefenses();
        banner=game.assets.t("wave")+"  "+save.wave;
        Gdx.input.setInputProcessor(new InputAdapter(){
            @Override public boolean touchDown(int sx,int sy,int pointer,int button){
                if(pointer>=touches.length) return false;
                Vector3 p=viewport.unproject(tmp3.set(sx,sy,0));
                if(handleUiDown(p.x,p.y)) return true;
                if(shopOpen||debuffShopOpen||effectShopOpen||elementConfigOpen||paused||defeated) return true;
                Touch t=touches[pointer]; t.down=true;t.x=t.startX=p.x;t.y=t.startY=p.y;t.lastTrailX=p.x;t.lastTrailY=p.y;t.startTime=save.playSeconds;t.lastTrailTime=save.playSeconds;t.chargeFxClock=0;t.dragged=false;t.twoFinger=false;
                markTwoFinger();
                return true;
            }
            @Override public boolean touchDragged(int sx,int sy,int pointer){
                if(pointer>=touches.length||shopOpen||debuffShopOpen||effectShopOpen||elementConfigOpen||paused||defeated) return false;
                Vector3 p=viewport.unproject(tmp3.set(sx,sy,0));
                Touch t=touches[pointer]; if(!t.down) return false;
                float oldX=t.x,oldY=t.y; t.x=p.x;t.y=p.y;
                if(Vector2.dst(t.startX,t.startY,t.x,t.y)>24) t.dragged=true;
                markTwoFinger();
                if(twoTouchesDown() && save.ultimateUnlocked){
                    doTwoFingerRift();
                } else if(t.dragged && save.trailUnlocked && save.playSeconds-t.lastTrailTime>0.028f){
                    doTrail(t.lastTrailX,t.lastTrailY,t.x,t.y); t.lastTrailX=t.x; t.lastTrailY=t.y; t.lastTrailTime=save.playSeconds;
                }
                return true;
            }
            @Override public boolean touchUp(int sx,int sy,int pointer,int button){
                if(pointer>=touches.length) return false;
                Vector3 p=viewport.unproject(tmp3.set(sx,sy,0));
                Touch t=touches[pointer];
                if(shopOpen||debuffShopOpen||effectShopOpen||elementConfigOpen||paused||defeated){t.down=false;return true;}
                boolean wasTwo=t.twoFinger||twoTouchesDown();
                float held=save.playSeconds-t.startTime;
                t.x=p.x;t.y=p.y;
                if(!wasTwo){
                    if(held>0.42f && !t.dragged && save.plasmaUnlocked) doPlasma(t.x,t.y,MathUtils.clamp(held/1.6f,.35f,1f));
                    else if(!t.dragged) doTap(t.x,t.y);
                } else if(save.ultimateUnlocked && save.fingerElement==Element.GRAVITY && save.gravityUnlocked){
                    holes.add(makeHole(t.x,t.y,260,2.5f,170f*save.generalDamageMultiplier()));
                }
                t.down=false;
                return true;
            }
            @Override public boolean keyDown(int key){
                if(key==Input.Keys.ESCAPE){ if(shopOpen){shopOpen=false;return true;} if(paused){paused=false;return true;} paused=true;return true; }
                if(key==Input.Keys.S){shopOpen=!shopOpen;return true;}
                if(key==Input.Keys.SPACE){paused=!paused;return true;}
                if(key==Input.Keys.F){save.fingerElement=save.fingerElement.nextCombat(true,save);return true;}
                if(key==Input.Keys.T){save.turretElement=save.turretElement.nextCombat(false,save);return true;}
                if(key==Input.Keys.D){save.droneElement=save.droneElement.nextCombat(false,save);return true;}
                return false;
            }
        });
    }

    private void markTwoFinger(){
        if(!twoTouchesDown()) return;
        int marked=0;
        for(Touch t:touches) if(t.down && marked<2){t.twoFinger=true;marked++;}
    }
    private boolean twoTouchesDown(){int n=0;for(Touch t:touches)if(t.down&&++n>=2)return true;return false;}

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
        if(new Rectangle(930,4,120,104).contains(x,y)){triggerUiPulse(982,58,Ui.CYAN);shopOpen=true;return true;}
        if(new Rectangle(810,4,104,104).contains(x,y)){triggerUiPulse(862,58,ELEC);elementConfigOpen=true;return true;}
        if(new Rectangle(690,4,104,104).contains(x,y)){triggerUiPulse(742,58,Ui.RED);debuffShopOpen=true;shopSellMode=false;return true;}
        if(new Rectangle(20,24,104,104).contains(x,y)){paused=true;return true;}
        if(tryCollectBonus(x,y)) return true;
        if(tryRepairTurret(x,y)) return true;
        return false;
    }


    private boolean tryRepairTurret(float x,float y){
        for(Turret t:turrets){
            if(dist2(x,y,t.x,t.y)>58f*58f) continue;
            if(!t.broken && t.shield>=t.maxShield-0.5f) return true;
            long repairCost=(long)Math.max(25,40+save.wave*8+(1f-t.shield/Math.max(1f,t.maxShield))*150f);
            if(!cheatsEnabled() && save.credits<repairCost){banner=game.assets.t("not_enough");bannerTime=1.1f;return true;}
            if(!cheatsEnabled()) save.credits-=repairCost;
            t.shield=t.maxShield;t.broken=false;
            burst(t.x,t.y,Ui.GREEN,18);game.assets.play(game.assets.buy,game.settings,.18f);vibrate(18);
            banner=game.assets.t("repair")+(cheatsEnabled()?"  C 0":"  C "+repairCost);bannerTime=1.0f;
            game.saves.save(save);
            return true;
        }
        return false;
    }

    @Override public void render(float delta){
        float d=Math.min(delta,0.05f);
        if(uiPulseTime>0f)uiPulseTime=Math.max(0f,uiPulseTime-d);
        // Shop / debuffs / effect setup / pause are true pauses: no credits, waves, cooldowns or play time advance.
        if(!paused&&!shopOpen&&!debuffShopOpen&&!elementConfigOpen&&!effectShopOpen&&!defeated) update(d);
        draw();
    }

    private void update(float d){
        save.playSeconds+=d; waveClock+=d; spawnTimer-=d; passiveClock+=d; saveClock+=d; hostilePulseClock+=d; autoRepairClock+=d; bonusTimer-=d;
        if(overdriveTime>0) overdriveTime-=d;
        if(bannerTime>0) bannerTime-=d;
        if(passiveClock>=1f){int n=(int)passiveClock;passiveClock-=n;save.credits+=save.passiveIncomePerSecond()*n;}
        if(saveClock>5f){saveClock=0;game.saves.save(save);}

        if(bossActive){ bossTimer-=d; if(bossTimer<=0 && bossEnemy!=null && !bossEnemy.dead){ save.integrity=0; defeat(); } }
        float waveDuration=save.wave==1?30f:35f;
        if(waveClock>=waveDuration){
            waveClock-=waveDuration; save.wave++;
            banner=game.assets.t("wave")+"  "+save.wave;bannerTime=2.2f;game.assets.play(game.assets.wave,game.settings,.55f);vibrate(45);
            if(save.wave%5==0) spawnBoss();
        }

        float waveRush=(save.wave>1&&waveClock>24f)?1.65f:1f;
        if(spawnTimer<=0 && !bossActive){
            int batchCount=1;
            float extraChance=Math.min(.78f,save.densityLevel*.085f);
            if(save.densityLevel>0&&MathUtils.random()<extraChance)batchCount++;
            if(save.densityLevel>=5&&MathUtils.random()<Math.min(.38f,(save.densityLevel-4)*.055f))batchCount++;
            for(int i=0;i<batchCount;i++) spawnEnemy(false);
            float baseSpawn=save.wave==1?1.60f:Math.max(.64f,1.08f-(save.wave-2)*.028f);
            spawnTimer=(baseSpawn/(save.spawnMultiplier()*waveRush))*MathUtils.random(.78f,1.18f);
        }

        if(bonusTimer<=0){spawnBonus();bonusTimer=MathUtils.random(18f,30f);}
        updateFingerChargeFx(d);
        updateEnemies(d); updateTurrets(d); updateDrones(d); updateProjectiles(d); updateHostileProjectiles(d); updateHoles(d); updateTrails(d); updateShockwaves(d); updateBonuses(d); updateParticles(d); updateBeams(d);

        if(enemies.size>MAX_ENEMIES_BEFORE_OVERRUN){ save.integrity-=(enemies.size-MAX_ENEMIES_BEFORE_OVERRUN)*1.5f*d; }
        if(save.wave>1 && hostilePulseClock>2.1f){hostilePulseClock=0;hostilePulse();}
        if(save.autoRepairUnlocked && autoRepairClock>1f){autoRepairClock=0;autoRepairTick();}
        if(save.integrity<=0) defeat();
    }

    private void updateFingerChargeFx(float d){
        if(!save.plasmaUnlocked)return;
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
            e.kind=EnemyKind.BOSS;e.r=118;e.maxHp=(1800+save.wave*480)*save.enemyHealthMultiplier();e.speed=11;e.reward=(900+save.wave*90)*save.creditMultiplier();
        } else if(save.wave==1){
            // Intro/tutorial wave: only simple slow targets, no enemy shooting.
            e.kind=EnemyKind.NORMAL;e.r=24;e.maxHp=18f*save.enemyHealthMultiplier();e.speed=22f;e.reward=14f*save.creditMultiplier();
        } else {
            float q=MathUtils.random();
            float eliteChance=Math.min(.16f,.055f+save.wave*.004f);
            float tankCut=eliteChance+.16f+Math.min(.08f,save.wave*.003f);
            float fastCut=tankCut+.22f+Math.min(.08f,save.wave*.0025f);
            if(q<eliteChance){e.kind=EnemyKind.ELITE;e.r=46;e.maxHp=(95+save.wave*10)*save.enemyHealthMultiplier();e.speed=23;e.reward=(42+save.wave*3)*save.creditMultiplier();}
            else if(q<tankCut){e.kind=EnemyKind.TANK;e.r=38;e.maxHp=(60+save.wave*7)*save.enemyHealthMultiplier();e.speed=18;e.reward=(28+save.wave*2.2f)*save.creditMultiplier();}
            else if(q<fastCut){e.kind=EnemyKind.FAST;e.r=17;e.maxHp=(13+save.wave*2.0f)*save.enemyHealthMultiplier();e.speed=52;e.reward=(8+save.wave*.9f)*save.creditMultiplier();}
            else {e.kind=EnemyKind.NORMAL;e.r=25;e.maxHp=(25+save.wave*3.2f)*save.enemyHealthMultiplier();e.speed=31;e.reward=(13+save.wave*1.2f)*save.creditMultiplier();}
        }
        e.speed*=save.enemySpeedMultiplier();
        e.hp=e.maxHp;e.x=MathUtils.random(75f,W-75f);e.y=boss?1660f:H+e.r;e.attackCd=MathUtils.random(1.2f,3.6f);enemies.add(e);
    }

    private void spawnBoss(){
        bossActive=true;bossTimer=32f;spawnEnemy(true);bossEnemy=enemies.peek();banner=game.assets.t("boss_incoming");bannerTime=3f;game.assets.play(game.assets.boss,game.settings,.75f);vibrate(100);
    }

    private void updateEnemies(float d){
        for(int i=enemies.size-1;i>=0;i--){
            Enemy e=enemies.get(i);
            if(e.dead){enemies.removeIndex(i);continue;}
            if(e.burnTime>0){e.burnTime-=d;damageRaw(e,e.burnDps*d);}
            if(e.freezeTime>0){e.freezeTime-=d;e.slow=.08f;}
            else if(e.slowTime>0){e.slowTime-=d;} else {e.slow=1f;e.chill=Math.max(0,e.chill-d*.45f);}
            e.y-=e.speed*e.slow*d;
            e.attackCd-=d;
            if(save.wave>1 && e.attackCd<=0 && (e.kind==EnemyKind.BOSS || e.y<920 || hasDroneNear(e.x,e.y,430f))) { e.attackCd=MathUtils.random(2f,4.2f); enemyAttack(e); }
            if(e.y-e.r<=GROUND_Y){
                save.integrity-=e.kind==EnemyKind.BOSS?60:(e.kind==EnemyKind.ELITE?13:e.kind==EnemyKind.TANK?9:5);
                explode(e.x,e.y,colorFor(e),e.kind==EnemyKind.BOSS?55:22);e.dead=true;
            }
        }
    }

    private void enemyAttack(Enemy e){
        if(turrets.size+drones.size==0)return;
        float dmg=(e.kind==EnemyKind.BOSS?18f:e.kind==EnemyKind.ELITE?7f:3.5f)*save.enemyDamageMultiplier();
        HostileProjectile p=new HostileProjectile();p.x=e.x;p.y=e.y;p.damage=dmg;p.r=e.kind==EnemyKind.BOSS?10f:7f;
        // Drones deliberately draw fire when they enter the enemy's local combat bubble.
        Drone nearestDrone=null;float droneBest=430f*430f;
        for(int i=0;i<drones.size;i++){Drone q=drones.get(i);if(!q.alive)continue;float ds=dist2(e.x,e.y,q.x,q.y);if(ds<droneBest){droneBest=ds;nearestDrone=q;}}
        if(nearestDrone!=null){p.droneTarget=nearestDrone;setHostileVelocity(p,nearestDrone.x,nearestDrone.y,e.kind==EnemyKind.BOSS?520f:420f);hostileProjectiles.add(p);return;}
        Turret nearestTurret=null;float turretBest=99999999f;
        for(int i=0;i<turrets.size;i++){Turret q=turrets.get(i);if(q.broken)continue;float ds=dist2(e.x,e.y,q.x,q.y);if(ds<turretBest){turretBest=ds;nearestTurret=q;}}
        if(nearestTurret!=null){p.turretTarget=nearestTurret;setHostileVelocity(p,nearestTurret.x,nearestTurret.y,e.kind==EnemyKind.BOSS?520f:420f);hostileProjectiles.add(p);}
    }

    private boolean hasDroneNear(float x,float y,float radius){float r2=radius*radius;for(int i=0;i<drones.size;i++){Drone d=drones.get(i);if(d.alive&&dist2(x,y,d.x,d.y)<r2)return true;}return false;}

    private void setHostileVelocity(HostileProjectile p,float tx,float ty,float speed){
        float dx=tx-p.x,dy=ty-p.y,len=(float)Math.sqrt(dx*dx+dy*dy);if(len<1f)len=1f;p.vx=dx/len*speed;p.vy=dy/len*speed;
    }

    private void updateTurrets(float d){
        float overdrive=overdriveTime>0?1.55f:1f;
        for(Turret t:turrets){
            t.recoil=Math.max(0f,t.recoil-d*115f);
            if(t.broken)continue;
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
            if(save.turretWeapon==1&&save.turretLaserUnlocked){
                float dmg=18f*(1f+save.turretDamageLevel*.18f)*save.generalDamageMultiplier()*(1f+save.turretSkillLevel*.008f);
                dealDamage(target,dmg,save.turretElement,true);gainSkill("turret",dmg);beam(t.x,t.y,target.x,target.y,elementColor(save.turretElement),6,0.11f);game.assets.play(game.assets.laser,game.settings,.16f);
                t.recoil=8f;t.cooldown=Math.max(.055f,.78f/rate);
            }else if(save.turretWeapon==2&&save.turretRocketsUnlocked){
                float rd=18f*(1f+save.turretDamageLevel*.20f)*save.generalDamageMultiplier()*(1f+save.turretSkillLevel*.008f);fireProjectile(t.x,t.y,target,rd,save.turretElement,ShotKind.ROCKET,72);gainSkill("turret",rd);game.assets.play(game.assets.rocket,game.settings,.13f);
                t.recoil=14f;t.cooldown=Math.max(.20f,1.45f/rate);
            }else{
                float bd=8f*(1f+save.turretDamageLevel*.17f)*save.generalDamageMultiplier()*(1f+save.turretSkillLevel*.008f);fireProjectile(t.x,t.y,target,bd,save.turretElement,ShotKind.BULLET,0);gainSkill("turret",bd);game.assets.play(game.assets.shot,game.settings,.09f);
                t.recoil=10f;t.cooldown=Math.max(.034f,.50f/rate);
            }
        }
    }

    private void updateDrones(float d){
        final float detectRange=620f; // intentionally about half the old range: drones must enter the fight.
        for(int di=0;di<drones.size;di++){
            Drone dr=drones.get(di);
            if(!dr.alive){dr.respawn-=d;if(dr.respawn<=0){dr.alive=true;dr.shield=dr.maxShield;dr.cooldown=.6f;dr.target=null;burst(dr.x,dr.y,Ui.CYAN,18);}continue;}
            dr.angle+=d*dr.orbitSpeed;
            float oldX=dr.x,oldY=dr.y;
            if(dr.target==null||dr.target.dead||dist2(dr.x,dr.y,dr.target.x,dr.target.y)>detectRange*detectRange*1.5f)dr.target=nearestEnemy(dr.x,dr.y,detectRange);

            if(dr.type==DroneType.KAMIKAZE && dr.target!=null){
                float dx=dr.target.x-dr.x,dy=dr.target.y-dr.y,len=(float)Math.sqrt(dx*dx+dy*dy);
                if(len<dr.target.r+29f){
                    float boom=(7f+save.droneDamageLevel*1.6f)*save.generalDamageMultiplier()*(1f+save.droneSkillLevel*.008f)*5.5f;
                    aoe(dr.target.x,dr.target.y,135,boom,save.droneElement);gainSkill("drone",boom);explode(dr.target.x,dr.target.y,elementColor(save.droneElement),40);killDrone(dr);dr.target=null;game.assets.play(game.assets.explosion,game.settings,.25f);vibrate(22);continue;
                }
                float speed=355f+save.droneRateLevel*7f;dr.x+=dx/Math.max(1f,len)*speed*d;dr.y+=dy/Math.max(1f,len)*speed*d;dr.heading=MathUtils.atan2(dy,dx);applyDroneAura(dr,d);continue;
            }

            if(dr.target!=null && dr.type!=DroneType.SUPPORT){
                // Gun/missile drones close to standoff range instead of firing from the other side of the screen.
                float dx=dr.target.x-dr.x,dy=dr.target.y-dr.y,len=(float)Math.sqrt(dx*dx+dy*dy);float desired=145f;
                if(len>desired){float speed=185f+save.droneRateLevel*3.5f;dr.x+=dx/Math.max(1f,len)*speed*d;dr.y+=dy/Math.max(1f,len)*speed*d;}
                else {float tangent=55f*d;dr.x+=-dy/Math.max(1f,len)*tangent;dr.y+=dx/Math.max(1f,len)*tangent;}
            }else{
                float baseX=540+dr.centerOffsetX+MathUtils.cos(dr.angle+dr.orbitPhase)*dr.orbitRadiusX+MathUtils.sin(dr.angle*.43f+dr.orbitPhase)*58f;
                float baseY=650+dr.centerOffsetY+MathUtils.sin(dr.angle*.71f+dr.orbitPhase)*dr.orbitRadiusY+MathUtils.cos(dr.angle*.37f)*38f;
                baseX=MathUtils.clamp(baseX,85f,W-85f);baseY=MathUtils.clamp(baseY,340f,1120f);
                float move=Math.min(1f,d*(.75f+dr.type.ordinal()*.08f));dr.x=MathUtils.lerp(dr.x,baseX,move);dr.y=MathUtils.lerp(dr.y,baseY,move);
            }
            dr.x=MathUtils.clamp(dr.x,55f,W-55f);dr.y=MathUtils.clamp(dr.y,245f,1500f);
            if(dist2(oldX,oldY,dr.x,dr.y)>.01f)dr.heading=MathUtils.atan2(dr.y-oldY,dr.x-oldX);
            dr.cooldown-=d;dr.auraTick-=d;applyDroneAura(dr,d);
            if(dr.type==DroneType.SUPPORT){healNearbyDrones(dr,d);continue;}
            if(dr.cooldown>0||dr.target==null)continue;
            int near=nearbyDroneCount(dr,190);
            float auraDamage=1f+(save.droneAuraElement==Element.FIRE?near*.07f:near*.02f);
            float auraRate=1f+(save.droneAuraElement==Element.LIGHTNING?near*.07f:save.droneAuraElement==Element.ICE?near*.03f:0f);
            float rate=(1f+save.droneRateLevel*.18f)*save.generalRateMultiplier()*auraRate*(1f+save.droneSkillLevel*.006f)*(overdriveTime>0?1.55f:1f);
            float dmg=(7f+save.droneDamageLevel*1.6f)*save.generalDamageMultiplier()*auraDamage*(1f+save.droneSkillLevel*.008f);
            if(dr.type==DroneType.GUN){fireProjectile(dr.x,dr.y,dr.target,dmg,save.droneElement,ShotKind.BULLET,0);gainSkill("drone",dmg);dr.cooldown=Math.max(.06f,.55f/rate);game.assets.play(game.assets.shot,game.settings,.055f);}
            else if(dr.type==DroneType.MISSILE){fireProjectile(dr.x,dr.y,dr.target,dmg*1.9f,save.droneElement,ShotKind.ROCKET,62+save.droneAuraLevel*3);gainSkill("drone",dmg*1.9f);dr.cooldown=Math.max(.22f,1.5f/rate);game.assets.play(game.assets.rocket,game.settings,.08f);}
        }
    }

    private void applyDroneAura(Drone dr,float d){
        float radius=125+save.droneAuraLevel*13;
        if(dr.type==DroneType.SUPPORT)return;
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
    private void killDrone(Drone d){d.alive=false;d.shield=0;d.respawn=Math.max(5f,11f-save.droneShieldLevel*.4f);burst(d.x,d.y,Ui.RED,26);}

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
                p.droneTarget.shield-=p.damage;if(p.droneTarget.shield<=0)killDrone(p.droneTarget);hit=true;
            }else if(p.turretTarget!=null && !p.turretTarget.broken && dist2(p.x,p.y,p.turretTarget.x,p.turretTarget.y)<(p.r+40f)*(p.r+40f)){
                p.turretTarget.shield-=p.damage;if(p.turretTarget.shield<=0){p.turretTarget.shield=0;p.turretTarget.broken=true;}hit=true;
            }
            if(hit){burst(p.x,p.y,Ui.RED,7);hostileProjectiles.removeIndex(i);}
            else if(p.life<=0||p.x<-30||p.x>W+30||p.y<-30||p.y>H+50)hostileProjectiles.removeIndex(i);
        }
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
                        if(t.element==Element.GRAVITY&&save.gravityUnlocked){
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
        float cooldown=Math.max(.035f,.22f/((1f+save.tapSpeedLevel*.18f)*save.generalRateMultiplier()));if(save.playSeconds-lastTapTime<cooldown)return;lastTapTime=save.playSeconds;
        if(save.fingerElement==Element.GRAVITY&&save.gravityUnlocked){holes.add(makeHole(x,y,105+save.tapDamageLevel*2.5f,.85f,18f*(1+save.tapDamageLevel*.18f)*save.generalDamageMultiplier()));gainSkill("tap",12f);game.assets.play(game.assets.plasma,game.settings,.12f);vibrate(18);return;}
        Enemy target=nearestEnemy(x,y,85);float dmg=18f*(1+save.tapDamageLevel*.22f)*save.generalDamageMultiplier()*(1f+save.tapSkillLevel*.01f);if(target!=null){dealDamage(target,dmg,save.fingerElement,true);gainSkill("tap",dmg);}burst(x,y,elementColor(save.fingerElement),10);vibrate(7);
    }

    private void doTrail(float x1,float y1,float x2,float y2){
        float dx=x2-x1,dy=y2-y1,len=(float)Math.sqrt(dx*dx+dy*dy);int steps=Math.max(1,(int)(len/34f));
        float dmg=4.5f*(1+save.tapDamageLevel*.13f)*save.generalDamageMultiplier()*(1f+save.trailSkillLevel*.008f);
        if(save.fingerElement==Element.GRAVITY&&save.gravityUnlocked){
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
        if(save.fingerElement==Element.GRAVITY&&save.gravityUnlocked){holes.add(makeHole(x,y,radius*1.3f,1.5f+charge*1.4f,dmg*1.25f));}
        else{aoe(x,y,radius,dmg,save.fingerElement);explode(x,y,elementColor(save.fingerElement),(int)(28+charge*35));}
        game.assets.play(game.assets.plasma,game.settings,.32f);vibrate((int)(30+charge*65));
    }

    private void doTwoFingerRift(){
        Touch a=null,b=null;for(Touch t:touches)if(t.down){if(a==null)a=t;else{b=t;break;}}if(a==null||b==null)return;
        float dmg=7.5f*save.generalDamageMultiplier()*(1+save.tapDamageLevel*.08f);
        if(save.fingerElement==Element.GRAVITY&&save.gravityUnlocked){for(Enemy e:enemies){float dist=distToSegment(e.x,e.y,a.x,a.y,b.x,b.y);if(dist<120){float mx=(a.x+b.x)*.5f,my=(a.y+b.y)*.5f;float gf=gravityPullFactor(e,.95f);e.x+=MathUtils.clamp(mx-e.x,-26,26)*gf;e.y+=MathUtils.clamp(my-e.y,-26,26)*gf;damageRaw(e,dmg);}}beam(a.x,a.y,b.x,b.y,GRAV,26,.14f);}
        else{for(Enemy e:enemies)if(distToSegment(e.x,e.y,a.x,a.y,b.x,b.y)<72+e.r)dealDamage(e,dmg,save.fingerElement,false);beam(a.x,a.y,b.x,b.y,elementColor(save.fingerElement),24,.14f);}
    }

    private BlackHole makeHole(float x,float y,float r,float life,float damage){BlackHole h=new BlackHole();h.x=x;h.y=y;h.radius=r;h.life=h.maxLife=life;h.damage=damage;h.pullStrength=MathUtils.clamp((r-80f)/210f+.15f,0.15f,1.35f);return h;}
    private void aoe(float x,float y,float r,float dmg,Element elem){for(int i=0;i<enemies.size;i++){Enemy e=enemies.get(i);if(!e.dead&&dist2(x,y,e.x,e.y)<(r+e.r)*(r+e.r))dealDamage(e,dmg,elem,true);}}

    private void dealDamage(Enemy e,float dmg,Element elem,boolean chain){
        if(e==null||e.dead)return;damageRaw(e,dmg);
        if(elem==Element.FIRE){e.burnDps=Math.max(e.burnDps,dmg*.14f);e.burnTime=Math.max(e.burnTime,3.3f);}
        else if(elem==Element.ICE){e.chill+=1f;e.slow=.56f;e.slowTime=Math.max(e.slowTime,2.2f);if(e.chill>=4f){e.freezeTime=Math.max(e.freezeTime,1.1f);e.chill=1.5f;}if(chain)game.assets.play(game.assets.ice,game.settings,.035f);}
        else if(elem==Element.LIGHTNING&&chain){chainLightning(e,dmg*.62f,2+(save.lightningUnlocked?1:0));game.assets.play(game.assets.electric,game.settings,.045f);}
    }
    private void damageRaw(Enemy e,float dmg){e.hp-=dmg;if(e.hp<=0&&!e.dead)killEnemy(e);}
    private void chainLightning(Enemy from,float dmg,int jumps){Enemy cur=from;Array<Enemy> used=new Array<>();used.add(from);for(int j=0;j<jumps;j++){Enemy next=null;float best=230*230;for(Enemy e:enemies){if(e.dead||used.contains(e,true))continue;float d=dist2(cur.x,cur.y,e.x,e.y);if(d<best){best=d;next=e;}}if(next==null)break;beam(cur.x,cur.y,next.x,next.y,ELEC,5,0.15f);damageRaw(next,dmg*(1f-j*.13f));used.add(next);cur=next;}}

    private void killEnemy(Enemy e){
        e.dead=true;save.credits+=e.reward;save.totalKills++;if(e.kind==EnemyKind.BOSS){save.totalBossKills++;bossActive=false;bossEnemy=null;bossTimer=0;banner=game.assets.t("boss_destroyed");bannerTime=2.5f;}
        explode(e.x,e.y,colorFor(e),e.kind==EnemyKind.BOSS?65:(e.kind==EnemyKind.TANK||e.kind==EnemyKind.ELITE?30:16));game.assets.play(e.kind==EnemyKind.BOSS?game.assets.explosion:game.assets.pop,game.settings,e.kind==EnemyKind.BOSS?.55f:.09f);if(e.kind==EnemyKind.BOSS)vibrate(120);
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

    private void resetSlot(){SaveData fresh=game.saves.fresh(save.slot);game.saves.save(fresh);game.changeScreen(new GameScreen(game,fresh));}
    private void defeat(){if(defeated)return;defeated=true;save.integrity=0;game.saves.save(save);vibrate(180);}

    private void draw(){
        viewport.apply();camera.update();Gdx.gl.glClearColor(Ui.BG.r,Ui.BG.g,Ui.BG.b,1);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        sr.setProjectionMatrix(camera.combined);batch.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.begin(ShapeRenderer.ShapeType.Filled);drawBackground();drawGround();drawHoles();drawEnemies();drawFingerCharge();drawTrails();drawShockwaves();drawBonuses();drawProjectiles();drawHostileProjectiles();drawTurrets();drawDrones();drawParticles();drawBeams();sr.end();
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
    private void drawGround(){sr.setColor(.04f,.55f,.9f,.18f);sr.rect(0,0,W,GROUND_Y);sr.setColor(Ui.CYAN);sr.rect(0,GROUND_Y,W,3);}

    private void drawEnemies(){
        for(Enemy e:enemies){
            if(e.dead)continue;Color c=colorFor(e);float hp=MathUtils.clamp(e.hp/e.maxHp,0,1);
            float pulse=1f+.035f*MathUtils.sin(save.playSeconds*4f+e.x*.01f);
            // Soft energy aura.
            sr.setColor(c.r,c.g,c.b,.045f);sr.circle(e.x,e.y,e.r*1.85f*pulse,40);
            sr.setColor(c.r,c.g,c.b,.11f);sr.circle(e.x,e.y,e.r*1.36f*pulse,36);
            // Bubble shell: bright rim with a dark translucent interior.
            sr.setColor(c.r,c.g,c.b,.70f);sr.circle(e.x,e.y,e.r*1.04f,40);
            sr.setColor(.018f,.035f,.065f,.92f);sr.circle(e.x,e.y,e.r*.86f,40);
            sr.setColor(c.r,c.g,c.b,.22f);sr.circle(e.x,e.y,e.r*.68f,36);
            sr.setColor(c.r,c.g,c.b,.78f);sr.circle(e.x,e.y,e.r*.23f,28);
            sr.setColor(1,1,1,.72f);sr.circle(e.x-e.r*.28f,e.y+e.r*.28f,Math.max(2.5f,e.r*.11f),20);
            if(e.burnTime>0){sr.setColor(FIRE.r,FIRE.g,FIRE.b,.22f);sr.circle(e.x,e.y,e.r*1.28f,36);}
            if(e.slow<1){sr.setColor(ICE.r,ICE.g,ICE.b,.20f);sr.circle(e.x,e.y,e.r*1.38f,36);}
            if(e.r>34){sr.setColor(.04f,.06f,.09f,.92f);sr.rect(e.x-e.r,e.y+e.r+12,e.r*2,6);sr.setColor(hp>.35f?Ui.GREEN:Ui.RED);sr.rect(e.x-e.r,e.y+e.r+12,e.r*2*hp,6);}
        }
    }

    private void drawFingerCharge(){
        if(!save.plasmaUnlocked)return;
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

    private void drawProjectiles(){for(Projectile p:projectiles){Color c=elementColor(p.element);if(p.kind==ShotKind.ROCKET){sr.setColor(c.r,c.g,c.b,.18f);sr.circle(p.x-p.vx*.025f,p.y-p.vy*.025f,14,16);sr.setColor(c);sr.circle(p.x,p.y,8,16);}else{sr.setColor(c.r,c.g,c.b,.18f);sr.circle(p.x,p.y,10,12);sr.setColor(c);sr.circle(p.x,p.y,4,12);}}}

    private void drawHostileProjectiles(){
        for(HostileProjectile p:hostileProjectiles){
            float sp=(float)Math.sqrt(p.vx*p.vx+p.vy*p.vy),nx=sp>1f?-p.vx/sp:0,ny=sp>1f?-p.vy/sp:0;
            sr.setColor(1f,.12f,.05f,.16f);lineRect(p.x,p.y,p.x+nx*25f,p.y+ny*25f,p.r*2.2f);
            sr.setColor(1f,.35f,.08f,.95f);sr.circle(p.x,p.y,p.r,14);sr.setColor(1f,.92f,.65f,.8f);sr.circle(p.x,p.y,Math.max(2f,p.r*.35f),12);
        }
    }

    private void drawTurrets(){
        for(Turret t:turrets){
            float hp=t.maxShield<=0?0:t.shield/t.maxShield;Color accent=t.broken?Ui.RED:elementColor(save.turretElement);
            float rad=t.angle*MathUtils.degreesToRadians,fx=MathUtils.cos(rad),fy=MathUtils.sin(rad),nx=-fy,ny=fx;
            // Shield glow and armored tripod base.
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
    }

    private void drawLightningTrail(TrailFx t,float alpha){
        float dx=t.x2-t.x1,dy=t.y2-t.y1,len=(float)Math.sqrt(dx*dx+dy*dy);if(len<1f)return;
        float nx=-dy/len,ny=dx/len;int parts=Math.max(3,Math.min(9,(int)(len/30f)+1));
        float px=t.x1,py=t.y1;
        for(int i=1;i<=parts;i++){
            float q=i/(float)parts;float off=i==parts?0f:MathUtils.sin(t.seed+i*2.11f)*14f;
            float qx=t.x1+dx*q+nx*off,qy=t.y1+dy*q+ny*off;
            sr.setColor(ELEC.r,ELEC.g,ELEC.b,.16f*alpha);lineRect(px,py,qx,qy,18f);
            sr.setColor(ELEC.r,ELEC.g,ELEC.b,.72f*alpha);lineRect(px,py,qx,qy,5.5f);
            sr.setColor(1f,1f,1f,.92f*alpha);lineRect(px,py,qx,qy,1.8f);
            px=qx;py=qy;
        }
    }

    private void drawShockwaves(){for(Shockwave w:shockwaves){float a=MathUtils.clamp(w.life/w.maxLife,0f,1f);sr.setColor(w.color.r,w.color.g,w.color.b,.12f*a);sr.circle(w.x,w.y,w.radius,48);sr.setColor(1f,1f,1f,.10f*a);sr.circle(w.x,w.y,Math.max(5f,w.radius*.45f),36);}}
    private void drawBonuses(){for(Bonus b:bonuses){float by=b.y+MathUtils.sin(b.bob)*8f;Color c=b.type==BonusType.CREDIT?Ui.GOLD:(b.type==BonusType.HEAL?Ui.GREEN:ELEC);float pulse=1f+.08f*MathUtils.sin(save.playSeconds*8f+b.x);sr.setColor(c.r,c.g,c.b,.08f);sr.circle(b.x,by,58f*pulse,32);sr.setColor(c.r,c.g,c.b,.30f);sr.circle(b.x,by,42f*pulse,32);sr.setColor(.01f,.03f,.06f,.88f);sr.circle(b.x,by,32f,28);}}
    private void drawBonusIcons(){if(bonuses.size==0)return;batch.begin();for(Bonus b:bonuses){Texture t=game.assets.icon(b.type==BonusType.CREDIT?"bonus_credit":b.type==BonusType.HEAL?"bonus_heal":"bonus_overdrive");if(t!=null){float by=b.y+MathUtils.sin(b.bob)*8f;batch.setColor(1,1,1,Math.min(1f,b.life));batch.draw(t,b.x-30,by-30,60,60);}}batch.setColor(Color.WHITE);batch.end();}

    private void drawHud(){
        float integrity=MathUtils.clamp(save.integrity/100f,0f,1f);
        float progress=bossActive&&bossEnemy!=null?MathUtils.clamp(bossEnemy.hp/bossEnemy.maxHp,0f,1f):MathUtils.clamp(waveClock/35f,0f,1f);
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
        // Bottom action buttons moved down so they do not crowd the planet HP bar.
        sr.setColor(1,1,1,.76f);sr.rect(42,28,10,54);sr.rect(68,28,10,54);
        float[] cx={742,862,982};for(float c:cx){sr.setColor(.01f,.04f,.075f,.76f);sr.circle(c,58,46,32);sr.setColor(Ui.CYAN.r,Ui.CYAN.g,Ui.CYAN.b,.15f);sr.circle(c,58,51,32);}
        sr.end();
        batch.begin();
        String creditText=cheatsEnabled()?"C ∞":"C "+(long)save.credits;
        float cw=game.assets.font.width(creditText,.78f);Ui.text(batch,game.assets.font,creditText,W-34-cw,1838,.78f,Ui.GOLD);
        String income="+"+String.format(java.util.Locale.US,"%.1f",save.passiveIncomePerSecond())+"/s";
        float iw=game.assets.font.width(income,.49f);Ui.text(batch,game.assets.font,income,W-34-iw,1788,.49f,new Color(.58f,.82f,.92f,.94f));
        Ui.text(batch,game.assets.font,Math.max(0,(int)save.integrity)+"%",500,128,.50f,Color.WHITE);
        if(bossActive)Ui.text(batch,game.assets.font,String.format(java.util.Locale.US,"%.0fs",bossTimer),910,1847,.42f,Ui.RED);
        if(overdriveTime>0)Ui.text(batch,game.assets.font,"BOOST "+String.format(java.util.Locale.US,"%.0fs",overdriveTime),32,174,.38f,ELEC);
        Texture deb=game.assets.icon("debuff_button");if(deb!=null)batch.draw(deb,710,24,64,64);
        Texture cfg=game.assets.icon("config");if(cfg!=null)batch.draw(cfg,830,24,64,64);
        Texture shp=game.assets.icon("shop_button");if(shp!=null)batch.draw(shp,950,24,64,64);
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

    private void drawShop(){
        drawOverlay();
        Rectangle panel=new Rectangle(40,125,1000,1660);sr.begin(ShapeRenderer.ShapeType.Filled);Ui.panel(sr,panel,Ui.CYAN);sr.end();
        batch.begin();Ui.text(batch,game.assets.font,game.assets.t("shop"),72,1720,1.12f,Color.WHITE);Ui.text(batch,game.assets.font,cheatsEnabled()?"C ∞":"C "+(long)save.credits,742,1718,.78f,Ui.GOLD);batch.end();
        String[] tabs={game.assets.t("finger"),game.assets.t("turrets"),game.assets.t("drones")};
        String[] tabIcons={"tab_finger","tab_turrets","tab_drones"};
        for(int i=0;i<tabs.length;i++){
            Rectangle r=new Rectangle(90+i*300,1540,280,100);
            sr.begin(ShapeRenderer.ShapeType.Filled);Ui.button(sr,r,true,i==shopTab);sr.end();
            batch.begin();Texture ti=game.assets.icon(tabIcons[i]);if(ti!=null)batch.draw(ti,r.x+86,r.y+28,70,70);Ui.centered(batch,game.assets.font,tabs[i],new Rectangle(r.x,r.y-42,r.width,54),.49f,Color.WHITE);batch.end();
        }
        Array<ShopEntry> list=shopEntries();int shown=Math.min(10,list.size);
        for(int i=0;i<shown;i++){
            ShopEntry e=list.get(i);int col=i%2,row=i/2;Rectangle r=new Rectangle(70+col*478,1310-row*230,456,204);boolean active=shopSellMode?canSell(e):e.enabled;
            sr.begin(ShapeRenderer.ShapeType.Filled);Ui.button(sr,r,active,false);sr.end();batch.begin();
            Texture icon=game.assets.icon(e.id);if(icon!=null){batch.setColor(1,1,1,active?1f:.36f);batch.draw(icon,r.x+16,r.y+39,122,122);batch.setColor(Color.WHITE);}
            Color tc=active?Color.WHITE:new Color(.5f,.53f,.58f,1);Ui.text(batch,game.assets.font,shopShortLabel(e),r.x+148,r.y+158,.60f,tc);Ui.text(batch,game.assets.font,shopLevelText(e),r.x+148,r.y+101,.49f,new Color(.6f,.78f,.9f,active?1f:.65f));
            String price;if(shopSellMode){long refund=refundFor(e);price=active?game.assets.t("sell_refund")+" C "+refund:game.assets.t("not_sellable");}else price=cheatsEnabled()&&e.cost>0?"C 0":(e.cost<=0?game.assets.t("max"):(e.cost>=Long.MAX_VALUE/4?game.assets.t("locked"):"C "+e.cost));
            Ui.text(batch,game.assets.font,price,r.x+148,r.y+45,.51f,shopSellMode?Ui.GREEN:(active?Ui.GOLD:new Color(.45f,.35f,.2f,1)));batch.end();
        }
        drawOverlayButton(new Rectangle(74,174,250,96),shopSellMode?game.assets.t("buy_mode"):game.assets.t("sell_mode"),true);drawOverlayButton(new Rectangle(350,174,380,96),game.assets.t("close"),true);
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
        drawOverlay();Rectangle panel=new Rectangle(70,250,940,1430);sr.begin(ShapeRenderer.ShapeType.Filled);Ui.panel(sr,panel,Ui.RED);sr.end();
        batch.begin();
        Ui.text(batch,game.assets.font,game.assets.t("debuffs"),110,1600,1.14f,Color.WHITE);
        Ui.text(batch,game.assets.font,cheatsEnabled()?"C ∞":"C "+(long)save.credits,745,1597,.78f,Ui.GOLD);
        Ui.text(batch,game.assets.font,game.assets.t("passive")+"  +"+String.format(java.util.Locale.US,"%.1f",save.passiveIncomePerSecond())+" C/s",110,1522,.58f,new Color(.70f,.86f,.94f,1));
        Ui.text(batch,game.assets.font,game.assets.t("debuff_threat"),110,1435,.62f,Ui.RED);
        Ui.text(batch,game.assets.font,game.assets.t("debuff_pressure"),110,1075,.62f,Ui.CYAN);
        Ui.text(batch,game.assets.font,game.assets.t("debuff_profit"),110,715,.62f,Ui.GOLD);
        batch.end();
        Array<ShopEntry> list=debuffEntries();
        float[] ys={1200,1200,840,840,480,480};
        for(int i=0;i<list.size;i++){
            ShopEntry e=list.get(i);int col=i%2;Rectangle r=new Rectangle(105+col*445,ys[i],420,200);drawDebuffCard(r,e);
        }
        drawOverlayButton(new Rectangle(130,300,310,105),shopSellMode?game.assets.t("buy_mode"):game.assets.t("sell_mode"),true);
        drawOverlayButton(new Rectangle(530,300,420,105),game.assets.t("close"),true);
    }

    private void debuffShopClick(float x,float y){
        if(new Rectangle(530,300,420,105).contains(x,y)){debuffShopOpen=false;shopSellMode=false;game.saves.save(save);return;}
        if(new Rectangle(130,300,310,105).contains(x,y)){shopSellMode=!shopSellMode;return;}
        Array<ShopEntry> list=debuffEntries();float[] ys={1200,1200,840,840,480,480};
        for(int i=0;i<list.size;i++){int col=i%2;Rectangle r=new Rectangle(105+col*445,ys[i],420,200);if(r.contains(x,y)){if(shopSellMode)sell(list.get(i));else buy(list.get(i));return;}}
    }

    private void drawDebuffCard(Rectangle r,ShopEntry e){
        boolean active=shopSellMode?canSell(e):e.enabled;
        sr.begin(ShapeRenderer.ShapeType.Filled);Ui.button(sr,r,active,false);sr.end();batch.begin();
        Texture icon=game.assets.icon(e.id);if(icon!=null){batch.setColor(1,1,1,active?1f:.38f);batch.draw(icon,r.x+18,r.y+45,104,104);batch.setColor(Color.WHITE);}
        Ui.text(batch,game.assets.font,shopShortLabel(e),r.x+136,r.y+155,.58f,active?Color.WHITE:new Color(.5f,.53f,.58f,1));
        Ui.text(batch,game.assets.font,shopLevelText(e),r.x+136,r.y+105,.49f,new Color(.65f,.80f,.92f,active?1f:.65f));
        String price;if(shopSellMode){long refund=refundFor(e);price=active?game.assets.t("sell_refund")+" C "+refund:game.assets.t("not_sellable");}else price=cheatsEnabled()?"C 0":"C "+e.cost;
        Ui.text(batch,game.assets.font,price,r.x+136,r.y+55,.50f,shopSellMode?Ui.GREEN:Ui.GOLD);
        if(!shopSellMode)Ui.text(batch,game.assets.font,"+"+String.format(java.util.Locale.US,"%.1f",passiveGainFor(e.id))+" C/s",r.x+270,r.y+55,.42f,Ui.GREEN);
        batch.end();
    }

    private float passiveGainFor(String id){
        return switch(id){case "density"->.80f;case "spawn"->.90f;case "value"->1.20f;case "enemySpeed"->.90f;case "enemyDamage","enemyHealth"->1.10f;default->0f;};
    }

    private void drawEffectShop(){
        drawOverlay();Rectangle p=new Rectangle(115,420,850,1010);sr.begin(ShapeRenderer.ShapeType.Filled);Ui.panel(sr,p,ELEC);sr.end();
        batch.begin();Ui.text(batch,game.assets.font,game.assets.t("effect_shop"),270,1340,1.04f,Color.WHITE);Ui.text(batch,game.assets.font,cheatsEnabled()?"C ∞":"C "+(long)save.credits,710,1340,.68f,Ui.GOLD);batch.end();
        drawEffectCard(new Rectangle(180,1030,720,190),"fire",game.assets.t("fire"),save.fireUnlocked,600);
        drawEffectCard(new Rectangle(180,800,720,190),"ice",game.assets.t("ice"),save.iceUnlocked,900);
        drawEffectCard(new Rectangle(180,570,720,190),"lightning",game.assets.t("lightning"),save.lightningUnlocked,1500);
        drawOverlayButton(new Rectangle(310,455,460,96),game.assets.t("back"),true);
    }

    private void drawEffectCard(Rectangle r,String iconName,String title,boolean unlocked,long cost){
        sr.begin(ShapeRenderer.ShapeType.Filled);Ui.button(sr,r,!unlocked,false);sr.end();batch.begin();Texture ic=game.assets.icon(iconName);if(ic!=null)batch.draw(ic,r.x+30,r.y+35,120,120);Ui.text(batch,game.assets.font,title,r.x+180,r.y+135,.76f,unlocked?Ui.GREEN:Color.WHITE);Ui.text(batch,game.assets.font,unlocked?game.assets.t("max"):(cheatsEnabled()?"C 0":"C "+cost),r.x+180,r.y+66,.58f,unlocked?Ui.GREEN:Ui.GOLD);batch.end();
    }

    private void effectShopClick(float x,float y){
        if(new Rectangle(310,455,460,96).contains(x,y)){effectShopOpen=false;elementConfigOpen=true;return;}
        if(new Rectangle(180,1030,720,190).contains(x,y)&&!save.fireUnlocked)buyEffect("fire",600);
        else if(new Rectangle(180,800,720,190).contains(x,y)&&!save.iceUnlocked)buyEffect("ice",900);
        else if(new Rectangle(180,570,720,190).contains(x,y)&&!save.lightningUnlocked)buyEffect("lightning",1500);
    }

    private void buyEffect(String id,long cost){
        if(!cheatsEnabled()&&save.credits<cost){banner=game.assets.t("not_enough");bannerTime=1.2f;return;}if(!cheatsEnabled())save.credits-=cost;
        if("fire".equals(id))save.fireUnlocked=true;else if("ice".equals(id))save.iceUnlocked=true;else if("lightning".equals(id))save.lightningUnlocked=true;game.assets.play(game.assets.buy,game.settings,.24f);game.saves.save(save);
    }

    private void drawElementConfig(){
        drawOverlay();
        Rectangle panel=new Rectangle(85,220,910,1360);sr.begin(ShapeRenderer.ShapeType.Filled);Ui.panel(sr,panel,Ui.CYAN);sr.end();
        batch.begin();Ui.text(batch,game.assets.font,game.assets.t("effect_setup"),190,1500,1.10f,Color.WHITE);batch.end();

        drawConfigGroup(new Rectangle(135,1130,810,250),"tab_finger",game.assets.t("finger"),Ui.CYAN);
        drawConfigChoice(new Rectangle(175,1165,730,120),game.assets.t("attack_element"),elementName(save.fingerElement),elementIconName(save.fingerElement),elementColor(save.fingerElement));

        drawConfigGroup(new Rectangle(135,785,810,285),"tab_turrets",game.assets.t("turrets"),Ui.GOLD);
        drawConfigChoice(new Rectangle(175,820,345,125),game.assets.t("attack_element"),elementName(save.turretElement),elementIconName(save.turretElement),elementColor(save.turretElement));
        String wIcon=save.turretWeapon==1?"laser":save.turretWeapon==2?"rockets":"buyTurret";
        drawConfigChoice(new Rectangle(550,820,355,125),game.assets.t("weapon"),turretWeaponName(),wIcon,Ui.GOLD);

        drawConfigGroup(new Rectangle(135,440,810,285),"tab_drones",game.assets.t("drones"),ELEC);
        drawConfigChoice(new Rectangle(175,475,345,125),game.assets.t("attack_element"),elementName(save.droneElement),elementIconName(save.droneElement),elementColor(save.droneElement));
        drawConfigChoice(new Rectangle(550,475,355,125),game.assets.t("aura_element"),elementName(save.droneAuraElement),elementIconName(save.droneAuraElement),elementColor(save.droneAuraElement));

        drawOverlayButton(new Rectangle(150,285,360,96),game.assets.t("effect_shop"),true);
        drawOverlayButton(new Rectangle(570,285,360,96),game.assets.t("close"),true);
    }

    private void drawConfigGroup(Rectangle r,String iconName,String title,Color edge){
        sr.begin(ShapeRenderer.ShapeType.Filled);sr.setColor(.018f,.045f,.075f,.94f);sr.rect(r.x,r.y,r.width,r.height);sr.setColor(edge.r,edge.g,edge.b,.45f);sr.rect(r.x,r.y+r.height-3,r.width,3);sr.end();
        batch.begin();Texture ic=game.assets.icon(iconName);if(ic!=null)batch.draw(ic,r.x+24,r.y+r.height-86,62,62);Ui.text(batch,game.assets.font,title,r.x+105,r.y+r.height-38,.68f,Color.WHITE);batch.end();
    }

    private void drawConfigChoice(Rectangle r,String title,String value,String iconName,Color accent){
        sr.begin(ShapeRenderer.ShapeType.Filled);Ui.button(sr,r,true,false);sr.end();batch.begin();
        Texture ic=game.assets.icon(iconName);if(ic!=null)batch.draw(ic,r.x+18,r.y+26,72,72);
        Ui.text(batch,game.assets.font,title,r.x+104,r.y+89,.43f,new Color(.66f,.78f,.88f,1));
        Ui.text(batch,game.assets.font,value,r.x+104,r.y+43,.53f,accent);batch.end();
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
        if(new Rectangle(175,1165,730,120).contains(x,y)){save.fingerElement=save.fingerElement.nextCombat(true,save);game.saves.save(save);return;}
        if(new Rectangle(175,820,345,125).contains(x,y)){save.turretElement=save.turretElement.nextCombat(false,save);game.saves.save(save);return;}
        if(new Rectangle(550,820,355,125).contains(x,y)){cycleTurretWeapon();game.saves.save(save);return;}
        if(new Rectangle(175,475,345,125).contains(x,y)){save.droneElement=save.droneElement.nextCombat(false,save);game.saves.save(save);return;}
        if(new Rectangle(550,475,355,125).contains(x,y)){save.droneAuraElement=save.droneAuraElement.nextCombat(false,save);game.saves.save(save);}
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
        return switch(e.id){
            case "gDamage","tapDmg","turretDmg","droneDmg" -> game.assets.t("damage");
            case "gRate","tapRate","turretRate","droneRate" -> game.assets.t("rate");
            case "yield" -> game.assets.t("reward");
            case "density" -> game.assets.t("density");
            case "spawn" -> game.assets.t("spawn_rate");
            case "value" -> game.assets.t("enemy_value");
            case "enemySpeed" -> game.assets.t("enemy_speed");
            case "enemyDamage" -> game.assets.t("enemy_damage");
            case "enemyHealth" -> game.assets.t("enemy_health");
            case "plasma" -> game.assets.t("plasma"); case "trail" -> game.assets.t("trail"); case "ultimate" -> game.assets.t("ultimate"); case "gravity" -> game.assets.t("gravity");
            case "buyTurret" -> game.assets.t("turrets"); case "turretShield","droneShield" -> game.assets.t("shield"); case "repairTurrets" -> game.assets.t("repair"); case "autoRepair" -> game.assets.t("auto_repair");
            case "laser" -> game.assets.t("pulse_laser"); case "rockets" -> game.assets.t("rockets"); case "cycleWeapon" -> turretWeaponName(); case "turretPlusTwo" -> "+2 "+game.assets.t("turrets");
            case "gunDrone" -> game.assets.t("gun_drone"); case "missileDrone" -> game.assets.t("missile_drone"); case "kamikaze" -> game.assets.t("kamikaze_drone"); case "support" -> game.assets.t("support_drone"); case "droneAura" -> game.assets.t("drone_aura"); case "dronePlusTwo" -> "+2 "+game.assets.t("drones");
            case "fire" -> game.assets.t("fire"); case "ice" -> game.assets.t("ice"); case "lightning" -> game.assets.t("lightning"); case "fingerElem" -> game.assets.t("finger"); case "turretElem" -> game.assets.t("turrets"); case "droneElem" -> game.assets.t("drones");
            default -> e.label;
        };
    }

    private String shopLevelText(ShopEntry e){
        if(e.id.equals("buyTurret"))return save.turretCount+" / "+save.turretCap();
        if(e.id.equals("gunDrone")||e.id.equals("missileDrone")||e.id.equals("kamikaze")||e.id.equals("support"))return save.droneCount()+" / "+save.droneCap();
        if(e.id.equals("fingerElem"))return elementName(save.fingerElement);if(e.id.equals("turretElem"))return elementName(save.turretElement);if(e.id.equals("droneElem"))return elementName(save.droneElement);
        String l=e.label;int ix=l.indexOf("Lv.");if(ix>=0)return l.substring(ix);
        return e.cost==0?game.assets.t("max"):"";
    }

    private Array<ShopEntry> shopEntries(){Array<ShopEntry>a=new Array<>();
        if(shopTab==0){
            a.add(entry("tapDmg",game.assets.t("tap")+" "+game.assets.t("damage")+"  Lv."+save.tapDamageLevel,cost(70,1.52,save.tapDamageLevel),true));
            a.add(entry("tapRate",game.assets.t("tap")+" "+game.assets.t("rate")+"  Lv."+save.tapSpeedLevel,cost(90,1.55,save.tapSpeedLevel),true));
            a.add(entry("plasma",save.plasmaUnlocked?game.assets.t("plasma")+" — "+game.assets.t("max"):game.assets.t("unlock_plasma"),save.plasmaUnlocked?0:550,!save.plasmaUnlocked));
            a.add(entry("trail",save.trailUnlocked?game.assets.t("trail")+" — "+game.assets.t("max"):game.assets.t("unlock_trail"),save.trailUnlocked?0:850,!save.trailUnlocked));
            a.add(entry("ultimate",save.ultimateUnlocked?game.assets.t("ultimate")+" — "+game.assets.t("max"):game.assets.t("unlock_ultimate"),save.ultimateUnlocked?0:3800,!save.ultimateUnlocked));
            a.add(entry("gravity",save.gravityUnlocked?game.assets.t("gravity")+" — "+game.assets.t("max"):game.assets.t("unlock_gravity"),save.gravityUnlocked?0:15000,!save.gravityUnlocked&&(cheatsEnabled()||save.wave>=10)));
        } else if(shopTab==1){
            a.add(entry("buyTurret",game.assets.t("buy_turret")+"  "+save.turretCount+"/"+save.turretCap(),save.turretCount>=save.turretCap()?0:cost(300,1.90,Math.max(0,save.turretCount)),save.turretCount<save.turretCap()));
            a.add(entry("turretDmg",game.assets.t("damage")+"  Lv."+save.turretDamageLevel,cost(180,1.56,save.turretDamageLevel),true));
            a.add(entry("turretRate",game.assets.t("rate")+"  Lv."+save.turretRateLevel,cost(220,1.60,save.turretRateLevel),true));
            a.add(entry("turretShield",game.assets.t("shield")+"  Lv."+save.turretShieldLevel,cost(180,1.55,save.turretShieldLevel),true));
            a.add(entry("repairTurrets",game.assets.t("repair")+" — "+game.assets.t("turrets"),Math.max(80,save.wave*18L),save.turretCount>0));
            a.add(entry("autoRepair",save.autoRepairUnlocked?game.assets.t("auto_repair")+" — "+game.assets.t("max"):game.assets.t("auto_repair"),save.autoRepairUnlocked?0:5200,!save.autoRepairUnlocked&&(cheatsEnabled()||save.wave>=8)));
            a.add(entry("laser",save.turretLaserUnlocked?game.assets.t("pulse_laser")+" — "+game.assets.t("max"):game.assets.t("unlock_laser"),save.turretLaserUnlocked?0:1800,!save.turretLaserUnlocked));
            a.add(entry("rockets",save.turretRocketsUnlocked?game.assets.t("rockets")+" — "+game.assets.t("max"):game.assets.t("unlock_rockets"),save.turretRocketsUnlocked?0:3200,!save.turretRocketsUnlocked));
            a.add(entry("turretPlusTwo",save.turretPlusTwo?game.assets.t("plus_two_turrets")+" — "+game.assets.t("max"):game.assets.t("plus_two_turrets"),save.turretPlusTwo?0:30000,!save.turretPlusTwo&&(cheatsEnabled()||save.wave>=15)));
        } else if(shopTab==2){
            boolean room=save.droneCount()<save.droneCap();long base=cost(700,1.48,save.droneCount());
            a.add(entry("gunDrone",game.assets.t("buy_gun_drone")+"  "+save.droneCount()+"/"+save.droneCap(),base,room));
            a.add(entry("missileDrone",game.assets.t("buy_missile_drone"),Math.max(1350,base+500),room));
            a.add(entry("kamikaze",save.kamikazeUnlocked?game.assets.t("spawn_kamikaze"):game.assets.t("unlock_kamikaze"),save.kamikazeUnlocked?Math.max(220,cost(220,1.16,Math.max(0,save.kamikazeDrones))):Math.max(1800,base+850),room));
            a.add(entry("support",game.assets.t("buy_support"),Math.max(2400,base+1400),room));
            a.add(entry("droneDmg",game.assets.t("damage")+"  Lv."+save.droneDamageLevel,cost(220,1.56,save.droneDamageLevel),true));
            a.add(entry("droneRate",game.assets.t("rate")+"  Lv."+save.droneRateLevel,cost(260,1.60,save.droneRateLevel),true));
            a.add(entry("droneAura",game.assets.t("drone_aura")+"  Lv."+save.droneAuraLevel,cost(300,1.62,save.droneAuraLevel),true));
            a.add(entry("droneShield",game.assets.t("shield")+"  Lv."+save.droneShieldLevel,cost(240,1.56,save.droneShieldLevel),true));
            a.add(entry("dronePlusTwo",save.dronePlusTwo?game.assets.t("plus_two_drones")+" — "+game.assets.t("max"):game.assets.t("plus_two_drones"),save.dronePlusTwo?0:32000,!save.dronePlusTwo&&(cheatsEnabled()||save.wave>=15)));
        }
        return a;}
    private ShopEntry entry(String id,String label,long cost,boolean enabled){return new ShopEntry(id,label,cost,enabled);}

    private void shopClick(float x,float y){
        if(new Rectangle(350,174,380,96).contains(x,y)){shopOpen=false;shopSellMode=false;game.saves.save(save);return;}
        if(new Rectangle(74,174,250,96).contains(x,y)){shopSellMode=!shopSellMode;return;}
        for(int i=0;i<3;i++)if(new Rectangle(90+i*300,1540,280,100).contains(x,y)){shopTab=i;return;}
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
            case "tapDmg"->cost(70,1.52,Math.max(0,save.tapDamageLevel-1));case "tapRate"->cost(90,1.55,Math.max(0,save.tapSpeedLevel-1));case "plasma"->550;case "trail"->850;case "ultimate"->3800;case "gravity"->15000;
            case "buyTurret"->cost(300,1.90,Math.max(0,save.turretCount-1));case "turretDmg"->cost(180,1.56,Math.max(0,save.turretDamageLevel-1));case "turretRate"->cost(220,1.60,Math.max(0,save.turretRateLevel-1));case "turretShield"->cost(180,1.55,Math.max(0,save.turretShieldLevel-1));case "autoRepair"->5200;case "laser"->1800;case "rockets"->3200;case "turretPlusTwo"->30000;
            case "gunDrone"->cost(700,1.48,Math.max(0,save.droneCount()-1));case "missileDrone"->Math.max(1350,cost(700,1.48,Math.max(0,save.droneCount()-1))+500);case "kamikaze"->Math.max(1800,cost(700,1.48,Math.max(0,save.droneCount()-1))+850);case "support"->Math.max(2400,cost(700,1.48,Math.max(0,save.droneCount()-1))+1400);case "droneDmg"->cost(220,1.56,Math.max(0,save.droneDamageLevel-1));case "droneRate"->cost(260,1.60,Math.max(0,save.droneRateLevel-1));case "droneAura"->cost(300,1.62,Math.max(0,save.droneAuraLevel-1));case "droneShield"->cost(240,1.56,Math.max(0,save.droneShieldLevel-1));case "dronePlusTwo"->32000;case "fire"->600;case "ice"->900;case "lightning"->1500;default->0;
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
        switch(e.id){
            case "gDamage"->save.generalDamageLevel++;case "gRate"->save.generalRateLevel++;case "yield"->save.creditYieldLevel++;case "density"->save.densityLevel++;case "spawn"->save.spawnRateLevel++;case "value"->save.enemyValueLevel++;case "enemySpeed"->{save.enemySpeedLevel++;for(Enemy en:enemies)en.speed*=1.08f;}case "enemyDamage"->save.enemyDamageLevel++;case "enemyHealth"->save.enemyHealthLevel++;
            case "tapDmg"->save.tapDamageLevel++;case "tapRate"->save.tapSpeedLevel++;case "plasma"->save.plasmaUnlocked=true;case "trail"->save.trailUnlocked=true;case "ultimate"->save.ultimateUnlocked=true;case "gravity"->save.gravityUnlocked=true;
            case "buyTurret"->{save.turretCount++;rebuildDefenses();}case "turretDmg"->save.turretDamageLevel++;case "turretRate"->save.turretRateLevel++;case "turretShield"->{save.turretShieldLevel++;rebuildDefenses();}case "repairTurrets"->{for(Turret t:turrets){t.shield=t.maxShield;t.broken=false;}}case "autoRepair"->save.autoRepairUnlocked=true;case "laser"->save.turretLaserUnlocked=true;case "rockets"->save.turretRocketsUnlocked=true;case "turretPlusTwo"->save.turretPlusTwo=true;
            case "gunDrone"->{save.gunDrones++;rebuildDefenses();}case "missileDrone"->{save.missileDrones++;rebuildDefenses();}case "kamikaze"->{if(!save.kamikazeUnlocked)save.kamikazeUnlocked=true;save.kamikazeDrones++;rebuildDefenses();}case "support"->{save.supportDrones++;rebuildDefenses();}case "droneDmg"->save.droneDamageLevel++;case "droneRate"->save.droneRateLevel++;case "droneAura"->save.droneAuraLevel++;case "droneShield"->{save.droneShieldLevel++;rebuildDefenses();}case "dronePlusTwo"->save.dronePlusTwo=true;
            case "fire"->save.fireUnlocked=true;case "ice"->save.iceUnlocked=true;case "lightning"->save.lightningUnlocked=true;case "fingerElem"->save.fingerElement=save.fingerElement.nextCombat(true,save);case "turretElem"->save.turretElement=save.turretElement.nextCombat(false,save);case "droneElem"->save.droneElement=save.droneElement.nextCombat(false,save);
        }
        game.assets.play(game.assets.buy,game.settings,.25f);vibrate(12);game.saves.save(save);
    }

    private void cycleTurretWeapon(){for(int k=0;k<3;k++){save.turretWeapon=(save.turretWeapon+1)%3;if(save.turretWeapon==0)return;if(save.turretWeapon==1&&save.turretLaserUnlocked)return;if(save.turretWeapon==2&&save.turretRocketsUnlocked)return;}}
    private String turretWeaponName(){return save.turretWeapon==1?game.assets.t("pulse_laser"):save.turretWeapon==2?game.assets.t("rockets"):game.assets.t("pulse_gun");}
    private boolean cheatsEnabled(){ return game.settings.cheatsEnabled; }
    private long cost(double base,double growth,int lvl){double v=base*Math.pow(growth,lvl);return (long)Math.min(v,9_000_000_000L);}

    private void gainSkill(String kind,float amount){
        float gain=Math.max(0.25f,amount*.035f);
        if("tap".equals(kind)){save.tapXp+=gain;while(save.tapXp>=skillNeed(save.tapSkillLevel)){save.tapXp-=skillNeed(save.tapSkillLevel);save.tapSkillLevel++;}}
        else if("plasma".equals(kind)){save.plasmaXp+=gain;while(save.plasmaXp>=skillNeed(save.plasmaSkillLevel)){save.plasmaXp-=skillNeed(save.plasmaSkillLevel);save.plasmaSkillLevel++;}}
        else if("trail".equals(kind)){save.trailXp+=gain;while(save.trailXp>=skillNeed(save.trailSkillLevel)){save.trailXp-=skillNeed(save.trailSkillLevel);save.trailSkillLevel++;}}
        else if("turret".equals(kind)){save.turretXp+=gain;while(save.turretXp>=skillNeed(save.turretSkillLevel)){save.turretXp-=skillNeed(save.turretSkillLevel);save.turretSkillLevel++;}}
        else if("drone".equals(kind)){save.droneXp+=gain;while(save.droneXp>=skillNeed(save.droneSkillLevel)){save.droneXp-=skillNeed(save.droneSkillLevel);save.droneSkillLevel++;}}
    }
    private float skillNeed(int level){return 40f+(float)Math.pow(level,1.32)*18f;}

    private Color colorFor(Enemy e){return switch(e.kind){case FAST->new Color(.18f,.95f,1f,1);case TANK->new Color(1f,.33f,.22f,1);case ELITE->new Color(.82f,.25f,1f,1);case BOSS->new Color(1f,.1f,.3f,1);default->new Color(.25f,.75f,1f,1);};}
    private Color elementColor(Element e){return switch(e){case FIRE->FIRE;case ICE->ICE;case LIGHTNING->ELEC;case GRAVITY->GRAV;default->Ui.CYAN;};}
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
