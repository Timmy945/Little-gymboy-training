package fithero.model.player;

/**
 * 身型生物特徵數據模型
 */
public class UserProfile {
    private double height;
    private double weight;
    private Gender gender; 

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
    public void setGender(Gender gender) { this.gender = gender; }
}