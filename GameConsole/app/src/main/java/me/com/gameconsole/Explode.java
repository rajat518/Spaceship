package me.com.gameconsole;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;



public class Explode {
    private int x;
    private int y;
    private  int width;
    private int height;
    private int row;
    private Animation animation = new Animation();
    private Bitmap spritesheet;

    public Explode(Bitmap res, int x, int y, int w,int h,int numofFrames){
        this.x=x;
        this.y=y;
        width=w;
        height=h;
        Bitmap[] image = new Bitmap[numofFrames];

        spritesheet = res;

        for(int i=0;i<image.length;i++){

            if(i%5==0&& i>0)row++;
            image[i]= Bitmap.createBitmap(spritesheet,(i-(5*row))*width,row*height,width,height);
        }
        animation.setFrames(image);
        animation.setDelay(10);
    }
    public void update(){

        if(!animation.playedOnce()){
            animation.update();
        }
    }
    public void draw(Canvas canvas){

        if(!animation.playedOnce()){
            canvas.drawBitmap(animation.getImage(),x,y,null);
        }
    }
    public int getHeight(){return height;}
}
