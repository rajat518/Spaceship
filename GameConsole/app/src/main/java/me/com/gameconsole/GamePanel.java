package me.com.gameconsole;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.test.suitebuilder.annotation.Smoke;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Random;



public class GamePanel extends SurfaceView implements SurfaceHolder.Callback{

    public static final int WIDTH = 856;
    public static final int HEIGHT = 480;
    public static final int MOVESPEED = -5;
    private long smokeStartTime;
    private long missileStartTime;
    final MediaPlayer mp;
    private  int best;
    private MainThread thread;
    private boolean startGameCreated;
    private Background bg;
    private Player player;
    private ArrayList<Smokepuff> smoke;
    private ArrayList<Missile> missiles;
    private ArrayList<TopBorder> topBorders;
    private ArrayList<BottomBorder> bottomBorders;
    private int maxBorderHeight;
    private int minBorderHeight;
    private int progressVal =20;
    private boolean topDown = true;
    private boolean bottomDown = true;
    private Random rand = new Random();
    private Explode explode;
    private long startReset;
    private boolean reset;
    private boolean started;
    private boolean disappear;
    public GamePanel(Context context)
    {
        super(context);

        getHolder().addCallback(this);

        thread = new MainThread(getHolder(), this);
        mp = MediaPlayer.create(context,R.raw.audio);
        mp.setLooping(true);
        mp.start();
        setFocusable(true);

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        boolean retry= true;
        int counter = 0;
        while (retry && counter < 1000)
        {
            counter++;
            try {
                thread.setRunning(false);
                thread.join();
                retry = false;

            }catch (InterruptedException e){e.printStackTrace();}

        }
        mp.stop();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        bg = new Background(BitmapFactory.decodeResource(getResources(),R.drawable.grassbg1));
        player = new Player(BitmapFactory.decodeResource(getResources(),R.drawable.helicopter),65,25,3);
        smoke = new ArrayList<Smokepuff>();
        missiles = new ArrayList<Missile>();
        topBorders = new ArrayList<TopBorder>();
        bottomBorders = new ArrayList<BottomBorder>();


        smokeStartTime= System.nanoTime();
        missileStartTime = System.nanoTime();
        thread.setRunning(true);
        thread.start();
    }
    @Override
    public boolean onTouchEvent(MotionEvent event){

        if(event.getAction()==MotionEvent.ACTION_DOWN)
        {
            if(!player.getPlaying()){
                player.setPlaying(true);
                player.setUp(true);
            }
            else
            {

                player.setUp(true);
            }
            return true;
        }
        if(event.getAction() == MotionEvent.ACTION_UP){

            player.setUp(false);
            return  true;
        }

        return super.onTouchEvent(event);
    }
    public void update()
    {
        if(player.getPlaying()) {

            bg.update();
            player.update();

            maxBorderHeight = 30 + player.getScore()/progressVal;
            if(maxBorderHeight>HEIGHT/4){
                maxBorderHeight = HEIGHT/4;
            }
            minBorderHeight = 5+player.getScore()/progressVal;
            for(int i =0 ; i<bottomBorders.size();i++){
                if(collision(bottomBorders.get(i),player))player.setPlaying(false);
            }
            for(int i =0 ; i<topBorders.size();i++){
                if(collision(topBorders.get(i),player))player.setPlaying(false);
            }
            this.updateTopBorder();
            this.updateBottomBorder();
            long missileElapsed = (System.nanoTime()-missileStartTime)/1000000;
            if(missileElapsed>(2000-player.getScore()/4)){
                if(missiles.size()==0){
                    missiles.add(new Missile(BitmapFactory.decodeResource(getResources(),R.drawable.missile),
                            WIDTH+10,HEIGHT/2,45,15,player.getScore(),13));
                }
                else
                {
                    missiles.add(new Missile(BitmapFactory.decodeResource(getResources(),R.drawable.missile),
                            WIDTH+10,(int)(rand.nextDouble()*(HEIGHT-(maxBorderHeight*2))+maxBorderHeight),45,15,player.getScore(),13));
                }
                missileStartTime = System.nanoTime();
            }

            for(int i =0; i<missiles.size();i++){
                missiles.get(i).update();
                if(collision(missiles.get(i),player)){
                    missiles.remove(i);
                    player.setPlaying(false);
                    break;
                }
                if(missiles.get(i).getX()<-100){
                    missiles.remove(i);
                    break;
                }
            }
            long elapsed = (System.nanoTime()-smokeStartTime)/1000000;
            if(elapsed > 120){
                smoke.add(new Smokepuff(player.getX(),player.getY()+10));
                smokeStartTime = System.nanoTime();
            }
            for(int i=0;i<smoke.size();i++){
                smoke.get(i).update();
                if(smoke.get(i).getX()<-10){
                    smoke.remove(i);
                }
            }

        }
        else
        {

            startGameCreated = false;
            if(!startGameCreated)
            startGame();
        }

    }

    public boolean collision(GameObject a, GameObject b){
        if(Rect.intersects(a.getRectangle(),b.getRectangle())){
            return true;
        }
        return false;
    }
    @Override
    public void draw(Canvas canvas){
        final float scaleFactorX = getWidth()/(WIDTH*1.f);
        final float scaleFactorY = getHeight()/(HEIGHT*1.f);

        if(canvas!=null) {
            final int savedState = canvas.save();
            canvas.scale(scaleFactorX, scaleFactorY);
            bg.draw(canvas);
            player.draw(canvas);

            for(Smokepuff sp:smoke){
                sp.draw(canvas);
            }
            for(BottomBorder b:bottomBorders){
                b.draw(canvas);
            }
            for(Missile m: missiles){
                m.draw(canvas);
            }
            for(TopBorder t:topBorders){
                t.draw(canvas);
            }
            if (started){
                explode.draw(canvas);
            }
            drawText(canvas);
            canvas.restoreToCount(savedState);
        }
    }

    public void updateTopBorder(){
        if(player.getScore()%50==0){
            topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),
                    topBorders.get(topBorders.size()-1).getX()+20,0,(int)(rand.nextDouble()*maxBorderHeight)+1));
        }

        for(int i=0; i<topBorders.size();i++){
            topBorders.get(i).update();
            if(topBorders.get(i).getX()<-20){
                topBorders.remove(i);
                if(topBorders.get(topBorders.size()-1).getHeight()>=maxBorderHeight){
                    topDown=false;
                }
                if(topBorders.get(topBorders.size()-1).getHeight()<=minBorderHeight)
                {
                    topDown=true;
                }
                if(topDown){

                    topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),topBorders.get(topBorders.size()-1).getX()+20
                    ,0,topBorders.get(topBorders.size()-1).getHeight()+1));
                }
                else{
                    topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),topBorders.get(topBorders.size()-1).getX()+20
                            ,0,topBorders.get(topBorders.size()-1).getHeight()-1));
                }
            }
        }
    }
    public  void  updateBottomBorder(){

        if(player.getScore()%40==0){
            bottomBorders.add(new BottomBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),
                    bottomBorders.get(bottomBorders.size()-1).getX()+20,(int)(rand.nextDouble()*maxBorderHeight+(HEIGHT-maxBorderHeight))));
        }

        for(int i=0;i<bottomBorders.size();i++){
            bottomBorders.get(i).update();
            if(bottomBorders.get(i).getX()<-20){
                bottomBorders.remove(i);
                if(bottomBorders.get(bottomBorders.size()-1).getY()<=HEIGHT-maxBorderHeight){
                    bottomDown=true;
                }
                if(bottomBorders.get(bottomBorders.size()-1).getY()>=HEIGHT-minBorderHeight)
                {
                    bottomDown=false;
                }
                if(bottomDown){

                    bottomBorders.add(new BottomBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),
                            bottomBorders.get(bottomBorders.size()-1).getX()+20,bottomBorders.get(bottomBorders.size()-1).getY()+1));
                }else {
                    bottomBorders.add(new BottomBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),
                            bottomBorders.get(bottomBorders.size()-1).getX()+20,bottomBorders.get(bottomBorders.size()-1).getY()-1));
                }
            }
        }

    }

    public void startGame(){
        //disappear=false;
        bottomBorders.clear();
        topBorders.clear();
        missiles.clear();
        smoke.clear();
        player.resetDYA();
        minBorderHeight=5;
        maxBorderHeight=30;
        player.resetScore();
        player.setY(HEIGHT/2);
        if(player.getScore()>best)
        {
            best = player.getScore();
        }
        for(int i =0 ;i*20<WIDTH+40;i++){
            if(i==0){
                topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),i*20,0,10));
            }
            else {
                topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),i*20,0,
                        topBorders.get(i-1).getHeight()+1));

            }
        }

        for(int i =0;i*20<WIDTH+40;i++)
        {
            if(i==0)
            {
                bottomBorders.add(new BottomBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),i*20,HEIGHT-minBorderHeight
                        ));
            }
            else {
                bottomBorders.add(new BottomBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick),
                        i*20,bottomBorders.get(i-1).getY()-1));
            }
        }
        startGameCreated = true;
    }

    public  void  drawText(Canvas canvas){
        Paint p = new Paint();
        p.setColor(Color.WHITE);
        p.setTextSize(30);
        p.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));
        canvas.drawText("Travelled: "+player.getScore()*3,10,HEIGHT-10,p);
        canvas.drawText("BEST: "+best,WIDTH-215,HEIGHT-10,p);
        if(!player.getPlaying()&&startGameCreated&&reset){

            Paint paint = new Paint();
            paint.setTextSize(40);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));
            canvas.drawText("Press to START",WIDTH/2-50,HEIGHT/2,paint);
            paint.setTextSize(20);
            canvas.drawText("PRESS TO GO UP SPACE", WIDTH/2-50,HEIGHT/4,paint);
            canvas.drawText("RELEASE TO GO UP SPACE", WIDTH/2-50,HEIGHT/4,paint);
        }


    }
}
