package com.example.infinitimeapp.services;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.UUID;

public class CurrentTimeService extends BaseService {
    private static final String CURRENT_TIME = "CURRENT_TIME";

    private static CurrentTimeService sInstance = null;

    private CurrentTimeService() {
        CHAR_MAP.put(CURRENT_TIME, "00002a2b-0000-1000-8000-00805f9b34fb");
    }

    public static CurrentTimeService getInstance() {
        if (sInstance == null)
            sInstance = new CurrentTimeService();

        return sInstance;
    }

    @Override
    public void onDataRecieved(UUID characteristicName, byte[] message) {
        switch(getCharacteristicName(characteristicName)) {
            case CURRENT_TIME:
                break;
            default:
        }
    }

    byte[] getCTSAsBytes() {
        Calendar time = Calendar.getInstance();

        int dayOfWeek = time.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SUNDAY) {
            dayOfWeek = 7;
        } else {
            dayOfWeek = dayOfWeek - 1;
        }

        return ByteBuffer.allocate(10)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort((short) time.get(Calendar.YEAR))
                .put((byte) (time.get(Calendar.MONTH) + 1))
                .put((byte) time.get(Calendar.DAY_OF_MONTH))
                .put((byte) time.get(Calendar.HOUR_OF_DAY))
                .put((byte) time.get(Calendar.MINUTE))
                .put((byte) time.get(Calendar.SECOND))
                .put((byte) dayOfWeek)
                .put((byte) (int)((time).get(Calendar.MILLISECOND) * 0.255F))
                .put((byte) 0)
                .array();
    }

    public void updateTime() {
        write(getCharacteristicUUID(CURRENT_TIME), getCTSAsBytes());
    }
}
