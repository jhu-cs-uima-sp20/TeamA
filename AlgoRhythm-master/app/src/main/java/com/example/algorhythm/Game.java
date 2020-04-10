package com.example.algorhythm;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.Timer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;


public class Game extends AppCompatActivity {

    Intent launcher;

    private TextView et_what;
    private String name;
    private int time;
    private MediaPlayer mp;
    private ArrayDeque<Note> notesOffScreen;
    private ArrayDeque<Note> notesOnScreen;
    private ProgressBar rhythmMeter;
    private ProgressBar songProgress;
    static int newNote = 0;
    private int songListPosition;
    private int oldbo = 0;
    private int newbo = 0;
    private int maxbo = 0;
    private int score = 0;
    private float target = -1;

    class Note {
        public ObjectAnimator animation;
        public final int ID;
        public final char type;
        public final ImageView image;
        public final int timestamp;

        public Note(char type, int time) {
            this.ID = ++newNote;
            this.type = type;
            image = new ImageView(getApplicationContext());
            image.setId(ID);
            switch(type){

                case 'l':
                    image.setImageDrawable(getResources().getDrawable(R.drawable.noteleft));
                    break;
                case 'r':
                    image.setImageDrawable(getResources().getDrawable(R.drawable.noteright));
                    break;
                case 't':
                    image.setImageDrawable(getResources().getDrawable(R.drawable.notetap));
                    break;
                default:
                    image.setImageDrawable(getResources().getDrawable(R.drawable.notetap));
                    break;
            }
            timestamp = time;
        }

        public float getY() {
            return image.getTranslationY();
        }

        public void draw() {
            ConstraintLayout parentLayout = (ConstraintLayout)findViewById(R.id.ConstraintLayout);

            // set view id, else getId() returns -1

            ConstraintSet set = new ConstraintSet();

            parentLayout.addView(image, 0);
            set.clone(parentLayout);
            // connect start and end point of views, in this case top of child to top of parent.
            set.connect(image.getId(), ConstraintSet.TOP, parentLayout.getId(), ConstraintSet.TOP, 60);
            set.connect(image.getId(), ConstraintSet.LEFT, parentLayout.getId(), ConstraintSet.LEFT, 60);
            set.connect(image.getId(), ConstraintSet.RIGHT, parentLayout.getId(), ConstraintSet.RIGHT, 60);


            // ... similarly add other constraints
            set.applyTo(parentLayout);
            image.bringToFront();
            image.invalidate();

            animation = ObjectAnimator.ofFloat(image, "translationY", image.getTranslationY() + 1600);
            animation.setDuration(2000);
            animation.setInterpolator(new LinearInterpolator());
            animation.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    //make sure it's actually fallen all the way down the screen
                    //so that it doesn't just remove the next note

                    if (!notesOnScreen.isEmpty() && notesOnScreen.peek().ID == ID) {

                        System.out.println("Automatically deleting");
                        removeNextNote();
                    }

                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }


            });
            animation.start();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);

        et_what = (TextView) findViewById(R.id.songName);
        notesOffScreen = new ArrayDeque<>();
        notesOnScreen = new ArrayDeque<>();

        launcher = getIntent();
        songListPosition = launcher.getIntExtra("position", 0);
        //et_what.setText(launcher.getStringExtra("name"));

        String[] times = (launcher.getStringExtra("length")).split(":");
        time = Integer.parseInt(times[1]) * 1000;
        time += Integer.parseInt(times[0]) * 60 * 1000;

        try {
            final InputStream file = getAssets().open(launcher.getStringExtra("textFile"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(file));
            int noteCount = Integer.parseInt(reader.readLine());
            for(int i = 0; i < noteCount; i++) {
                String[] fields = reader.readLine().split(" ");
                float timestamp = Float.parseFloat(fields[0]);
                char noteType = fields[1].charAt(0);
                if(noteType == 'h') {
                    //do more stuff
                }
                notesOffScreen.add(new Note(noteType, (int) (timestamp * 1000)));
            }
        } catch(Exception e) {
            //shouldn't get here

        }

        name = launcher.getStringExtra("name");
        TextView sung = (TextView) findViewById(R.id.songName);
        sung.setText(name);
        name = name.toLowerCase();
        name = name.replace(" ", "_");
        //et_what.setText(Integer.toString(time));
        try {
            int resource = getResources().getIdentifier(name, "raw", getPackageName());

            playSong(0, time, resource /*, noteMap*/);
        } catch (Exception e) {
            //et_what.setText("Error");
        }

        ImageView goZone = findViewById(R.id.goZone);
        rhythmMeter = (ProgressBar) findViewById(R.id.rhythmmeter);
        rhythmMeter.setMax(100);
        rhythmMeter.setProgress(50);

        songProgress = (ProgressBar) findViewById(R.id.songProgress);
        //songProgress.setMax();
        //songProgress.setProgress(0);


        goZone.setOnTouchListener(new View.OnTouchListener() {
            final float SWIPE_THRESHHOLD = 250;
            private float x1;
            private float x2;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x1 = event.getX();
                        System.out.println("x1 = " + x1);
                        return true;
                    case MotionEvent.ACTION_UP:
                        x2 = event.getX();

                        Note nextNote;
                        if (!notesOnScreen.isEmpty()) {
                            nextNote = notesOnScreen.peek();
                        } else {
                            //no notes to deal with; break combo (false input)
                            newbo = 0;
                            return true;
                        }

                        try{
                            Character newt = nextNote.type;
                            if (Math.abs(x2 - x1) >= SWIPE_THRESHHOLD) {
                                if (x1 > x2) {
                                    System.out.println("LEFT");
                                    /*if (target == -1) {
                                        //ImageView goZone = (ImageView) findViewById(R.id.goZone);
                                        //target = goZone.getTop();
                                    }*/
                                    float y = nextNote.getY();

                                    System.out.println(y);
                                    System.out.println(target);
                                    if (newt == 'l' &&  y + 100 > target) {
                                        newbo++;
                                    } else {
                                        newbo = 0;
                                    }

                                    removeNextNote();


                                } else {
                                    System.out.println("RIGHT");

                                    if (target == -1) {
                                        ImageView goZone = (ImageView) findViewById(R.id.goZone);
                                        target = goZone.getTop();
                                    }
                                    float y = nextNote.getY();
                                    if (newt == 'r' && y + 100 > target) {
                                        newbo++;
                                    } else {
                                        newbo = 0;
                                    }

                                    removeNextNote();


                                }
                            } else {
                                System.out.println("TAP");

                                if (target == -1) {
                                    ImageView goZone = (ImageView) findViewById(R.id.goZone);
                                    target = goZone.getTop();
                                }
                                float y = nextNote.getY();
                                if (newt == 't' && y + 100 > target) {
                                    newbo++;
                                } else {
                                    newbo = 0;
                                }

                                removeNextNote();

                            }
                        } catch(Exception e) {
                            //
                        }


                        break;
                }

            return true;
            }
        });


    }




    private void playSong(int delay, int time, int song) {

        //delay functionality should probably be moved to setNoteTimers

        final int nestedsong = song;
        final int nestedtime = time;
        //TreeMap<Integer, Character> nutes = new TreeMap<Integer, Character>();
        /*for(Map.Entry<Integer, Character> entry : notes.entrySet()) {
            nutes.put(entry.getKey() + delay, entry.getValue());
        }*/
        mp = MediaPlayer.create(this, nestedsong);

        new Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        mp.start();

                        new Timer().schedule(
                                new java.util.TimerTask() {
                                    @Override
                                    public void run() {
                                        mp.stop();
                                        songEnd();
                                    }
                                }, nestedtime);
                    }
                }, delay);

        setNoteTimers();
    }

    private void setNoteTimers() {

        final Runnable noteMove = new Runnable() {
            public void run() {
                if (!notesOffScreen.isEmpty()) {
                    Note nextNote = notesOffScreen.poll();
                    nextNote.draw();
                    notesOnScreen.addLast(nextNote);
                }
            }
        };
        Timer timer = new Timer();


        for(Note n : notesOffScreen) {
            timer.schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            runOnUiThread(noteMove);
                        }
                    }, n.timestamp);
        }
    }

    @Override
    public void onBackPressed() {
        Log.d("Settings", "onBackPressed");
        mp.stop();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            Log.d("Settings", "onOptionsItemSelected");
            mp.stop();
            finish();
        }
        return true;
    }


    public void removeNextNote(){

        try {
            Note nextNote;
            if (!notesOnScreen.isEmpty()) {
                nextNote = notesOnScreen.poll();
            } else {
                return;
            }
            ImageView note = nextNote.image;
            //nextNote.animation.cancel();
            note.setVisibility(View.GONE);
            ConstraintLayout parentLayout = (ConstraintLayout) findViewById(R.id.ConstraintLayout);
            parentLayout.removeView(note);

            if(oldbo != newbo && newbo > 0) {
                oldbo = newbo;
                if(oldbo > maxbo) {
                    maxbo = oldbo;
                }
                score += newbo;
            } else {
                if(newbo > 0) {
                    score++;
                }
                oldbo = 0;
                newbo = 0;
            }
            et_what.setText(Integer.toString(newbo) + " - " + Integer.toString(maxbo) + " - " + Integer.toString(score));
        } catch (Exception e) {
            //whatever
        }

    }

    public void songEnd(){
        Intent intent = new Intent(getApplicationContext(), SongSelect.class);
        SongItem s = SongSelect.jobItems.get(songListPosition);
//        if(currentHighScore > s.getHighScore() || currentCombo > s.getMaxCombo() || currentRank.compareTo(s.getRank()) < 0)
        SongSelect.jobItems.get(songListPosition).updateScore(Math.max(s.getHighScore(), score), Math.max(maxbo, s.getMaxCombo()), "?");
        startActivity(intent);
    }
}
