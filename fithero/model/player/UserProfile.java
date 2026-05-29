package com.fithero.model.player;

public class UserProfile {
    private double height;
    private double weight;
    private Gender gender; // 新增性別欄位

    public UserProfile(double height, double weight, Gender gender) {
        this.height = height;
        this.weight = weight;
        this.gender = gender;
    }

    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
    public Gender getGender() { return gender; }
}