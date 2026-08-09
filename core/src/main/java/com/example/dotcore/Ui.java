package com.example.dotcore;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public final class Ui {
    public static final Color BG = new Color(0.01f,0.02f,0.05f,1f);
    public static final Color CYAN = new Color(0.05f,0.85f,1f,1f);
    public static final Color BLUE = new Color(0.15f,0.35f,1f,1f);
    public static final Color RED = new Color(1f,0.18f,0.28f,1f);
    public static final Color GREEN = new Color(0.15f,1f,0.55f,1f);
    public static final Color GOLD = new Color(1f,0.72f,0.12f,1f);
    public static final Color PANEL = new Color(0.03f,0.06f,0.12f,0.94f);
    public static final Color PANEL2 = new Color(0.05f,0.10f,0.18f,0.96f);
    private Ui() {}

    public static void panel(ShapeRenderer sr, Rectangle r, Color edge) {
        sr.setColor(PANEL); sr.rect(r.x,r.y,r.width,r.height); sr.setColor(edge);
        sr.rect(r.x,r.y,r.width,2); sr.rect(r.x,r.y+r.height-2,r.width,2); sr.rect(r.x,r.y,2,r.height); sr.rect(r.x+r.width-2,r.y,2,r.height);
    }
    public static void button(ShapeRenderer sr, Rectangle r, boolean enabled, boolean hot) {
        sr.setColor(enabled ? (hot ? new Color(0.08f,0.35f,0.48f,0.96f) : PANEL2) : new Color(0.04f,0.04f,0.06f,0.85f)); sr.rect(r.x,r.y,r.width,r.height);
        sr.setColor(enabled ? (hot ? CYAN : new Color(CYAN.r,CYAN.g,CYAN.b,0.55f)) : new Color(0.25f,0.28f,0.32f,0.5f));
        sr.rect(r.x,r.y,r.width,2); sr.rect(r.x,r.y+r.height-2,r.width,2); sr.rect(r.x,r.y,2,r.height); sr.rect(r.x+r.width-2,r.y,2,r.height);
    }
    public static void text(SpriteBatch batch, GlyphFont font, String text, float x, float y, float scale, Color color) { font.draw(batch,text,x,y,scale*1.18f,color); }
    public static void centered(SpriteBatch batch, GlyphFont font, String text, Rectangle r, float scale, Color color) {
        float tw=font.width(text,scale), th=font.height(text,scale); font.draw(batch,text,r.x+(r.width-tw)/2f,r.y+(r.height+th*.72f)/2f,scale,color);
    }
}
