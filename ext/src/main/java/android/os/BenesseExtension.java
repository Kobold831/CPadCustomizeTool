package android.os;

import java.io.File;

public class BenesseExtension {
    public static final File COUNT_DCHA_COMPLETED_FILE = new File("/factory/count_dcha_completed");

    public static int getDchaState() {
        return -1;
    }

    public static boolean putInt(String name, int value) {
        return false;
    }

    public static void setDchaState(int status) {
    }

    public static boolean setForcedDisplaySize(int width, int height) {
        return false;
    }
}
