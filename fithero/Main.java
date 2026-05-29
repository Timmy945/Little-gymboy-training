package com.fithero;

import com.fithero.model.player.Avatar;
import com.fithero.model.player.Gender;
import com.fithero.logic.manager.GameManager;

public class Main {
    public static void main(String[] args) {
        Avatar tom = new Avatar("小明", 170.0, 65.0, Gender.MALE);
        GameManager game = new GameManager(tom);

        game.submitAerobicWorkout("尊巴舞蹈 (Zumba)", 60.0);
        game.printPlayerStatus();

        game.submitResistanceWorkout("槓鈴臥推", 20.0, 10, 3);
        game.printPlayerStatus();

        game.submitResistanceWorkout("槓鈴臥推", 90.0, 5, 4);
        game.printPlayerStatus();

        game.submitResistanceWorkout("啞鈴飛鳥", 20.0, 10, 4);
        game.printPlayerStatus();

        game.submitAerobicWorkout("慢跑 (輕鬆)", 120);
        game.printPlayerStatus();
    }
}