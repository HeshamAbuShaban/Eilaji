package dev.anonymous.eilaji.storage.enums;

public enum SoundNumbers {
    SoundTalking(1),
    SoundLong(2),
    SoundNice(3),
    SoundBell(4),
    SoundNotify(5);

    public final int soundNumber;

    SoundNumbers(int soundNumber) {
        this.soundNumber = soundNumber;
    }
}
