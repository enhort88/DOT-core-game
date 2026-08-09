package com.example.dotcore;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** Readable raster UI font. Only the generated PNG atlas is distributed. */
public class GlyphFont {
    private static final String CHARS = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~∞•—–ЁёАБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдежзийклмнопрстуфхцчшщъыьэюя";
    private static final int CELL_W=48, CELL_H=56, COLS=16;
    private final Texture texture = new Texture(Gdx.files.internal("ui-glyphs.png"));

    public GlyphFont() { texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear); }

    private float advance(char ch) {
        if (ch==' ') return 15f;
        if ("ilI1.,:;!'|`".indexOf(ch)>=0) return 14f;
        if ("fjrt()[]{}".indexOf(ch)>=0) return 21f;
        if ("mwMW@%&ЖШЩЮФжшщюф".indexOf(ch)>=0) return 37f;
        if (Character.isUpperCase(ch) || "0123456789".indexOf(ch)>=0) return 29f;
        return 25f;
    }

    public void draw(SpriteBatch batch,String text,float x,float y,float scale,Color color) {
        batch.setColor(color); float cx=x, cy=y-CELL_H*scale;
        for(int i=0;i<text.length();i++){
            char ch=text.charAt(i);
            if(ch=='\n'){cx=x;cy-=CELL_H*1.12f*scale;continue;}
            int idx=CHARS.indexOf(ch); if(idx<0) idx=CHARS.indexOf('?');
            int col=idx%COLS,row=idx/COLS; int sx=col*CELL_W,sy=row*CELL_H;
            batch.draw(texture,cx,cy,CELL_W*scale,CELL_H*scale,sx,sy,CELL_W,CELL_H,false,false);
            cx+=advance(ch)*scale;
        }
        batch.setColor(Color.WHITE);
    }

    public float width(String text,float scale) {
        float max=0,cur=0;
        for(int i=0;i<text.length();i++){
            char ch=text.charAt(i);
            if(ch=='\n'){max=Math.max(max,cur);cur=0;} else cur+=advance(ch)*scale;
        }
        return Math.max(max,cur);
    }

    public float height(String text,float scale) {
        int lines=1;for(int i=0;i<text.length();i++)if(text.charAt(i)=='\n')lines++;
        return lines*CELL_H*1.12f*scale;
    }
    public void dispose(){texture.dispose();}
}
