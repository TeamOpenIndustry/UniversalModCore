package cam72cam.mod.util;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sampler {
    private final int interval;
    private final int dump;
    private final Map<String, Long> counts;
    private int counter;
    private long lastDump;

    public Sampler(int interval, int dump) {
        this.interval = interval;
        this.dump = dump * 1000;
        counts = new HashMap<>();
        lastDump = System.currentTimeMillis();
    }

    public synchronized void sample() {
        counter ++;
        if (counter % this.interval == 0) {
            try {
                throw new Exception();
            } catch (Exception e) {
                String msg = ExceptionUtils.getStackTrace(e);
                if (!counts.containsKey(msg)) {
                    counts.put(msg, 0L);
                }
                counts.put(msg, counts.get(msg) + 1);
            }
            counter = 0;
        }
        if (lastDump + dump < System.currentTimeMillis()) {
            List<Map.Entry<String, Long>> entries = new ArrayList<>(counts.entrySet());
            entries.sort(Map.Entry.comparingByValue());
            for (int i = 0; i < Math.min(3, entries.size()); i++) {
                System.out.println(String.format("%s: %s", entries.get(entries.size() - 1 - i).getValue() * this.interval, entries.get(entries.size() - 1 - i).getKey()));
            }
            counts.clear();
            lastDump = System.currentTimeMillis();
        }

    }
}
