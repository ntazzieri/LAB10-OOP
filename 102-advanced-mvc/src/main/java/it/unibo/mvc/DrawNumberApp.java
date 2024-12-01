package it.unibo.mvc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

/**
 */
public final class DrawNumberApp implements DrawNumberViewObserver {
    private static final int DEF_MIN = 0;
    private static final int DEF_MAX = 100;
    private static final int DEF_ATTEMPTS = 10;
    private static final String HOME_PATH = System.getProperty("user.home");

    private static final int N_CONF_ARG = 3;
    private final DrawNumber model;
    private final List<DrawNumberView> views;

    /**
     * @param views
     *            the views to attach
     */
    public DrawNumberApp(final DrawNumberView... views) {
        /*
         * Side-effect proof
         */
        final Configuration conf = getConfigurationFromFile(ClassLoader.getSystemResourceAsStream("config.yml"));
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view: views) {
            view.setObserver(this);
            view.start();
        }
        this.model = new DrawNumberImpl(conf);
    }

    @Override
    public void newAttempt(final int n) {
        try {
            final DrawResult result = model.attempt(n);
            for (final DrawNumberView view: views) {
                view.result(result);
            }
        } catch (final IllegalArgumentException e) {
            for (final DrawNumberView view: views) {
                view.numberIncorrect();
            }
        }
    }

    @Override
    public void resetGame() {
        this.model.reset();
    }

    @Override
    public void quit() {
        /*
         * A bit harsh. A good application should configure the graphics to exit by
         * natural termination when closing is hit. To do things more cleanly, attention
         * should be paid to alive threads, as the application would continue to persist
         * until the last thread terminates.
         */
        System.exit(0);
    }

    private Configuration getConfigurationFromFile(final InputStream stream) {
        int minimum = DEF_MIN;
        int maximum = DEF_MAX;
        int attempts = DEF_ATTEMPTS;
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(Objects.requireNonNull(stream), "UTF-8"));) {
            for (int i = 0; i < N_CONF_ARG; i++) {
                final String line = reader.readLine().replace(" ", "");
                final StringTokenizer st = new StringTokenizer(line, ":");
                st.nextToken();
                final Integer confData = Integer.parseInt(st.nextToken());
                if (i == 0) {
                    minimum = confData;
                } else if (i == 1) {
                    maximum = confData;
                } else if (i == 2) {
                    attempts = confData;
                }
            }
        } catch (final IOException ex) {
            views.forEach(v -> v.displayError("Cannot read configuration from file: " + ex.getMessage()));
        }
        return new Configuration.Builder()
                    .setMin(minimum)
                    .setMax(maximum)
                    .setAttempts(attempts)
                    .build();
    }

    /**
     * @param args
     *            ignored
     * @throws FileNotFoundException 
     */
    public static void main(final String... args) throws FileNotFoundException {
        new DrawNumberApp(new DrawNumberViewImpl(), new PrintStreamView(HOME_PATH + File.separator + "log.txt"));
    }
}
