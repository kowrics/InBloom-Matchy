package com.example.inbloom;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private GridLayout gridLayout;
    private TextView tvMoves, tvPairs, tvTime, tvBestTime;
    private Button btnRestart;

    private final ArrayList<Integer> cardImages = new ArrayList<>();
    private final AppCompatButton[] cardButtons = new AppCompatButton[16];
    private final boolean[] matched = new boolean[16];

    private int firstIndex = -1;
    private int secondIndex = -1;
    private boolean isBusy = false;
    private int moves = 0;
    private int pairsFound = 0;

    private final Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private int seconds = 0;

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "bloom_match_prefs";
    private static final String KEY_BEST_TIME = "best_time";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridLayout = findViewById(R.id.gridLayout);
        tvMoves = findViewById(R.id.tvMoves);
        tvPairs = findViewById(R.id.tvPairs);
        tvTime = findViewById(R.id.tvTime);
        tvBestTime = findViewById(R.id.tvBestTime);
        btnRestart = findViewById(R.id.btnRestart);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        btnRestart.setOnClickListener(v -> setupGame());

        showBestTime();
        setupGame();
    }

    private void setupGame() {
        stopTimer();

        gridLayout.removeAllViews();
        cardImages.clear();

        moves = 0;
        pairsFound = 0;
        firstIndex = -1;
        secondIndex = -1;
        isBusy = false;
        seconds = 0;

        tvTime.setText("Time: 0s");
        updateStats();
        showBestTime();

        int[] flowers = {
                R.drawable.rose,
                R.drawable.sunflower,
                R.drawable.tulips,
                R.drawable.daisy,
                R.drawable.lily,
                R.drawable.orchid,
                R.drawable.hydrangea,
                R.drawable.gumamela
        };

        for (int flower : flowers) {
            cardImages.add(flower);
            cardImages.add(flower);
        }

        Collections.shuffle(cardImages);

        for (int i = 0; i < 16; i++) {
            matched[i] = false;

            AppCompatButton button = new AppCompatButton(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = dpToPx(76);
            params.height = dpToPx(76);
            params.setMargins(10, 10, 10, 10);
            params.setGravity(Gravity.CENTER);
            button.setLayoutParams(params);

            button.setBackgroundResource(R.drawable.flower_card_back);            button.setTag(i);
            button.setPadding(0, 0, 0, 0);
            button.setAllCaps(false);
            button.setOnClickListener(cardClickListener);

            cardButtons[i] = button;
            gridLayout.addView(button);
        }

        startTimer();
    }

    private final View.OnClickListener cardClickListener = v -> {
        if (isBusy) return;

        int index = (int) v.getTag();

        if (matched[index]) return;
        if (index == firstIndex) return;

        revealCard(index);

        if (firstIndex == -1) {
            firstIndex = index;
        } else {
            secondIndex = index;
            moves++;
            updateStats();
            checkMatch();
        }
    };

    private void revealCard(int index) {
        cardButtons[index].setBackgroundResource(cardImages.get(index));
    }

    private void hideCard(int index) {
        cardButtons[index].setBackgroundResource(R.drawable.flower_card_back);
    }

    private void checkMatch() {
        if (firstIndex == -1 || secondIndex == -1) return;

        if (cardImages.get(firstIndex).equals(cardImages.get(secondIndex))) {
            matched[firstIndex] = true;
            matched[secondIndex] = true;
            pairsFound++;
            updateStats();

            firstIndex = -1;
            secondIndex = -1;

            if (pairsFound == 8) {
                stopTimer();
                saveBestTimeIfNeeded();
                Toast.makeText(this,
                        "You matched all flowers in " + seconds + "s!",
                        Toast.LENGTH_LONG).show();
            }
        } else {
            isBusy = true;
            new Handler().postDelayed(() -> {
                hideCard(firstIndex);
                hideCard(secondIndex);
                firstIndex = -1;
                secondIndex = -1;
                isBusy = false;
            }, 700);
        }
    }

    private void updateStats() {
        tvMoves.setText("Moves: " + moves);
        tvPairs.setText("Pairs: " + pairsFound + "/8");
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                seconds++;
                tvTime.setText("Time: " + seconds + "s");
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void showBestTime() {
        int bestTime = prefs.getInt(KEY_BEST_TIME, -1);
        if (bestTime == -1) {
            tvBestTime.setText("Best: --");
        } else {
            tvBestTime.setText("Best: " + bestTime + "s");
        }
    }

    private void saveBestTimeIfNeeded() {
        int bestTime = prefs.getInt(KEY_BEST_TIME, -1);
        if (bestTime == -1 || seconds < bestTime) {
            prefs.edit().putInt(KEY_BEST_TIME, seconds).apply();
            showBestTime();
            Toast.makeText(this,
                    "New best time: " + seconds + "s!",
                    Toast.LENGTH_LONG).show();
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
    }
}